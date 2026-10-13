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
package org.ojalgo.optimisation.solver.joptimizer;

import static org.ojalgo.function.constant.PrimitiveMath.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.WriterAppender;
import org.ojalgo.array.ArrayR064;
import org.ojalgo.matrix.MatrixR064;
import org.ojalgo.matrix.decomposition.Cholesky;
import org.ojalgo.matrix.decomposition.Eigenvalue;
import org.ojalgo.matrix.store.MatrixStore;
import org.ojalgo.matrix.store.PhysicalStore;
import org.ojalgo.matrix.store.R064Store;
import org.ojalgo.netio.BasicLogger;
import org.ojalgo.optimisation.Expression;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.Optimisation;
import org.ojalgo.optimisation.Variable;
import org.ojalgo.structure.Access1D;
import org.ojalgo.structure.Access2D;
import org.ojalgo.structure.Structure1D.IntIndex;
import org.ojalgo.structure.Structure2D.IntRowColumn;

import com.joptimizer.exception.InfeasibleProblemException;
import com.joptimizer.exception.JOptimizerException;
import com.joptimizer.functions.ConvexMultivariateRealFunction;
import com.joptimizer.functions.LinearMultivariateRealFunction;
import com.joptimizer.functions.PSDQuadraticMultivariateRealFunction;
import com.joptimizer.optimizers.JOptimizer;
import com.joptimizer.optimizers.LPOptimizationRequest;
import com.joptimizer.optimizers.LPPrimalDualMethod;
import com.joptimizer.optimizers.NewtonUnconstrained;
import com.joptimizer.optimizers.OptimizationRequest;
import com.joptimizer.optimizers.OptimizationRequestHandler;
import com.joptimizer.optimizers.OptimizationResponse;

import cern.colt.matrix.impl.DenseDoubleMatrix1D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;

public final class SolverJOptimizer implements Optimisation.Solver {

    public static class Configurator {

        /**
         * JOptimizer logs through log4j, and all of its loggers are below this one.
         */
        private static final Logger LOGGER = Logger.getLogger("com.joptimizer");

        private static BasicLogger ourAppender = null;
        private static WriterAppender ourWriterAppender = null;

        /**
         * JOptimizer's defaults (1E-5 and 1E-6) give solutions accurate to only about 5 digits. With these some
         * (harder) LPs fail, and are then retried with the defaults - see {@link #configureFallback}.
         */
        private static final double TOLERANCE = 1E-9;
        private static final double TOLERANCE_FEAS = 1E-8;

        /**
         * Log4j's loggers are global, so this can't be per solve. While the options supply an appender, it is
         * attached to JOptimizer's logger - once, not on every solve - replacing whichever was attached
         * before. Without one, the log4j configuration is left as it is.
         */
        private static synchronized void attach(final BasicLogger appender) {

            if (appender == ourAppender) {
                return;
            }

            if (ourWriterAppender != null) {
                LOGGER.removeAppender(ourWriterAppender);
                ourWriterAppender = null;
                LOGGER.setLevel(null);
            }

            ourAppender = appender;

            if (appender != null) {
                appender.asWriter().ifPresent(writer -> {
                    ourWriterAppender = new WriterAppender(new PatternLayout("%d{ISO8601} [%t] %-5p %c %x - %m%n"), writer);
                    LOGGER.addAppender(ourWriterAppender);
                    LOGGER.setLevel(Level.DEBUG);
                });
            }
        }

        protected void configure(final OptimizationRequest request, final OptimizationRequestHandler handler, final Optimisation.Options options) {

            Configurator.attach(options.logger_appender);

            request.setTolerance(TOLERANCE);
            request.setToleranceFeas(TOLERANCE_FEAS);

            if (request instanceof LPOptimizationRequest) {
                ((LPOptimizationRequest) request).setPresolvingDisabled(true);
            }

        }

        /**
         * Used for a second attempt, when an LP failed with what {@link #configure} set. Configures the same,
         * but with JOptimizer's default (looser) tolerances.
         */
        protected void configureFallback(final OptimizationRequest request, final OptimizationRequestHandler handler, final Optimisation.Options options) {

            this.configure(request, handler, options);

            request.setTolerance(JOptimizer.DEFAULT_TOLERANCE);
            request.setToleranceFeas(JOptimizer.DEFAULT_FEASIBILITY_TOLERANCE);
        }

