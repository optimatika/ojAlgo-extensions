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
package org.ojalgo.optimisation.solver.copt;

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

import copt.*;

/**
 * COPT's Java API is very similar to Gurobi's – this integration mirrors {@code SolverGurobi} closely,
 * substituting COPT's classes and method calls.
 * <p>
 * {@code copt.Model} is always referred to by its fully qualified name here because
 * {@link Optimisation.Solver} inherits the nested type {@link Optimisation.Model}, and that inherited member
 * type would otherwise shadow a plain {@code import copt.Model;} throughout this class.
 *
 * @author apete
 */
public final class SolverCOPT implements Optimisation.Solver, UpdatableSolver {

    public static class Configurator {

        protected void configure(final copt.Model delegateSolver, final Optimisation.Options options) {
            try {
                delegateSolver.setIntParam(IntParam.Logging, 0);

                long effectiveTimeMillis = Math.min(options.time_suffice, options.time_abort);
                if (effectiveTimeMillis > 0L && effectiveTimeMillis < Long.MAX_VALUE) {
                    delegateSolver.setDblParam(DblParam.TimeLimit, effectiveTimeMillis / 1000.0);
                }

                int effectiveIterations = Math.min(options.iterations_suffice, options.iterations_abort);
                if (effectiveIterations > 0 && effectiveIterations < Integer.MAX_VALUE) {
                    delegateSolver.setIntParam(IntParam.BarIterLimit, effectiveIterations);
                }

                int parallelism = options.getParallelism();
                if (parallelism > 0) {
                    delegateSolver.setIntParam(IntParam.Threads, parallelism);
                }

                // The feasibility tolerance, only if configured to be something other than the default – a
                // requirement on both the constraints and the integrality, within COPT's allowed ranges
                Optional<NumberContext> feasibility = options.getConfiguredFeasibilityTolerance();
                if (feasibility.isPresent()) {
                    double tolerance = feasibility.get().getAbsoluteError();
                    delegateSolver.setDblParam(DblParam.FeasTol, Math.min(Math.max(tolerance, 1E-9), 1E-4));
                    delegateSolver.setDblParam(DblParam.IntTol, Math.min(Math.max(tolerance, 1E-9), 1E-1));
                }

            } catch (CoptException cause) {
                throw new RuntimeException(cause);
            }
        }

        protected copt.Model newModel(final Envr environment) {
            try {
                return environment.createModel("ojAlgo");
            } catch (CoptException cause) {
                throw new RuntimeException(cause);
            }
        }

    }

    static final class COPTEntityMap implements ExpressionsBasedModel.EntityMap {

        private final ConstraintsMetaData myConstraints;
        /**
         * For each constraint, its index among COPT's linear or quadratic constraints (see {@link #myQuadratic}).
         */
        private final int[] myGroupIndices;
        private final int myNumEqualityConstraints;
        private final int myNumInequalityConstraints;
        private final int myNumVars;
        private final boolean[] myQuadratic;

