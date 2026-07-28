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
package org.ojalgo.optimisation.solver.cplex;

import static org.ojalgo.function.constant.PrimitiveMath.*;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import org.ojalgo.netio.BasicLogger;
import org.ojalgo.optimisation.Expression;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.Optimisation;
import org.ojalgo.optimisation.Variable;
import org.ojalgo.structure.Structure1D.IntIndex;
import org.ojalgo.structure.Structure2D.IntRowColumn;

import ilog.concert.IloException;
import ilog.concert.IloLQNumExpr;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloNumVarType;
import ilog.concert.IloQuadNumExpr;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.IntParam;
import ilog.cplex.IloCplex.Status;

/**
 * SolverCPLEX
 *
 * @author apete
 */
public final class SolverCPLEX implements Optimisation.Solver {

    public static class Configurator {

        protected IloCplex newIloCplex(final Optimisation.Options options) throws IloException {

            IloCplex retVal = new IloCplex();

            if (options.logger_appender != null) {

                int displayLevel = options.logger_detailed ? 5 : 1;

                retVal.setParam(IntParam.MIPDisplay, displayLevel);
                retVal.setParam(IntParam.SimDisplay, displayLevel);
                retVal.setParam(IntParam.BarDisplay, displayLevel);

                retVal.setParam(IntParam.MIPInterval, 5);

                retVal.setOut(new OutputStream() {

                    @Override
                    public void write(final int b) throws IOException {
                        options.logger_appender.print((char) b);
                    }
                });

            } else {

                retVal.setOut(OutputStream.nullOutputStream());
            }

            if (options.time_abort > 0L && options.time_abort < Long.MAX_VALUE) {
                double seconds = options.time_abort / 1000.0;
                retVal.setParam(IloCplex.DoubleParam.TiLim, seconds);
            }

            if (options.iterations_abort > 0 && options.iterations_abort < Integer.MAX_VALUE) {
                retVal.setParam(IntParam.ItLim, options.iterations_abort);
                retVal.setParam(IloCplex.LongParam.BarItLim, options.iterations_abort);
            }

            double feasTol = options.feasibility.epsilon();
            if (feasTol > ZERO && feasTol < ONE) {
                retVal.setParam(IloCplex.DoubleParam.EpRHS, feasTol);
                retVal.setParam(IloCplex.DoubleParam.EpOpt, feasTol);
            }

            return retVal;
        }

    }

    static final class Integration extends ExpressionsBasedModel.Integration<SolverCPLEX> {

        private static final boolean ADJUST = false;

        Integration() {
            super();
        }

        @Override
        public SolverCPLEX build(final ExpressionsBasedModel model) {

            try {

                Configurator configurator = model.getConfigurator(DEFAULT);

                IloCplex solver = configurator.newIloCplex(model.options);

                boolean mip = model.isAnyVariableInteger();

                List<Variable> mVars = model.getVariables();
                int nbVars = mVars.size();

                IloNumVar[] sVars = new IloNumVar[nbVars];

                for (int i = 0; i < nbVars; i++) {

                    Variable mVar = mVars.get(i);

                    IloNumVarType type = IloNumVarType.Float;
                    if (mip) {
                        if (mVar.isBinary()) {
                            type = IloNumVarType.Bool;
                        } else if (mVar.isInteger()) {
                            type = IloNumVarType.Int;
                        }
                    }
                    double lb = mVar.getLowerLimit(false, NEGATIVE_INFINITY);
                    double ub = mVar.getUpperLimit(false, POSITIVE_INFINITY);
                    String name = mVar.getName();

                    solver.add(sVars[i] = solver.numVar(lb, ub, type, name));
                }

                model.constraints().forEach(mConstr -> {

                    try {

                        IloNumExpr sConstr = this.buildExpression(mConstr, solver, sVars);

                        if (mConstr.isEqualityConstraint()) {
                            solver.addEq(mConstr.getLowerLimit(ADJUST, ZERO), sConstr);
                        } else {
                            if (mConstr.isLowerConstraint()) {
                                solver.addLe(mConstr.getLowerLimit(ADJUST, NEGATIVE_INFINITY), sConstr);
                            }
                            if (mConstr.isUpperConstraint()) {
                                solver.addGe(mConstr.getUpperLimit(ADJUST, POSITIVE_INFINITY), sConstr);
                            }
                        }

                    } catch (IloException cause) {
                        throw new RuntimeException(cause);
                    }
                });

                Expression mObj = model.objective();
                IloNumExpr sObj = this.buildExpression(mObj, solver, sVars);

                if (model.getOptimisationSense() == Optimisation.Sense.MAX) {
                    solver.addMaximize(sObj);
                } else {
                    solver.addMinimize(sObj);
                }

                return new SolverCPLEX(solver, sVars);

            } catch (IloException cause) {
                throw new RuntimeException(cause);
            }
        }

