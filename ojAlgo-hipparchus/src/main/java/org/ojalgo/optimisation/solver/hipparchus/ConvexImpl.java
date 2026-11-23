/*
 * Copyright 1997-2026 Optimatika
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.ojalgo.optimisation.solver.hipparchus;

import static org.ojalgo.function.constant.PrimitiveMath.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import org.hipparchus.exception.Localizable;
import org.hipparchus.linear.Array2DRowRealMatrix;
import org.hipparchus.linear.ArrayRealVector;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.linear.RealVector;
import org.hipparchus.optim.LocalizedOptimFormats;
import org.hipparchus.optim.nonlinear.scalar.GoalType;
import org.hipparchus.optim.nonlinear.scalar.ObjectiveFunction;
import org.hipparchus.optim.nonlinear.vector.constrained.ConstraintOptimizer;
import org.hipparchus.optim.nonlinear.vector.constrained.EqualityConstraint;
import org.hipparchus.optim.nonlinear.vector.constrained.InequalityConstraint;
import org.hipparchus.optim.nonlinear.vector.constrained.LagrangeSolution;
import org.hipparchus.optim.nonlinear.vector.constrained.LinearBoundedConstraint;
import org.hipparchus.optim.nonlinear.vector.constrained.LinearEqualityConstraint;
import org.hipparchus.optim.nonlinear.vector.constrained.LinearInequalityConstraint;
import org.hipparchus.optim.nonlinear.vector.constrained.QuadraticFunction;
import org.ojalgo.array.ArrayR064;
import org.ojalgo.matrix.decomposition.Cholesky;
import org.ojalgo.matrix.decomposition.Eigenvalue;
import org.ojalgo.matrix.store.MatrixStore;
import org.ojalgo.matrix.store.R064Store;
import org.ojalgo.optimisation.ConstraintsMetaData;
import org.ojalgo.optimisation.Expression;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.Optimisation;
import org.ojalgo.structure.Access1D;
import org.ojalgo.structure.Access2D;
import org.ojalgo.structure.Primitive1D;

/**
 * The Hipparchus constrained optimisers ignore {@link GoalType} – a maximisation problem is minimised with
 * the objective negated.
 * <p>
 * Their multipliers are those of the Lagrangian L = f - λ'c, where c(x) = 0 for equality and c(x) >= 0 for
 * inequality constraints, except for the ADMM optimiser's two-sided (bounded) constraints, l <= Ax <= u,
 * whose multipliers are positive when the upper limit is active and negative when the lower is. They are all
 * converted to that latter convention, and then matched to the model entities.
 */
abstract class ConvexImpl<S extends ConstraintOptimizer> extends AbstractImpl<S, LagrangeSolution> {

    abstract static class Integration<S extends ConvexImpl<?>> extends AbstractImpl.Integration<S> {

        /**
         * Continuous models with a quadratic objective, or – if {@link #isQuadraticConstraintSupported()} –
         * with convex quadratic constraints.
         */
        @Override
        public final boolean isCapable(final ExpressionsBasedModel model) {
            if (model.isAnyVariableInteger()) {
                return false;
            } else if (model.isAnyConstraintQuadratic()) {
                return this.isQuadraticConstraintSupported() && model.constraints().allMatch(ConvexImpl::isConvex);
            } else {
                return model.isAnyObjectiveQuadratic();
            }
        }

        /**
         * Whether the optimiser accepts two-sided {@link LinearBoundedConstraint}s. If not, the inequalities
         * are passed as {@link LinearInequalityConstraint}s, one per limit.
         */
        abstract boolean isBoundedConstraintSupported();

        /**
         * Whether the optimiser accepts nonlinear constraints. The constraints are then passed as one
         * {@link EqualityConstraint} and one {@link InequalityConstraint}, each with the value and Jacobian of
         * all its rows, linear and quadratic. False by default.
         */
        boolean isQuadraticConstraintSupported() {
            return false;
        }