        protected OptimizationRequestHandler newOptimizationRequestHandler(final OptimizationRequest request) {
            return request instanceof LPOptimizationRequest ? new LPPrimalDualMethod() : new NewtonUnconstrained(true);
        }

    }

    static final class Integration extends ExpressionsBasedModel.Integration<SolverJOptimizer> {

        /**
         * Variable bounds are native bounds for linear problems, so this must be false. (For quadratic
         * problems they are inequality constraints, with coefficient 1 or the variable's adjustment factor.)
         */
        private static final boolean ADJUSTED_BOUNDS = false;
        private static final boolean ADJUSTED_CONSTRAINTS = true;
        private static final boolean ADJUSTED_OBJECTIVE = true;

        Integration() {
            super();
        }

        @Override
        public SolverJOptimizer build(final ExpressionsBasedModel model) {

            Configurator configurator = model.getConfigurator(DEFAULT);

            boolean max = model.getOptimisationSense() == Optimisation.Sense.MAX;

            OptimizationRequest request;
            LPOptimizationRequest reqLP;

            if (model.isAnyExpressionQuadratic()) {
                request = new OptimizationRequest();
                reqLP = null;
            } else {
                reqLP = new LPOptimizationRequest();
                request = reqLP;
            }

            List<Variable> variables = model.getFreeVariables();
            int nbVars = variables.size();

            Set<IntIndex> fixed = model.getFixedVariables();

            Expression objective = model.objective().compensate(fixed);
            double objectiveScale = ADJUSTED_OBJECTIVE ? objective.getAdjustmentFactor() : ONE;
            ConvexMultivariateRealFunction objectiveFunction;

            // Compensated first - with some of its variables fixed a quadratic constraint may be linear
            List<Expression> constraints = model.constraints().map(expr -> expr.compensate(fixed)).collect(Collectors.toList());

            for (Expression constraint : constraints) {
                if (constraint.isAnyQuadraticFactorNonZero() && constraint.isLowerLimitSet() && constraint.isUpperLimitSet()) {
                    throw new IllegalArgumentException("JOptimizer needs convex constraints, and '" + constraint.getName()
                            + "' is two-sided (or an equality) - the region between two quadratic limits is not convex");
                }
            }

            List<Expression> equalities = constraints.stream().filter(Expression::isEqualityConstraint).collect(Collectors.toList());
            int nbEquals = equalities.size();

            if (nbEquals > 0) {

                DenseDoubleMatrix2D A = new DenseDoubleMatrix2D(nbEquals, nbVars);
                DenseDoubleMatrix1D b = new DenseDoubleMatrix1D(nbEquals);

                for (int i = 0; i < nbEquals; i++) {
                    Expression constr = equalities.get(i);

                    for (IntIndex key : constr.getLinearKeySet()) {
                        int freeIndex = model.indexOfFreeVariable(key);
                        if (freeIndex >= 0) {
                            A.set(i, freeIndex, constr.doubleValue(key, ADJUSTED_CONSTRAINTS));
                        }
                    }

                    b.set(i, constr.getLowerLimit(ADJUSTED_CONSTRAINTS, Double.NEGATIVE_INFINITY));
                }

                request.setA(A);
                request.setB(b);
            }

            boolean[] boxedLower = null;
            boolean[] boxedUpper = null;

            if (reqLP != null) {
                // LP

                DenseDoubleMatrix1D c = new DenseDoubleMatrix1D(nbVars);
                DenseDoubleMatrix1D lb = new DenseDoubleMatrix1D(nbVars);
                DenseDoubleMatrix1D ub = new DenseDoubleMatrix1D(nbVars);

                boxedLower = new boolean[nbVars];
                boxedUpper = new boolean[nbVars];

                for (int j = 0; j < nbVars; j++) {

                    Variable var = variables.get(j);

                    double low = var.getLowerLimit(ADJUSTED_BOUNDS, Double.NEGATIVE_INFINITY);
                    lb.setQuick(j, Double.isFinite(low) ? low : LPPrimalDualMethod.DEFAULT_MIN_LOWER_BOUND);
                    boxedLower[j] = low < LPPrimalDualMethod.DEFAULT_MIN_LOWER_BOUND;

                    double upp = var.getUpperLimit(ADJUSTED_BOUNDS, Double.POSITIVE_INFINITY);
                    ub.setQuick(j, Double.isFinite(upp) ? upp : LPPrimalDualMethod.DEFAULT_MAX_UPPER_BOUND);
                    boxedUpper[j] = upp > LPPrimalDualMethod.DEFAULT_MAX_UPPER_BOUND;
                }

                for (IntIndex key : objective.getLinearKeySet()) {

                    int freeIndex = model.indexOfFreeVariable(key);

                    if (freeIndex >= 0) {
                        double cost = objective.doubleValue(key, ADJUSTED_OBJECTIVE);
                        c.setQuick(freeIndex, max ? -cost : cost);
                    }
                }

                objectiveFunction = new LinearMultivariateRealFunction(c, ZERO);

                List<Expression> lowIneq = constraints.stream().filter(Expression::isLowerConstraint).collect(Collectors.toList());
                int nbLowIneq = lowIneq.size();

                List<Expression> uppIneq = constraints.stream().filter(Expression::isUpperConstraint).collect(Collectors.toList());
                int nbUppIneq = uppIneq.size();

                DenseDoubleMatrix2D G = new DenseDoubleMatrix2D(nbLowIneq + nbUppIneq, nbVars);
                DenseDoubleMatrix1D h = new DenseDoubleMatrix1D(nbLowIneq + nbUppIneq);

                for (int i = 0; i < nbLowIneq; i++) {
                    Expression constr = lowIneq.get(i);

                    for (IntIndex key : constr.getLinearKeySet()) {
                        int freeIndex = model.indexOfFreeVariable(key);
                        if (freeIndex >= 0) {
                            G.set(i, freeIndex, -constr.doubleValue(key, ADJUSTED_CONSTRAINTS));
                        }
                    }

                    h.set(i, -constr.getLowerLimit(ADJUSTED_CONSTRAINTS, Double.NEGATIVE_INFINITY));
                }

                for (int i = 0; i < nbUppIneq; i++) {
                    Expression constr = uppIneq.get(i);

                    for (IntIndex key : constr.getLinearKeySet()) {
                        int freeIndex = model.indexOfFreeVariable(key);
                        if (freeIndex >= 0) {
                            G.set(nbLowIneq + i, freeIndex, constr.doubleValue(key, ADJUSTED_CONSTRAINTS));
                        }
                    }

                    h.set(nbLowIneq + i, constr.getUpperLimit(ADJUSTED_CONSTRAINTS, Double.POSITIVE_INFINITY));
                }

                reqLP.setC(c);

                reqLP.setLb(lb);
                reqLP.setUb(ub);

                reqLP.setG(G);
                reqLP.setH(h);

            } else {
                // QP

                objectiveFunction = SolverJOptimizer.toObjectiveFunction(objective, nbVars, model, ADJUSTED_OBJECTIVE);
                request.setF0(objectiveFunction);

                // Inequality constraints, fi(x) <= 0

                List<ConvexMultivariateRealFunction> constrs = new ArrayList<>();

                constraints.stream().filter(Expression::isLowerConstraint)
                        .forEach(compC -> constrs.add(SolverJOptimizer.toLowerConstraint(compC, nbVars, model, ADJUSTED_CONSTRAINTS)));

                constraints.stream().filter(Expression::isUpperConstraint)
                        .forEach(compC -> constrs.add(SolverJOptimizer.toUpperConstraint(compC, nbVars, model, ADJUSTED_CONSTRAINTS)));

                /*
                 * Without an initial point JOptimizer starts its phase I (finding a strictly feasible point)
                 * at the origin, and that fails for some quite simple models. Starting strictly inside the
                 * variable bounds works better - and if that point happens to be strictly feasible, phase I
                 * is skipped.
                 */
                double[] initial = new double[nbVars];

                for (int j = 0; j < nbVars; j++) {

                    Variable var = variables.get(j);
                    double coefficient = ADJUSTED_BOUNDS ? var.getAdjustmentFactor() : ONE;

                    double low = var.getLowerLimit(ADJUSTED_BOUNDS, Double.NEGATIVE_INFINITY);
                    double upp = var.getUpperLimit(ADJUSTED_BOUNDS, Double.POSITIVE_INFINITY);

                    if (var.isLowerLimitSet()) {
                        double[] q = new double[nbVars];
                        q[j] = -coefficient;
                        constrs.add(new LinearMultivariateRealFunction(q, low));
                    }
                    if (var.isUpperLimitSet()) {
                        double[] q = new double[nbVars];
                        q[j] = coefficient;
                        constrs.add(new LinearMultivariateRealFunction(q, -upp));
                    }

                    if (Double.isFinite(low) && Double.isFinite(upp)) {
                        initial[j] = (low + upp) / TWO;
                    } else if (Double.isFinite(low)) {
                        initial[j] = low + ONE;
                    } else if (Double.isFinite(upp)) {
                        initial[j] = upp - ONE;
                    }
                }

                if (constrs.size() > 0) {
                    request.setFi(constrs.toArray(new ConvexMultivariateRealFunction[constrs.size()]));
                }

                if (nbEquals > 0) {
                    // Phase I requires the initial point to satisfy the equality constraints - project it onto them
                    initial = SolverJOptimizer.project(initial, request.getA().toArray(), request.getB().toArray());
                }

                request.setNotFeasibleInitialPoint(initial);
            }

            return new SolverJOptimizer(configurator, request, model.options, objectiveFunction, objectiveScale, boxedLower, boxedUpper);
        }

