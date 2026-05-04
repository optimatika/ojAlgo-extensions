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
package org.ojalgo.optimisation.solver.mosek;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.ojalgo.array.ArrayR064;
import org.ojalgo.netio.BasicLogger;
import org.ojalgo.optimisation.Expression;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.Optimisation;
import org.ojalgo.optimisation.Variable;
import org.ojalgo.structure.Structure1D.IntIndex;
import org.ojalgo.structure.Structure2D.IntRowColumn;
import org.ojalgo.type.ForgetfulMap;

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

            if (options.logger_appender != null) {

                Stream stream = new Stream() {

                    @Override
                    public void stream(final String message) {
                        options.logger_appender.print(message);
                    }
                };

                environment.set_Stream(streamtype.log, stream);
                task.set_Stream(streamtype.log, stream);
            }

            return task;
        }

    }

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

            Configurator configurator = options.getConfigurator(Configurator.class).orElse(DEFAULT);

            List<Variable> freeVariables = model.getFreeVariables();
            Set<IntIndex> fixedVariables = model.getFixedVariables();

            List<Expression> constraints = model.constraints().collect(Collectors.toList());

            int nbVariables = freeVariables.size();
            int nbConstraints = constraints.size();

            Task task = configurator.newTask(environment, nbConstraints, nbVariables, options);

            SolverMosek retVal = new SolverMosek(task, options);

            for (int v = 0; v < nbVariables; v++) {
                Variable tmpVariable = freeVariables.get(v);
                retVal.putVariable(v, tmpVariable);
            }

            for (int c = 0; c < nbConstraints; c++) {
                Expression tmpConstraint = constraints.get(c).compensate(fixedVariables);
                retVal.putConstraint(c, tmpConstraint, model);
            }

            Expression tmpObjective = model.objective().compensate(fixedVariables);
            retVal.putObjective(tmpObjective, model);

            retVal.setSolutionType(model);

            return retVal;
        }

        @Override
        public boolean isCapable(final ExpressionsBasedModel model) {
            return true; // Can handle any variation of an ExpressionsBasedModel
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

        Env getEnvironment() {
            return myEnvironmentCache.getCachedObject();
        }

    }

    public static final SolverMosek.Integration INTEGRATION = new Integration();

    static final Configurator DEFAULT = new Configurator();

    private final Optimisation.Options myOptions;
    private soltype mySolutionType = soltype.bas;
    private final Task myTask;

    SolverMosek(final Task task, final Optimisation.Options options) {

        super();

        myTask = task;
        myOptions = options;
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

        try {

            if (myTask.optimize() == rescode.ok) {

                solsta[] tmpSolverState = new solsta[1];
                myTask.getsolsta(mySolutionType, tmpSolverState);

                myTask.getxx(mySolutionType, tmpSolution);

                switch (tmpSolverState[0]) {
                    case optimal:
                        tmpSate = Optimisation.State.OPTIMAL;
                        tmpValue = myTask.getprimalobj(mySolutionType);
                        break;
                    case dual_infeas_cer:
                    case prim_infeas_cer:
                        tmpSate = Optimisation.State.INFEASIBLE;
                        break;
                    default:
                        tmpSate = Optimisation.State.FAILED;
                        break;
                }
            }

        } catch (Exception xcptn) {
            throw xcptn;
        }

        return new Optimisation.Result(tmpSate, tmpValue, ArrayR064.wrap(tmpSolution));
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

    void putConstraint(final int index, final Expression constraint, final ExpressionsBasedModel model) {

        Set<IntIndex> tmpLinearFactorKeys = constraint.getLinearKeySet();
        int tmpLinearSize = tmpLinearFactorKeys.size();
        if (tmpLinearSize > 0) {

            int[] tmpCols = new int[tmpLinearSize];
            double[] tmpVals = new double[tmpLinearSize];

            int i = 0;
            for (IntIndex tmpKey : tmpLinearFactorKeys) {
                int tmpRow = tmpKey.index;
                if (tmpRow >= 0) {
                    tmpCols[i] = tmpRow;
                    tmpVals[i] = constraint.doubleValue(tmpKey, true);
                } else {
                    tmpCols[i] = 0;
                    tmpVals[i] = 0.0;
                }
                i++;
            }
            myTask.putarow(index, tmpCols, tmpVals);
        }

        Set<IntRowColumn> tmpQuadraticFactorKeys = constraint.getQuadraticKeySet();
        int tmpQuadraticSize = tmpQuadraticFactorKeys.size();
        if (tmpQuadraticSize > 0) {

            int[] tmpRows = new int[tmpQuadraticSize];
            int[] tmpCols = new int[tmpQuadraticSize];
            double[] tmpVals = new double[tmpQuadraticSize];

            int i = 0;
            for (IntRowColumn tmpKey : tmpQuadraticFactorKeys) {
                int tmpRow = tmpKey.row;
                int tmpCol = tmpKey.column;
                if (tmpRow >= 0 && tmpCol >= 0) {
                    if (tmpRow == tmpCol) {
                        tmpRows[i] = tmpRow;
                        tmpCols[i] = tmpCol;
                        tmpVals[i] = 2.0 * constraint.doubleValue(tmpKey, true);
                    } else {
                        tmpRows[i] = Math.max(tmpRow, tmpCol);
                        tmpCols[i] = Math.min(tmpRow, tmpCol);
                        tmpVals[i] = constraint.doubleValue(tmpKey, true);
                    }
                } else {
                    tmpRows[i] = 0;
                    tmpCols[i] = 0;
                    tmpVals[i] = 0.0;
                }
                i++;
            }
            myTask.putqconk(index, tmpRows, tmpCols, tmpVals);
        }

        boundkey tmpBoundType = this.getBoundKey(constraint);
        double tmpLowerBound = constraint.getLowerLimit(true, Double.NEGATIVE_INFINITY);
        double tmpUpperBound = constraint.getUpperLimit(true, Double.POSITIVE_INFINITY);

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
                int tmpRow = tmpKey.index;
                if (tmpRow >= 0) {
                    tmpRows[i] = tmpRow;
                    tmpVals[i] = objective.doubleValue(tmpKey, true);
                } else {
                    tmpRows[i] = 0;
                    tmpVals[i] = 0.0;
                }
                i++;
            }
            myTask.putclist(tmpRows, tmpVals);
        }

        Set<IntRowColumn> tmpQuadraticFactorKeys = objective.getQuadraticKeySet();
        int tmpQuadraticSize = tmpQuadraticFactorKeys.size();
        if (tmpQuadraticSize > 0) {

            int[] tmpRows = new int[tmpQuadraticSize];
            int[] tmpCols = new int[tmpQuadraticSize];
            double[] tmpVals = new double[tmpQuadraticSize];

            int i = 0;
            for (IntRowColumn tmpKey : tmpQuadraticFactorKeys) {
                int tmpRow = tmpKey.row;
                int tmpCol = tmpKey.column;
                if (tmpRow >= 0 && tmpCol >= 0) {
                    if (tmpRow == tmpCol) {
                        tmpRows[i] = tmpRow;
                        tmpCols[i] = tmpCol;
                        tmpVals[i] = 2.0 * objective.doubleValue(tmpKey, true);
                    } else {
                        tmpRows[i] = Math.max(tmpRow, tmpCol);
                        tmpCols[i] = Math.min(tmpRow, tmpCol);
                        tmpVals[i] = objective.doubleValue(tmpKey, true);
                    }
                } else {
                    tmpRows[i] = 0;
                    tmpCols[i] = 0;
                    tmpVals[i] = 0.0;
                }
                i++;
            }
            myTask.putqobj(tmpRows, tmpCols, tmpVals);
        }

        myTask.putobjsense(model.getOptimisationSense() == Optimisation.Sense.MIN ? objsense.minimize : objsense.maximize);
    }

    void putVariable(final int index, final Variable variable) {

        boundkey boundType = this.getBoundKey(variable);
        double lowerBound = variable.getLowerLimit(false, Double.NEGATIVE_INFINITY);
        double upperBound = variable.getUpperLimit(false, Double.POSITIVE_INFINITY);
        variabletype variableType = variable.isInteger() ? variabletype.type_int : variabletype.type_cont;

        myTask.putvarbound(index, boundType, lowerBound, upperBound);
        myTask.putvartype(index, variableType);
    }

    void setSolutionType(final ExpressionsBasedModel model) {
        mySolutionType = model.isAnyVariableInteger() ? soltype.itg : model.isAnyExpressionQuadratic() ? soltype.itr : soltype.bas;
    }

}
