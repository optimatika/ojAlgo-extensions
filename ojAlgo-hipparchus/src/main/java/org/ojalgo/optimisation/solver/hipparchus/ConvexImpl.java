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
import java.util.List;

import org.hipparchus.exception.Localizable;
import org.hipparchus.optim.LocalizedOptimFormats;
import org.hipparchus.optim.nonlinear.scalar.GoalType;
import org.hipparchus.optim.nonlinear.scalar.ObjectiveFunction;
import org.hipparchus.optim.nonlinear.vector.constrained.ConstraintOptimizer;
import org.hipparchus.optim.nonlinear.vector.constrained.LagrangeSolution;
import org.hipparchus.optim.nonlinear.vector.constrained.LinearBoundedConstraint;
import org.hipparchus.optim.nonlinear.vector.constrained.LinearEqualityConstraint;
import org.hipparchus.optim.nonlinear.vector.constrained.LinearInequalityConstraint;
import org.hipparchus.optim.nonlinear.vector.constrained.QuadraticFunction;
import org.ojalgo.array.ArrayR064;
import org.ojalgo.optimisation.ConstraintsMetaData;
import org.ojalgo.optimisation.Expression;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.Optimisation;
import org.ojalgo.structure.Access1D;
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

        @Override
        public final boolean isCapable(final ExpressionsBasedModel model) {
            return !model.isAnyVariableInteger() && model.isAnyObjectiveQuadratic() && !model.isAnyConstraintQuadratic();
        }

        /**
         * Whether the optimiser accepts two-sided {@link LinearBoundedConstraint}s. If not, the inequalities
         * are passed as {@link LinearInequalityConstraint}s, one per limit.
         */
        abstract boolean isBoundedConstraintSupported();

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

            int nbEql = equalities.size();
            if (nbEql > 0) {
                double[][] a = new double[nbEql][];
                double[] b = new double[nbEql];
                for (int i = 0; i < nbEql; i++) {
                    ConstraintTemplate constraintTemplate = equalities.get(i);
                    a[i] = constraintTemplate.body;
                    b[i] = constraintTemplate.lower;
                    rows.add(constraintTemplate);
                    types.add(ConstraintType.EQUALITY);
                    signs.add(NEG);
                }
                translation.data.add(new LinearEqualityConstraint(a, b));
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
                List<Double> b = new ArrayList<>();
                for (ConstraintTemplate constraintTemplate : ranges) {
                    if (constraintTemplate.isLower()) {
                        a.add(constraintTemplate.body);
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
                        b.add(-constraintTemplate.upper);
                        rows.add(constraintTemplate);
                        types.add(ConstraintType.UPPER);
                        signs.add(ONE);
                    }
                }
                if (a.size() > 0) {
                    translation.data
                            .add(new LinearInequalityConstraint(a.toArray(new double[a.size()][]), b.stream().mapToDouble(Double::doubleValue).toArray()));
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