        /**
         * CPLEX can handle anything/everything ExpressionsBasedModel can model.
         *
         * @see org.ojalgo.optimisation.Optimisation.Integration#isCapable(org.ojalgo.optimisation.Optimisation.Model)
         */
        @Override
        public boolean isCapable(final ExpressionsBasedModel model) {
            return true;
        }

        private void copyLinear(final Expression source, final IloNumVar[] variables, final IloLinearNumExpr destination) throws IloException {
            for (IntIndex key : source.getLinearKeySet()) {
                destination.addTerm(source.doubleValue(key, ADJUST), variables[key.index]);
            }
        }

        private void copyQuadratic(final Expression source, final IloNumVar[] variables, final IloQuadNumExpr destination) throws IloException {
            for (IntRowColumn key : source.getQuadraticKeySet()) {
                destination.addTerm(source.doubleValue(key, ADJUST), variables[key.row], variables[key.column]);
            }
        }

        @Override
        protected Optimisation.Sense getSolverSense() {
            return null;
        }

        IloNumExpr buildExpression(final Expression expression, final IloCplex cplex, final IloNumVar[] variables) throws IloException {

            if (expression.isFunctionQuadratic()) {

                IloLQNumExpr tmpIloLQNumExpr = cplex.lqNumExpr();

                this.copyQuadratic(expression, variables, tmpIloLQNumExpr);
                this.copyLinear(expression, variables, tmpIloLQNumExpr);

                return tmpIloLQNumExpr;

            } else if (expression.isFunctionPureQuadratic()) {

                IloQuadNumExpr tmpIloQuadNumExpr = cplex.quadNumExpr();

                this.copyQuadratic(expression, variables, tmpIloQuadNumExpr);

                return tmpIloQuadNumExpr;

            } else if (expression.isFunctionLinear()) {

                IloLinearNumExpr tmpIloLinearNumExpr = cplex.linearNumExpr();

                this.copyLinear(expression, variables, tmpIloLinearNumExpr);

                return tmpIloLinearNumExpr;

            } else {

                return cplex.linearNumExpr();
            }
        }

    }

    public static final ExpressionsBasedModel.Integration<SolverCPLEX> INTEGRATION = new Integration();

    static final Configurator DEFAULT = new Configurator();

    static Optimisation.State translate(final IloCplex.Status status) {
        if (status.equals(Status.Bounded)) {
            return State.VALID;
        } else if (status.equals(Status.Error)) {
            return State.FAILED;
        } else if (status.equals(Status.Feasible)) {
            return State.FEASIBLE;
        } else if (status.equals(Status.Infeasible)) {
            return State.INFEASIBLE;
        } else if (status.equals(Status.InfeasibleOrUnbounded)) {
            return State.INVALID;
        } else if (status.equals(Status.Optimal)) {
            return State.OPTIMAL;
        } else if (status.equals(Status.Unbounded)) {
            return State.UNBOUNDED;
        } else if (status.equals(Status.Unknown)) {
            return State.UNEXPLORED;
        } else {
            return State.FAILED;
        }
    }

    private final IloCplex mySolver;
    private final IloNumVar[] myVariables;

    SolverCPLEX(final IloCplex solver, final IloNumVar[] variables) {
        super();
        mySolver = solver;
        myVariables = variables;
    }

    @Override
    public void dispose() {

        Solver.super.dispose();

        if (mySolver != null) {
            mySolver.end();
        }
    }

    @Override
    public Optimisation.Result solve(final Result kickStarter) {

        double retValue = NaN;
        Optimisation.State retState = Optimisation.State.UNEXPLORED;
        double[] retSolution = new double[myVariables.length];

        try {

            if (mySolver.solve()) {

                for (int i = 0; i < myVariables.length; i++) {
                    retSolution[i] = mySolver.getValue(myVariables[i]);
                }

                retValue = mySolver.getObjValue();
            }

            retState = SolverCPLEX.translate(mySolver.getStatus());

        } catch (IloException cause) {
            BasicLogger.error(cause, "CPLEX solve failed!");
            retValue = NaN;
            retState = Optimisation.State.FAILED;
        }

        return Optimisation.Result.of(retValue, retState, retSolution);
    }
}
