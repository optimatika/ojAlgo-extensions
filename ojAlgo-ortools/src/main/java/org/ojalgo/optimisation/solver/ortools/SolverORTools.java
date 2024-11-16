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

import static org.ojalgo.function.constant.PrimitiveMath.NEGATIVE_INFINITY;
import static org.ojalgo.function.constant.PrimitiveMath.POSITIVE_INFINITY;

import java.util.List;

import org.ojalgo.optimisation.Expression;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.Optimisation;
import org.ojalgo.optimisation.Variable;
import org.ojalgo.structure.Structure1D.IntIndex;

import com.google.ortools.Loader;
import com.google.ortools.modelbuilder.ModelBuilder;
import com.google.ortools.modelbuilder.ModelBuilderHelper;
import com.google.ortools.modelbuilder.ModelSolver;
import com.google.ortools.modelbuilder.ModelSolverHelper;
import com.google.ortools.modelbuilder.SolveStatus;

public final class SolverORTools implements Optimisation.Solver {

    @FunctionalInterface
    public interface Configurator {

        void configure(ModelBuilderHelper builder, ModelSolverHelper solver, Optimisation.Options options);

    }

    public static final class Integration extends ExpressionsBasedModel.Integration<SolverORTools> {

        private final String mySolverLP;
        private final String mySolverMIP;

        Integration(final String solverMIP, final String solverLP) {
            super();
            mySolverMIP = solverMIP;
            mySolverLP = solverLP;
        }

        @Override
        public SolverORTools build(final ExpressionsBasedModel model) {

            ModelBuilderHelper builder = new ModelBuilderHelper();

            boolean mip = model.isAnyVariableInteger();

            List<Variable> mVars = model.getVariables();
            int nbVars = mVars.size();

            for (int i = 0; i < nbVars; i++) {

                Variable mVar = mVars.get(i);

                String name = mVar.getName();
                boolean integer = mip && mVar.isInteger();
                double lb = mVar.getLowerLimit(false, NEGATIVE_INFINITY);
                double ub = mVar.getUpperLimit(false, POSITIVE_INFINITY);

                int index = builder.addVar();
                if (index != i) {
                    throw new IllegalStateException();
                }

                builder.setVarName(index, name);
                builder.setVarIntegrality(index, integer);
                builder.setVarLowerBound(index, lb);
                builder.setVarUpperBound(index, ub);
            }

            model.constraints().forEach(mConstr -> {

                double lb = mConstr.getLowerLimit(false, NEGATIVE_INFINITY);
                double ub = mConstr.getUpperLimit(false, POSITIVE_INFINITY);

                int index = builder.addLinearConstraint();

                for (IntIndex key : mConstr.getLinearKeySet()) {
                    double coefficient = mConstr.doubleValue(key, false);
                    builder.addConstraintTerm(index, key.index, coefficient);
                }

                builder.setConstraintLowerBound(index, lb);
                builder.setConstraintUpperBound(index, ub);
            });

            Expression mObj = model.objective();
            for (IntIndex key : mObj.getLinearKeySet()) {
                double coefficient = mObj.doubleValue(key, false);
                builder.setVarObjectiveCoefficient(key.index, coefficient);
            }
            builder.setMaximize(model.getOptimisationSense() == Optimisation.Sense.MAX);

            return this.newSolver(builder, mip, model.options);
        }

        @Override
        public boolean isCapable(final ExpressionsBasedModel model) {
            return !model.isAnyExpressionQuadratic();
        }

        public SolverORTools.Integration withSolverLP(final String solver) {
            return new SolverORTools.Integration(mySolverMIP, solver);
        }

        public SolverORTools.Integration withSolverMIP(final String solver) {
            return new SolverORTools.Integration(solver, mySolverLP);
        }

        private SolverORTools newSolver(final ModelBuilderHelper builder, final boolean mip, final Optimisation.Options options) {

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

            ModelSolverHelper solver = mip ? new ModelSolverHelper(mySolverMIP) : new ModelSolverHelper(mySolverLP);

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
            // GLOP/HIGHS for LP
            // PDLP for QP

            DEFAULT.configure(builder, solver, options);
            options.getConfigurator(Configurator.class).ifPresent(c -> c.configure(builder, solver, options));

            return new SolverORTools(builder, solver);
        }

    }

    public static final SolverORTools.Integration INTEGRATION = new Integration("SCIP", "GLOP");

    static final SolverORTools.Configurator DEFAULT = (builder, solver, options) -> {

        solver.clearLogCallback();
        solver.setLogCallback(str -> {});
    };

    static {
        Loader.loadNativeLibraries();
    }

    static Optimisation.State translate(final SolveStatus status) {
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

    /**
     * Look at {@link ModelBuilder} for hints on how to use this.
     */
    private final ModelBuilderHelper myBuilder;
    /**
     * Look at {@link ModelSolver} for hints on how to use this.
     */
    private final ModelSolverHelper mySolver;

    SolverORTools(final ModelBuilderHelper builder, final ModelSolverHelper solver) {

        super();

        myBuilder = builder;
        mySolver = solver;
    }

    @Override
    public void dispose() {

        Solver.super.dispose();

        myBuilder.delete();
        mySolver.delete();
    }

    @Override
    public Optimisation.Result solve(final Result kickStarter) {

        mySolver.solve(myBuilder);

        SolveStatus status = mySolver.hasResponse() ? mySolver.getStatus() : SolveStatus.UNKNOWN_STATUS;

        Optimisation.State retState = SolverORTools.translate(status);



        if (retState.isFeasible()) {

            double retValue = mySolver.getObjectiveValue();

            double[] retSolution = new double[myBuilder.numVariables()];
            for (int i = 0; i < retSolution.length; i++) {
                retSolution[i] = mySolver.getVariableValue(i);
            }

            return Optimisation.Result.of(retValue, retState, retSolution);

        } else {

            return Optimisation.Result.of(retState);
        }
    }

}