        COPTEntityMap(final int numVars, final List<EntryPair<ModelEntity<?>, ConstraintType>> constraints) {
            myNumVars = numVars;
            int nbConstraints = constraints.size();
            myConstraints = ConstraintsMetaData.newEntityMap(nbConstraints);
            myConstraints.setSignedRows(true);
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
     * COPT gets the model as it is: all variables, including the fixed ones, and the constraints and the
     * objective function without any scaling (adjustment). COPT's own presolve and scaling does the rest.
     */
    static final class Integration extends ExpressionsBasedModel.Integration<SolverCOPT> {

        private final ForgetfulMap.ValueCache<Envr> myEnvironmentCache;

        Integration() {

            super();

            myEnvironmentCache = ForgetfulMap.newBuilder().expireAfterAccess(1, TimeUnit.HOURS).build(this::newEnvr, this::dispose);
        }

        @Override
        public SolverCOPT build(final ExpressionsBasedModel model) {

            Envr environment = this.getEnvironment();

            Configurator configurator = model.getConfigurator(DEFAULT);

            try {

                copt.Model delegateSolver = configurator.newModel(environment);

                configurator.configure(delegateSolver, model.options);

                List<Variable> modVars = model.getVariables();

                Expression modObj = model.objective();

                int numberOfVariables = modVars.size();

                double[] lb = new double[numberOfVariables];
                double[] ub = new double[numberOfVariables];
                double[] obj = new double[numberOfVariables];
                char[] type = new char[numberOfVariables];
                String[] name = new String[numberOfVariables];

                for (int v = 0; v < numberOfVariables; v++) {
                    Variable variable = modVars.get(v);

                    lb[v] = variable.getLowerLimit(false, NEGATIVE_INFINITY);
                    ub[v] = variable.getUpperLimit(false, POSITIVE_INFINITY);

                    type[v] = Consts.CONTINUOUS;
                    if (variable.isBinary()) {
                        type[v] = Consts.BINARY;
                    } else if (variable.isInteger()) {
                        type[v] = Consts.INTEGER;
                    }

                    name[v] = variable.getName();
                }

                VarArray varArray = delegateSolver.addVars(numberOfVariables, lb, ub, obj, type, "x");
                delegateSolver.setNames(varArray, name);

                Var[] delegateVariables = new Var[numberOfVariables];
                for (int i = 0; i < numberOfVariables; i++) {
                    delegateVariables[i] = varArray.getVar(i);
                }

                List<EntryPair<ModelEntity<?>, ConstraintType>> constrInfo = new ArrayList<>();

                List<Expression> modConstrs = model.constraints().collect(Collectors.toList());
                for (Expression expr : modConstrs) {
                    SolverCOPT.buildAndConstrain(expr, delegateVariables, delegateSolver, constrInfo);
                }

                int sense = model.getOptimisationSense() == Optimisation.Sense.MAX ? Consts.MAXIMIZE : Consts.MINIMIZE;

                if (modObj.isAnyQuadraticFactorNonZero()) {
                    QuadExpr solObj = SolverCOPT.buildQuadExpression(modObj, delegateVariables);
                    delegateSolver.setQuadObjective(solObj, sense);
                } else {
                    Expr solObj = SolverCOPT.buildLinExpression(modObj, delegateVariables);
                    delegateSolver.setObjective(solObj, sense);
                }
                delegateSolver.setObjConst(ExpressionsBasedModel.Integration.getObjectiveConstant(model));

                // The MIP gap, only if configured to be something other than the default
                Optional<NumberContext> gap = model.options.getConfiguredGapTolerance();
                if (gap.isPresent()) {
                    delegateSolver.setDblParam(DblParam.RelGap, gap.get().getRelativeError());
                    delegateSolver.setDblParam(DblParam.AbsGap, gap.get().getAbsoluteError());
                }

                COPTEntityMap entityMap = new COPTEntityMap(numberOfVariables, constrInfo);

                return new SolverCOPT(delegateSolver, delegateVariables, entityMap, model.options, model.getOptimisationSense() == Optimisation.Sense.MAX);

            } catch (CoptException exception) {
                exception.printStackTrace();
                return null;
            }

        }

        @Override
        public boolean isCapable(final ExpressionsBasedModel model) {
            return true;
        }

        private void dispose(final Envr env) {
            env.dispose();
        }

        private Envr newEnvr() {
            try {
                return new Envr();
            } catch (CoptException cause) {
                throw new RuntimeException(cause);
            }
        }

        @Override
        protected Optimisation.Sense getSolverSense() {
            return null;
        }

        Envr getEnvironment() {
            return myEnvironmentCache.getCachedObject();
        }

    }

    public static final ExpressionsBasedModel.Integration<SolverCOPT> INTEGRATION = new Integration();

    static final Configurator DEFAULT = new Configurator();

    static void addLinConstraint(final copt.Model model, final Expr expr, final char sense, final double rhs, final String name) {
        try {
            model.addConstr(expr, sense, rhs, name);
        } catch (CoptException exception) {
            exception.printStackTrace();
        }
    }

    static void addLinRangeConstraint(final copt.Model model, final Expr expr, final double lower, final double upper, final String name) {
        try {
            model.addConstr(expr, lower, upper, name);
        } catch (CoptException exception) {
            exception.printStackTrace();
        }
    }

    static void addQuadConstraint(final copt.Model model, final QuadExpr expr, final char sense, final double rhs, final String name) {
        try {
            model.addQConstr(expr, sense, rhs, name);
        } catch (CoptException exception) {
            exception.printStackTrace();
        }
    }

    static void buildAndConstrain(final Expression expression, final Var[] vars, final copt.Model delegateSolver,
            final List<EntryPair<ModelEntity<?>, ConstraintType>> constrInfo) throws CoptException {

        Expr linExpr = null;

        if (expression.isAnyLinearFactorNonZero()) {

            linExpr = new Expr();

            for (IntIndex key : expression.getLinearKeySet()) {
                linExpr.addTerm(vars[key.index], expression.doubleValue(key, false));
            }
        }

        if (expression.isAnyQuadraticFactorNonZero()) {

            QuadExpr quadExpr = new QuadExpr();
            if (linExpr != null) {
                quadExpr.addLinExpr(linExpr, ONE);
            }

            for (IntRowColumn key : expression.getQuadraticKeySet()) {
                quadExpr.addTerm(vars[key.row], vars[key.column], expression.doubleValue(key, false));
            }

            SolverCOPT.setQuadBounds(quadExpr, expression, delegateSolver, constrInfo);

        } else if (linExpr != null) {

            SolverCOPT.setLinBounds(linExpr, expression, delegateSolver, constrInfo);
        }
    }

    static Expr buildLinExpression(final Expression expression, final Var[] vars) {

        Expr linExpr = new Expr();

        if (expression.isAnyLinearFactorNonZero()) {
            for (IntIndex key : expression.getLinearKeySet()) {
                linExpr.addTerm(vars[key.index], expression.doubleValue(key, false));
            }
        }

        return linExpr;
    }

    static QuadExpr buildQuadExpression(final Expression expression, final Var[] vars) {

        QuadExpr quadExpr = new QuadExpr();

        if (expression.isAnyLinearFactorNonZero()) {
            Expr linExpr = SolverCOPT.buildLinExpression(expression, vars);
            quadExpr.addLinExpr(linExpr, ONE);
        }

        if (expression.isAnyQuadraticFactorNonZero()) {
            for (IntRowColumn key : expression.getQuadraticKeySet()) {
                quadExpr.addTerm(vars[key.row], vars[key.column], expression.doubleValue(key, false));
            }
        }

        return quadExpr;
    }

    static void setLinBounds(final Expr solExpr, final Expression modExpr, final copt.Model delegateSolver,
            final List<EntryPair<ModelEntity<?>, ConstraintType>> constrInfo) {
        if (modExpr.isEqualityConstraint()) {
            SolverCOPT.addLinConstraint(delegateSolver, solExpr, Consts.EQUAL, modExpr.getLowerLimit(false, ZERO), modExpr.getName());
            constrInfo.add(EntryPair.of(modExpr, ConstraintType.EQUALITY));
        } else if (modExpr.isLowerConstraint() && modExpr.isUpperConstraint()) {
            double lb = modExpr.getLowerLimit(false, NEGATIVE_INFINITY);
            double ub = modExpr.getUpperLimit(false, POSITIVE_INFINITY);
            SolverCOPT.addLinRangeConstraint(delegateSolver, solExpr, lb, ub, modExpr.getName());
            constrInfo.add(EntryPair.of(modExpr, ConstraintType.RANGE));
        } else if (modExpr.isLowerConstraint()) {
            SolverCOPT.addLinConstraint(delegateSolver, solExpr, Consts.GREATER_EQUAL, modExpr.getLowerLimit(false, NEGATIVE_INFINITY), modExpr.getName());
            constrInfo.add(EntryPair.of(modExpr, ConstraintType.LOWER));
        } else if (modExpr.isUpperConstraint()) {
            SolverCOPT.addLinConstraint(delegateSolver, solExpr, Consts.LESS_EQUAL, modExpr.getUpperLimit(false, POSITIVE_INFINITY), modExpr.getName());
            constrInfo.add(EntryPair.of(modExpr, ConstraintType.UPPER));
        }
    }

    static void setQuadBounds(final QuadExpr solExpr, final Expression modExpr, final copt.Model delegateSolver,
            final List<EntryPair<ModelEntity<?>, ConstraintType>> constrInfo) {
        if (modExpr.isEqualityConstraint()) {
            SolverCOPT.addQuadConstraint(delegateSolver, solExpr, Consts.EQUAL, modExpr.getLowerLimit(false, ZERO), modExpr.getName());
            constrInfo.add(EntryPair.of(modExpr, ConstraintType.EQUALITY));
        } else {
            if (modExpr.isLowerConstraint()) {
                SolverCOPT.addQuadConstraint(delegateSolver, solExpr, Consts.GREATER_EQUAL, modExpr.getLowerLimit(false, NEGATIVE_INFINITY),
                        modExpr.getName());
                constrInfo.add(EntryPair.of(modExpr, ConstraintType.LOWER));
            }
            if (modExpr.isUpperConstraint()) {
                SolverCOPT.addQuadConstraint(delegateSolver, solExpr, Consts.LESS_EQUAL, modExpr.getUpperLimit(false, POSITIVE_INFINITY), modExpr.getName());
                constrInfo.add(EntryPair.of(modExpr, ConstraintType.UPPER));
            }
        }
    }

    /**
     * Interrupting the solving thread reaches COPT through {@link copt.Model#interrupt()}.
     */
    static void solveInterruptibly(final copt.Model model) throws CoptException {
        try (InterruptForwarder interrupter = InterruptForwarder.watch(() -> {
            try {
                model.interrupt();
            } catch (CoptException ignored) {
            }
        })) {
            model.solve();
        }
    }

    static State translate(final int status) {
        if (status == Status.INFEASIBLE) {
            return State.INFEASIBLE;
        } else if (status == Status.NODELIMIT || status == Status.NUMERICAL || status == Status.TIMEOUT || status == Status.ITERLIMIT) {
            return State.APPROXIMATE;
        } else if (status == Status.INF_OR_UNB) {
            return State.INVALID;
        } else if (status == Status.UNFINISHED || status == Status.INTERRUPTED) {
            return State.UNEXPLORED;
        } else if (status == Status.OPTIMAL) {
            return State.OPTIMAL;
        } else if (status == Status.UNBOUNDED) {
            return State.UNBOUNDED;
        } else {
            return State.FAILED;
        }
    }

    private final copt.Model myCOPTModel;
    private final Var[] myCOPTVariables;
    private final COPTEntityMap myEntityMap;
    private final int myIterationsAbort;
    private final int myIterationsSuffice;
    private final boolean myMaximisation;
    private final long myTimeAbortMillis;
    private final long myTimeSufficeMillis;

    SolverCOPT(final copt.Model model, final Var[] variables, final COPTEntityMap entityMap, final Optimisation.Options options, final boolean maximisation) {
        super();
        myCOPTModel = model;
        myCOPTVariables = variables;
        myEntityMap = entityMap;
        myIterationsAbort = options.iterations_abort;
        myIterationsSuffice = options.iterations_suffice;
        myTimeAbortMillis = options.time_abort;
        myTimeSufficeMillis = options.time_suffice;
        myMaximisation = maximisation;
    }

    @Override
    public void dispose() {
        UpdatableSolver.super.dispose();
        if (myCOPTModel != null) {
            myCOPTModel.dispose();
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
                return myCOPTModel.getQConstrs().getQConstr(groupIndex).get(DblInfo.Dual);
            } else {
                return myCOPTModel.getConstr(groupIndex).get(DblInfo.Dual);
            }
        } catch (CoptException cause) {
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
            return myCOPTVariables[index].get(DblInfo.RedCost);
        } catch (CoptException cause) {
            throw new RuntimeException(cause);
        }
    }

