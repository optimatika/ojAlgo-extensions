/*
 * Copyright 1997-2023 Optimatika
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

    @FunctionalInterface
    public interface Configurator {

        void configure(IloCplex cplex, Optimisation.Options options);

    }

    public static final class Integration extends ExpressionsBasedModel.Integration<SolverCPLEX> {

        Integration() {
            super();
        }

        @Override
        public SolverCPLEX build(final ExpressionsBasedModel model) {

            try {

                boolean mip = model.isAnyVariableInteger();

                IloCplex solver = new IloCplex();

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
                    double lb = mVar.getLowerLimit(false, Double.NEGATIVE_INFINITY);
                    double ub = mVar.getUpperLimit(false, Double.POSITIVE_INFINITY);
                    String name = mVar.getName();

                    solver.add(sVars[i] = solver.numVar(lb, ub, type, name));
                }

                SolverCPLEX retVal = new SolverCPLEX(solver, sVars);

                model.constraints().forEach(mConstr -> {

                    try {

                        IloNumExpr sConstr = retVal.buildExpression(mConstr);

                        if (mConstr.isEqualityConstraint()) {
                            solver.addEq(mConstr.getLowerLimit(false, 0.0), sConstr);
                        } else {
                            if (mConstr.isLowerConstraint()) {
                                solver.addLe(mConstr.getLowerLimit(false, Double.NEGATIVE_INFINITY), sConstr);
                            }
                            if (mConstr.isUpperConstraint()) {
                                solver.addGe(mConstr.getUpperLimit(false, Double.POSITIVE_INFINITY), sConstr);
                            }
                        }

                    } catch (IloException cause) {
                        throw new RuntimeException(cause);
                    }
                });

                Expression mObj = model.objective();
                IloNumExpr sObj = retVal.buildExpression(mObj);

                if (model.getOptimisationSense() == Optimisation.Sense.MAX) {
                    solver.addMaximize(sObj);
                } else {
                    solver.addMinimize(sObj);
                }

                DEFAULT.configure(solver, model.options);
                model.options.getConfigurator(Configurator.class).ifPresent(c -> c.configure(solver, model.options));

                return retVal;

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

    }

    public static final SolverCPLEX.Integration INTEGRATION = new Integration();

    static final Configurator DEFAULT = (cplex, options) -> {

        if (options.logger_appender != null) {
            try {
                cplex.setParam(IntParam.MIPDisplay, 5);
            } catch (IloException cause) {
                throw new RuntimeException(cause);
            }
            try {
                cplex.setParam(IntParam.MIPInterval, 5);
            } catch (IloException cause) {
                throw new RuntimeException(cause);
            }
            cplex.setOut(new OutputStream() {

                @Override
                public void write(final int b) throws IOException {
                    options.logger_appender.print((char) b);
                }
            });
        } else {
            cplex.setOut(OutputStream.nullOutputStream());
        }
    };

    static Optimisation.State translate(final IloCplex.Status status) {
        if (status.equals(Status.Bounded)) {
            return State.VALID;
        }
        if (status.equals(Status.Error)) {
            return State.FAILED;
        }
        if (status.equals(Status.Feasible)) {
            return State.FEASIBLE;
        }
        if (status.equals(Status.Infeasible)) {
            return State.INFEASIBLE;
        }
        if (status.equals(Status.InfeasibleOrUnbounded)) {
            return State.INVALID;
        }
        if (status.equals(Status.Optimal)) {
            return State.OPTIMAL;
        }
        if (status.equals(Status.Unbounded)) {
            return State.UNBOUNDED;
        }
        if (status.equals(Status.Unknown)) {
            return State.UNEXPLORED;
        }
        return State.FAILED;
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

        double retValue = Double.NaN;
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
            retValue = Double.NaN;
            retState = Optimisation.State.FAILED;
        }

        return Optimisation.Result.of(retValue, retState, retSolution);
    }

    private void copyLinear(final Expression source, final IloLinearNumExpr destination) throws IloException {
        for (IntIndex key : source.getLinearKeySet()) {
            destination.addTerm(source.doubleValue(key, false), myVariables[key.index]);
        }
    }

    private void copyQuadratic(final Expression source, final IloQuadNumExpr destination) throws IloException {
        for (IntRowColumn key : source.getQuadraticKeySet()) {
            destination.addTerm(source.doubleValue(key, false), myVariables[key.row], myVariables[key.column]);
        }
    }

    IloNumExpr buildExpression(final Expression expression) throws IloException {

        if (expression.isFunctionQuadratic()) {

            IloLQNumExpr tmpIloLQNumExpr = mySolver.lqNumExpr();

            this.copyQuadratic(expression, tmpIloLQNumExpr);
            this.copyLinear(expression, tmpIloLQNumExpr);

            return tmpIloLQNumExpr;

        } else if (expression.isFunctionPureQuadratic()) {

            IloQuadNumExpr tmpIloQuadNumExpr = mySolver.quadNumExpr();

            this.copyQuadratic(expression, tmpIloQuadNumExpr);

            return tmpIloQuadNumExpr;

        } else if (expression.isFunctionLinear()) {

            IloLinearNumExpr tmpIloLinearNumExpr = mySolver.linearNumExpr();

            this.copyLinear(expression, tmpIloLinearNumExpr);

            return tmpIloLinearNumExpr;

        } else {

            throw new IllegalStateException();
        }
    }

}
