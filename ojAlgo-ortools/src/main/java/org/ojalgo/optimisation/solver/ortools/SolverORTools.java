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
package org.ojalgo.optimisation.solver.ortools;

import static org.ojalgo.function.constant.PrimitiveMath.*;

import java.util.List;

import org.ojalgo.optimisation.Expression;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.Optimisation;
import org.ojalgo.optimisation.UpdatableSolver;
import org.ojalgo.optimisation.Variable;
import org.ojalgo.structure.Structure1D.IntIndex;

import com.google.ortools.Loader;
import com.google.ortools.modelbuilder.ModelBuilder;
import com.google.ortools.modelbuilder.ModelBuilderHelper;
import com.google.ortools.modelbuilder.ModelSolver;
import com.google.ortools.modelbuilder.ModelSolverHelper;
import com.google.ortools.modelbuilder.SolveStatus;

/**
 * OR-Tools is a suite of operations research tools developed by Google. It includes solvers for linear
 * programming (LP), mixed-integer programming (MIP), and other operations research problems.
 * <p>
 * This solver integration is based on the {@link ModelBuilder} API. More specifically the
 * {@link ModelBuilderHelper} and {@link ModelSolverHelper} classes are used.
 */
public final class SolverORTools implements UpdatableSolver {

    /**
     * Configuration options for the underlying solver. The default implementation is suitable for most cases,
     * but you can override it if you need to.
     * <ol>
     * <li>Define a subclass of this class and override/change the methods you need to.
     * <li>{@link Optimisation.Options#setConfigurator(Object)} to an instance of your subclass.
     * </ol>
     */
    public static class Configurator {

        private final InternalSolver[] myPreferences;

        public Configurator(final InternalSolver... preferences) {
            super();
            myPreferences = preferences;
        }

        /**
         * @param integer   Is any variable an integer?
         * @param quadratic Is any expression quadratic?
         */
        protected String getSolverName(final boolean integer, final boolean quadratic) {

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

            for (int i = 0; i < myPreferences.length; i++) {
                InternalSolver candidate = myPreferences[i];
                if (candidate.isCapableMIP() == integer && candidate.isCapableQP() == quadratic) {
                    return candidate.name();
                }
            }

            if (quadratic) {
                return integer ? null : "PDLP";
            } else {
                return integer ? "SCIP" : "GLOP";
            }
            // HIGHS is probably a good alternative for any/all type;

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
        }

        protected ModelBuilderHelper newModelBuilderHelper() {
            return new ModelBuilderHelper();
        }

        protected ModelSolverHelper newModelSolverHelper(final String solverName, final Optimisation.Options options) {

            ModelSolverHelper retVal = new ModelSolverHelper(solverName);

            boolean verbose = options.logger_appender != null && options.logger_detailed;

            retVal.clearLogCallback();
            if (verbose) {
                retVal.setLogCallback(str -> options.logger_appender.println(str));
                retVal.enableOutput(true);
            } else {
                retVal.setLogCallback(str -> {});
                retVal.enableOutput(false);
            }

            if (options.time_abort > 0L && options.time_abort < Long.MAX_VALUE) {
                retVal.setTimeLimitInSeconds(options.time_abort / THOUSAND);
            }

            return retVal;
        }
    }

    /**
     * Solvers available internally via OR-Tools ModelBuilder API.
     * <p>
     * Categorized by licensing and problem type support.
     */
    public enum InternalSolver {

        /**
         * IBM CPLEX. Requires commercial license.
         */
        CPLEX(License.COMMERCIAL, true, true, true, false),

        /**
         * Google's simplex LP solver. Default for LP.
         */
        GLOP(License.OPEN_SOURCE, true, false, false, false),

        /**
         * GNU Linear Programming Kit. GPL license restricts commercial use.
         */
        GLPK(License.COPYLEFT, true, false, true, false),

        /**
         * Gurobi Optimizer. Requires commercial license.
         */
        GUROBI(License.COMMERCIAL, true, true, true, false),

        /**
         * High-performance open-source solver (MIT license).
         */
        HIGHS(License.OPEN_SOURCE, true, false, true, false),

        /**
         * Google's first-order LP/QP solver.
         */
        PDLP(License.OPEN_SOURCE, true, true, false, false),

        /**
         * Google's CP-SAT solver. Handles pure integer and constraint problems.
         */
        SAT(License.OPEN_SOURCE, false, false, true, false),

        /**
         * Open-source MIP solver (Apache 2.0 license).
         */
        SCIP(License.OPEN_SOURCE, true, false, true, false),

        /**
         * FICO Xpress. Requires commercial license.
         */
        XPRESS(License.COMMERCIAL, true, true, true, false);

        private final boolean myCapableLP;
        private final boolean myCapableMIP;
        private final boolean myCapableQCQP;
        private final boolean myCapableQP;
        private final License myLicense;

        InternalSolver(final License license, final boolean lp, final boolean qp, final boolean mip, final boolean qcqp) {
            myLicense = license;
            myCapableLP = lp;
            myCapableQP = qp;
            myCapableMIP = mip;
            myCapableQCQP = qcqp;
        }

        /**
         * @return true if this solver can handle Linear Programming problems
         */
        public boolean isCapableLP() {
            return myCapableLP;
        }

