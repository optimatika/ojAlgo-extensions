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
package org.ojalgo.optimisation.solver.ortools;

import static org.ojalgo.function.constant.PrimitiveMath.*;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.ojalgo.array.ArrayR064;
import org.ojalgo.concurrent.InterruptForwarder;
import org.ojalgo.netio.BasicLogger;
import org.ojalgo.optimisation.ConstraintsMetaData;
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
 * Integration with Google OR-Tools, via its {@link ModelBuilder} API. Handles LP and MIP.
 *
 * @see "https://developers.google.com/optimization"
 */
public final class SolverORTools implements Optimisation.Solver, UpdatableSolver {

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

            long effectiveTimeMillis = Math.min(options.time_suffice, options.time_abort);
            if (effectiveTimeMillis > 0L && effectiveTimeMillis < Long.MAX_VALUE) {
                retVal.setTimeLimitInSeconds(effectiveTimeMillis / THOUSAND);
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

        /**
         * Must be false. OR-Tools handles variable bounds natively, not as constraint rows, so adjusting them
         * would mean rescaling the variables themselves.
         */
        private static final boolean ADJUSTED_BOUNDS = false;
        private static final boolean ADJUSTED_CONSTRAINTS = false;
        private static final boolean ADJUSTED_OBJECTIVE = false;

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
                double lb = mVar.getLowerLimit(ADJUSTED_BOUNDS, NEGATIVE_INFINITY);
                double ub = mVar.getUpperLimit(ADJUSTED_BOUNDS, POSITIVE_INFINITY);

                int index = builder.addVar();
                if (index != i) {
                    throw new IllegalStateException();
                }

                builder.setVarName(index, name);
                builder.setVarIntegrality(index, integer);
                builder.setVarLowerBound(index, lb);
                builder.setVarUpperBound(index, ub);
            }

            Expression mObj = model.objective();
            double objectiveScale = ADJUSTED_OBJECTIVE ? mObj.getAdjustmentFactor() : ONE;

            List<Expression> mConstrs = model.constraints().collect(Collectors.toList());

            ConstraintsMetaData constraintsMetaData = ConstraintsMetaData.newEntityMap(mConstrs.size());
            constraintsMetaData.setMultiplierScale(objectiveScale);

            for (Expression mConstr : mConstrs) {

                double lb = mConstr.getLowerLimit(ADJUSTED_CONSTRAINTS, NEGATIVE_INFINITY);
                double ub = mConstr.getUpperLimit(ADJUSTED_CONSTRAINTS, POSITIVE_INFINITY);

                int index = builder.addLinearConstraint();

                for (IntIndex key : mConstr.getLinearKeySet()) {
                    double coefficient = mConstr.doubleValue(key, ADJUSTED_CONSTRAINTS);
                    builder.addConstraintTerm(index, key.index, coefficient);
                }

                builder.setConstraintLowerBound(index, lb);
                builder.setConstraintUpperBound(index, ub);

                constraintsMetaData.setEntry(index, mConstr, mConstr.getConstraintType(), false, ADJUSTED_CONSTRAINTS);
            }

            for (IntIndex key : mObj.getLinearKeySet()) {
                double coefficient = mObj.doubleValue(key, ADJUSTED_OBJECTIVE);
                builder.setVarObjectiveCoefficient(key.index, coefficient);
            }
            builder.setMaximize(model.getOptimisationSense() == Optimisation.Sense.MAX);

            String solverName = configurator.getSolverName(mip, false);
            ModelSolverHelper solver = configurator.newModelSolverHelper(solverName, model.options);

            double objectiveConstant = ExpressionsBasedModel.Integration.getObjectiveConstant(model);

            // Dual values and reduced costs only exist for continuous (LP) models
            ConstraintsMetaData duals = mip ? null : constraintsMetaData;

            return new SolverORTools(builder, solver, nbVars, model.options, duals, objectiveScale, objectiveConstant);
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
    /**
     * Null for MIP models – dual values and reduced costs only exist for continuous (LP) models.
     */
    private final ConstraintsMetaData myConstraintsMetaData;
    private final int myIterationsAbort;
    private final int myIterationsSuffice;
    private final int myNumVars;
    /**
     * The objective constant, in model units.
     */
    private final double myObjectiveConstant;
    /**
     * The objective's adjustment factor – the objective OR-Tools solves is scaled by this.
     */
    private final double myObjectiveScale;
    /**
     * Look at {@link ModelSolver} for hints on how to use this.
     */
    private final ModelSolverHelper mySolver;
    private boolean mySolutionAvailable;
    private final long myTimeAbortMillis;
    private final long myTimeSufficeMillis;
    /**
     * Whether the backend has already been found not to support interrupting, so that saying so once is
     * enough.
     */
    private final AtomicBoolean myInterruptRefused = new AtomicBoolean(false);

