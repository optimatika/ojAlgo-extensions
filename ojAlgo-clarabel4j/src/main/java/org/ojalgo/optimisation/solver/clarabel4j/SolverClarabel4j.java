package org.ojalgo.optimisation.solver.clarabel4j;

import static org.ojalgo.function.constant.PrimitiveMath.*;

import java.util.ArrayList;
import java.util.List;

import org.ojalgo.matrix.decomposition.Cholesky;
import org.ojalgo.matrix.store.MatrixStore;
import org.ojalgo.matrix.store.PhysicalStore;
import org.ojalgo.matrix.store.R064Store;
import org.ojalgo.optimisation.Expression;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.Optimisation;
import org.ojalgo.optimisation.Variable;
import org.ojalgo.structure.Structure1D.IntIndex;
import org.ojalgo.structure.Structure2D.IntRowColumn;
import org.ojalgo.type.FloatingPointReceptacle;

import com.ustermetrics.clarabel4j.*;

/**
 * Integration wrapper around the Clarabel native solver (clarabel4j).
 * <p>
 * This class converts an ojAlgo {@link ExpressionsBasedModel} into the sparse CSC representations expected by
 * Clarabel and exposes the clarabel4j {@link com.ustermetrics.clarabel4j.Model} as an ojAlgo
 * {@link Optimisation.Solver}.
 */
public final class SolverClarabel4j implements Optimisation.Solver {

    public static class Configurator {

        protected void configure(final Optimisation.Options optimisationOptions, final Parameters.ParametersBuilder parametersBuilder) {

            // Iteration limit
            if (optimisationOptions.iterations_abort > 0L) {
                int maxIter = Math.min(Integer.MAX_VALUE, optimisationOptions.iterations_abort);
                parametersBuilder.maxIter(Integer.valueOf(maxIter));
            }

            // Time limit (ojAlgo stores ms; Clarabel expects seconds)
            if (optimisationOptions.time_abort > 0L) {
                double seconds = optimisationOptions.time_abort / 1_000.0;
                parametersBuilder.timeLimit(Double.valueOf(seconds));
            }

            // Verbosity: if the user has a logger configured and detailed logging is enabled,
            // turn on Clarabel's verbose output.
            boolean verbose = optimisationOptions.logger_appender != null && optimisationOptions.logger_detailed;
            parametersBuilder.verbose(Boolean.valueOf(verbose));

            // Solution accuracy: map ojAlgo's solution NumberContext scale to Clarabel tolerances.
            double relativeError = optimisationOptions.feasibility.getRelativeError();
            double absoluteError = optimisationOptions.feasibility.getAbsoluteError();
            double epsilon = optimisationOptions.feasibility.epsilon();

            // Use a single epsilon to populate the main feasibility and gap tolerances.
            parametersBuilder.tolFeas(Double.valueOf(epsilon));

            parametersBuilder.tolInfeasRel(Double.valueOf(relativeError));
            parametersBuilder.tolInfeasAbs(Double.valueOf(absoluteError));

            parametersBuilder.tolGapRel(Double.valueOf(relativeError));
            parametersBuilder.tolGapAbs(Double.valueOf(absoluteError));

            // Reduced tolerances – slightly tighter to encourage cleaner post-solve residuals.
            relativeError *= HUNDREDTH;
            absoluteError *= HUNDREDTH;
            epsilon *= HUNDREDTH;

            parametersBuilder.reducedTolFeas(Double.valueOf(epsilon));

            parametersBuilder.reducedTolInfeasRel(Double.valueOf(relativeError));
            parametersBuilder.reducedTolInfeasAbs(Double.valueOf(absoluteError));

            parametersBuilder.reducedTolGapRel(Double.valueOf(relativeError));
            parametersBuilder.reducedTolGapAbs(Double.valueOf(absoluteError));

            int parallelism = optimisationOptions.getParallelism();
            if (parallelism > 0) {
                parametersBuilder.maxThreads(Integer.valueOf(parallelism));
            }
        }

        /**
         * Configure the Clarabel model using ojAlgo {@link Optimisation.Options}.
         * <p>
         * This implementation maps a small subset of generic options (verbosity and simple limits) into
         * Clarabel {@link Parameters} using its builder, and wires Clarabel's textual output into ojAlgo's
         * logging when a logger is present.
         */
        final void configure(final com.ustermetrics.clarabel4j.Model model, final Options options) {

            Parameters.ParametersBuilder builder = Parameters.builder();

            this.configure(options, builder);

            Parameters parameters = builder.build();

            model.setParameters(parameters);

            //  model.setOutput(output);
        }

    }