        /**
         * @return true if this solver can handle Mixed-Integer Programming problems
         */
        public boolean isCapableMIP() {
            return myCapableMIP;
        }

        /**
         * @return true if this solver can handle Quadratically Constrained Quadratic Programming problems
         *         (via ModelBuilder)
         */
        public boolean isCapableQCQP() {
            return myCapableQCQP;
        }

        /**
         * @return true if this solver can handle Quadratic Programming problems (via ModelBuilder)
         */
        public boolean isCapableQP() {
            return myCapableQP;
        }

        /**
         * @return true if the solver is freely available for commercial use
         */
        public boolean isFreeForCommercialUse() {
            return myLicense == License.OPEN_SOURCE;
        }

        License getLicense() {
            return myLicense;
        }

    }

    static final class Integration extends ExpressionsBasedModel.Integration<SolverORTools> {

        Integration() {
            super();
        }

        @Override
        public SolverORTools build(final ExpressionsBasedModel model) {

            Configurator configurator = model.getConfigurator(DEFAULT);

            ModelBuilderHelper builder = configurator.newModelBuilderHelper();

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

            String solverName = configurator.getSolverName(mip, false);
            ModelSolverHelper solver = configurator.newModelSolverHelper(solverName, model.options);

            return new SolverORTools(builder, solver, nbVars);
        }

        @Override
        public boolean isCapable(final ExpressionsBasedModel model) {
            // ModelBuilder API does not expose quadratic objective support
            return !model.isAnyExpressionQuadratic();
        }

        @Override
        protected Optimisation.Sense getSolverSense() {
            return null;
        }

    }

    enum License {
        /**
         * Requires commercial license.
         */
        COMMERCIAL,
        /**
         * Copyleft license (GPL) that restricts commercial use.
         */
        COPYLEFT,
        /**
         * Permissive open-source (MIT, Apache 2.0, etc.)
         */
        OPEN_SOURCE
    }

    public static final ExpressionsBasedModel.Integration<SolverORTools> INTEGRATION = new Integration();

    static final SolverORTools.Configurator DEFAULT = new Configurator();

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
                return Optimisation.State.UNBOUNDED;
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
    private final int myNumVars;
    /**
     * Look at {@link ModelSolver} for hints on how to use this.
     */
    private final ModelSolverHelper mySolver;
    private boolean mySolutionAvailable;

    SolverORTools(final ModelBuilderHelper builder, final ModelSolverHelper solver, final int numVars) {

        super();

        myBuilder = builder;
        mySolver = solver;
        myNumVars = numVars;
    }

    @Override
    public void dispose() {

        UpdatableSolver.super.dispose();

        myBuilder.delete();
        mySolver.delete();
    }

    @Override
    public boolean fixVariable(final int index, final double value) {
        return this.updateRange(index, value, value);
    }

    /**
     * The dual value of constraint {@code index} from the most recent feasible solve. OR-Tools' ModelBuilder
     * API exposes no warm-start / basis controls, so this is purely informational.
     */
    @Override
    public double getDualMultiplier(final int index) {

        if (!mySolutionAvailable) {
            throw new IllegalStateException();
        }
        return mySolver.getDualValue(index);
    }

    /**
     * The reduced cost of variable {@code index} from the most recent feasible solve. OR-Tools' ModelBuilder
     * API exposes no warm-start / basis controls, so this is purely informational.
     */
    @Override
    public double getReducedGradient(final int index) {

        if (!mySolutionAvailable) {
            throw new IllegalStateException();
        }
        return mySolver.getReducedCost(index);
    }

    @Override
    public Optimisation.Result solve(final Result kickStarter) {

        mySolver.solve(myBuilder);

        SolveStatus status = mySolver.hasResponse() ? mySolver.getStatus() : SolveStatus.UNKNOWN_STATUS;

        Optimisation.State retState = SolverORTools.translate(status);

        if (retState.isFeasible() && retState != Optimisation.State.UNBOUNDED) {

            mySolutionAvailable = true;

            double retValue = mySolver.getObjectiveValue();

            double[] retSolution = new double[myNumVars];
            for (int i = 0; i < retSolution.length; i++) {
                retSolution[i] = mySolver.getVariableValue(i);
            }

            return Optimisation.Result.of(retValue, retState, retSolution);

        } else {

            mySolutionAvailable = false;

            return Optimisation.Result.of(retState);
        }
    }

    /**
     * Update a single variable's bounds in the underlying {@link ModelBuilderHelper}. OR-Tools exposes only
     * per-variable bound setters via SWIG — there is no native bulk / range / set / mask variant — so this is
     * two JNI calls. The benefit over rebuilding the model is avoiding constraint matrix re-translation, not
     * basis warm-start: the ModelBuilder API exposes no basis or solution-hint controls, so whether the
     * underlying backend warm-starts on re-solve is opaque and solver-dependent.
     */
    @Override
    public boolean updateRange(final int index, final double lower, final double upper) {

        if (index < 0 || index >= myNumVars) {
            return false;
        }
        myBuilder.setVarLowerBound(index, lower);
        myBuilder.setVarUpperBound(index, upper);
        mySolutionAvailable = false;
        return true;
    }

}
