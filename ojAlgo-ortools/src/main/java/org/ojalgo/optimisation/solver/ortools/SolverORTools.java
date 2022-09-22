/*
 * Copyright 1997-2022 Optimatika
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
package org.ojalgo.optimisation.solver.ortools;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.ojalgo.optimisation.Expression;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.Optimisation;
import org.ojalgo.optimisation.Variable;
import org.ojalgo.structure.Structure1D.IntIndex;

import com.google.ortools.Loader;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPObjective;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPSolver.ResultStatus;
import com.google.ortools.linearsolver.MPVariable;

public final class SolverORTools implements Optimisation.Solver {

    @FunctionalInterface
    public interface Configurator {

        void configure(MPSolver delegate, Optimisation.Options options);

    }

    public static final class Integration extends ExpressionsBasedModel.Integration<SolverORTools> {

        Integration() {
            super();
        }

        @Override
        public SolverORTools build(final ExpressionsBasedModel model) {

            MPSolver solver = SolverORTools.newSolver(model);

            List<Variable> freeModVars = model.getFreeVariables();
            Set<IntIndex> fixedModVars = model.getFixedVariables();

            int nbVariables = freeModVars.size();

            double posInf = MPSolver.infinity();
            double negInf = -posInf;

            MPVariable[] solverVariables = new MPVariable[nbVariables];
            for (int i = 0; i < nbVariables; i++) {

                Variable modelVariable = freeModVars.get(i);

                double lb = modelVariable.getUnadjustedLowerLimit(negInf);
                double ub = modelVariable.getUnadjustedUpperLimit(posInf);
                boolean integer = modelVariable.isInteger();
                String name = modelVariable.getName();

                solverVariables[i] = solver.makeVar(lb, ub, integer, name);
            }

            List<Expression> modelConstraints = model.constraints().map(e -> e.compensate(fixedModVars)).collect(Collectors.toList());
            for (Expression modelConstraint : modelConstraints) {

                double lb = modelConstraint.getAdjustedLowerLimit(negInf);
                double ub = modelConstraint.getAdjustedUpperLimit(posInf);
                String name = modelConstraint.getName();

                MPConstraint solverConstraint = solver.makeConstraint(lb, ub, name);

                for (IntIndex linearKey : modelConstraint.getLinearKeySet()) {
                    solverConstraint.setCoefficient(solverVariables[model.indexOfFreeVariable(linearKey)], modelConstraint.getAdjustedLinearFactor(linearKey));
                }
            }

            Expression modelObjective = model.objective().compensate(fixedModVars);
            MPObjective solverObjective = solver.objective();
            solverObjective.setOptimizationDirection(model.getOptimisationSense() == Optimisation.Sense.MAX);

            for (IntIndex linearKey : modelObjective.getLinearKeySet()) {
                solverObjective.setCoefficient(solverVariables[model.indexOfFreeVariable(linearKey)], modelObjective.getAdjustedLinearFactor(linearKey));
            }

            DEFAULT.configure(solver, model.options);
            Optional<Configurator> optional = model.options.getConfigurator(Configurator.class);
            if (optional.isPresent()) {
                optional.get().configure(solver, model.options);
            }

            return new SolverORTools(solverVariables, solver);
        }

        @Override
        public boolean isCapable(final ExpressionsBasedModel model) {
            return !model.isAnyExpressionQuadratic();
        }

        @Override
        protected boolean isSolutionMapped() {
            return true;
        }

    }

    public static final SolverORTools.Integration INTEGRATION = new Integration();

    static final SolverORTools.Configurator DEFAULT = (delegate, options) -> {
    };

    static {
        Loader.loadNativeLibraries();
    }

    static MPSolver newSolver(final ExpressionsBasedModel model) {

        // BOP: Only pure IP (no continuous variables)

        // GLOP: LP only. Fails quietly with MIP
        // CLP: LP only. Fails quietly with MIP (GLOP verkar snabbare)

        // CBC: LP & MIP, men MIP verkar inte så bra.
        // SCIP: LP & MIP, men MIP verkar inte så bra. CBC är nog bättre.
        // SAT: Can solve some LP and MIP models, but not very good/fast at all

        // Support for ??? not linked in, or the license was not found.
        // ==========
        // CPLEX
        // GLPK
        // GUROBI
        // XPRESS

        if (model.isAnyVariableInteger()) {
            // Maybe BOP for pure IP models?
            return MPSolver.createSolver("CBC"); // MIP
        } else {
            return MPSolver.createSolver("GLOP"); // LP
        }
    }

    static Optimisation.State translate(final ResultStatus status) {
        switch (status) {
        case NOT_SOLVED:
            return Optimisation.State.UNEXPLORED;
        case MODEL_INVALID:
            return Optimisation.State.INVALID;
        case ABNORMAL:
            return Optimisation.State.FAILED;
        case UNBOUNDED:
        case INFEASIBLE:
            return Optimisation.State.INFEASIBLE;
        case FEASIBLE:
            return Optimisation.State.FEASIBLE;
        case OPTIMAL:
            return Optimisation.State.OPTIMAL;
        default:
            return Optimisation.State.FAILED;
        }
    }

    private final MPSolver mySolver;
    private final MPVariable[] myVariables;

    SolverORTools(final MPVariable[] variables, final MPSolver solver) {

        super();

        myVariables = variables;
        mySolver = solver;
    }

    @Override
    public void dispose() {

        Solver.super.dispose();

        if (mySolver != null) {
            mySolver.delete();
        }
    }

    @Override
    public Result solve(final Result kickStarter) {

        Optimisation.State retState = SolverORTools.translate(mySolver.solve());

        double retValue = mySolver.objective().value();

        double[] retSolution = new double[myVariables.length];
        if (retState.isFeasible()) {
            for (int i = 0; i < retSolution.length; i++) {
                retSolution[i] = myVariables[i].solutionValue();
            }
        }

        return Optimisation.Result.of(retValue, retState, retSolution);
    }

}
