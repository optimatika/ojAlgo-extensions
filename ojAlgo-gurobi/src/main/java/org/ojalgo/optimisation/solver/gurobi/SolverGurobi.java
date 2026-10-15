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
package org.ojalgo.optimisation.solver.gurobi;

import static org.ojalgo.function.constant.PrimitiveMath.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.ojalgo.array.ArrayR064;
import org.ojalgo.concurrent.InterruptForwarder;
import org.ojalgo.optimisation.ConstraintsMetaData;
import org.ojalgo.optimisation.Expression;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.ModelEntity;
import org.ojalgo.optimisation.Optimisation;
import org.ojalgo.optimisation.Optimisation.ConstraintType;
import org.ojalgo.optimisation.UpdatableSolver;
import org.ojalgo.optimisation.Variable;
import org.ojalgo.structure.Structure1D.IntIndex;
import org.ojalgo.structure.Structure2D.IntRowColumn;
import org.ojalgo.type.ForgetfulMap;
import org.ojalgo.type.context.NumberContext;
import org.ojalgo.type.keyvalue.EntryPair;

import com.gurobi.gurobi.GRB;
import com.gurobi.gurobi.GRBEnv;
import com.gurobi.gurobi.GRBException;
import com.gurobi.gurobi.GRBExpr;
import com.gurobi.gurobi.GRBLinExpr;
import com.gurobi.gurobi.GRBModel;
import com.gurobi.gurobi.GRBQConstr;
import com.gurobi.gurobi.GRBQuadExpr;
import com.gurobi.gurobi.GRBVar;

/**
 * https://docs.gurobi.com/projects/optimizer/en/current/reference/java/env.html#java-method-GRBEnv-start
 *
 * @author apete
 */
public final class SolverGurobi implements Optimisation.Solver, UpdatableSolver {

    public static class Configurator {

        /**
         * Configure the environment. It is cached, and shared by all models built with the same
         * {@link Integration}, so this is only for settings that are the same for all of them. Each model gets
         * its own copy of the environment's parameters when it is created – settings specific to a model go in
         * {@link #configure(GRBModel, Optimisation.Options)}.
         */
        protected void configure(final GRBEnv environment, final Optimisation.Options options) {
            try {
                environment.set(GRB.IntParam.OutputFlag, 0);
            } catch (GRBException cause) {
                throw new RuntimeException(cause);
            }
        }

        /**
         * Configure a model, its own copy of the environment's parameters.
         */
        protected void configure(final GRBModel model, final Optimisation.Options options) {
            try {

                long effectiveTimeMillis = Math.min(options.time_suffice, options.time_abort);
                if (effectiveTimeMillis > 0L && effectiveTimeMillis < Long.MAX_VALUE) {
                    model.set(GRB.DoubleParam.TimeLimit, effectiveTimeMillis / 1000.0);
                }

                int effectiveIterations = Math.min(options.iterations_suffice, options.iterations_abort);
                if (effectiveIterations > 0 && effectiveIterations < Integer.MAX_VALUE) {
                    model.set(GRB.DoubleParam.IterationLimit, effectiveIterations);
                }

                int parallelism = options.getParallelism();
                if (parallelism > 0) {
                    model.set(GRB.IntParam.Threads, parallelism);
                }

                // The feasibility tolerance, only if configured to be something other than the default – a
                // requirement on both the constraints and the integrality, within Gurobi's allowed ranges
                Optional<NumberContext> feasibility = options.getConfiguredFeasibilityTolerance();
                if (feasibility.isPresent()) {
                    double tolerance = feasibility.get().getAbsoluteError();
                    model.set(GRB.DoubleParam.FeasibilityTol, Math.min(Math.max(tolerance, 1E-9), 1E-2));
                    model.set(GRB.DoubleParam.IntFeasTol, Math.min(Math.max(tolerance, 1E-9), 1E-1));
                }

            } catch (GRBException cause) {
                throw new RuntimeException(cause);
            }
        }

