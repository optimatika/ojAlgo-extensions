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

import com.gurobi.gurobi.GRB;
import com.gurobi.gurobi.GRBEnv;
import com.gurobi.gurobi.GRBException;
import com.gurobi.gurobi.GRBExpr;
import com.gurobi.gurobi.GRBLinExpr;
import com.gurobi.gurobi.GRBModel;
import com.gurobi.gurobi.GRBQuadExpr;
import com.gurobi.gurobi.GRBVar;

/**
 * https://docs.gurobi.com/projects/optimizer/en/current/reference/java/env.html#java-method-GRBEnv-start
 *
 * @author apete
 */
public final class SolverGurobi implements Optimisation.Solver, UpdatableSolver {

    public static class Configurator {

        protected void configure(final GRBEnv environment, final Optimisation.Options options) {
            try {
                environment.set(GRB.IntParam.OutputFlag, 0);

                long effectiveTimeMillis = Math.min(options.time_suffice, options.time_abort);
                if (effectiveTimeMillis > 0L && effectiveTimeMillis < Long.MAX_VALUE) {
                    environment.set(GRB.DoubleParam.TimeLimit, effectiveTimeMillis / 1000.0);
                }

                int effectiveIterations = Math.min(options.iterations_suffice, options.iterations_abort);
                if (effectiveIterations > 0 && effectiveIterations < Integer.MAX_VALUE) {
                    environment.set(GRB.DoubleParam.IterationLimit, effectiveIterations);
                }

                int parallelism = options.getParallelism();
                if (parallelism > 0) {
                    environment.set(GRB.IntParam.Threads, parallelism);
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

        private final List<EntryPair<ModelEntity<?>, ConstraintType>> myConstraints;
        private final int myNumEqualityConstraints;
        private final int myNumInequalityConstraints;
        private final int myNumVars;
        private final int[] mySolverToModel;

        GurobiEntityMap(final int numVars, final ExpressionsBasedModel model, final List<Variable> freeVariables,
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

    static final class Integration extends ExpressionsBasedModel.Integration<SolverGurobi> {

        private static final boolean ADJUSTED_BOUNDS = false;
        private static final boolean ADJUSTED_CONSTRAINTS = true;
        private static final boolean ADJUSTED_OBJECTIVE = true;

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

                    type[v] = GRB.CONTINUOUS;
                    if (variable.isBinary()) {
                        type[v] = GRB.BINARY;
                    } else if (variable.isInteger()) {
                        type[v] = GRB.INTEGER;
                    }

                    name[v] = variable.getName();
                }

                delegateSolver.addVars(lb, ub, obj, type, name);
                delegateSolver.update();

                GRBVar[] delegateVariables = delegateSolver.getVars();

                List<EntryPair<ModelEntity<?>, ConstraintType>> constrInfo = new ArrayList<>();

                List<Expression> tmpCollect = model.constraints().map(e -> e.compensate(fixedModVars)).collect(Collectors.toList());
                for (Expression expr : tmpCollect) {

                    GRBExpr solExpr = SolverGurobi.buildExpression(expr, model, delegateVariables, ADJUSTED_CONSTRAINTS);

                    SolverGurobi.setBounds(solExpr, expr, delegateSolver, ADJUSTED_CONSTRAINTS, constrInfo);
                }

                GRBExpr solObj = SolverGurobi.buildExpression(modObj, model, delegateVariables, ADJUSTED_OBJECTIVE);

                if (model.getOptimisationSense() == Optimisation.Sense.MAX) {
                    delegateSolver.setObjective(solObj, GRB.MAXIMIZE);
                } else {
                    delegateSolver.setObjective(solObj, GRB.MINIMIZE);
                }

                delegateSolver.update();

                GurobiEntityMap entityMap = new GurobiEntityMap(numberOfVariables, model, freeModVars, constrInfo);

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

        @Override
        public Result toModelState(final Result solverState, final ExpressionsBasedModel model) {
            return ExpressionsBasedModel.Integration.expandFreeToFull(solverState, model, ArrayR064.FACTORY);
        }

        @Override
        public Result toSolverState(final Result modelState, final ExpressionsBasedModel model) {
            return ExpressionsBasedModel.Integration.reduceFullToFree(modelState, model, ArrayR064.FACTORY);
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
        protected int getIndexInSolver(final ExpressionsBasedModel model, final Variable variable) {
            return ExpressionsBasedModel.Integration.getIndexOfFreeInSolver(model, variable);
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

    static GRBExpr buildExpression(final Expression expression, final ExpressionsBasedModel model, final GRBVar[] vars, final boolean adjusted)
            throws GRBException {

        GRBLinExpr linExpr = null;
        GRBQuadExpr quadExpr;
        GRBExpr retVal = null;

        if (expression.isAnyLinearFactorNonZero()) {

            retVal = linExpr = new GRBLinExpr();

            for (IntIndex key : expression.getLinearKeySet()) {

                int freeInd = model.indexOfFreeVariable(key.index);
                if (freeInd >= 0) {
                    linExpr.addTerm(expression.doubleValue(key, adjusted), vars[freeInd]);
                }
            }
        }

        if (expression.isAnyQuadraticFactorNonZero()) {

            quadExpr = new GRBQuadExpr();
            if (linExpr != null) {
                quadExpr.add(linExpr);
            }
            retVal = quadExpr;

            for (IntRowColumn key : expression.getQuadraticKeySet()) {

                int freeRow = model.indexOfFreeVariable(key.row);
                int freeCol = model.indexOfFreeVariable(key.column);
                if (freeRow >= 0 && freeCol >= 0) {
                    quadExpr.addTerm(expression.doubleValue(key, adjusted), vars[freeRow], vars[freeCol]);
                }
            }
        }

        return retVal;
    }

    static void setBounds(final GRBExpr solExpr, final Expression modExpr, final GRBModel delegateSolver, final boolean adjusted,
            final List<EntryPair<ModelEntity<?>, ConstraintType>> constrInfo) {
        if (modExpr.isEqualityConstraint()) {
            SolverGurobi.addConstraint(delegateSolver, solExpr, GRB.EQUAL, modExpr.getLowerLimit(adjusted, ZERO), modExpr.getName());
            constrInfo.add(EntryPair.of(modExpr, ConstraintType.EQUALITY));
        } else {
            if (modExpr.isLowerConstraint()) {
                SolverGurobi.addConstraint(delegateSolver, solExpr, GRB.GREATER_EQUAL, modExpr.getLowerLimit(adjusted, NEGATIVE_INFINITY), modExpr.getName());
                constrInfo.add(EntryPair.of(modExpr, ConstraintType.LOWER));
            }
            if (modExpr.isUpperConstraint()) {
                SolverGurobi.addConstraint(delegateSolver, solExpr, GRB.LESS_EQUAL, modExpr.getUpperLimit(adjusted, POSITIVE_INFINITY), modExpr.getName());
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
                // Perhaps a problem here – ojAlgo expects a feasible solution with this state
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
            return myGRBModel.getConstr(index).get(GRB.DoubleAttr.Pi);
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

    @Override
    public Result solve(final Result kickStarter) {

        GRBVar[] tmpVars = myGRBModel.getVars();

        State retState = State.UNEXPLORED;
        double retValue = NaN;
        ArrayR064 retSolution = ArrayR064.make(myGRBModel.getVars().length);

        try {

            long startMillis = System.currentTimeMillis();

            // Gurobi's own way of being stopped from elsewhere, which is what makes an interrupt of the
            // solving thread reach it.
            try (InterruptForwarder interrupter = InterruptForwarder.watch(myGRBModel::terminate)) {
                myGRBModel.optimize();
            }

            retState = SolverGurobi.translate(myGRBModel.get(GRB.IntAttr.Status));

            if (!retState.isFeasible() && myTimeSufficeMillis < myTimeAbortMillis) {
                long elapsed = System.currentTimeMillis() - startMillis;
                long remaining = myTimeAbortMillis - elapsed;
                if (remaining > 1000L) {
                    myGRBModel.getEnv().set(GRB.DoubleParam.TimeLimit, remaining / 1000.0);
                    if (myIterationsSuffice < myIterationsAbort) {
                        myGRBModel.getEnv().set(GRB.DoubleParam.IterationLimit, myIterationsAbort);
                    }

                    try (InterruptForwarder interrupter = InterruptForwarder.watch(myGRBModel::terminate)) {
                        myGRBModel.optimize();
                    }
                    retState = SolverGurobi.translate(myGRBModel.get(GRB.IntAttr.Status));
                }
            }

            if (retState.isFeasible()) {

                retValue = myGRBModel.get(GRB.DoubleAttr.ObjVal);

                for (int i = 0; i < tmpVars.length; i++) {
                    retSolution.set(i, tmpVars[i].get(GRB.DoubleAttr.X));
                }
            }

        } catch (GRBException cause) {
            throw new RuntimeException(cause);
        }

        return new Result(retState, retValue, retSolution);
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

}
