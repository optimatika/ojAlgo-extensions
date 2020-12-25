/*
 * Copyright 1997-2020 Optimatika
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.ojalgo.array.Primitive64Array;
import org.ojalgo.netio.CharacterRing;
import org.ojalgo.netio.CharacterRing.PrinterBuffer;
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
import ilog.cplex.IloCplex.Status;

public final class SolverCPLEX implements Optimisation.Solver {

    @FunctionalInterface
    public static interface Configurator {

        void configure(final IloCplex cplex, final Optimisation.Options options);

    }

    static final class Integration extends ExpressionsBasedModel.Integration<SolverCPLEX> {

        private final PrinterBuffer myLog = new CharacterRing().asPrinter();

        Integration() {
            super();
        }

        public SolverCPLEX build(final ExpressionsBasedModel model) {

            final SolverCPLEX retVal = new SolverCPLEX(model.options);
            final IloCplex delegateSolver = retVal.getDelegateSolver();

            try {

                final List<Variable> freeModVars = model.getFreeVariables();
                final Set<IntIndex> fixedModVars = model.getFixedVariables();

                final Expression modObj = model.objective().compensate(fixedModVars);

                for (final Variable var : freeModVars) {

                    IloNumVarType type = IloNumVarType.Float;
                    if (var.isBinary()) {
                        type = IloNumVarType.Bool;
                    } else if (var.isInteger()) {
                        type = IloNumVarType.Int;
                    }

                    double unadjustedLowerLimit = var.getUnadjustedLowerLimit();
                    double unadjustedUpperLimit = var.getUnadjustedUpperLimit();
                    final IloNumVar solverVar = delegateSolver.numVar(unadjustedLowerLimit, unadjustedUpperLimit, type, var.getName());
                    retVal.getDelegateVariables().add(solverVar);
                }

                for (final Expression expr : model.constraints().map(e -> e.compensate(fixedModVars)).collect(Collectors.toList())) {

                    final IloNumExpr solExpr = SolverCPLEX.buildExpression(model, expr, delegateSolver, retVal.getDelegateVariables());

                    SolverCPLEX.setBounds(solExpr, expr, delegateSolver);
                }

                final IloNumExpr solObj = SolverCPLEX.buildExpression(model, modObj, delegateSolver, retVal.getDelegateVariables());

                if (model.isMaximisation()) {
                    delegateSolver.addMaximize(solObj);
                } else {
                    delegateSolver.addMinimize(solObj);
                }

            } catch (final IloException exception) {
                exception.printStackTrace();
            }

            return retVal;
        }

        public boolean isCapable(final ExpressionsBasedModel model) {
            return true; // CPLEX can handle anything/everything ExpressionsBasedModel can model.
        }

        @Override
        protected boolean isSolutionMapped() {
            return true;
        }

    }

    public static final SolverCPLEX.Integration INTEGRATION = new Integration();

    static final Configurator DEFAULT = new Configurator() {

        public void configure(final IloCplex cplex, final Options options) {
            //            if (options.logger_appender != null) {
            //                cplex.setOut(new OutputStream() {
            //
            //                    @Override
            //                    public void write(final int b) throws IOException {
            //                        options.logger_appender.print(b);
            //                    }
            //                });
            //            } else {
            //                cplex.setOut(new OutputStream() {
            //
            //                    @Override
            //                    public void write(final int b) throws IOException {
            //                    }
            //                });
            //            }
        }
    };

    static void addLinear(final Expression source, final IloLinearNumExpr destination, final ExpressionsBasedModel model, final List<IloNumVar> variables)
            throws IloException {

        for (final IntIndex key : source.getLinearKeySet()) {

            final int freeInd = model.indexOfFreeVariable(key.index);

            if (freeInd >= 0) {
                destination.addTerm(source.getAdjustedLinearFactor(key), variables.get(freeInd));
            }
        }
    }

    static void addQuadratic(final Expression source, final IloQuadNumExpr destination, final ExpressionsBasedModel model, final List<IloNumVar> variables)
            throws IloException {

        for (final IntRowColumn key : source.getQuadraticKeySet()) {

            final int freeRow = model.indexOfFreeVariable(key.row);
            final int freeCol = model.indexOfFreeVariable(key.column);

            if ((freeRow >= 0) && (freeCol >= 0)) {
                destination.addTerm(source.getAdjustedQuadraticFactor(key), variables.get(freeRow), variables.get(freeCol));
            }
        }
    }

    static IloNumExpr buildExpression(final ExpressionsBasedModel model, final Expression expression, final IloCplex solver, final List<IloNumVar> variables)
            throws IloException {

        if (expression.isFunctionQuadratic()) {

            final IloLQNumExpr tmpIloLQNumExpr = solver.lqNumExpr();

            SolverCPLEX.addQuadratic(expression, tmpIloLQNumExpr, model, variables);
            SolverCPLEX.addLinear(expression, tmpIloLQNumExpr, model, variables);

            return tmpIloLQNumExpr;

        } else if (expression.isFunctionPureQuadratic()) {

            final IloQuadNumExpr tmpIloQuadNumExpr = solver.quadNumExpr();

            SolverCPLEX.addQuadratic(expression, tmpIloQuadNumExpr, model, variables);

            return tmpIloQuadNumExpr;

        } else if (expression.isFunctionLinear()) {

            final IloLinearNumExpr tmpIloLinearNumExpr = solver.linearNumExpr();

            SolverCPLEX.addLinear(expression, tmpIloLinearNumExpr, model, variables);

            return tmpIloLinearNumExpr;
        }

        return null;
    }

    static void setBounds(final IloNumExpr expression, final Expression model, final IloCplex solver) throws IloException {

        if (model.isEqualityConstraint()) {
            solver.addEq(model.getAdjustedLowerLimit(), expression);
        } else {
            if (model.isLowerConstraint()) {
                solver.addLe(model.getAdjustedLowerLimit(), expression);
            }
            if (model.isUpperConstraint()) {
                solver.addGe(model.getAdjustedUpperLimit(), expression);
            }
        }
    }

    private final IloCplex myDelegateSolver;

    private final List<IloNumVar> myDelegateVariables;
    private final Optimisation.Options myOptions;

    SolverCPLEX(final Optimisation.Options options) {

        super();

        myOptions = options;

        IloCplex tmpDelegate = null;

        try {
            tmpDelegate = new IloCplex();
        } catch (final IloException ex) {
            ex.printStackTrace();
            tmpDelegate = null;
        }

        myDelegateSolver = tmpDelegate;
        myDelegateVariables = new ArrayList<>();
    }

    public void dispose() {

        Solver.super.dispose();

        if (myDelegateSolver != null) {
            myDelegateSolver.end();
        }
    }

    public Result solve(final Result kickStarter) {

        try {

            final List<IloNumVar> solVariables = this.getDelegateVariables();

            Optimisation.State retState = Optimisation.State.UNEXPLORED;
            double retValue = Double.NaN;
            final Primitive64Array retSolution = Primitive64Array.make(solVariables.size());

            DEFAULT.configure(myDelegateSolver, myOptions);
            final Optional<Configurator> optional = myOptions.getConfigurator(Configurator.class);
            if (optional.isPresent()) {
                optional.get().configure(myDelegateSolver, myOptions);
            }

            if (myDelegateSolver.solve()) {
                // Feasible or Optimal

                for (int i = 0; i < solVariables.size(); i++) {
                    retSolution.set(i, myDelegateSolver.getValue(solVariables.get(i)));
                }

                retValue = myDelegateSolver.getObjValue();
            }

            retState = this.translate(myDelegateSolver.getStatus());

            return new Result(retState, retValue, retSolution);

        } catch (final IloException exception) {

            exception.printStackTrace();
        }

        return new Result(Optimisation.State.FAILED, Double.NaN, kickStarter);
    }

    @Override
    protected void finalize() throws Throwable {

        this.dispose();

        super.finalize();
    }

    IloCplex getDelegateSolver() {
        return myDelegateSolver;
    }

    List<IloNumVar> getDelegateVariables() {
        return myDelegateVariables;
    }

    Optimisation.State translate(final IloCplex.Status status) {
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

}
