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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.ojalgo.array.ArrayR064;
import org.ojalgo.concurrent.InterruptForwarder;
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

        private final List<EntryPair<ModelEntity<?>, ConstraintType>> myConstraints;
        private final int myNumEqualityConstraints;
        private final int myNumInequalityConstraints;
        private final int myNumVars;
        private final int[] mySolverToModel;

        COPTEntityMap(final int numVars, final ExpressionsBasedModel model, final List<Variable> freeVariables,
                final List<EntryPair<ModelEntity<?>, ConstraintType>> constraints) {
            myNumVars = numVars;
            mySolverToModel = new int[numVars];
            for (int i = 0; i < numVars; i++) {
                mySolverToModel[i] = model.indexOf(freeVariables.get(i));
            }
            myConstraints = constraints;
            int eq = 0;
            int ineq = 0;
            for (EntryPair<ModelEntity<?>, ConstraintType> entry : constraints) {
                if (entry.getValue() == ConstraintType.EQUALITY) {
                    eq++;
                } else {
                    ineq++;
                }
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
            return myConstraints.get(idc);
        }

        @Override
        public EntryPair<ModelEntity<?>, ConstraintType> getSlack(final int ids) {
            return null;
        }

        @Override
        public int indexOf(final int solverIndex) {
            if (solverIndex >= 0 && solverIndex < mySolverToModel.length) {
                return mySolverToModel[solverIndex];
            }
            return -1;
        }

        @Override
        public boolean isNegated(final int solverIndex) {
            return false;
        }

    }

    static final class Integration extends ExpressionsBasedModel.Integration<SolverCOPT> {

        private static final boolean ADJUSTED_BOUNDS = false;
        private static final boolean ADJUSTED_CONSTRAINTS = true;
        private static final boolean ADJUSTED_OBJECTIVE = true;

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

                List<Variable> freeModVars = model.getFreeVariables();
                Set<IntIndex> fixedModVars = model.getFixedVariables();

                Expression modObj = model.objective().compensate(fixedModVars);

                int numberOfVariables = freeModVars.size();

                double[] lb = new double[numberOfVariables];
                double[] ub = new double[numberOfVariables];
                double[] obj = new double[numberOfVariables];
                char[] type = new char[numberOfVariables];
                String[] name = new String[numberOfVariables];

                for (int v = 0; v < numberOfVariables; v++) {
                    Variable variable = freeModVars.get(v);

                    lb[v] = variable.getLowerLimit(ADJUSTED_BOUNDS, NEGATIVE_INFINITY);
                    ub[v] = variable.getUpperLimit(ADJUSTED_BOUNDS, POSITIVE_INFINITY);

                    BigDecimal weight = variable.getContributionWeight();
                    obj[v] = weight != null ? weight.doubleValue() : ZERO;

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

                List<Expression> tmpCollect = model.constraints().map(e -> e.compensate(fixedModVars)).collect(Collectors.toList());
                for (Expression expr : tmpCollect) {
                    SolverCOPT.buildAndConstrain(expr, model, delegateVariables, ADJUSTED_CONSTRAINTS, delegateSolver, constrInfo);
                }

                int sense = model.getOptimisationSense() == Optimisation.Sense.MAX ? Consts.MAXIMIZE : Consts.MINIMIZE;

                if (modObj.isAnyQuadraticFactorNonZero()) {
                    QuadExpr solObj = SolverCOPT.buildQuadExpression(modObj, model, delegateVariables, ADJUSTED_OBJECTIVE);
                    delegateSolver.setQuadObjective(solObj, sense);
                } else {
                    Expr solObj = SolverCOPT.buildLinExpression(modObj, model, delegateVariables, ADJUSTED_OBJECTIVE);
                    delegateSolver.setObjective(solObj, sense);
                }

                COPTEntityMap entityMap = new COPTEntityMap(numberOfVariables, model, freeModVars, constrInfo);

                return new SolverCOPT(delegateSolver, delegateVariables, entityMap, model.options);

            } catch (CoptException exception) {
                exception.printStackTrace();
                return null;
            }

        }

        @Override
        public boolean isCapable(final ExpressionsBasedModel model) {
            return true;
        }

        @Override
        public Result toModelState(final Result solverState, final ExpressionsBasedModel model) {
            return ExpressionsBasedModel.Integration.expandFreeToFull(solverState, model, ArrayR064.FACTORY);
        }

        @Override
        public Result toSolverState(final Result modelState, final ExpressionsBasedModel model) {
            return ExpressionsBasedModel.Integration.reduceFullToFree(modelState, model, ArrayR064.FACTORY);
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
        protected int getIndexInSolver(final ExpressionsBasedModel model, final Variable variable) {
            return ExpressionsBasedModel.Integration.getIndexOfFreeInSolver(model, variable);
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

    static void buildAndConstrain(final Expression expression, final ExpressionsBasedModel model, final Var[] vars, final boolean adjusted,
            final copt.Model delegateSolver, final List<EntryPair<ModelEntity<?>, ConstraintType>> constrInfo) throws CoptException {

        Expr linExpr = null;

        if (expression.isAnyLinearFactorNonZero()) {

            linExpr = new Expr();

            for (IntIndex key : expression.getLinearKeySet()) {
                int freeInd = model.indexOfFreeVariable(key.index);
                if (freeInd >= 0) {
                    linExpr.addTerm(vars[freeInd], expression.doubleValue(key, adjusted));
                }
            }
        }

        if (expression.isAnyQuadraticFactorNonZero()) {

            QuadExpr quadExpr = new QuadExpr();
            if (linExpr != null) {
                quadExpr.addLinExpr(linExpr, ONE);
            }

            for (IntRowColumn key : expression.getQuadraticKeySet()) {
                int freeRow = model.indexOfFreeVariable(key.row);
                int freeCol = model.indexOfFreeVariable(key.column);
                if (freeRow >= 0 && freeCol >= 0) {
                    quadExpr.addTerm(vars[freeRow], vars[freeCol], expression.doubleValue(key, adjusted));
                }
            }

            SolverCOPT.setQuadBounds(quadExpr, expression, delegateSolver, adjusted, constrInfo);

        } else if (linExpr != null) {

            SolverCOPT.setLinBounds(linExpr, expression, delegateSolver, adjusted, constrInfo);
        }
    }

    static Expr buildLinExpression(final Expression expression, final ExpressionsBasedModel model, final Var[] vars, final boolean adjusted) {

        Expr linExpr = new Expr();

        if (expression.isAnyLinearFactorNonZero()) {
            for (IntIndex key : expression.getLinearKeySet()) {
                int freeInd = model.indexOfFreeVariable(key.index);
                if (freeInd >= 0) {
                    linExpr.addTerm(vars[freeInd], expression.doubleValue(key, adjusted));
                }
            }
        }

        return linExpr;
    }

    static QuadExpr buildQuadExpression(final Expression expression, final ExpressionsBasedModel model, final Var[] vars, final boolean adjusted) {

        QuadExpr quadExpr = new QuadExpr();

        if (expression.isAnyLinearFactorNonZero()) {
            Expr linExpr = SolverCOPT.buildLinExpression(expression, model, vars, adjusted);
            quadExpr.addLinExpr(linExpr, ONE);
        }

        if (expression.isAnyQuadraticFactorNonZero()) {
            for (IntRowColumn key : expression.getQuadraticKeySet()) {
                int freeRow = model.indexOfFreeVariable(key.row);
                int freeCol = model.indexOfFreeVariable(key.column);
                if (freeRow >= 0 && freeCol >= 0) {
                    quadExpr.addTerm(vars[freeRow], vars[freeCol], expression.doubleValue(key, adjusted));
                }
            }
        }

        return quadExpr;
    }

    static void setLinBounds(final Expr solExpr, final Expression modExpr, final copt.Model delegateSolver, final boolean adjusted,
            final List<EntryPair<ModelEntity<?>, ConstraintType>> constrInfo) {
        if (modExpr.isEqualityConstraint()) {
            SolverCOPT.addLinConstraint(delegateSolver, solExpr, Consts.EQUAL, modExpr.getLowerLimit(adjusted, ZERO), modExpr.getName());
            constrInfo.add(EntryPair.of(modExpr, ConstraintType.EQUALITY));
        } else if (modExpr.isLowerConstraint() && modExpr.isUpperConstraint()) {
            double lb = modExpr.getLowerLimit(adjusted, NEGATIVE_INFINITY);
            double ub = modExpr.getUpperLimit(adjusted, POSITIVE_INFINITY);
            SolverCOPT.addLinRangeConstraint(delegateSolver, solExpr, lb, ub, modExpr.getName());
            constrInfo.add(EntryPair.of(modExpr, ConstraintType.RANGE));
        } else if (modExpr.isLowerConstraint()) {
            SolverCOPT.addLinConstraint(delegateSolver, solExpr, Consts.GREATER_EQUAL, modExpr.getLowerLimit(adjusted, NEGATIVE_INFINITY), modExpr.getName());
            constrInfo.add(EntryPair.of(modExpr, ConstraintType.LOWER));
        } else if (modExpr.isUpperConstraint()) {
            SolverCOPT.addLinConstraint(delegateSolver, solExpr, Consts.LESS_EQUAL, modExpr.getUpperLimit(adjusted, POSITIVE_INFINITY), modExpr.getName());
            constrInfo.add(EntryPair.of(modExpr, ConstraintType.UPPER));
        }
    }

    static void setQuadBounds(final QuadExpr solExpr, final Expression modExpr, final copt.Model delegateSolver, final boolean adjusted,
            final List<EntryPair<ModelEntity<?>, ConstraintType>> constrInfo) {
        if (modExpr.isEqualityConstraint()) {
            SolverCOPT.addQuadConstraint(delegateSolver, solExpr, Consts.EQUAL, modExpr.getLowerLimit(adjusted, ZERO), modExpr.getName());
            constrInfo.add(EntryPair.of(modExpr, ConstraintType.EQUALITY));
        } else {
            if (modExpr.isLowerConstraint()) {
                SolverCOPT.addQuadConstraint(delegateSolver, solExpr, Consts.GREATER_EQUAL, modExpr.getLowerLimit(adjusted, NEGATIVE_INFINITY),
                        modExpr.getName());
                constrInfo.add(EntryPair.of(modExpr, ConstraintType.LOWER));
            }
            if (modExpr.isUpperConstraint()) {
                SolverCOPT.addQuadConstraint(delegateSolver, solExpr, Consts.LESS_EQUAL, modExpr.getUpperLimit(adjusted, POSITIVE_INFINITY), modExpr.getName());
                constrInfo.add(EntryPair.of(modExpr, ConstraintType.UPPER));
            }
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
    private final long myTimeAbortMillis;
    private final long myTimeSufficeMillis;

    SolverCOPT(final copt.Model model, final Var[] variables, final COPTEntityMap entityMap, final Optimisation.Options options) {
        super();
        myCOPTModel = model;
        myCOPTVariables = variables;
        myEntityMap = entityMap;
        myIterationsAbort = options.iterations_abort;
        myIterationsSuffice = options.iterations_suffice;
        myTimeAbortMillis = options.time_abort;
        myTimeSufficeMillis = options.time_suffice;
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
            return myCOPTModel.getConstr(index).get(DblInfo.Dual);
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

        try {

            long startMillis = System.currentTimeMillis();

            try (InterruptForwarder interrupter = InterruptForwarder.watch(() -> {
                try {
                    myCOPTModel.interrupt();
                } catch (CoptException ignored) {
                }
            })) {
                myCOPTModel.solve();
            }

            retState = SolverCOPT.translate(myCOPTModel.getIntAttr(IntAttr.Status));

            if (!retState.isFeasible() && myTimeSufficeMillis < myTimeAbortMillis) {
                long elapsed = System.currentTimeMillis() - startMillis;
                long remaining = myTimeAbortMillis - elapsed;
                if (remaining > 1000L) {
                    myCOPTModel.setDblParam(DblParam.TimeLimit, remaining / 1000.0);
                    if (myIterationsSuffice < myIterationsAbort) {
                        myCOPTModel.setIntParam(IntParam.BarIterLimit, myIterationsAbort);
                    }

                    try (InterruptForwarder interrupter = InterruptForwarder.watch(() -> {
                        try {
                            myCOPTModel.interrupt();
                        } catch (CoptException ignored) {
                        }
                    })) {
                        myCOPTModel.solve();
                    }
                    retState = SolverCOPT.translate(myCOPTModel.getIntAttr(IntAttr.Status));
                }
            }

            if (retState.isFeasible() && retState != State.UNBOUNDED) {

                retValue = myCOPTModel.getDblAttr(DblAttr.ObjVal);

                for (int i = 0; i < myCOPTVariables.length; i++) {
                    retSolution.set(i, myCOPTVariables[i].get(DblInfo.Value));
                }
            }

        } catch (CoptException cause) {
            throw new RuntimeException(cause);
        }

        return new Result(retState, retValue, retSolution);
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

}
