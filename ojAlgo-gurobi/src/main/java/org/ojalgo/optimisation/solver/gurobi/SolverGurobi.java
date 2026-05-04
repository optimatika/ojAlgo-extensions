/*
 * Copyright 1997-2025 Optimatika
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
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.ojalgo.array.ArrayR064;
import org.ojalgo.optimisation.Expression;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.Optimisation;
import org.ojalgo.optimisation.Variable;
import org.ojalgo.structure.Structure1D.IntIndex;
import org.ojalgo.structure.Structure2D.IntRowColumn;
import org.ojalgo.type.ForgetfulMap;

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
public final class SolverGurobi implements Optimisation.Solver {

    public static class Configurator {

        protected void configure(final GRBEnv environment, final Optimisation.Options options) {
            try {
                environment.set(GRB.IntParam.OutputFlag, 0);
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

            Configurator configurator = model.options.getConfigurator(Configurator.class).orElse(DEFAULT);

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

                    lb[v] = variable.getLowerLimit(false, NEGATIVE_INFINITY);
                    ub[v] = variable.getUpperLimit(false, POSITIVE_INFINITY);

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

                List<Expression> tmpCollect = model.constraints().map(e -> e.compensate(fixedModVars)).collect(Collectors.toList());
                for (Expression expr : tmpCollect) {

                    GRBExpr solExpr = SolverGurobi.buildExpression(expr, model, delegateVariables);

                    SolverGurobi.setBounds(solExpr, expr, delegateSolver);
                }

                GRBExpr solObj = SolverGurobi.buildExpression(modObj, model, delegateVariables);

                if (model.getOptimisationSense() == Optimisation.Sense.MAX) {
                    delegateSolver.setObjective(solObj, GRB.MAXIMIZE);
                } else {
                    delegateSolver.setObjective(solObj, GRB.MINIMIZE);
                }

                delegateSolver.update();

                return new SolverGurobi(delegateSolver);

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

        @Override
        protected Optimisation.Sense getSolverSense() {
            return null;
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

    static GRBExpr buildExpression(final Expression expression, final ExpressionsBasedModel model, final GRBVar[] vars) throws GRBException {

        GRBLinExpr linExpr = null;
        GRBQuadExpr quadExpr;
        GRBExpr retVal = null;

        if (expression.isAnyLinearFactorNonZero()) {

            retVal = linExpr = new GRBLinExpr();

            for (IntIndex key : expression.getLinearKeySet()) {

                int freeInd = model.indexOfFreeVariable(key.index);
                if (freeInd >= 0) {
                    linExpr.addTerm(expression.doubleValue(key, true), vars[freeInd]);
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
                    quadExpr.addTerm(expression.doubleValue(key, true), vars[freeRow], vars[freeCol]);
                }
            }
        }

        return retVal;
    }

    static void setBounds(final GRBExpr solExpr, final Expression modExpr, final GRBModel delegateSolver) {
        if (modExpr.isEqualityConstraint()) {
            SolverGurobi.addConstraint(delegateSolver, solExpr, GRB.EQUAL, modExpr.getLowerLimit(true, ZERO), modExpr.getName());
        } else {
            if (modExpr.isLowerConstraint()) {
                SolverGurobi.addConstraint(delegateSolver, solExpr, GRB.GREATER_EQUAL, modExpr.getLowerLimit(true, NEGATIVE_INFINITY), modExpr.getName());
            }
            if (modExpr.isUpperConstraint()) {
                SolverGurobi.addConstraint(delegateSolver, solExpr, GRB.LESS_EQUAL, modExpr.getUpperLimit(true, POSITIVE_INFINITY), modExpr.getName());
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

    private final GRBModel myGRBModel;

    SolverGurobi(final GRBModel model) {
        super();
        myGRBModel = model;
    }

    @Override
    public void dispose() {
        Solver.super.dispose();
        if (myGRBModel != null) {
            myGRBModel.dispose();
        }
    }

    @Override
    public Result solve(final Result kickStarter) {

        GRBVar[] tmpVars = myGRBModel.getVars();

        State retState = State.UNEXPLORED;
        double retValue = NaN;
        ArrayR064 retSolution = ArrayR064.make(myGRBModel.getVars().length);

        try {

            myGRBModel.optimize();

            retState = SolverGurobi.translate(myGRBModel.get(GRB.IntAttr.Status));

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

}