        @Override
        public boolean isCapable(final ExpressionsBasedModel model) {
            return !model.isAnyVariableInteger();
        }

        @Override
        public Result toModelState(final Result solverState, final ExpressionsBasedModel model) {
            return ExpressionsBasedModel.Integration.expandFreeToFull(solverState, model, ArrayR064.FACTORY, Optional.empty(), this.getSolverSense());
        }

        @Override
        public Result toSolverState(final Result modelState, final ExpressionsBasedModel model) {
            return ExpressionsBasedModel.Integration.reduceFullToFree(modelState, model, ArrayR064.FACTORY);
        }

        @Override
        protected Optimisation.Sense getSolverSense() {
            return Optimisation.Sense.MIN;
        }

    }

    public static final ExpressionsBasedModel.Integration<SolverJOptimizer> INTEGRATION = new Integration();

    /**
     * A solution value within this fraction of a box limit is considered to be at it.
     */
    private static final double BOX_LIMIT_FACTOR = ONE - 1E-6;

    static final Configurator DEFAULT = new Configurator();

    /**
     * JOptimizer needs convex functions - the Hessian must be positive semidefinite. Otherwise it may well
     * return a non-optimal point, reported as optimal. Only the rows/columns of the variables involved are
     * checked.
     * <p>
     * Cholesky is the quick test, but it fails for a semidefinite (singular) Hessian as well as for an
     * indefinite one. Telling those apart needs the eigenvalues.
     */
    private static void assertPositiveSemidefinite(final DenseDoubleMatrix2D hessian, final SortedSet<Integer> involved, final Expression expression) {

        int[] indices = involved.stream().mapToInt(Integer::intValue).toArray();
        int dim = indices.length;

        PhysicalStore<Double> block = R064Store.FACTORY.make(dim, dim);
        for (int j = 0; j < dim; j++) {
            for (int i = 0; i < dim; i++) {
                block.set(i, j, hessian.getQuick(indices[i], indices[j]));
            }
        }

        Cholesky<Double> cholesky = Cholesky.R064.make(block);
        if (cholesky.decompose(block) && cholesky.isSPD()) {
            return;
        }

        Eigenvalue<Double> evd = Eigenvalue.R064.make(block, true);
        if (!evd.decompose(block)) {
            throw new IllegalArgumentException("Failed to decompose the quadratic form of '" + expression.getName() + "'");
        }

        MatrixStore<Double> mtrxD = evd.getD();

        double largest = ZERO;
        for (int i = 0; i < dim; i++) {
            largest = Math.max(largest, Math.abs(mtrxD.doubleValue(i, i)));
        }
        double tolerance = dim * Math.max(largest, ONE) * MACHINE_EPSILON;

        for (int i = 0; i < dim; i++) {
            double eigenvalue = mtrxD.doubleValue(i, i);
            if (eigenvalue < -tolerance) {
                throw new IllegalArgumentException(
                        "JOptimizer needs a convex problem, and '" + expression.getName() + "' is not convex (eigenvalue " + eigenvalue + ")");
            }
        }
    }

