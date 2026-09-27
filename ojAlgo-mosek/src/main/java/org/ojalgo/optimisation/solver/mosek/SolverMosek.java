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
package org.ojalgo.optimisation.solver.mosek;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.ojalgo.array.ArrayR064;
import org.ojalgo.netio.BasicLogger;
import org.ojalgo.optimisation.ConstraintsMetaData;
import org.ojalgo.optimisation.Expression;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.Optimisation;
import org.ojalgo.optimisation.Variable;
import org.ojalgo.structure.Structure1D.IntIndex;
import org.ojalgo.structure.Structure2D.IntRowColumn;
import org.ojalgo.type.ForgetfulMap;
import org.ojalgo.type.context.NumberContext;

import mosek.*;
import mosek.Exception;

/**
 * TODO: Clean up / refactor, look at SolverORTools and SolverCPLEX.
 *
 * @author apete
 */
public final class SolverMosek implements Optimisation.Solver {

    public static class Configurator {

        protected Task newTask(final Env environment, final int nbConstraints, final int nbVariables, final Optimisation.Options options) {

            Task task = new Task(environment, nbConstraints, nbVariables);

            task.appendcons(nbConstraints);
            task.appendvars(nbVariables);

            // Only the task's log - the environment is cached, and shared by all models built with the same
            // Integration, so it must not be sent to this model's logger.
            if (options.logger_appender != null) {

                Stream stream = new Stream() {

                    @Override
                    public void stream(final String message) {
                        options.logger_appender.print(message);
                    }
                };

                task.set_Stream(streamtype.log, stream);
            }

            long effectiveTimeMillis = Math.min(options.time_suffice, options.time_abort);
            if (effectiveTimeMillis > 0L && effectiveTimeMillis < Long.MAX_VALUE) {
                double seconds = effectiveTimeMillis / 1000.0;
                task.putdouparam(dparam.optimizer_max_time, seconds);
            }

            int effectiveIterations = Math.min(options.iterations_suffice, options.iterations_abort);
            if (effectiveIterations > 0 && effectiveIterations < Integer.MAX_VALUE) {
                task.putintparam(iparam.sim_max_iterations, effectiveIterations);
                task.putintparam(iparam.intpnt_max_iterations, effectiveIterations);
            }

            int parallelism = options.getParallelism();
            if (parallelism > 0) {
                task.putintparam(iparam.num_threads, parallelism);
            }

            // The feasibility tolerance, only if configured to be something other than the default – a
            // requirement on both the constraints and the integrality, within MOSEK's allowed ranges. The
            // interior-point tolerances are relative measures, and are left as they are.
            Optional<NumberContext> feasibility = options.getConfiguredFeasibilityTolerance();
            if (feasibility.isPresent()) {
                double tolerance = Math.max(feasibility.get().getAbsoluteError(), 1E-9);
                task.putdouparam(dparam.basis_tol_x, tolerance);
                task.putdouparam(dparam.mio_tol_feas, Math.min(tolerance, 1E-3));
                task.putdouparam(dparam.mio_tol_abs_relax_int, tolerance);
            }

            return task;
        }

    }

    /**
     * MOSEK gets the model as it is: all variables, including the fixed ones, and the constraints and the
     * objective function without any scaling (adjustment). MOSEK's own presolve and scaling does the rest.
     */
    public static final class Integration extends ExpressionsBasedModel.Integration<SolverMosek> {

        private final ForgetfulMap.ValueCache<Env> myEnvironmentCache;

        Integration() {

            super();

            myEnvironmentCache = ForgetfulMap.newBuilder().expireAfterAccess(1, TimeUnit.HOURS).build(this::newEnv, this::dispose);
        }