        protected GRBModel newGRBModel(final GRBEnv environment) {
            try {
                environment.start();
                return new GRBModel(environment);
            } catch (GRBException cause) {
                throw new RuntimeException(cause);
            }
        }

    }

    static final class GurobiEntityMap implements ExpressionsBasedModel.EntityMap {

        private final ConstraintsMetaData myConstraints;
        /**
         * For each constraint, its index among Gurobi's linear or quadratic constraints (see
         * {@link #myQuadratic}).
         */
        private final int[] myGroupIndices;
        private final int myNumEqualityConstraints;
        private final int myNumInequalityConstraints;
        private final int myNumVars;
        private final boolean[] myQuadratic;

        GurobiEntityMap(final int numVars, final List<EntryPair<ModelEntity<?>, ConstraintType>> constraints) {
            myNumVars = numVars;
            int nbConstraints = constraints.size();
            myConstraints = ConstraintsMetaData.newEntityMap(nbConstraints);
            myGroupIndices = new int[nbConstraints];
            myQuadratic = new boolean[nbConstraints];
            int eq = 0;
            int ineq = 0;
            int linear = 0;
            int quadratic = 0;
            for (int i = 0; i < nbConstraints; i++) {
                EntryPair<ModelEntity<?>, ConstraintType> entry = constraints.get(i);
                myConstraints.setEntry(i, entry.left(), entry.right(), false, false);
                if (entry.getValue() == ConstraintType.EQUALITY) {
                    eq++;
                } else {
                    ineq++;
                }
                myQuadratic[i] = ((Expression) entry.left()).isAnyQuadraticFactorNonZero();
                myGroupIndices[i] = myQuadratic[i] ? quadratic++ : linear++;
            }
            myNumEqualityConstraints = eq;
            myNumInequalityConstraints = ineq;
        }

        @Override
        public int countAdditionalConstraints() {
            return 0;
        }

        @Override
        public int countEqualityConstraints() {
            return myNumEqualityConstraints;
        }

        @Override
        public int countInequalityConstraints() {
            return myNumInequalityConstraints;
        }

        @Override
        public int countModelVariables() {
            return myNumVars;
        }

        @Override
        public int countSlackVariables() {
            return 0;
        }

        @Override
        public int countVariables() {
            return myNumVars;
        }

        @Override
        public EntryPair<ModelEntity<?>, ConstraintType> getConstraint(final int idc) {
            return myConstraints.getEntry(idc);
        }

        @Override
        public EntryPair<ModelEntity<?>, ConstraintType> getSlack(final int ids) {
            return null;
        }

        @Override
        public int indexOf(final int solverIndex) {
            if (solverIndex >= 0 && solverIndex < myNumVars) {
                return solverIndex;
            }
            return -1;
        }

        @Override
        public boolean isNegated(final int solverIndex) {
            return false;
        }

        ConstraintsMetaData getConstraintsMetaData() {
            return myConstraints;
        }

        int getGroupIndex(final int idc) {
            return myGroupIndices[idc];
        }

        boolean isQuadratic(final int idc) {
            return myQuadratic[idc];
        }

    }

    /**
     * Gurobi gets the model as it is: all variables, including the fixed ones, and the constraints and the
     * objective function without any scaling (adjustment). Gurobi's own presolve and scaling does the rest.
     */
    static final class Integration extends ExpressionsBasedModel.Integration<SolverGurobi> {

        private final boolean myEmpty;
        private final ForgetfulMap.ValueCache<GRBEnv> myEnvironmentCache;

        Integration(final boolean empty) {

            super();

            myEmpty = empty;

            myEnvironmentCache = ForgetfulMap.newBuilder().expireAfterAccess(1, TimeUnit.HOURS).build(this::newGRBEnv, this::dispose);
        }