    private static MatrixR064 column(final double[] values) {
        double[][] column = new double[values.length][1];
        for (int i = 0; i < values.length; i++) {
            column[i][0] = values[i];
        }
        return MatrixR064.FACTORY.copy(Access2D.wrap(column));
    }

    private static ConvexMultivariateRealFunction toFunction(final Expression expression, final int dim, final boolean negate,
            final ExpressionsBasedModel model, final double constant, final boolean adjusted) {

        DenseDoubleMatrix2D p = null;
        DenseDoubleMatrix1D q = null;
        double r = constant;

        if (expression.isAnyQuadraticFactorNonZero()) {
            p = new DenseDoubleMatrix2D(dim, dim);

            SortedSet<Integer> involved = new TreeSet<>();

            for (IntRowColumn key : expression.getQuadraticKeySet()) {
                double val = expression.doubleValue(key, adjusted);
                double d = negate ? -val : val;
                int rowIndex = model.indexOfFreeVariable(key.row);
                int colIndex = model.indexOfFreeVariable(key.column);
                p.set(rowIndex, colIndex, p.get(rowIndex, colIndex) + d);
                p.set(colIndex, rowIndex, p.get(colIndex, rowIndex) + d);
                involved.add(Integer.valueOf(rowIndex));
                involved.add(Integer.valueOf(colIndex));
            }

            SolverJOptimizer.assertPositiveSemidefinite(p, involved, expression);
        }

        if (expression.isAnyLinearFactorNonZero()) {
            q = new DenseDoubleMatrix1D(dim);

            for (IntIndex key : expression.getLinearKeySet()) {
                double val = expression.doubleValue(key, adjusted);
                q.set(model.indexOfFreeVariable(key.index), negate ? -val : val);
            }
        }

        if (p != null) {
            return new PSDQuadraticMultivariateRealFunction(p, q, r);
        } else {
            return new LinearMultivariateRealFunction(q, r);
        }
    }