        @Override
        public SolverMosek build(final ExpressionsBasedModel model) {

            Options options = model.options;

            Env environment = this.getEnvironment();

            Configurator configurator = model.getConfigurator(DEFAULT);

            List<Variable> variables = model.getVariables();

            List<Expression> constraints = model.constraints().collect(Collectors.toList());

            int nbVariables = variables.size();
            int nbConstraints = constraints.size();

            Task task = configurator.newTask(environment, nbConstraints, nbVariables, options);

            // The MIP gap, only if configured to be something other than the default
            Optional<NumberContext> gap = model.options.getConfiguredGapTolerance();
            if (gap.isPresent()) {
                task.putdouparam(dparam.mio_tol_rel_gap, gap.get().getRelativeError());
                task.putdouparam(dparam.mio_tol_abs_gap, gap.get().getAbsoluteError());
            }

            ConstraintsMetaData constraintsMetaData = ConstraintsMetaData.newEntityMap(nbConstraints);
            constraintsMetaData.setSignedRows(true);

            SolverMosek retVal = new SolverMosek(task, options, constraintsMetaData, model.getOptimisationSense() == Optimisation.Sense.MAX);

            for (int v = 0; v < nbVariables; v++) {
                retVal.putVariable(v, variables.get(v));
            }

            for (int c = 0; c < nbConstraints; c++) {
                Expression constraint = constraints.get(c);
                retVal.putConstraint(c, constraint);
                constraintsMetaData.setEntry(c, constraint, constraint.getConstraintType(), false, false);
            }

            retVal.putObjective(model.objective(), model);
            task.putcfix(ExpressionsBasedModel.Integration.getObjectiveConstant(model));

            retVal.setSolutionType(model);

            return retVal;
        }

        @Override
        public boolean isCapable(final ExpressionsBasedModel model) {
            return true; // Can handle any variation of an ExpressionsBasedModel
        }

        private void dispose(final Env env) {
            env.dispose();
        }

        private Env newEnv() {

            Env env = new Env();

            env.set_Stream(streamtype.err, new Stream() {
                @Override
                public void stream(final String message) {
                    BasicLogger.ERROR.print(message);
                }
            });

            return env;
        }

        @Override
        protected Optimisation.Sense getSolverSense() {
            return null;
        }

        Env getEnvironment() {
            return myEnvironmentCache.getCachedObject();
        }

    }

    public static final SolverMosek.Integration INTEGRATION = new Integration();

    static final Configurator DEFAULT = new Configurator();

    /**
     * The lower triangular part of Q, where the quadratic part of the expression is x'Qx/2 (as MOSEK wants it).
     * The keys (i,j) and (j,i) are the same element, and are added.
     */
    private static Map<IntRowColumn, Double> getLowerTriangle(final Expression expression) {

        Map<IntRowColumn, Double> retVal = new LinkedHashMap<>();

        for (IntRowColumn key : expression.getQuadraticKeySet()) {
            int row = key.row;
            int col = key.column;
            double value = expression.doubleValue(key, false);
            IntRowColumn element = new IntRowColumn(Math.max(row, col), Math.min(row, col));
            retVal.merge(element, row == col ? 2.0 * value : value, Double::sum);
        }

        return retVal;
    }

    private final ConstraintsMetaData myConstraintsMetaData;
    private final int myIterationsAbort;
    private final int myIterationsSuffice;
    private final boolean myMaximisation;
    private final Optimisation.Options myOptions;
    private soltype mySolutionType = soltype.bas;
    /**
     * The thread inside {@link #solve(Result)}. Read from the progress callback, which MOSEK may make from a
     * thread of its own, so it is that thread's interrupt flag – not the calling one's – that has to be asked
     * about.
     */
    private volatile Thread mySolving = null;
    private final Task myTask;
    private final long myTimeAbortMillis;
    private final long myTimeSufficeMillis;

    SolverMosek(final Task task, final Optimisation.Options options, final ConstraintsMetaData constraintsMetaData, final boolean maximisation) {

        super();

        myTask = task;
        myOptions = options;
        myConstraintsMetaData = constraintsMetaData;
        myMaximisation = maximisation;
        myIterationsAbort = options.iterations_abort;
        myIterationsSuffice = options.iterations_suffice;
        myTimeAbortMillis = options.time_abort;
        myTimeSufficeMillis = options.time_suffice;

        myTask.set_Progress(this.newProgress());
    }

    @Override
    public void dispose() {

        Solver.super.dispose();

        if (myTask != null) {
            myTask.dispose();
        }
    }

