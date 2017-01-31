/*
 * Copyright 1997-2015 Optimatika (www.optimatika.se)
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
package org.ojalgo.optimisation.external;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.ojalgo.access.IntIndex;
import org.ojalgo.access.IntRowColumn;
import org.ojalgo.array.Primitive64Array;
import org.ojalgo.netio.BasicLogger.Printer;
import org.ojalgo.netio.CharacterRing;
import org.ojalgo.netio.CharacterRing.PrinterBuffer;
import org.ojalgo.optimisation.Expression;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.Optimisation;
import org.ojalgo.optimisation.Variable;

import mosek.Env;
import mosek.Env.boundkey;
import mosek.Env.objsense;
import mosek.Env.rescode;
import mosek.Env.solsta;
import mosek.Env.soltype;
import mosek.Env.streamtype;
import mosek.Env.variabletype;
import mosek.Stream;
import mosek.Task;

public final class SolverMosek implements Optimisation.Solver {

    static final class Environment {

        private final Env myEnv = new Env();
        private final PrinterBuffer myLog = new CharacterRing().asPrinter();
        private final Stream myStream = new Stream() {

            @Override
            public void stream(final String message) {
                Environment.this.printToLog(message);
            }
        };

        Environment() {

            super();

            myEnv.set_Stream(streamtype.log, myStream);
        }

        @Override
        protected final void finalize() throws Throwable {

            if (myEnv != null) {
                myEnv.dispose();
            }

            super.finalize();
        }

        void flushLog(final Printer target) {
            myLog.flush(target);
        }

        SolverMosek makeSolver(final int numberOfConstraints, final int numberOfVariables) {

            final Task tmpTask = new Task(myEnv, numberOfConstraints, numberOfVariables);

            tmpTask.appendcons(numberOfConstraints);
            tmpTask.appendvars(numberOfVariables);

            tmpTask.set_Stream(streamtype.log, myStream);

            return new SolverMosek(tmpTask);
        }

        void printToLog(final Object message) {
            myLog.print(message);
        }

    }

    public static final ExpressionsBasedModel.Integration<SolverMosek> INTEGRATION = new ExpressionsBasedModel.Integration<SolverMosek>() {

        public SolverMosek build(final ExpressionsBasedModel model) {

            final List<Variable> tmpFreeVariables = model.getFreeVariables();
            final Set<IntIndex> tmpFixedVariables = model.getFixedVariables();

            final List<Expression> tmpConstraints = model.constraints().collect(Collectors.toList());

            final int tmpNumberOfVariables = tmpFreeVariables.size();
            final int tmpNumberOfConstraints = tmpConstraints.size();

            final SolverMosek retVal = ENVIRONMENT.makeSolver(tmpNumberOfConstraints, tmpNumberOfVariables);

            for (int v = 0; v < tmpNumberOfVariables; v++) {
                final Variable tmpVariable = tmpFreeVariables.get(v);
                retVal.putVariable(v, tmpVariable);
            }

            for (int c = 0; c < tmpNumberOfConstraints; c++) {
                final Expression tmpConstraint = tmpConstraints.get(c).compensate(tmpFixedVariables);
                retVal.putConstraint(c, tmpConstraint, model);
            }

            final Expression tmpObjective = model.objective().compensate(tmpFixedVariables);
            retVal.putObjective(tmpObjective, model);

            retVal.setSolutionType(model);

            return retVal;
        }

        public boolean isCapable(final ExpressionsBasedModel model) {
            return true; // Can handle any variation of an ExpressionsBasedModel
        }

    };

    static final Environment ENVIRONMENT = new Environment();

    private soltype mySolutionType = soltype.bas;
    private final Task myTask;

    SolverMosek(final Task task) {

        super();

        myTask = task;
    }

    public void dispose() {

        Solver.super.dispose();

        if (myTask != null) {
            myTask.dispose();
        }
    }

    public Result solve(final Result kickStarter) {

        final int tmpNumberOfVariables = myTask.getnumvar();

        Optimisation.State tmpSate = Optimisation.State.FAILED;
        double tmpValue = Double.NaN;
        final double[] tmpSolution = new double[tmpNumberOfVariables];

        try {

            if (myTask.optimize() == rescode.ok) {

                final solsta[] tmpSolverState = new solsta[1];
                myTask.getsolsta(mySolutionType, tmpSolverState);

                myTask.getxx(mySolutionType, tmpSolution);

                switch (tmpSolverState[0]) {
                case optimal:
                case near_optimal:
                    tmpSate = Optimisation.State.OPTIMAL;
                    tmpValue = myTask.getprimalobj(mySolutionType);
                    break;
                case dual_infeas_cer:
                case prim_infeas_cer:
                case near_dual_infeas_cer:
                case near_prim_infeas_cer:
                    tmpSate = Optimisation.State.INFEASIBLE;
                    break;
                default:
                    tmpSate = Optimisation.State.FAILED;
                    break;
                }
            }

        } catch (final Exception xcptn) {
            throw xcptn;
        }

        return new Optimisation.Result(tmpSate, tmpValue, Primitive64Array.wrap(tmpSolution));
    }

    private boundkey getBoundKey(final Optimisation.Constraint modelEntity) {

        if (modelEntity.getLowerLimit() != null) {
            if (modelEntity.getUpperLimit() != null) {
                if (modelEntity.getLowerLimit().compareTo(modelEntity.getUpperLimit()) == 0) {
                    return boundkey.fx;
                } else {
                    return boundkey.ra;
                }
            } else {
                return boundkey.lo;
            }
        } else {
            if (modelEntity.getUpperLimit() != null) {
                return boundkey.up;
            } else {
                return boundkey.fr;
            }
        }
    }

    @Override
    protected void finalize() throws Throwable {

        this.dispose();

        super.finalize();
    }

    void configure(final Optimisation.Options options) {

        myTask.putdouparam(Env.dparam.mio_max_time, options.time_abort);

        final Object verbose = options.debug_solver;

        if ((verbose != null) && (verbose instanceof Number)) {
            final Number number = (Number) verbose;
            final int value = number.intValue();
            if (value == 0) {
                myTask.putintparam(Env.iparam.log, 0);
            } else if (value > 0) {
                myTask.putintparam(Env.iparam.log, 1);
            }
        }

    }

    void putConstraint(final int index, final Expression constraint, final ExpressionsBasedModel model) {

        final Set<IntIndex> tmpLinearFactorKeys = constraint.getLinearKeySet();
        final int tmpLinearSize = tmpLinearFactorKeys.size();
        if (tmpLinearSize > 0) {

            final int[] tmpCols = new int[tmpLinearSize];
            final double[] tmpVals = new double[tmpLinearSize];

            int i = 0;
            for (final IntIndex tmpKey : tmpLinearFactorKeys) {
                final int tmpRow = tmpKey.index;
                if (tmpRow >= 0) {
                    tmpCols[i] = tmpRow;
                    tmpVals[i] = constraint.getAdjustedLinearFactor(tmpKey);
                } else {
                    tmpCols[i] = 0;
                    tmpVals[i] = 0.0;
                }
                i++;
            }
            myTask.putarow(index, tmpCols, tmpVals);
        }

        final Set<IntRowColumn> tmpQuadraticFactorKeys = constraint.getQuadraticKeySet();
        final int tmpQuadraticSize = tmpQuadraticFactorKeys.size();
        if (tmpQuadraticSize > 0) {

            final int[] tmpRows = new int[tmpQuadraticSize];
            final int[] tmpCols = new int[tmpQuadraticSize];
            final double[] tmpVals = new double[tmpQuadraticSize];

            int i = 0;
            for (final IntRowColumn tmpKey : tmpQuadraticFactorKeys) {
                final int tmpRow = tmpKey.row;
                final int tmpCol = tmpKey.column;
                if ((tmpRow >= 0) && (tmpCol >= 0)) {
                    if (tmpRow == tmpCol) {
                        tmpRows[i] = tmpRow;
                        tmpCols[i] = tmpCol;
                        tmpVals[i] = 2.0 * constraint.getAdjustedQuadraticFactor(tmpKey);
                    } else {
                        tmpRows[i] = Math.max(tmpRow, tmpCol);
                        tmpCols[i] = Math.min(tmpRow, tmpCol);
                        tmpVals[i] = constraint.getAdjustedQuadraticFactor(tmpKey);
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

        final boundkey tmpBoundType = this.getBoundKey(constraint);
        final double tmpLowerBound = constraint.getAdjustedLowerLimit();
        final double tmpUpperBound = constraint.getAdjustedUpperLimit();

        myTask.putconbound(index, tmpBoundType, tmpLowerBound, tmpUpperBound);
    }

    void putObjective(final Expression objective, final ExpressionsBasedModel model) {

        final Set<IntIndex> tmpLinearFactorKeys = objective.getLinearKeySet();
        final int tmpLinearSize = tmpLinearFactorKeys.size();
        if (tmpLinearSize > 0) {

            final int[] tmpRows = new int[tmpLinearSize];
            final double[] tmpVals = new double[tmpLinearSize];

            int i = 0;
            for (final IntIndex tmpKey : tmpLinearFactorKeys) {
                final int tmpRow = tmpKey.index;
                if (tmpRow >= 0) {
                    tmpRows[i] = tmpRow;
                    tmpVals[i] = objective.getAdjustedLinearFactor(tmpKey);
                } else {
                    tmpRows[i] = 0;
                    tmpVals[i] = 0.0;
                }
                i++;
            }
            myTask.putclist(tmpRows, tmpVals);
        }

        final Set<IntRowColumn> tmpQuadraticFactorKeys = objective.getQuadraticKeySet();
        final int tmpQuadraticSize = tmpQuadraticFactorKeys.size();
        if (tmpQuadraticSize > 0) {

            final int[] tmpRows = new int[tmpQuadraticSize];
            final int[] tmpCols = new int[tmpQuadraticSize];
            final double[] tmpVals = new double[tmpQuadraticSize];

            int i = 0;
            for (final IntRowColumn tmpKey : tmpQuadraticFactorKeys) {
                final int tmpRow = tmpKey.row;
                final int tmpCol = tmpKey.column;
                if ((tmpRow >= 0) && (tmpCol >= 0)) {
                    if (tmpRow == tmpCol) {
                        tmpRows[i] = tmpRow;
                        tmpCols[i] = tmpCol;
                        tmpVals[i] = 2.0 * objective.getAdjustedQuadraticFactor(tmpKey);
                    } else {
                        tmpRows[i] = Math.max(tmpRow, tmpCol);
                        tmpCols[i] = Math.min(tmpRow, tmpCol);
                        tmpVals[i] = objective.getAdjustedQuadraticFactor(tmpKey);
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

        myTask.putobjsense(model.isMinimisation() ? objsense.minimize : objsense.maximize);
    }

    void putVariable(final int index, final Variable variable) {

        final boundkey tmpBoundType = this.getBoundKey(variable);
        final double tmpLowerBound = variable.getAdjustedLowerLimit();
        final double tmpUpperBound = variable.getAdjustedUpperLimit();
        final variabletype tmpVariableType = variable.isInteger() ? variabletype.type_int : variabletype.type_cont;

        myTask.putvarbound(index, tmpBoundType, tmpLowerBound, tmpUpperBound);
        myTask.putvartype(index, tmpVariableType);
    }

    void setSolutionType(final ExpressionsBasedModel model) {
        mySolutionType = model.isAnyVariableInteger() ? soltype.itg : (model.isAnyExpressionQuadratic() ? soltype.itr : soltype.bas);
    }

}