    /**
     * The point closest to x that satisfies [A][x]=[b]: x + A<sup>T</sup>(AA<sup>T</sup>)<sup>-1</sup>(b -
     * Ax)
     */
    static double[] project(final double[] x, final double[][] A, final double[] b) {

        MatrixR064 mtrxA = MatrixR064.FACTORY.copy(Access2D.wrap(A));
        MatrixR064 vctrX = SolverJOptimizer.column(x);

        MatrixR064 residual = SolverJOptimizer.column(b).subtract(mtrxA.multiply(vctrX));
        MatrixR064 correction = mtrxA.transpose().multiply(mtrxA.multiply(mtrxA.transpose()).solve(residual));

        return vctrX.add(correction).toRawCopy1D();
    }

    /**
     * lower - g(x) &lt;= 0
     */
    static ConvexMultivariateRealFunction toLowerConstraint(final Expression expression, final int dim, final ExpressionsBasedModel model,
            final boolean adjusted) {
        return SolverJOptimizer.toFunction(expression, dim, true, model, expression.getLowerLimit(adjusted, Double.NEGATIVE_INFINITY), adjusted);
    }

    static ConvexMultivariateRealFunction toObjectiveFunction(final Expression expression, final int dim, final ExpressionsBasedModel model,
            final boolean adjusted) {
        return SolverJOptimizer.toFunction(expression, dim, model.getOptimisationSense() == Optimisation.Sense.MAX, model, 0.0, adjusted);
    }

    /**
     * g(x) - upper &lt;= 0
     */
    static ConvexMultivariateRealFunction toUpperConstraint(final Expression expression, final int dim, final ExpressionsBasedModel model,
            final boolean adjusted) {
        return SolverJOptimizer.toFunction(expression, dim, false, model, -expression.getUpperLimit(adjusted, Double.POSITIVE_INFINITY), adjusted);
    }

    /**
     * JOptimizer's LP solver confines all variables to [{@link LPPrimalDualMethod#DEFAULT_MIN_LOWER_BOUND},
     * {@link LPPrimalDualMethod#DEFAULT_MAX_UPPER_BOUND}] – infinite bounds are replaced by these, and finite
     * bounds beyond them are clamped. These mark the variables (of an LP) where that box is an extra,
     * artificial, limit. A solution at such a limit is not proven optimal, and infeasibility within the box
     * is not proven infeasibility.
     */
    private final boolean[] myBoxedLower;
    private final boolean[] myBoxedUpper;
    private final Configurator myConfigurator;
    /**
     * The (minimisation form) objective function, of the free variables.
     */
    private final ConvexMultivariateRealFunction myObjective;
    /**
     * The objective's adjustment factor – the objective JOptimizer solves is scaled by this.
     */
    private final double myObjectiveScale;
    private final Optimisation.Options myOptions;
    private final OptimizationRequest myRequest;