        @Override
        void translate(final Translation translation, final Expression objective, final int dim, final boolean maximise,
                final List<ConstraintTemplate> equalities, final List<ConstraintTemplate> ranges, final boolean negative) {

            double[][] hessian = AbstractImpl.Integration.getHessian(objective, dim);
            double[] linear = AbstractImpl.Integration.getLinearFactors(objective, dim);
            if (maximise) {
                for (int i = 0; i < dim; i++) {
                    for (int j = 0; j < dim; j++) {
                        hessian[i][j] = -hessian[i][j];
                    }
                    linear[i] = -linear[i];
                }
            }
            translation.data.add(new ObjectiveFunction(new QuadraticFunction(hessian, linear, ZERO)));
            translation.data.add(GoalType.MINIMIZE);
            translation.negated = maximise;

            // The rows in the order of the solver's multipliers: the templates, their type and the sign that
            // converts the multiplier.
            List<ConstraintTemplate> rows = new ArrayList<>();
            List<ConstraintType> types = new ArrayList<>();
            List<Double> signs = new ArrayList<>();

            boolean quadratic = equalities.stream().anyMatch(row -> row.quadratic != null) || ranges.stream().anyMatch(row -> row.quadratic != null);
            if (quadratic) {
                if (!this.isQuadraticConstraintSupported()) {
                    throw new IllegalArgumentException("This optimiser does not handle quadratic constraints!");
                }
                // Also when the integration is forced, and isCapable(..) not checked
                for (List<ConstraintTemplate> templates : List.of(equalities, ranges)) {
                    for (ConstraintTemplate row : templates) {
                        if (row.quadratic != null && !ConvexImpl.isConvex(row.quadratic, row.isLower(), row.isUpper())) {
                            throw new IllegalArgumentException("Only convex quadratic constraints are handled, and '" + row.entity.getName()
                                    + "' is not convex - an equality, two-sided, or with an indefinite quadratic form (or one of the wrong sign)!");
                        }
                    }
                }
            }

            int nbEql = equalities.size();
            if (nbEql > 0) {
                double[][] a = new double[nbEql][];
                QuadraticPart[] q = new QuadraticPart[nbEql];
                double[] b = new double[nbEql];
                for (int i = 0; i < nbEql; i++) {
                    ConstraintTemplate constraintTemplate = equalities.get(i);
                    a[i] = constraintTemplate.body;
                    q[i] = constraintTemplate.quadratic;
                    b[i] = constraintTemplate.lower;
                    rows.add(constraintTemplate);
                    types.add(ConstraintType.EQUALITY);
                    signs.add(NEG);
                }
                translation.data.add(quadratic ? new QuadraticEqualityConstraint(new QuadraticRows(a, q), b) : new LinearEqualityConstraint(a, b));
            }

            if (this.isBoundedConstraintSupported()) {

                int nbRng = ranges.size();
                if (nbRng > 0) {
                    double[][] a = new double[nbRng][];
                    double[] l = new double[nbRng];
                    double[] u = new double[nbRng];
                    for (int i = 0; i < nbRng; i++) {
                        ConstraintTemplate constraintTemplate = ranges.get(i);
                        a[i] = constraintTemplate.body;
                        l[i] = constraintTemplate.lower;
                        u[i] = constraintTemplate.upper;
                        rows.add(constraintTemplate);
                        if (constraintTemplate.isLower() && constraintTemplate.isUpper()) {
                            types.add(ConstraintType.RANGE);
                        } else if (constraintTemplate.isLower()) {
                            types.add(ConstraintType.LOWER);
                        } else {
                            types.add(ConstraintType.UPPER);
                        }
                        signs.add(ONE);
                    }
                    translation.data.add(new LinearBoundedConstraint(a, l, u));
                }

            } else {

                // a'x >= l as is, and a'x <= u as -a'x >= -u
                List<double[]> a = new ArrayList<>();
                List<QuadraticPart> q = new ArrayList<>();
                List<Double> b = new ArrayList<>();
                for (ConstraintTemplate constraintTemplate : ranges) {
                    if (constraintTemplate.isLower()) {
                        a.add(constraintTemplate.body);
                        q.add(constraintTemplate.quadratic);
                        b.add(constraintTemplate.lower);
                        rows.add(constraintTemplate);
                        types.add(ConstraintType.LOWER);
                        signs.add(NEG);
                    }
                    if (constraintTemplate.isUpper()) {
                        double[] body = new double[dim];
                        for (int j = 0; j < dim; j++) {
                            body[j] = -constraintTemplate.body[j];
                        }
                        a.add(body);
                        q.add(constraintTemplate.quadratic != null ? constraintTemplate.quadratic.negate() : null);
                        b.add(-constraintTemplate.upper);
                        rows.add(constraintTemplate);
                        types.add(ConstraintType.UPPER);
                        signs.add(ONE);
                    }
                }
                if (a.size() > 0) {
                    double[][] matrix = a.toArray(new double[a.size()][]);
                    double[] limits = b.stream().mapToDouble(Double::doubleValue).toArray();
                    if (quadratic) {
                        translation.data.add(new QuadraticInequalityConstraint(new QuadraticRows(matrix, q.toArray(new QuadraticPart[q.size()])), limits));
                    } else {
                        translation.data.add(new LinearInequalityConstraint(matrix, limits));
                    }
                }
            }

            int nbRows = rows.size();

            translation.constraints = ConstraintsMetaData.newEntityMap(nbRows);
            translation.constraints.setMultiplierScale(translation.objectiveScale);
            translation.constraints.setSignedRows(true);
            translation.dualSigns = new double[nbRows];
            translation.boundVariables = new int[nbRows];
            translation.boundCoefficients = new double[nbRows];

            for (int i = 0; i < nbRows; i++) {
                ConstraintTemplate row = rows.get(i);
                translation.constraints.setEntry(i, row.entity, types.get(i), false, row.adjusted);
                translation.dualSigns[i] = signs.get(i).doubleValue();
                translation.boundVariables[i] = row.variable;
                translation.boundCoefficients[i] = row.boundCoefficient;
            }
        }

    }