    @Override
    public Result solve(final Result kickStarter) {

        int tmpNumberOfVariables = myTask.getnumvar();

        Optimisation.State tmpSate = Optimisation.State.FAILED;
        double tmpValue = Double.NaN;
        double[] tmpSolution = new double[tmpNumberOfVariables];

        long startMillis = System.currentTimeMillis();

        mySolving = Thread.currentThread();

        try {

            tmpSate = this.getState(this.optimise());

            if (tmpSate == Optimisation.State.APPROXIMATE && myTimeSufficeMillis < myTimeAbortMillis) {
                long elapsed = System.currentTimeMillis() - startMillis;
                long remaining = myTimeAbortMillis - elapsed;
                if (remaining > 1000L) {
                    tmpSate = this.optimiseUntilAbort(remaining);
                }
            }

            if (tmpSate == Optimisation.State.UNBOUNDED) {
                return this.solveForFeasibleSolution();
            }

            if (tmpSate.isFeasible()) {
                myTask.getxx(mySolutionType, tmpSolution);
                tmpValue = myTask.getprimalobj(mySolutionType);
            }

            if (tmpSate.isOptimal() && mySolutionType != soltype.itg) {

                // MOSEK's dual values (y) are the rates of change of the objective function value with the
                // constraint limits. For a minimisation that is minus the multipliers signed as for two-sided
                // rows (positive when the upper limit is active), for a maximisation it is those multipliers.
                double sign = myMaximisation ? 1.0 : -1.0;
                double[] duals = new double[myTask.getnumcon()];
                myTask.gety(mySolutionType, duals);
                for (int i = 0; i < duals.length; i++) {
                    duals[i] *= sign;
                }

                // The reduced costs are in the model's sense
                double[] reducedGradient = new double[tmpNumberOfVariables];
                myTask.getreducedcosts(mySolutionType, 0, tmpNumberOfVariables, reducedGradient);

                return new Optimisation.Result(tmpSate, tmpValue, ArrayR064.wrap(tmpSolution))
                        .withDualValues(myConstraintsMetaData, () -> ArrayR064.wrap(duals)).withReducedGradient(() -> ArrayR064.wrap(reducedGradient));
            }

        } catch (Exception xcptn) {
            throw xcptn;
        } finally {
            mySolving = null;
        }

        return new Optimisation.Result(tmpSate, tmpValue, ArrayR064.wrap(tmpSolution));
    }

    /**
     * Wraps {@link Task#optimize()} to keep the interrupt out of the returned {@link rescode}: a solve
     * stopped from the progress callback comes back as {@code trm_user_callback}, which is a termination code
     * rather than something having gone wrong, but there is no solution to read either way.
     */
    private rescode optimise() {

        rescode retVal = myTask.optimize();

        if (retVal == rescode.trm_user_callback) {
            BasicLogger.debug("MOSEK stopped on request");
        }

        return retVal;
    }

    /**
     * The objective function value, including the constant, of a solution to this task.
     */
    private double evaluateObjective(final double[] solution) {

        double retVal = myTask.getcfix();

        double[] linear = myTask.getc();
        for (int j = 0; j < linear.length; j++) {
            retVal += linear[j] * solution[j];
        }

        // MOSEK's quadratic part is x'Qx/2, with only the lower triangle of Q stored
        int nbQuadratic = (int) myTask.getnumqobjnz();
        if (nbQuadratic > 0) {
            int[] rows = new int[nbQuadratic];
            int[] cols = new int[nbQuadratic];
            double[] vals = new double[nbQuadratic];
            myTask.getqobj(new long[1], rows, cols, vals);
            for (int k = 0; k < nbQuadratic; k++) {
                double product = vals[k] * solution[rows[k]] * solution[cols[k]];
                retVal += rows[k] == cols[k] ? product / 2.0 : product;
            }
        }

        return retVal;
    }

    /**
     * MOSEK's solution status, and the problem status when that does not settle it. Stopping at a limit is
     * not an error, but there may or may not be a solution. Should the problem be unbounded, or possibly so, that
     * is for {@link #solveForFeasibleSolution()} to settle.
     */
    private Optimisation.State getState(final rescode code) {

        if (code == rescode.trm_user_callback) {
            return Optimisation.State.UNEXPLORED;
        }

        boolean terminated = code != rescode.ok;

        if (!myTask.solutiondef(mySolutionType)) {
            return terminated ? Optimisation.State.APPROXIMATE : Optimisation.State.FAILED;
        }

        return switch (myTask.getsolsta(mySolutionType)) {
            case optimal, integer_optimal -> Optimisation.State.OPTIMAL;
            case prim_feas, prim_and_dual_feas -> Optimisation.State.FEASIBLE;
            case prim_infeas_cer -> Optimisation.State.INFEASIBLE;
            case dual_infeas_cer -> Optimisation.State.UNBOUNDED;
            default -> switch (myTask.getprosta(mySolutionType)) {
                case prim_infeas, prim_and_dual_infeas -> Optimisation.State.INFEASIBLE;
                case dual_infeas, prim_infeas_or_unbounded -> Optimisation.State.UNBOUNDED;
                default -> terminated ? Optimisation.State.APPROXIMATE : Optimisation.State.FAILED;
            };
        };
    }