        @Override
        public SolverGurobi build(final ExpressionsBasedModel model) {

            GRBEnv environment = this.getEnvironment();

            Configurator configurator = model.getConfigurator(DEFAULT);

            try {

                configurator.configure(environment, model.options);

                GRBModel delegateSolver = configurator.newGRBModel(environment);

                configurator.configure(delegateSolver, model.options);

                List<Variable> modVars = model.getVariables();

                int numberOfVariables = modVars.size();

                double[] lb = new double[numberOfVariables];
                double[] ub = new double[numberOfVariables];
                char[] type = new char[numberOfVariables];
                String[] name = new String[numberOfVariables];

                for (int v = 0; v < numberOfVariables; v++) {
                    Variable variable = modVars.get(v);

                    lb[v] = variable.getLowerLimit(false, NEGATIVE_INFINITY);
                    ub[v] = variable.getUpperLimit(false, POSITIVE_INFINITY);

                    type[v] = GRB.CONTINUOUS;
                    if (variable.isBinary()) {
                        type[v] = GRB.BINARY;
                    } else if (variable.isInteger()) {
                        type[v] = GRB.INTEGER;
                    }

                    name[v] = variable.getName();
                }

                delegateSolver.addVars(lb, ub, null, type, name);
                delegateSolver.update();

                GRBVar[] delegateVariables = delegateSolver.getVars();

                List<EntryPair<ModelEntity<?>, ConstraintType>> constrInfo = new ArrayList<>();

                List<Expression> modConstrs = model.constraints().collect(Collectors.toList());
                for (Expression expr : modConstrs) {

                    GRBExpr solExpr = SolverGurobi.buildExpression(expr, delegateVariables);

                    SolverGurobi.setBounds(solExpr, expr, delegateSolver, constrInfo);
                }

                GRBExpr solObj = SolverGurobi.buildExpression(model.objective(), delegateVariables);
                if (solObj == null) {
                    solObj = new GRBLinExpr();
                }
                double objectiveConstant = ExpressionsBasedModel.Integration.getObjectiveConstant(model);
                if (solObj instanceof GRBQuadExpr) {
                    ((GRBQuadExpr) solObj).addConstant(objectiveConstant);
                } else {
                    ((GRBLinExpr) solObj).addConstant(objectiveConstant);
                }

                if (model.getOptimisationSense() == Optimisation.Sense.MAX) {
                    delegateSolver.setObjective(solObj, GRB.MAXIMIZE);
                } else {
                    delegateSolver.setObjective(solObj, GRB.MINIMIZE);
                }

                delegateSolver.update();

                // Dual values with quadratic constraints are only computed when asked for, and only for continuous
                // models
                if (delegateSolver.get(GRB.IntAttr.NumQConstrs) > 0 && !model.isAnyVariableInteger()) {
                    delegateSolver.set(GRB.IntParam.QCPDual, 1);
                }

                // The MIP gap, only if configured to be something other than the default
                Optional<NumberContext> gap = model.options.getConfiguredGapTolerance();
                if (gap.isPresent()) {
                    delegateSolver.set(GRB.DoubleParam.MIPGap, gap.get().getRelativeError());
                    delegateSolver.set(GRB.DoubleParam.MIPGapAbs, gap.get().getAbsoluteError());
                }

                GurobiEntityMap entityMap = new GurobiEntityMap(numberOfVariables, constrInfo);

                return new SolverGurobi(delegateSolver, entityMap, model.options);

            } catch (GRBException exception) {
                exception.printStackTrace();
                return null;
            }

        }

        @Override
        public boolean isCapable(final ExpressionsBasedModel model) {
            return true;
        }

        private void dispose(final GRBEnv env) {
            try {
                env.dispose();
            } catch (GRBException cause) {
                throw new RuntimeException(cause);
            }
        }

        private GRBEnv newGRBEnv() {
            try {
                return new GRBEnv(myEmpty);
            } catch (GRBException cause) {
                throw new RuntimeException(cause);
            }
        }