    /**
     * Equality constraints with (some) quadratic rows.
     */
    static final class QuadraticEqualityConstraint extends EqualityConstraint {

        private final QuadraticRows myRows;

        QuadraticEqualityConstraint(final QuadraticRows rows, final double[] value) {
            super(new ArrayRealVector(value));
            myRows = rows;
        }

        @Override
        public int dim() {
            return myRows.dim();
        }

        @Override
        public RealMatrix jacobian(final RealVector x) {
            return myRows.jacobian(x);
        }

        @Override
        public RealVector value(final RealVector x) {
            return myRows.value(x);
        }

    }

    /**
     * Inequality constraints, c(x) >= l, with (some) quadratic rows.
     */
    static final class QuadraticInequalityConstraint extends InequalityConstraint {

        private final QuadraticRows myRows;

        QuadraticInequalityConstraint(final QuadraticRows rows, final double[] lower) {
            super(new ArrayRealVector(lower));
            myRows = rows;
        }

        @Override
        public int dim() {
            return myRows.dim();
        }

        @Override
        public RealMatrix jacobian(final RealVector x) {
            return myRows.jacobian(x);
        }

        @Override
        public RealVector value(final RealVector x) {
            return myRows.value(x);
        }

    }

    /**
     * Constraint rows, a'x + Σ v x(r) x(c), and their Jacobian.
     */
    static final class QuadraticRows {

        private final double[][] myLinear;
        private final QuadraticPart[] myQuadratic;

        QuadraticRows(final double[][] linear, final QuadraticPart[] quadratic) {
            super();
            myLinear = linear;
            myQuadratic = quadratic;
        }

        int dim() {
            return myLinear[0].length;
        }

        RealMatrix jacobian(final RealVector x) {
            double[] point = x.toArray();
            double[][] retVal = new double[myLinear.length][];
            for (int i = 0; i < myLinear.length; i++) {
                retVal[i] = myLinear[i].clone();
                if (myQuadratic[i] != null) {
                    myQuadratic[i].addGradient(point, retVal[i]);
                }
            }
            return new Array2DRowRealMatrix(retVal, false);
        }

        RealVector value(final RealVector x) {
            double[] point = x.toArray();
            double[] retVal = new double[myLinear.length];
            for (int i = 0; i < myLinear.length; i++) {
                double[] row = myLinear[i];
                double sum = myQuadratic[i] != null ? myQuadratic[i].value(point) : ZERO;
                for (int j = 0; j < row.length; j++) {
                    sum += row[j] * point[j];
                }
                retVal[i] = sum;
            }
            return new ArrayRealVector(retVal, false);
        }

    }

    /**
     * A quadratic constraint is convex if it is one-sided, with a positive semidefinite quadratic form if the
     * upper limit is set, or a negative semidefinite one if the lower limit is. Only the rows/columns of the
     * variables involved are checked. A linear constraint is always convex.
     */
    static boolean isConvex(final Expression constraint) {
        return ConvexImpl.isConvex(QuadraticPart.of(constraint, false), constraint.isLowerLimitSet(), constraint.isUpperLimitSet());
    }

