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

import static gurobi.GRB.*;
import static org.ojalgo.constant.PrimitiveMath.*;

import java.util.Set;
import java.util.stream.Collectors;

import org.ojalgo.access.Structure1D.IntIndex;
import org.ojalgo.access.Structure2D.IntRowColumn;
import org.ojalgo.array.Primitive64Array;
import org.ojalgo.netio.CharacterRing;
import org.ojalgo.netio.CharacterRing.PrinterBuffer;
import org.ojalgo.optimisation.Expression;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.Optimisation;
import org.ojalgo.optimisation.Variable;

import gurobi.GRB.DoubleAttr;
import gurobi.GRB.IntAttr;
import gurobi.GRB.Status;
import gurobi.GRBEnv;
import gurobi.GRBException;
import gurobi.GRBExpr;
import gurobi.GRBLinExpr;
import gurobi.GRBModel;
import gurobi.GRBQuadExpr;
import gurobi.GRBVar;

public final class SolverGurobi implements Optimisation.Solver {

    static final class Environment {

        private final GRBEnv myGRBEnv;
        private final PrinterBuffer myLog = new CharacterRing().asPrinter();

        Environment() {

            super();

            GRBEnv tmpGRBEnv;
            try {
                tmpGRBEnv = new GRBEnv();
            } catch (final GRBException anException) {
                tmpGRBEnv = null;
            }

            myGRBEnv = tmpGRBEnv;
        }

        @Override
        protected final void finalize() throws Throwable {

            if (myGRBEnv != null) {
                myGRBEnv.dispose();
            }

            super.finalize();
        }

        final GRBEnv getGRBEnv() {
            return myGRBEnv;
        }

    }

    public static final ExpressionsBasedModel.Integration<SolverGurobi> INTEGRATION = new ExpressionsBasedModel.Integration<SolverGurobi>() {

        public SolverGurobi build(final ExpressionsBasedModel model) {

            try {

                final GRBModel tmpModel = new GRBModel(ENVIRONMENT.getGRBEnv());

                for (final Variable tmpVariable : model.getVariables()) {

                    final double tmpLB = tmpVariable.getAdjustedLowerLimit();
                    final double tmpUB = tmpVariable.getAdjustedUpperLimit();
                    final String tmpName = tmpVariable.getName();
                    char tmpType = CONTINUOUS;
                    if (tmpVariable.isBinary()) {
                        tmpType = BINARY;
                    } else if (tmpVariable.isInteger()) {
                        tmpType = INTEGER;
                    }

                    tmpModel.addVar(tmpLB, tmpUB, ZERO, tmpType, tmpName);
                }
                tmpModel.update();
                final GRBVar[] tmpVars = tmpModel.getVars();

                final Set<IntIndex> tmpFixedVariables = model.getFixedVariables();
                for (final Expression tmpExpression : model.constraints().collect(Collectors.toList())) {
                    final Expression tmpCompensated = tmpExpression.compensate(tmpFixedVariables);

                    final GRBExpr tmpExpr = this.buildExpression(tmpCompensated, model, tmpVars);

                    if (tmpExpression.isEqualityConstraint()) {
                        this.addConstraint(tmpModel, tmpExpr, EQUAL, tmpCompensated.getAdjustedLowerLimit(), tmpCompensated.getName());
                    } else {
                        if (tmpExpression.isLowerConstraint()) {
                            this.addConstraint(tmpModel, tmpExpr, LESS_EQUAL, tmpCompensated.getAdjustedLowerLimit(), tmpCompensated.getName());
                        }
                        if (tmpExpression.isUpperConstraint()) {
                            this.addConstraint(tmpModel, tmpExpr, GREATER_EQUAL, tmpCompensated.getAdjustedUpperLimit(), tmpCompensated.getName());
                        }
                    }
                }

                final Expression tmpObjective = model.objective().compensate(tmpFixedVariables);
                final GRBExpr tmpExpr = this.buildExpression(tmpObjective, model, tmpVars);
                if (model.isMaximisation()) {
                    tmpModel.setObjective(tmpExpr, MAXIMIZE);
                } else {
                    tmpModel.setObjective(tmpExpr, MINIMIZE);
                }

                return new SolverGurobi(tmpModel);

            } catch (final GRBException exception) {
                exception.printStackTrace();
                return null;
            }

        }

        public boolean isCapable(final ExpressionsBasedModel model) {
            return true;
        }

        void addConstraint(final GRBModel model, final GRBExpr expr, final char sense, final double rhs, final String name) {
            try {
                if (expr instanceof GRBQuadExpr) {
                    model.addQConstr((GRBQuadExpr) expr, sense, rhs, name);
                } else if (expr instanceof GRBLinExpr) {
                    model.addConstr((GRBLinExpr) expr, sense, rhs, name);
                }
            } catch (final GRBException exception) {
                // TODO Auto-generated catch block
                exception.printStackTrace();
            }
        }

        GRBExpr buildExpression(final Expression expression, final ExpressionsBasedModel model, final GRBVar[] vars) throws GRBException {

            GRBExpr retVal = null;

            if (expression.isAnyLinearFactorNonZero()) {

                final GRBLinExpr tmpLinExpr = new GRBLinExpr();
                retVal = tmpLinExpr;

                for (final IntIndex tmpKey : expression.getLinearKeySet()) {

                    final int tmpFreeIndex = tmpKey.index;
                    if (tmpFreeIndex >= 0) {
                        tmpLinExpr.addTerm(expression.getAdjustedLinearFactor(tmpKey), vars[tmpFreeIndex]);
                    }
                }
            }

            if (expression.isAnyQuadraticFactorNonZero()) {

                final GRBQuadExpr tmpQuadExpr = new GRBQuadExpr();
                if (retVal != null) {
                    tmpQuadExpr.add((GRBLinExpr) retVal);
                }
                retVal = tmpQuadExpr;

                for (final IntRowColumn tmpKey : expression.getQuadraticKeySet()) {

                    final int tmpFreeRow = tmpKey.row;
                    final int tmpFreeColumn = tmpKey.column;
                    if ((tmpFreeRow >= 0) && (tmpFreeColumn >= 0)) {
                        tmpQuadExpr.addTerm(expression.getAdjustedQuadraticFactor(tmpKey), vars[tmpFreeRow], vars[tmpFreeColumn]);
                    }
                }
            }

            return retVal;
        }

    };