        @Override
        protected Optimisation.Sense getSolverSense() {
            return null;
        }

        GRBEnv getEnvironment() {
            return myEnvironmentCache.getCachedObject();
        }

    }

    public static final ExpressionsBasedModel.Integration<SolverGurobi> INTEGRATION = new Integration(false);

    static final Configurator DEFAULT = new Configurator();

    /**
     * Will create "empty" {@link GRBEnv} instances. Use
     * {@link Configurator#configure(GRBEnv, Optimisation.Options)} to configure the environment.
     * {@link GRBEnv#start()} will be called automatically.
     */
    public static ExpressionsBasedModel.Integration<SolverGurobi> newConfigurableIntegration() {
        return new Integration(true);
    }

    static void addConstraint(final GRBModel model, final GRBExpr expr, final char sense, final double rhs, final String name) {
        try {
            if (expr instanceof GRBQuadExpr) {
                model.addQConstr((GRBQuadExpr) expr, sense, rhs, name);
            } else if (expr instanceof GRBLinExpr) {
                model.addConstr((GRBLinExpr) expr, sense, rhs, name);
            }
        } catch (GRBException exception) {
            exception.printStackTrace();
        }
    }

    static GRBExpr buildExpression(final Expression expression, final GRBVar[] vars) throws GRBException {

        GRBLinExpr linExpr = null;
        GRBQuadExpr quadExpr;
        GRBExpr retVal = null;

        if (expression.isAnyLinearFactorNonZero()) {

            retVal = linExpr = new GRBLinExpr();

            for (IntIndex key : expression.getLinearKeySet()) {
                linExpr.addTerm(expression.doubleValue(key, false), vars[key.index]);
            }
        }

        if (expression.isAnyQuadraticFactorNonZero()) {

            quadExpr = new GRBQuadExpr();
            if (linExpr != null) {
                quadExpr.add(linExpr);
            }
            retVal = quadExpr;

            for (IntRowColumn key : expression.getQuadraticKeySet()) {
                quadExpr.addTerm(expression.doubleValue(key, false), vars[key.row], vars[key.column]);
            }
        }

        return retVal;
    }

    static void setBounds(final GRBExpr solExpr, final Expression modExpr, final GRBModel delegateSolver,
            final List<EntryPair<ModelEntity<?>, ConstraintType>> constrInfo) {
        if (solExpr == null) {
            // No (non-zero) factors - there is no constraint to add
            return;
        }
        if (modExpr.isEqualityConstraint()) {
            SolverGurobi.addConstraint(delegateSolver, solExpr, GRB.EQUAL, modExpr.getLowerLimit(false, ZERO), modExpr.getName());
            constrInfo.add(EntryPair.of(modExpr, ConstraintType.EQUALITY));
        } else {
            if (modExpr.isLowerConstraint()) {
                SolverGurobi.addConstraint(delegateSolver, solExpr, GRB.GREATER_EQUAL, modExpr.getLowerLimit(false, NEGATIVE_INFINITY), modExpr.getName());
                constrInfo.add(EntryPair.of(modExpr, ConstraintType.LOWER));
            }
            if (modExpr.isUpperConstraint()) {
                SolverGurobi.addConstraint(delegateSolver, solExpr, GRB.LESS_EQUAL, modExpr.getUpperLimit(false, POSITIVE_INFINITY), modExpr.getName());
                constrInfo.add(EntryPair.of(modExpr, ConstraintType.UPPER));
            }
        }
    }