    /**
     * MOSEK stops itself: it asks this while optimising, and anything but zero means stop. So an interrupt of
     * the solving thread is all it takes, and nothing needs to watch that thread from outside.
     */
    private Progress newProgress() {
        return new Progress() {

            @Override
            public int progress(final callbackcode caller) {
                Thread solving = mySolving;
                return solving != null && solving.isInterrupted() ? 1 : 0;
            }

        };
    }

    /**
     * Optimise again, with the abort limits instead of the suffice limits. The task's limits are restored
     * afterwards, so that the next solve starts from the configured ones.
     */
    private Optimisation.State optimiseUntilAbort(final long remainingMillis) {

        double timeLimit = myTask.getdouparam(dparam.optimizer_max_time);
        int simplexLimit = myTask.getintparam(iparam.sim_max_iterations);
        int interiorPointLimit = myTask.getintparam(iparam.intpnt_max_iterations);

        try {
            myTask.putdouparam(dparam.optimizer_max_time, remainingMillis / 1000.0);
            if (myIterationsSuffice < myIterationsAbort) {
                myTask.putintparam(iparam.sim_max_iterations, myIterationsAbort);
                myTask.putintparam(iparam.intpnt_max_iterations, myIterationsAbort);
            }
            return this.getState(this.optimise());
        } finally {
            myTask.putdouparam(dparam.optimizer_max_time, timeLimit);
            myTask.putintparam(iparam.sim_max_iterations, simplexLimit);
            myTask.putintparam(iparam.intpnt_max_iterations, interiorPointLimit);
        }
    }

    /**
     * MOSEK's certificate of dual infeasibility only says that the objective can improve without limit, not
     * that there is a feasible solution, which ojAlgo's unbounded state does - and for a MIP MOSEK may not be
     * able to tell infeasible from unbounded at all. Solving a copy of the task with a zero objective finds a
     * feasible solution, and then the problem is unbounded, or proves there is none.
     */
    private Optimisation.Result solveForFeasibleSolution() {

        int nbVariables = myTask.getnumvar();

        Task copy = new Task(myTask);
        try {

            copy.putcslice(0, nbVariables, new double[nbVariables]);
            copy.putcfix(0.0);
            copy.putqobj(new int[0], new int[0], new double[0]);
            copy.set_Progress(this.newProgress());

            copy.optimize();

            if (copy.solutiondef(mySolutionType)) {

                solsta status = copy.getsolsta(mySolutionType);

                if (status == solsta.optimal || status == solsta.integer_optimal || status == solsta.prim_feas || status == solsta.prim_and_dual_feas) {
                    double[] values = new double[nbVariables];
                    copy.getxx(mySolutionType, values);
                    return new Optimisation.Result(Optimisation.State.UNBOUNDED, this.evaluateObjective(values), ArrayR064.wrap(values));
                }

                prosta problem = copy.getprosta(mySolutionType);

                if (status == solsta.prim_infeas_cer || problem == prosta.prim_infeas || problem == prosta.prim_and_dual_infeas) {
                    return new Optimisation.Result(Optimisation.State.INFEASIBLE, Double.NaN, ArrayR064.make(nbVariables));
                }
            }

            return new Optimisation.Result(Optimisation.State.INVALID, Double.NaN, ArrayR064.make(nbVariables));

        } finally {
            copy.dispose();
        }
    }

    boundkey getBoundKey(final Optimisation.Constraint modelEntity) {

        if (modelEntity.getLowerLimit() != null) {
            if (modelEntity.getUpperLimit() == null) {
                return boundkey.lo;
            }
            if (modelEntity.getLowerLimit().compareTo(modelEntity.getUpperLimit()) == 0) {
                return boundkey.fx;
            }
            return boundkey.ra;
        }
        if (modelEntity.getUpperLimit() != null) {
            return boundkey.up;
        }
        return boundkey.fr;
    }