    static final Environment ENVIRONMENT = new Environment();

    private final GRBModel myGRBModel;

    SolverGurobi(final GRBModel model) {

        super();

        myGRBModel = model;
    }

    public void dispose() {

        Solver.super.dispose();

        if (myGRBModel != null) {
            myGRBModel.dispose();
        }
    }

    public Result solve(final Result kickStarter) {

        Optimisation.State retState = Optimisation.State.UNEXPLORED;
        double retValue = NaN;
        Primitive64Array retSolution = null;

        try {

            myGRBModel.optimize();

            final int tmpStatus = myGRBModel.get(IntAttr.Status);

            retState = this.translate(tmpStatus);

            retValue = myGRBModel.get(DoubleAttr.ObjVal);

            final GRBVar[] tmpVars = myGRBModel.getVars();
            retSolution = Primitive64Array.make(tmpVars.length);
            for (int i = 0; i < tmpVars.length; i++) {
                retSolution.set(i, tmpVars[i].get(DoubleAttr.X));
            }

        } catch (final GRBException exception) {
            // TODO Auto-generated catch block
            exception.printStackTrace();
        }

        return new Optimisation.Result(retState, retValue, retSolution);
    }

    private State translate(final int status) {
        switch (status) {
        case Status.INFEASIBLE:
            return Optimisation.State.INFEASIBLE;
        case Status.ITERATION_LIMIT:
            return Optimisation.State.APPROXIMATE;
        case Status.CUTOFF:
            return Optimisation.State.APPROXIMATE;
        case Status.INF_OR_UNBD:
            return Optimisation.State.INVALID;
        case Status.INPROGRESS:
            return Optimisation.State.UNEXPLORED;
        case Status.INTERRUPTED:
            return Optimisation.State.UNEXPLORED;
        case Status.LOADED:
            return Optimisation.State.UNEXPLORED;
        case Status.NODE_LIMIT:
            return Optimisation.State.APPROXIMATE;
        case Status.NUMERIC:
            return Optimisation.State.APPROXIMATE;
        case Status.OPTIMAL:
            return Optimisation.State.OPTIMAL;
        case Status.SOLUTION_LIMIT:
            return Optimisation.State.APPROXIMATE;
        case Status.SUBOPTIMAL:
            return Optimisation.State.APPROXIMATE;
        case Status.TIME_LIMIT:
            return Optimisation.State.APPROXIMATE;
        case Status.UNBOUNDED:
            return Optimisation.State.UNBOUNDED;
        default:
            return Optimisation.State.FAILED;
        }

    }

    @Override
    protected void finalize() throws Throwable {

        this.dispose();

        super.finalize();
    }

    GRBModel getGRBModel() {
        return myGRBModel;
    }

}