    static State translate(final int status) {
        switch (status) {
            case GRB.Status.INFEASIBLE:
                return State.INFEASIBLE;
            case GRB.Status.ITERATION_LIMIT:
            case GRB.Status.CUTOFF:
            case GRB.Status.NODE_LIMIT:
            case GRB.Status.NUMERIC:
            case GRB.Status.SOLUTION_LIMIT:
            case GRB.Status.SUBOPTIMAL:
            case GRB.Status.TIME_LIMIT:
                return State.APPROXIMATE;
            case GRB.Status.INF_OR_UNBD:
                return State.INVALID;
            case GRB.Status.INPROGRESS:
            case GRB.Status.INTERRUPTED:
            case GRB.Status.LOADED:
                return State.UNEXPLORED;
            case GRB.Status.OPTIMAL:
                return State.OPTIMAL;
            case GRB.Status.UNBOUNDED:
                return State.UNBOUNDED;
            default:
                return State.FAILED;
        }
    }

    private final GurobiEntityMap myEntityMap;
    private final GRBModel myGRBModel;
    private final int myIterationsAbort;
    private final int myIterationsSuffice;
    private final int myNumVars;
    /**
     * A MIP start was set, and has to be cleared should the next solve not have one.
     */
    private boolean myStart = false;
    private final long myTimeAbortMillis;
    private final long myTimeSufficeMillis;

    SolverGurobi(final GRBModel model, final GurobiEntityMap entityMap, final Optimisation.Options options) {
        super();
        myEntityMap = entityMap;
        myGRBModel = model;
        myIterationsAbort = options.iterations_abort;
        myIterationsSuffice = options.iterations_suffice;
        myNumVars = model.getVars().length;
        myTimeAbortMillis = options.time_abort;
        myTimeSufficeMillis = options.time_suffice;
    }

    @Override
    public void dispose() {
        UpdatableSolver.super.dispose();
        if (myGRBModel != null) {
            myGRBModel.dispose();
        }
    }

    @Override
    public boolean fixVariable(final int index, final double value) {
        return this.updateRange(index, value, value);
    }

    @Override
    public double getDualMultiplier(final int index) {
        try {
            int groupIndex = myEntityMap.getGroupIndex(index);
            if (myEntityMap.isQuadratic(index)) {
                return myGRBModel.getQConstrs()[groupIndex].get(GRB.DoubleAttr.QCPi);
            } else {
                return myGRBModel.getConstr(groupIndex).get(GRB.DoubleAttr.Pi);
            }
        } catch (GRBException cause) {
            throw new RuntimeException(cause);
        }
    }

    @Override
    public Optional<ExpressionsBasedModel.EntityMap> getEntityMap() {
        return Optional.of(myEntityMap);
    }

    @Override
    public double getReducedGradient(final int index) {
        try {
            return myGRBModel.getVar(index).get(GRB.DoubleAttr.RC);
        } catch (GRBException cause) {
            throw new RuntimeException(cause);
        }
    }