    @Override
    public Result solve(final Result kickStarter) {

        State retState = State.UNEXPLORED;
        double retValue = NaN;
        ArrayR064 retSolution = ArrayR064.make(myCOPTVariables.length);
        double[] retDuals = null;
        double[] retReducedGradient = null;

        try {

            long startMillis = System.currentTimeMillis();

            retState = this.optimize();

            if (retState == State.APPROXIMATE && myTimeSufficeMillis < myTimeAbortMillis) {
                long elapsed = System.currentTimeMillis() - startMillis;
                long remaining = myTimeAbortMillis - elapsed;
                if (remaining > 1000L) {
                    retState = this.optimizeUntilAbort(remaining);
                }
            }

            int status = myCOPTModel.getIntAttr(IntAttr.Status);
            if (status == Status.UNBOUNDED || status == Status.INF_OR_UNB) {
                return this.solveForFeasibleSolution();
            }

            if (retState.isApproximate() && this.hasSolution()) {

                retValue = myCOPTModel.getDblAttr(DblAttr.ObjVal);

                for (int i = 0; i < myCOPTVariables.length; i++) {
                    retSolution.set(i, myCOPTVariables[i].get(DblInfo.Value));
                }

                // Dual values only exist for continuous models, and are (here) not read with quadratic
                // constraints
                boolean duals = myCOPTModel.getIntAttr(IntAttr.HasLpSol) == 1 && myCOPTModel.getIntAttr(IntAttr.IsMIP) == 0
                        && myCOPTModel.getIntAttr(IntAttr.QConstrs) == 0;

                if (duals && retState.isOptimal()) {

                    // COPT's dual values are the rates of change of the objective function value with the
                    // constraint limits. For a minimisation that is minus the multipliers signed as for
                    // two-sided rows (positive when the upper limit is active), for a maximisation it is
                    // those multipliers.
                    double sign = myMaximisation ? ONE : NEG;
                    retDuals = new double[myEntityMap.getConstraintsMetaData().size()];
                    for (int i = 0; i < retDuals.length; i++) {
                        retDuals[i] = sign * myCOPTModel.getConstr(myEntityMap.getGroupIndex(i)).get(DblInfo.Dual);
                    }

                    // The reduced costs are in the model's sense
                    retReducedGradient = new double[myCOPTVariables.length];
                    for (int j = 0; j < retReducedGradient.length; j++) {
                        retReducedGradient[j] = myCOPTVariables[j].get(DblInfo.RedCost);
                    }
                }
            }

        } catch (CoptException cause) {
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

        if (index < 0 || index >= myCOPTVariables.length) {
            return false;
        }

        try {
            myCOPTVariables[index].set(DblInfo.LB, lower);
            myCOPTVariables[index].set(DblInfo.UB, upper);
            return true;
        } catch (CoptException cause) {
            return false;
        }
    }

    /**
     * The objective function value, including the constant, of a solution to this model.
     */
    private double evaluateObjective(final double[] solution) throws CoptException {
        QuadExpr objective = myCOPTModel.getQuadObjective();
        Expr linear = objective.getLinExpr();
        double retVal = myCOPTModel.getDblAttr(DblAttr.ObjConst);
        for (int i = 0, limit = (int) linear.size(); i < limit; i++) {
            retVal += linear.getCoeff(i) * solution[linear.getVar(i).getIdx()];
        }
        for (int i = 0, limit = (int) objective.size(); i < limit; i++) {
            retVal += objective.getCoeff(i) * solution[objective.getVar1(i).getIdx()] * solution[objective.getVar2(i).getIdx()];
        }
        return retVal;
    }

    /**
     * COPT's status alone does not say if there is a solution when a limit was reached. A MIP incumbent is
     * feasible, while a continuous model stopped early is at most approximate.
     */
    private State getState() throws CoptException {
        State state = SolverCOPT.translate(myCOPTModel.getIntAttr(IntAttr.Status));
        if (state == State.APPROXIMATE && myCOPTModel.getIntAttr(IntAttr.IsMIP) != 0 && myCOPTModel.getIntAttr(IntAttr.HasMipSol) != 0) {
            return State.FEASIBLE;
        }
        return state;
    }

    private boolean hasSolution() throws CoptException {
        String solution = myCOPTModel.getIntAttr(IntAttr.IsMIP) != 0 ? IntAttr.HasMipSol : IntAttr.HasLpSol;
        return myCOPTModel.getIntAttr(solution) != 0;
    }

    private State optimize() throws CoptException {
        SolverCOPT.solveInterruptibly(myCOPTModel);
        return this.getState();
    }

    /**
     * Optimise again, with the abort limits instead of the suffice limits. The model's limits are restored
     * afterwards, so that the next solve starts from the configured ones.
     */
    private State optimizeUntilAbort(final long remainingMillis) throws CoptException {
        double timeLimit = myCOPTModel.getDblParam(DblParam.TimeLimit);
        int iterationLimit = myCOPTModel.getIntParam(IntParam.BarIterLimit);
        try {
            myCOPTModel.setDblParam(DblParam.TimeLimit, remainingMillis / 1000.0);
            if (myIterationsSuffice < myIterationsAbort) {
                myCOPTModel.setIntParam(IntParam.BarIterLimit, myIterationsAbort);
            }
            return this.optimize();
        } finally {
            myCOPTModel.setDblParam(DblParam.TimeLimit, timeLimit);
            myCOPTModel.setIntParam(IntParam.BarIterLimit, iterationLimit);
        }
    }

    /**
     * COPT's unbounded status only says that the objective can improve without limit, not that there is a
     * feasible solution, which ojAlgo's does - and for a MIP COPT may not be able to tell infeasible from
     * unbounded at all. Solving a copy of the model with a zero objective finds a feasible solution, and then
     * the model is unbounded, or proves there is none.
     */
    private Result solveForFeasibleSolution() throws CoptException {
        copt.Model copy = myCOPTModel.clone();
        try {
            copy.setQuadObjective(new QuadExpr(), myMaximisation ? Consts.MAXIMIZE : Consts.MINIMIZE);
            copy.setObjConst(ZERO);
            SolverCOPT.solveInterruptibly(copy);
            String solution = copy.getIntAttr(IntAttr.IsMIP) != 0 ? IntAttr.HasMipSol : IntAttr.HasLpSol;
            if (copy.getIntAttr(solution) != 0) {
                double[] values = copy.get(DblInfo.Value, copy.getVars());
                return new Result(State.UNBOUNDED, this.evaluateObjective(values), ArrayR064.wrap(values));
            }
            State state = copy.getIntAttr(IntAttr.Status) == Status.INFEASIBLE ? State.INFEASIBLE : State.INVALID;
            return new Result(state, NaN, ArrayR064.make(myCOPTVariables.length));
        } finally {
            copy.dispose();
        }
    }

    copt.Model getCOPTModel() {
        return myCOPTModel;
    }

}