    static final class Integration extends ExpressionsBasedModel.Integration<SolverClarabel4j> {

        private static final boolean ADJUSTED_BOUNDS = true;
        private static final boolean ADJUSTED_CONSTRAINTS = true;
        private static final boolean ADJUSTED_OBJECTIVE = true;

        Integration() {
            super();
        }

        @Override
        public SolverClarabel4j build(final ExpressionsBasedModel model) {

            Configurator configurator = model.getConfigurator(DEFAULT);

            List<Variable> variables = model.getVariables();

            // The number of model variables present in the solver problem
            int nbProbVars = variables.size();

            List<Cone> cones = new ArrayList<>();

            MatrixBuilder bldrA = new MatrixBuilder(0, nbProbVars);
            FloatingPointReceptacle bldrB = new FloatingPointReceptacle();

            // Linear equality constraints -> ZeroCone (A rows + b entries)
            Expression[] linEquExprs = model.constraints().filter(expr -> !expr.isAnyQuadraticFactorNonZero()).filter(Expression::isEqualityConstraint)
                    .toArray(Expression[]::new);

            ZeroCone zeroCone = SolverClarabel4j.makeZeroCone(linEquExprs, bldrA, bldrB, model);
            if (zeroCone != null) {
                cones.add(zeroCone);
            }

            // Linear inequalities (including variable bounds) -> NonnegativeCone (A rows + b entries)
            Expression[] linInqExprs = model.constraints().filter(constr -> !constr.isAnyQuadraticFactorNonZero())
                    .filter(constr -> !constr.isEqualityConstraint()).toArray(Expression[]::new);

            NonnegativeCone nonnegativeCone = SolverClarabel4j.makeNonnegativeCone(linInqExprs, variables, bldrA, bldrB, model);
            if (nonnegativeCone != null) {
                cones.add(nonnegativeCone);
            }

            // Any quadratic constraints (potential SOC)
            Expression[] quadExprs = model.constraints().filter(Expression::isAnyQuadraticFactorNonZero).toArray(Expression[]::new);

            for (Expression expression : quadExprs) {
                SecondOrderCone secondOrderCone = SolverClarabel4j.makeSecondOrderCone(expression, bldrA, bldrB, model);
                if (secondOrderCone != null) {
                    cones.add(secondOrderCone);
                }
            }

            ExponentialCone exponentialCone = SolverClarabel4j.makeExponentialCone(model);
            if (exponentialCone != null) {
                cones.add(exponentialCone);
            }

            PowerCone powerCone = SolverClarabel4j.makePowerCone(model);
            if (powerCone != null) {
                cones.add(powerCone);
            }

            GenPowerCone genPowerCone = SolverClarabel4j.makeGenPowerCone(model);
            if (genPowerCone != null) {
                cones.add(genPowerCone);
            }

            int m = bldrA.getRowDim();
            int n = bldrA.getColDim();

            boolean max = model.getOptimisationSense() == Optimisation.Sense.MAX;

            Expression mObj = model.objective();

            double[] q = new double[n];
            for (IntIndex key : mObj.getLinearKeySet()) {
                double val = mObj.doubleValue(key, ADJUSTED_OBJECTIVE);
                q[key.index] = max ? -val : val;
            }

            MatrixBuilder bldrP = new MatrixBuilder(n, n);
            for (IntRowColumn key : mObj.getQuadraticKeySet()) {
                double val = mObj.doubleValue(key, ADJUSTED_OBJECTIVE);
                bldrP.add(key.row, key.column, max ? -val : val);
                bldrP.add(key.column, key.row, max ? -val : val);
            }
            Matrix mtrxP = bldrP.build();

            Matrix mtrxA = null;
            double[] vctrB = null;
            if (m > 0) {
                mtrxA = bldrA.build();
                vctrB = bldrB.toDoubles();
            }

            com.ustermetrics.clarabel4j.Model clarabelModel = new com.ustermetrics.clarabel4j.Model();

            configurator.configure(clarabelModel, model.options);

            if (m == 0) {
                clarabelModel.setup(mtrxP, q);
            } else {
                clarabelModel.setup(mtrxP, q, mtrxA, vctrB, cones);
            }

            // Clarabel sees an extended variable vector of length n, but only the first nbProbVars entries
            // correspond to model variables.
            return new SolverClarabel4j(clarabelModel, max, nbProbVars);
        }