    void putConstraint(final int index, final Expression constraint) {

        Set<IntIndex> tmpLinearFactorKeys = constraint.getLinearKeySet();
        int tmpLinearSize = tmpLinearFactorKeys.size();
        if (tmpLinearSize > 0) {

            int[] tmpCols = new int[tmpLinearSize];
            double[] tmpVals = new double[tmpLinearSize];

            int i = 0;
            for (IntIndex tmpKey : tmpLinearFactorKeys) {
                tmpCols[i] = tmpKey.index;
                tmpVals[i] = constraint.doubleValue(tmpKey, false);
                i++;
            }
            myTask.putarow(index, tmpCols, tmpVals);
        }

        if (constraint.isAnyQuadraticFactorNonZero()) {

            Map<IntRowColumn, Double> lowerTriangle = SolverMosek.getLowerTriangle(constraint);

            int[] tmpRows = new int[lowerTriangle.size()];
            int[] tmpCols = new int[lowerTriangle.size()];
            double[] tmpVals = new double[lowerTriangle.size()];

            int i = 0;
            for (Entry<IntRowColumn, Double> element : lowerTriangle.entrySet()) {
                tmpRows[i] = element.getKey().row;
                tmpCols[i] = element.getKey().column;
                tmpVals[i] = element.getValue().doubleValue();
                i++;
            }
            myTask.putqconk(index, tmpRows, tmpCols, tmpVals);
        }

        boundkey tmpBoundType = this.getBoundKey(constraint);
        double tmpLowerBound = constraint.getLowerLimit(false, Double.NEGATIVE_INFINITY);
        double tmpUpperBound = constraint.getUpperLimit(false, Double.POSITIVE_INFINITY);

        myTask.putconbound(index, tmpBoundType, tmpLowerBound, tmpUpperBound);
    }

    void putObjective(final Expression objective, final ExpressionsBasedModel model) {

        Set<IntIndex> tmpLinearFactorKeys = objective.getLinearKeySet();
        int tmpLinearSize = tmpLinearFactorKeys.size();
        if (tmpLinearSize > 0) {

            int[] tmpRows = new int[tmpLinearSize];
            double[] tmpVals = new double[tmpLinearSize];

            int i = 0;
            for (IntIndex tmpKey : tmpLinearFactorKeys) {
                tmpRows[i] = tmpKey.index;
                tmpVals[i] = objective.doubleValue(tmpKey, false);
                i++;
            }
            myTask.putclist(tmpRows, tmpVals);
        }

        if (objective.isAnyQuadraticFactorNonZero()) {

            Map<IntRowColumn, Double> lowerTriangle = SolverMosek.getLowerTriangle(objective);

            int[] tmpRows = new int[lowerTriangle.size()];
            int[] tmpCols = new int[lowerTriangle.size()];
            double[] tmpVals = new double[lowerTriangle.size()];

            int i = 0;
            for (Entry<IntRowColumn, Double> element : lowerTriangle.entrySet()) {
                tmpRows[i] = element.getKey().row;
                tmpCols[i] = element.getKey().column;
                tmpVals[i] = element.getValue().doubleValue();
                i++;
            }
            myTask.putqobj(tmpRows, tmpCols, tmpVals);
        }

        myTask.putobjsense(model.getOptimisationSense() == Optimisation.Sense.MAX ? objsense.maximize : objsense.minimize);
    }

    void putVariable(final int index, final Variable variable) {

        boundkey boundType = this.getBoundKey(variable);
        double lowerBound = variable.getLowerLimit(false, Double.NEGATIVE_INFINITY);
        double upperBound = variable.getUpperLimit(false, Double.POSITIVE_INFINITY);
        variabletype variableType = variable.isInteger() ? variabletype.type_int : variabletype.type_cont;

        myTask.putvarbound(index, boundType, lowerBound, upperBound);
        myTask.putvartype(index, variableType);
    }

    Task getTask() {
        return myTask;
    }

    void setSolutionType(final ExpressionsBasedModel model) {
        mySolutionType = model.isAnyVariableInteger() ? soltype.itg : model.isAnyExpressionQuadratic() ? soltype.itr : soltype.bas;
    }

}