    /**
     * For a continuous model the dual values and the reduced gradient are Gurobi's own (Pi or QCPi, for
     * linear or quadratic constraints, and RC): the rates of change of the objective function value with the
     * constraint limits and the variable bounds, as described at
     * {@link Optimisation.Result#getDualSolution()}.
     */
    @Override
    public Result solve(final Result kickStarter) {

        GRBVar[] tmpVars = myGRBModel.getVars();

        State retState = State.UNEXPLORED;
        double retValue = NaN;
        ArrayR064 retSolution = ArrayR064.make(myGRBModel.getVars().length);
        double[] retDuals = null;
        double[] retReducedGradient = null;

        try {

            this.setStart(kickStarter, tmpVars);

            long startMillis = System.currentTimeMillis();

            retState = this.optimize();

            if (retState == State.APPROXIMATE && myTimeSufficeMillis < myTimeAbortMillis) {
                long elapsed = System.currentTimeMillis() - startMillis;
                long remaining = myTimeAbortMillis - elapsed;
                if (remaining > 1000L) {
                    retState = this.optimizeUntilAbort(remaining);
                }
            }

            if (retState == State.UNBOUNDED) {
                return this.solveForFeasibleSolution();
            }

            if (retState.isApproximate() && myGRBModel.get(GRB.IntAttr.SolCount) > 0) {

                retValue = myGRBModel.get(GRB.DoubleAttr.ObjVal);

                for (int i = 0; i < tmpVars.length; i++) {
                    retSolution.set(i, tmpVars[i].get(GRB.DoubleAttr.X));
                }

                // Gurobi only computes dual values for continuous models, and not for non-convex quadratic
                // constraints
                if (retState.isOptimal() && myGRBModel.get(GRB.IntAttr.IsMIP) == 0) {
                    try {

                        double[] pi = myGRBModel.get(GRB.DoubleAttr.Pi, myGRBModel.getConstrs());
                        GRBQConstr[] quadraticConstraints = myGRBModel.getQConstrs();
                        double[] qcpi = quadraticConstraints.length > 0 ? myGRBModel.get(GRB.DoubleAttr.QCPi, quadraticConstraints) : new double[0];
                        retDuals = new double[myEntityMap.getConstraintsMetaData().size()];
                        for (int i = 0; i < retDuals.length; i++) {
                            int groupIndex = myEntityMap.getGroupIndex(i);
                            retDuals[i] = myEntityMap.isQuadratic(i) ? qcpi[groupIndex] : pi[groupIndex];
                        }

                        retReducedGradient = myGRBModel.get(GRB.DoubleAttr.RC, tmpVars);

                    } catch (GRBException cause) {
                        if (cause.getErrorCode() != GRB.Error.DATA_NOT_AVAILABLE) {
                            throw cause;
                        }
                        retDuals = null;
                        retReducedGradient = null;
                    }
                }
            }

        } catch (GRBException cause) {
            throw new RuntimeException(cause);
        }

        Result retVal = new Result(retState, retValue, retSolution);

        if (retDuals != null) {
            double[] duals = retDuals;
            double[] reducedGradient = retReducedGradient;
            retVal = retVal.withDualValues(myEntityMap.getConstraintsMetaData(), () -> ArrayR064.wrap(duals))
                    .withReducedGradient(() -> ArrayR064.wrap(reducedGradient));
        }

        return retVal;
    }

    @Override
    public boolean updateRange(final int index, final double lower, final double upper) {

        if (index < 0 || index >= myNumVars) {
            return false;
        }

        try {
            GRBVar var = myGRBModel.getVar(index);
            var.set(GRB.DoubleAttr.LB, lower);
            var.set(GRB.DoubleAttr.UB, upper);
            myGRBModel.update();
            return true;
        } catch (GRBException cause) {
            return false;
        }
    }

    /**
     * Gurobi's status alone does not say if there is a solution when a limit was reached, the solution count
     * does. A MIP incumbent is feasible, while a continuous model stopped early is at most approximate.
     */
    private State getState() throws GRBException {
        State state = SolverGurobi.translate(myGRBModel.get(GRB.IntAttr.Status));
        if (state == State.APPROXIMATE && myGRBModel.get(GRB.IntAttr.IsMIP) != 0 && myGRBModel.get(GRB.IntAttr.SolCount) > 0) {
            return State.FEASIBLE;
        }
        return state;
    }

    /**
     * Should Gurobi only conclude that the model is infeasible or unbounded, it is optimised again without dual
     * reductions to find out which.
     * <p>
     * Asked for dual values with quadratic constraints (QCPDual), Gurobi refuses to solve a non-convex model. It
     * is then optimised again without them - Gurobi solves non-convex models to global optimality, but has no
     * dual values for them anyway.
     * <p>
     * Interrupting the solving thread reaches Gurobi through {@link GRBModel#terminate()}.
     */
    private State optimize() throws GRBException {
        try (InterruptForwarder interrupter = InterruptForwarder.watch(myGRBModel::terminate)) {
            try {
                myGRBModel.optimize();
            } catch (GRBException cause) {
                if (cause.getErrorCode() != GRB.Error.ERROR_Q_NOT_PSD || myGRBModel.get(GRB.IntParam.QCPDual) == 0) {
                    throw cause;
                }
                myGRBModel.set(GRB.IntParam.QCPDual, 0);
                myGRBModel.optimize();
            }
            if (myGRBModel.get(GRB.IntAttr.Status) == GRB.Status.INF_OR_UNBD) {
                int dualReductions = myGRBModel.get(GRB.IntParam.DualReductions);
                myGRBModel.set(GRB.IntParam.DualReductions, 0);
                try {
                    myGRBModel.optimize();
                } finally {
                    myGRBModel.set(GRB.IntParam.DualReductions, dualReductions);
                }
            }
        }
        return this.getState();
    }