    SolverORTools(final ModelBuilderHelper builder, final ModelSolverHelper solver, final int numVars, final Optimisation.Options options,
            final ConstraintsMetaData constraintsMetaData, final double objectiveScale, final double objectiveConstant) {

        super();

        myBuilder = builder;
        mySolver = solver;
        myNumVars = numVars;
        myConstraintsMetaData = constraintsMetaData;
        myObjectiveScale = objectiveScale;
        myObjectiveConstant = objectiveConstant;
        myIterationsAbort = options.iterations_abort;
        myIterationsSuffice = options.iterations_suffice;
        myTimeAbortMillis = options.time_abort;
        myTimeSufficeMillis = options.time_suffice;
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

    /**
     * For a continuous model the dual values and the reduced gradient are OR-Tools' own: the rates of change
     * of the objective function value with the constraint limits and the variable bounds, as described at
     * {@link Optimisation.Result#getDualSolution()}.
     */
    @Override
    public Optimisation.Result solve(final Result kickStarter) {

        long startMillis = System.currentTimeMillis();

        try (InterruptForwarder interrupter = InterruptForwarder.watch(this::interruptSolve)) {
            mySolver.solve(myBuilder);
        }

        SolveStatus status = mySolver.hasResponse() ? mySolver.getStatus() : SolveStatus.UNKNOWN_STATUS;

        Optimisation.State retState = SolverORTools.translate(status);

        if (!retState.isFeasible() && myTimeSufficeMillis < myTimeAbortMillis) {
            long elapsed = System.currentTimeMillis() - startMillis;
            long remaining = myTimeAbortMillis - elapsed;
            if (remaining > 1000L) {
                mySolver.setTimeLimitInSeconds(remaining / THOUSAND);

                try (InterruptForwarder interrupter = InterruptForwarder.watch(this::interruptSolve)) {
                    mySolver.solve(myBuilder);
                }

                status = mySolver.hasResponse() ? mySolver.getStatus() : SolveStatus.UNKNOWN_STATUS;
                retState = SolverORTools.translate(status);
            }
        }

        if (retState.isFeasible() && retState != Optimisation.State.UNBOUNDED) {

            mySolutionAvailable = true;

            double retValue = mySolver.getObjectiveValue() / myObjectiveScale + myObjectiveConstant;

            double[] retSolution = new double[myNumVars];
            for (int i = 0; i < retSolution.length; i++) {
                retSolution[i] = mySolver.getVariableValue(i);
            }

            Optimisation.Result retVal = Optimisation.Result.of(retValue, retState, retSolution);

            if (myConstraintsMetaData != null) {

                double[] duals = new double[myConstraintsMetaData.size()];
                for (int i = 0; i < duals.length; i++) {
                    duals[i] = mySolver.getDualValue(i);
                }

                double[] reducedGradient = new double[myNumVars];
                for (int j = 0; j < reducedGradient.length; j++) {
                    reducedGradient[j] = mySolver.getReducedCost(j) / myObjectiveScale;
                }

                retVal = retVal.withDualValues(myConstraintsMetaData, () -> ArrayR064.wrap(duals)).withReducedGradient(() -> ArrayR064.wrap(reducedGradient));
            }

            return retVal;

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

    /**
     * Asks OR-Tools to stop, which is all that can be done from here – it sets a flag, and whether anything
     * comes of it is up to the backend that is solving. Measured with OR-Tools 9.15 on a model that would
     * otherwise run for hours: SAT stops within milliseconds, SCIP and HIGHS were still solving twenty
     * seconds later. Nothing distinguishes those cases in advance, since the return value only says that the
     * flag exists, not that the backend reads it.
     */
    private void interruptSolve() {

        if (!mySolver.interruptSolve() && myInterruptRefused.compareAndSet(false, true)) {
            BasicLogger.error("This OR-Tools backend will not interrupt a solve - it has to run to completion!");
        }
    }

}
