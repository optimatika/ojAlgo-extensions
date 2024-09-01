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
package org.ojalgo.optimisation.solver.ortools;

import java.util.List;

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

        void configure(MPSolver solver, Optimisation.Options options);

    }

    public static final class Integration extends ExpressionsBasedModel.Integration<SolverORTools> {

        Integration() {
            super();
        }

        @Override
        public SolverORTools build(final ExpressionsBasedModel model) {

            boolean mip = model.isAnyVariableInteger();

            MPSolver solver = SolverORTools.newSolver(model);

            List<Variable> mVars = model.getVariables();
            int nbVars = mVars.size();

            double posInf = MPSolver.infinity();
            double negInf = -posInf;

            MPVariable[] sVars = new MPVariable[nbVars];

            for (int i = 0; i < nbVars; i++) {

                Variable mVar = mVars.get(i);

                double lb = mVar.getLowerLimit(false, negInf);
                double ub = mVar.getUpperLimit(false, posInf);
                boolean integer = mip && mVar.isInteger();
                String name = mVar.getName();

                sVars[i] = solver.makeVar(lb, ub, integer, name);
            }

            model.constraints().forEach(mConstr -> {

                double lb = mConstr.getLowerLimit(false, negInf);
                double ub = mConstr.getUpperLimit(false, posInf);
                String name = mConstr.getName();

                MPConstraint sConstr = solver.makeConstraint(lb, ub, name);

                for (IntIndex key : mConstr.getLinearKeySet()) {
                    sConstr.setCoefficient(sVars[key.index], mConstr.doubleValue(key, false));
                }
            });

            Expression mObj = model.objective();
            MPObjective sObj = solver.objective();
            sObj.setOptimizationDirection(model.getOptimisationSense() == Optimisation.Sense.MAX);
            for (IntIndex key : mObj.getLinearKeySet()) {
                sObj.setCoefficient(sVars[key.index], mObj.doubleValue(key, false));
            }

            DEFAULT.configure(solver, model.options);
            model.options.getConfigurator(Configurator.class).ifPresent(c -> c.configure(solver, model.options));

            return new SolverORTools(solver, sVars);
        }

        @Override
        public boolean isCapable(final ExpressionsBasedModel model) {
            return !model.isAnyExpressionQuadratic();
        }

    }

    public static final SolverORTools.Integration INTEGRATION = new Integration();

    static final SolverORTools.Configurator DEFAULT = (solver, options) -> {
    };

    static {
        Loader.loadNativeLibraries();
    }

    static MPSolver newSolver(final ExpressionsBasedModel model) {

        // BOP: Only pure IP (no continuous variables)

        // GLOP: LP only. Fails quietly with MIP
        // CLP: LP only. Fails quietly with MIP (GLOP seems faster)

        // CBC: LP & MIP, but MIP not so great
        // SCIP: LP & MIP, but even slower than CBC
        // SAT: Can solve some LP and MIP models, but not very good/fast at all

        // Support for ??? not linked in, or the license was not found.
        // ==========
        // CPLEX
        // GLPK
        // GUROBI
        // XPRESS

        if (model.isAnyVariableInteger()) {
            // Maybe BOP for pure IP models?
            return MPSolver.createSolver("SCIP"); // MIP
        } else {
            return MPSolver.createSolver("GLOP"); // LP
        }

        // Copied from message announcing v10.0.0:
        //        For the solvers:
        //            We would like to remove CLP/CBC from the default build, definitely CBC.
        //            We plan to add Highs support for MathOpt.
        //            SCIP is there, with the good license.
        //            Gurobi is pre-loaded.
        //            Our internal solvers (CP-SAT, GLOP, PDLP) are there.
        //            We are working with RTE and the Fico team on supporting XPRESS-MP. RTE is working hard on the side-loading of the XPRESS-MP shared libraries (no need to recompile). This is done for MPSolver. It should be propagated to ModelBuilder. I do not know yet when it will be available for MathOpt.
        //            We have just started talking with CardinalOpt. My goal is to have support with side-loading for ModelBuilder and MathOpt. 

        // Changes for the v10

        // Switch to ModelBuilder, and these default solvers

        // SCIP for MIP
        // GLOP for LP
        // PDLP for QP

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

    SolverORTools(final MPSolver solver, final MPVariable[] variables) {

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
