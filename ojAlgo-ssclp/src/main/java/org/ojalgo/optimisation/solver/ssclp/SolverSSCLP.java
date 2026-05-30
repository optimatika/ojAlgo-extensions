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
package org.ojalgo.optimisation.solver.ssclp;

import static org.ojalgo.function.constant.PrimitiveMath.*;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.ojalgo.netio.BasicLogger;
import org.ojalgo.optimisation.Expression;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.Optimisation;
import org.ojalgo.optimisation.Variable;
import org.ojalgo.structure.Structure1D.IntIndex;

import org.ssclab.log.SscLogger;
import org.ssclab.pl.milp.ConsType;
import org.ssclab.pl.milp.GoalType;
import org.ssclab.pl.milp.LP;
import org.ssclab.pl.milp.LinearObjectiveFunction;
import org.ssclab.pl.milp.MILP;
import org.ssclab.pl.milp.Solution;
import org.ssclab.pl.milp.SolutionType;

/**
 * SSC-LP solver integration for ojAlgo.
 * <p>
 * SSC-LP is a pure-Java library for solving Linear Programming (LP) and Mixed-Integer Linear Programming
 * (MILP) problems. The implementation is based on the simplex method (LP) combined with branch-and-bound
 * (MILP).
 *
 * @see <a href="https://www.ssclab.org/en/esempi.html">SSC Software www.sscLab.org</a>
 */
public final class SolverSSCLP implements Optimisation.Solver {

    /**
     * Configuration options for the SSC-LP solver. Subclass and override to customize.
     * <ol>
     * <li>Define a subclass of this class and override the methods you need to.
     * <li>Set an instance of your subclass via {@link Optimisation.Options#setConfigurator(Object)}.
     * </ol>
     */
    public static class Configurator {

        /**
         * Configure the LP solver instance (continuous problems). Override to set additional options such as
         * parallelisation, tolerances, or a custom title.
         */
        protected LP configure(final LP solver, final Optimisation.Options options) throws Exception {

            if (options.iterations_abort > 0 && options.iterations_abort < Integer.MAX_VALUE) {
                solver.setNumMaxIteration(options.iterations_abort);
            }

            return solver;
        }

        /**
         * Configure the MILP solver instance (with integer variables). Override to set additional options such
         * as thread count, branch-and-bound limits, or epsilons.
         */
        protected MILP configure(final MILP solver, final Optimisation.Options options) throws Exception {

            if (options.iterations_abort > 0 && options.iterations_abort < Integer.MAX_VALUE) {
                solver.setNumMaxIterationForSingleSimplex(options.iterations_abort);
            }

            return solver;
        }

        /**
         * Configure SSC-LP's {@code java.util.logging} logger. By default routes messages to ojAlgo's
         * {@link BasicLogger} when a logger appender is configured, otherwise suppresses output.
         */
        protected void configureLogging(final Optimisation.Options options) {

            Logger sscLogger = SscLogger.getLogger();
            boolean verbose = options.logger_appender != null && options.logger_detailed;
            sscLogger.setLevel(verbose ? Level.INFO : Level.OFF);
        }

    }

    static final class Integration extends ExpressionsBasedModel.Integration<SolverSSCLP> {

        private static final boolean ADJUSTED = true;

        Integration() {
            super();
        }