        @Override
        public boolean isCapable(final ExpressionsBasedModel model) {
            return !model.isAnyVariableInteger();
        }

        @Override
        protected Optimisation.Sense getSolverSense() {
            return Optimisation.Sense.MIN;
        }

    }

    public static final ExpressionsBasedModel.Integration<SolverClarabel4j> INTEGRATION = new Integration();

    private static final boolean ADJUSTED = true;

    static final Configurator DEFAULT = new Configurator();

    private static SecondOrderCone makeSecondOrderCone(final Expression constraint, final double bound, final boolean negate, final MatrixBuilder bldrA,
            final FloatingPointReceptacle bldrB, final ExpressionsBasedModel model) {

        int rowOffset = bldrB.size();

        List<Integer> varIdx = new ArrayList<>();
        for (IntRowColumn key : constraint.getQuadraticKeySet()) {
            if (!varIdx.contains(key.row)) {
                varIdx.add(key.row);
            }
            if (!varIdx.contains(key.column)) {
                varIdx.add(key.column);
            }
        }

        int dim = varIdx.size();
        if (dim == 0) {
            return null;
        }

        PhysicalStore<Double> qStore = R064Store.FACTORY.make(dim, dim);
        for (IntRowColumn key : constraint.getQuadraticKeySet()) {
            int i = varIdx.indexOf(key.row);
            int j = varIdx.indexOf(key.column);
            double val = HALF * constraint.doubleValue(key, ADJUSTED);
            if (negate) {
                val = -val;
            }
            qStore.add(i, j, val);
            qStore.add(j, i, val);
        }

        Cholesky<Double> cholesky = Cholesky.R064.make(qStore);
        if (!cholesky.compute(qStore)) {
            return null;
        }
        MatrixStore<Double> lStore = cholesky.getL();

        bldrB.append(Math.sqrt(negate ? -bound : bound));

        int rowsInA = 0;
        for (int i = 0; i < dim; i++) {
            int row = rowOffset + 1 + rowsInA;
            for (int j = 0; j < dim; j++) {
                double lij = lStore.doubleValue(i, j);
                if (lij != ZERO) {
                    int col = varIdx.get(j);
                    bldrA.add(row, col, lij);
                }
            }
            bldrB.append(ZERO);
            rowsInA++;
        }

        return new SecondOrderCone(1 + rowsInA);
    }

    static ExponentialCone makeExponentialCone(final ExpressionsBasedModel model) {
        return null;
    }

    static GenPowerCone makeGenPowerCone(final ExpressionsBasedModel model) {
        return null;
    }

    static NonnegativeCone makeNonnegativeCone(final Expression[] expressions, final List<Variable> variables, final MatrixBuilder bldrA,
            final FloatingPointReceptacle bldrB, final ExpressionsBasedModel model) {

        int initial = bldrB.size();

        int row = initial;

        for (Expression expression : expressions) {

            double lb = expression.getLowerLimit(ADJUSTED, NEGATIVE_INFINITY);
            double ub = expression.getUpperLimit(ADJUSTED, POSITIVE_INFINITY);

            if (ub < POSITIVE_INFINITY) {
                for (IntIndex key : expression.getLinearKeySet()) {
                    bldrA.add(row, key.index, expression.doubleValue(key, ADJUSTED));
                }
                bldrB.append(ub);
                row++;
            }

            if (lb > NEGATIVE_INFINITY) {
                for (IntIndex key : expression.getLinearKeySet()) {
                    bldrA.add(row, key.index, -expression.doubleValue(key, ADJUSTED));
                }
                bldrB.append(-lb);
                row++;
            }
        }

        for (Variable variable : variables) {

            int col = model.indexOf(variable);

            double lb = variable.getLowerLimit(ADJUSTED, NEGATIVE_INFINITY);
            double ub = variable.getUpperLimit(ADJUSTED, POSITIVE_INFINITY);

            double val = variable.getAdjustmentFactor();

            if (ub < POSITIVE_INFINITY) {
                bldrA.add(row, col, val);
                bldrB.append(ub);
                row++;
            }

            if (lb > NEGATIVE_INFINITY) {
                bldrA.add(row, col, -val);
                bldrB.append(-lb);
                row++;
            }
        }

        if (row <= initial) {
            return null;
        } else {
            return new NonnegativeCone(row - initial);
        }
    }