    /**
     * @param quadratic The quadratic part of the constraint, or null if it is linear
     * @param lower     Whether the lower limit is set
     * @param upper     Whether the upper limit is set
     */
    static boolean isConvex(final QuadraticPart quadratic, final boolean lower, final boolean upper) {

        if (quadratic == null) {
            return true;
        }

        if (lower == upper) {
            // An equality, or two-sided - the region between two quadratic limits is not convex
            return false;
        }

        int[] involved = IntStream.concat(Arrays.stream(quadratic.rows), Arrays.stream(quadratic.columns)).distinct().sorted().toArray();
        int dim = involved.length;

        double sign = upper ? ONE : NEG;
        double[][] form = new double[dim][dim];
        for (int k = 0; k < quadratic.factors.length; k++) {
            int i = Arrays.binarySearch(involved, quadratic.rows[k]);
            int j = Arrays.binarySearch(involved, quadratic.columns[k]);
            form[i][j] += sign * quadratic.factors[k];
            form[j][i] += sign * quadratic.factors[k];
        }

        return ConvexImpl.isPositiveSemidefinite(R064Store.FACTORY.copy(Access2D.wrap(form)));
    }

    /**
     * Cholesky is the quick test, but it fails for a semidefinite (singular) matrix as well as for an
     * indefinite one. Telling those apart needs the eigenvalues.
     */
    static boolean isPositiveSemidefinite(final R064Store matrix) {

        Cholesky<Double> cholesky = Cholesky.R064.make(matrix);
        if (cholesky.decompose(matrix) && cholesky.isSPD()) {
            return true;
        }

        Eigenvalue<Double> evd = Eigenvalue.R064.make(matrix, true);
        if (!evd.decompose(matrix)) {
            return false;
        }

        MatrixStore<Double> eigenvalues = evd.getD();
        int dim = matrix.getMinDim();

        double largest = ZERO;
        for (int i = 0; i < dim; i++) {
            largest = Math.max(largest, Math.abs(eigenvalues.doubleValue(i, i)));
        }
        double tolerance = dim * Math.max(largest, ONE) * MACHINE_EPSILON;

        for (int i = 0; i < dim; i++) {
            if (eigenvalues.doubleValue(i, i) < -tolerance) {
                return false;
            }
        }
        return true;
    }

    ConvexImpl(final Translation translation, final Options options, final Configurator<S> configurator) {
        super(translation, options, configurator);
    }

    /**
     * The dual values and the reduced gradient – in the bounded-variable sense, in model units and the
     * model's sense. With the multipliers y signed as for OSQP, the solution satisfies ∇f + A'y = 0, so the
     * objective's gradient plus the constraint rows' terms equals minus the variable-bound rows' terms.
     */
    @Override
    Optimisation.Result complete(final Optimisation.Result result, final LagrangeSolution solutionAndValue, final Translation translation) {

        double[] lambda = solutionAndValue.getLambda().toArray();

        int nbRows = translation.dualSigns.length;
        if (lambda.length != nbRows) {
            return result;
        }

        double[] signed = new double[nbRows];
        for (int i = 0; i < nbRows; i++) {
            signed[i] = translation.dualSigns[i] * lambda[i];
        }

        double scale = translation.negated ? -ONE / translation.objectiveScale : ONE / translation.objectiveScale;

        double[] reducedGradient = new double[result.size()];
        for (int i = 0; i < nbRows; i++) {
            int variable = translation.boundVariables[i];
            if (variable >= 0) {
                reducedGradient[variable] -= signed[i] * translation.boundCoefficients[i] * scale;
            }
        }

        return result.withDualValues(translation.constraints, () -> ArrayR064.wrap(signed)).withReducedGradient(() -> ArrayR064.wrap(reducedGradient));
    }

    @Override
    Access1D<?> extractSolution(final LagrangeSolution solutionAndValue) {
        return Primitive1D.of(solutionAndValue.getX().toArray());
    }

    @Override
    State extractState(final Localizable exceptionSpecifier) {

        if (LocalizedOptimFormats.NO_FEASIBLE_SOLUTION.equals(exceptionSpecifier)) {
            return Optimisation.State.INFEASIBLE;
        } else if (LocalizedOptimFormats.UNBOUNDED_SOLUTION.equals(exceptionSpecifier)) {
            return Optimisation.State.UNBOUNDED;
        } else {
            return Optimisation.State.FAILED;
        }
    }

    @Override
    double extractValue(final LagrangeSolution solutionAndValue) {
        return solutionAndValue.getValue();
    }
}