        @Override
        public SolverSSCLP build(final ExpressionsBasedModel model) {

            try {

                Configurator configurator = model.getConfigurator(DEFAULT);
                configurator.configureLogging(model.options);

                List<Variable> variables = model.getVariables();
                int n = variables.size();

                boolean mip = model.isAnyVariableInteger();

                Expression objective = model.objective();

                double[] objCoeffs = new double[n];
                for (IntIndex key : objective.getLinearKeySet()) {
                    objCoeffs[key.index] = objective.doubleValue(key, ADJUSTED);
                }

                GoalType goal = model.getOptimisationSense() == Optimisation.Sense.MAX ? GoalType.MAX : GoalType.MIN;
                LinearObjectiveFunction fo = new LinearObjectiveFunction(objCoeffs, goal);

                ArrayList<org.ssclab.pl.milp.Constraint> constraints = new ArrayList<>();

                model.constraints().forEach(mConstr -> {
                    double[] row = new double[n];
                    for (IntIndex key : mConstr.getLinearKeySet()) {
                        row[key.index] = mConstr.doubleValue(key, ADJUSTED);
                    }

                    String name = mConstr.getName();
                    try {
                        if (mConstr.isEqualityConstraint()) {
                            double rhs = mConstr.getLowerLimit(ADJUSTED, ZERO);
                            constraints.add(new org.ssclab.pl.milp.Constraint(row, ConsType.EQ, rhs, name));
                        } else {
                            if (mConstr.isLowerConstraint()) {
                                double rhs = mConstr.getLowerLimit(ADJUSTED, NEGATIVE_INFINITY);
                                constraints.add(new org.ssclab.pl.milp.Constraint(row, ConsType.GE, rhs, name));
                            }
                            if (mConstr.isUpperConstraint()) {
                                double rhs = mConstr.getUpperLimit(ADJUSTED, POSITIVE_INFINITY);
                                // Range constraints need a second row with a fresh array — SSC-LP keeps the
                                // reference to the array we pass in.
                                double[] rowUpper = mConstr.isLowerConstraint() ? row.clone() : row;
                                constraints.add(new org.ssclab.pl.milp.Constraint(rowUpper, ConsType.LE, rhs, name));
                            }
                        }
                    } catch (Exception cause) {
                        throw new RuntimeException(cause);
                    }
                });

                // Always pass the full variable bounds. SSC-LP defaults to lower=0, upper=+Inf which does not
                // match ojAlgo's default of "no bound", so we always set both rows explicitly.
                double[] lowers = new double[n];
                double[] uppers = new double[n];
                double[] integers = mip ? new double[n] : null;
                double[] binaries = mip ? new double[n] : null;
                boolean anyInteger = false;
                boolean anyBinary = false;

                for (int v = 0; v < n; v++) {
                    Variable variable = variables.get(v);
                    lowers[v] = variable.getLowerLimit(false, NEGATIVE_INFINITY);
                    uppers[v] = variable.getUpperLimit(false, POSITIVE_INFINITY);
                    if (mip) {
                        if (variable.isBinary()) {
                            binaries[v] = ONE;
                            anyBinary = true;
                        } else if (variable.isInteger()) {
                            integers[v] = ONE;
                            anyInteger = true;
                        }
                    }
                }

                constraints.add(new org.ssclab.pl.milp.Constraint(lowers, ConsType.LOWER, ZERO));
                constraints.add(new org.ssclab.pl.milp.Constraint(uppers, ConsType.UPPER, ZERO));
                if (anyInteger) {
                    constraints.add(new org.ssclab.pl.milp.Constraint(integers, ConsType.INT, ZERO));
                }
                if (anyBinary) {
                    constraints.add(new org.ssclab.pl.milp.Constraint(binaries, ConsType.BIN, ZERO));
                }

                if (mip) {
                    MILP milp = new MILP(fo, constraints);
                    configurator.configure(milp, model.options);
                    return new SolverSSCLP(milp, n);
                } else {
                    LP lp = new LP(fo, constraints);
                    configurator.configure(lp, model.options);
                    return new SolverSSCLP(lp, n);
                }

            } catch (Exception cause) {
                throw new RuntimeException(cause);
            }
        }

        /**
         * SSC-LP supports LP and MILP, but not quadratic objectives or constraints.
         */
        @Override
        public boolean isCapable(final ExpressionsBasedModel model) {
            return !model.isAnyExpressionQuadratic();
        }

        @Override
        protected Optimisation.Sense getSolverSense() {
            return null;
        }

    }

    public static final ExpressionsBasedModel.Integration<SolverSSCLP> INTEGRATION = new Integration();

    static final Configurator DEFAULT = new Configurator();

    static Optimisation.State translate(final SolutionType type) {
        if (type == null) {
            return Optimisation.State.FAILED;
        }
        switch (type) {
            case OPTIMUM:
                return Optimisation.State.OPTIMAL;
            case FEASIBLE:
                return Optimisation.State.FEASIBLE;
            case ILLIMITATUM:
                return Optimisation.State.UNBOUNDED;
            case VUOTUM:
                return Optimisation.State.INFEASIBLE;
            case MAX_ITERATIUM:
            case MAX_NUM_SIMPLEX:
                return Optimisation.State.APPROXIMATE;
            default:
                return Optimisation.State.FAILED;
        }
    }

    private final LP myLP;
    private final MILP myMILP;
    private final int myNumVars;

    SolverSSCLP(final LP lp, final int numVars) {
        super();
        myLP = lp;
        myMILP = null;
        myNumVars = numVars;
    }

    SolverSSCLP(final MILP milp, final int numVars) {
        super();
        myLP = null;
        myMILP = milp;
        myNumVars = numVars;
    }

    @Override
    public Optimisation.Result solve(final Result kickStarter) {

        try {

            SolutionType type;
            Solution solution = null;

            if (myMILP != null) {
                type = myMILP.resolve();
                if (type == SolutionType.OPTIMUM || type == SolutionType.FEASIBLE) {
                    solution = myMILP.getSolution();
                }
            } else {
                type = myLP.resolve();
                if (type == SolutionType.OPTIMUM || type == SolutionType.FEASIBLE) {
                    solution = myLP.getSolution();
                }
            }

            Optimisation.State state = SolverSSCLP.translate(type);

            if (solution != null) {
                double objectiveValue = solution.getOptimumValue();
                double[] values = solution.getValuesSolution();
                double[] result = new double[myNumVars];
                System.arraycopy(values, 0, result, 0, Math.min(values.length, myNumVars));
                return Optimisation.Result.of(objectiveValue, state, result);
            }

            return Optimisation.Result.of(state, new double[myNumVars]);

        } catch (Exception cause) {
            BasicLogger.error(cause, "SSC-LP solve failed!");
            return Optimisation.Result.of(Optimisation.State.FAILED, new double[myNumVars]);
        }
    }

}