    /**
     * Optimise again, with the abort limits instead of the suffice limits. The model's limits are restored
     * afterwards, so that the next solve starts from the configured ones.
     */
    private State optimizeUntilAbort(final long remainingMillis) throws GRBException {
        double timeLimit = myGRBModel.get(GRB.DoubleParam.TimeLimit);
        double iterationLimit = myGRBModel.get(GRB.DoubleParam.IterationLimit);
        try {
            myGRBModel.set(GRB.DoubleParam.TimeLimit, remainingMillis / 1000.0);
            if (myIterationsSuffice < myIterationsAbort) {
                myGRBModel.set(GRB.DoubleParam.IterationLimit, myIterationsAbort);
            }
            return this.optimize();
        } finally {
            myGRBModel.set(GRB.DoubleParam.TimeLimit, timeLimit);
            myGRBModel.set(GRB.DoubleParam.IterationLimit, iterationLimit);
        }
    }

    /**
     * A feasible solution to start from is Gurobi's MIP start. Anything else, such as what ojAlgo derives from
     * the variables' values when no solution is supplied, is not passed on - Gurobi would spend time trying to
     * repair it. Ignored for continuous models.
     */
    private void setStart(final Result kickStarter, final GRBVar[] variables) throws GRBException {

        if (myGRBModel.get(GRB.IntAttr.IsMIP) == 0) {
            return;
        }

        boolean start = kickStarter != null && kickStarter.getState().isFeasible() && kickStarter.size() == variables.length;

        if (start || myStart) {
            double[] values = new double[variables.length];
            for (int i = 0; i < values.length; i++) {
                values[i] = start ? kickStarter.doubleValue(i) : GRB.UNDEFINED;
            }
            myGRBModel.set(GRB.DoubleAttr.Start, variables, values);
            myStart = start;
        }
    }

    /**
     * Gurobi's unbounded status only says that the objective can improve without limit, not that there is a
     * feasible solution, which ojAlgo's does. Solving a copy of the model with a zero objective finds one, or
     * proves there is none.
     * <p>
     * A MIP may already have incumbents, but they lie far out along the unbounded ray, where Gurobi considers
     * the values infinite.
     */
    private Result solveForFeasibleSolution() throws GRBException {
        GRBModel copy = new GRBModel(myGRBModel);
        try {
            GRBExpr objective = copy.getObjective();
            copy.setObjective(new GRBLinExpr());
            copy.set(GRB.IntParam.QCPDual, 0);
            try (InterruptForwarder interrupter = InterruptForwarder.watch(copy::terminate)) {
                copy.optimize();
            }
            if (copy.get(GRB.IntAttr.SolCount) > 0) {
                double value = objective instanceof GRBQuadExpr ? ((GRBQuadExpr) objective).getValue() : ((GRBLinExpr) objective).getValue();
                return new Result(State.UNBOUNDED, value, ArrayR064.wrap(copy.get(GRB.DoubleAttr.X, copy.getVars())));
            }
            State state = copy.get(GRB.IntAttr.Status) == GRB.Status.INFEASIBLE ? State.INFEASIBLE : State.INVALID;
            return new Result(state, NaN, ArrayR064.make(myNumVars));
        } finally {
            copy.dispose();
        }
    }

    GRBModel getGRBModel() {
        return myGRBModel;
    }

}