    SolverJOptimizer(final Configurator configurator, final OptimizationRequest request, final Optimisation.Options options,
            final ConvexMultivariateRealFunction objective, final double objectiveScale, final boolean[] boxedLower, final boolean[] boxedUpper) {

        super();

        myConfigurator = configurator;
        myRequest = request;
        myOptions = options;
        myObjective = objective;
        myObjectiveScale = objectiveScale;
        myBoxedLower = boxedLower;
        myBoxedUpper = boxedUpper;
    }

    @Override
    public Result solve(final Result kickStarter) {

        Result retVal = this.solve(kickStarter, false);

        if (retVal.getState() == Optimisation.State.FAILED && myRequest instanceof LPOptimizationRequest) {
            // The tight tolerances make some LPs fail that JOptimizer solves with its defaults

            if (myOptions.logger_appender != null) {
                myOptions.logger_appender.println("Retrying with JOptimizer's default tolerances");
            }

            retVal = this.solve(kickStarter, true);
        }

        return retVal;
    }

    private Result solve(final Result kickStarter, final boolean fallback) {

        OptimizationRequestHandler handler = myConfigurator.newOptimizationRequestHandler(myRequest);

        if (fallback) {
            myConfigurator.configureFallback(myRequest, handler, myOptions);
        } else {
            myConfigurator.configure(myRequest, handler, myOptions);
        }

        Optimisation.State retState = Optimisation.State.UNEXPLORED;
        double retValue = NaN;
        Access1D<Double> retSolution = null;

        //        if (kickStarter != null && kickStarter.getState().isFeasible()) {
        //            retState = kickStarter.getState();
        //            myRequest.setInitialPoint(kickStarter.toRawCopy1D());
        //        }

        handler.setOptimizationRequest(myRequest);

        try {

            handler.optimize();

            OptimizationResponse response = handler.getOptimizationResponse();

            double[] solution = response.getSolution();
            /*
             * At an artificial box limit the problem is either unbounded, or its optimum is beyond the box.
             * Either way the solution is feasible, but not optimal.
             */
            retState = this.isAnyAtBoxLimit(solution) ? Optimisation.State.FEASIBLE : Optimisation.State.OPTIMAL;
            retValue = myObjective.value(new DenseDoubleMatrix1D(solution)) / myObjectiveScale;
            retSolution = Access1D.wrap(solution);

        } catch (InfeasibleProblemException cause) {

            if (myOptions.logger_appender != null) {
                myOptions.logger_appender.print(cause.toString());
            }

            // Infeasible within the artificial box doesn't mean infeasible
            retState = this.isAnyBoxed() ? Optimisation.State.FAILED : Optimisation.State.INFEASIBLE;

        } catch (JOptimizerException | RuntimeException cause) {
            // For some models JOptimizer can't handle, e.g. with rank deficient equality constraints, it throws unchecked exceptions

            if (myOptions.logger_appender != null) {
                myOptions.logger_appender.print(cause.toString());
            }

            retState = Optimisation.State.FAILED;

        } finally {

            if (retSolution == null && kickStarter != null) {
                retSolution = Access1D.wrap(kickStarter.toRawCopy1D());
            }
        }

        return new Optimisation.Result(retState, retValue, retSolution);
    }

    private boolean isAnyAtBoxLimit(final double[] solution) {

        if (myBoxedLower == null || myBoxedUpper == null) {
            return false;
        }

        for (int j = 0; j < solution.length; j++) {
            if ((myBoxedLower[j] && solution[j] <= LPPrimalDualMethod.DEFAULT_MIN_LOWER_BOUND * BOX_LIMIT_FACTOR)
                    || (myBoxedUpper[j] && solution[j] >= LPPrimalDualMethod.DEFAULT_MAX_UPPER_BOUND * BOX_LIMIT_FACTOR)) {
                return true;
            }
        }

        return false;
    }

    private boolean isAnyBoxed() {

        if (myBoxedLower == null || myBoxedUpper == null) {
            return false;
        }

        for (int j = 0; j < myBoxedLower.length; j++) {
            if (myBoxedLower[j] || myBoxedUpper[j]) {
                return true;
            }
        }

        return false;
    }

}