    static PowerCone makePowerCone(final ExpressionsBasedModel model) {
        return null;
    }

    static SecondOrderCone makeSecondOrderCone(final Expression constraint, final MatrixBuilder bldrA, final FloatingPointReceptacle bldrB,
            final ExpressionsBasedModel model) {

        double ub = constraint.getUpperLimit(ADJUSTED, POSITIVE_INFINITY);
        double lb = constraint.getLowerLimit(ADJUSTED, NEGATIVE_INFINITY);

        if (ub < POSITIVE_INFINITY) {
            return SolverClarabel4j.makeSecondOrderCone(constraint, ub, false, bldrA, bldrB, model);
        } else if (lb > NEGATIVE_INFINITY) {
            // lb < x'Qx + c'x  =>  -x'Qx -c'x < -lb
            return SolverClarabel4j.makeSecondOrderCone(constraint, lb, true, bldrA, bldrB, model);
        } else {
            return null;
        }
    }

    static ZeroCone makeZeroCone(final Expression[] expressions, final MatrixBuilder bldrA, final FloatingPointReceptacle bldrB,
            final ExpressionsBasedModel model) {

        if (expressions == null || expressions.length <= 0) {
            return null;
        }

        int initial = bldrB.size();
        int row = initial;

        for (Expression expression : expressions) {
            for (IntIndex key : expression.getLinearKeySet()) {
                double val = expression.doubleValue(key, ADJUSTED);
                bldrA.add(row, key.index, val);
            }
            bldrB.append(expression.getUpperLimit(ADJUSTED, POSITIVE_INFINITY));
            row++;
        }

        return new ZeroCone(expressions.length);
    }

    static Optimisation.State translate(final Status status) {

        if (status == null) {
            return Optimisation.State.FAILED;
        }

        return switch (status) {
            case SOLVED                                                                                          -> Optimisation.State.OPTIMAL;
            case ALMOST_SOLVED, ALMOST_PRIMAL_INFEASIBLE, ALMOST_DUAL_INFEASIBLE                                 -> Optimisation.State.APPROXIMATE;
            case PRIMAL_INFEASIBLE                                                                               -> Optimisation.State.INFEASIBLE;
            case DUAL_INFEASIBLE                                                                                 -> Optimisation.State.UNBOUNDED;
            case NUMERICAL_ERROR, MAX_ITERATIONS, MAX_TIME, INSUFFICIENT_PROGRESS, CLARABEL_CALL_BACK_TERMINATED -> Optimisation.State.FAILED;
            case UNSOLVED                                                                                        -> Optimisation.State.UNEXPLORED;
        };
    }

    private final boolean myMaximisation;

    private final com.ustermetrics.clarabel4j.Model myModel;

    private final int myNumberOfVariables;

    SolverClarabel4j(final com.ustermetrics.clarabel4j.Model model, final boolean maximisation, final int numberOfVariables) {
        super();
        myModel = model;
        myMaximisation = maximisation;
        myNumberOfVariables = numberOfVariables;
    }

    @Override
    public void dispose() {
        Optimisation.Solver.super.dispose();
        try {
            if (myModel != null) {
                myModel.close();
            }
        } catch (Exception ignore) {
        }
    }

    @Override
    public Optimisation.Result solve(final Result kickStarter) {

        Status status = myModel.optimize();

        Optimisation.State state = SolverClarabel4j.translate(status);

        if (state.isFeasible() && state != Optimisation.State.UNBOUNDED) {
            double obj = NaN;
            try {
                obj = myModel.objVal();
            } catch (Exception ignore) {
            }
            if (myMaximisation && !Double.isNaN(obj)) {
                obj = -obj;
            }

            double[] x = null;
            try {
                x = myModel.x();
            } catch (Exception ignore) {
            }

            if (x == null || x.length != myNumberOfVariables) {
                x = new double[myNumberOfVariables];
            }

            return Optimisation.Result.of(obj, state, x);

        } else {

            return Optimisation.Result.of(state, new double[myNumberOfVariables]);
        }
    }

}
