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
package org.ojalgo.optimisation.solver.scip;

import static org.ojalgo.function.constant.PrimitiveMath.*;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.ojalgo.concurrent.InterruptForwarder;
import org.ojalgo.optimisation.Expression;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.Optimisation;
import org.ojalgo.optimisation.UpdatableSolver;
import org.ojalgo.optimisation.Variable;
import org.ojalgo.structure.Structure1D.IntIndex;
import org.ojalgo.structure.Structure2D.IntRowColumn;
import org.ojalgo.type.context.NumberContext;

/**
 * SCIP solver integration for ojAlgo.
 * <p>
 * SCIP (Solving Constraint Integer Programs) is one of the fastest non-commercial solvers for mixed-integer
 * programming (MIP) and mixed-integer nonlinear programming (MINLP). This integration uses Java's Foreign
 * Function &amp; Memory API (FFM) to directly call the SCIP C library.
 *
 * @see <a href="https://www.scipopt.org">SCIP Optimization Suite</a>
 */
public final class SolverSCIP implements Optimisation.Solver, UpdatableSolver {

    /**
     * Configuration options for the SCIP solver.
     * <p>
     * Subclass this to customize SCIP-specific settings.
     */
    public static class Configurator {

        private static final int PARALLELISM_THRESHOLD = 18;
        /**
         * Below this many constraints PaPILO's presolving is switched off at solve time. Constraints rather
         * than variables because PaPILO's reductions are mostly row oriented - dominated rows, parallel rows,
         * redundant constraints, singleton rows - so row count is the better measure of whether there is
         * structure worth presolving. A first guess from the MIPLIB "easy set" measurements, where neither
         * count actually predicted the outcome; worth tuning once there is data on larger models.
         */
        private static final int PAPILO_THRESHOLD = 1_000;

        protected void configure(final ExpressionsBasedModel model, final MemorySegment scip, final Arena arena) {

            int nbConstraints = model.countExpressions();

            Optimisation.Options options = model.options;

            MemorySegment verbLevel = SCIPBindings.allocateString(arena, "display/verblevel");
            boolean verbose = options.logger_appender != null && options.logger_detailed;
            SCIPBindings.setIntParam(scip, verbLevel, verbose ? 4 : 0);

            long effectiveTimeMillis = Math.min(options.time_suffice, options.time_abort);
            if (effectiveTimeMillis > 0L) {
                MemorySegment timeLimit = SCIPBindings.allocateString(arena, "limits/time");
                double seconds = effectiveTimeMillis / 1000.0;
                SCIPBindings.setRealParam(scip, timeLimit, seconds);
            }

            long effectiveIterations = Math.min((long) options.iterations_suffice, (long) options.iterations_abort);
            if (effectiveIterations > 0L) {
                MemorySegment nodeLimit = SCIPBindings.allocateString(arena, "limits/nodes");
                SCIPBindings.setLongintParam(scip, nodeLimit, effectiveIterations);
            }

            int parallelism = options.getParallelism();
            if (parallelism > PARALLELISM_THRESHOLD) {
                MemorySegment maxThreads = SCIPBindings.allocateString(arena, "parallel/maxnthreads");
                SCIPBindings.setIntParam(scip, maxThreads, parallelism - PARALLELISM_THRESHOLD);
            }

            // 0 disables PaPILO, -1 is SCIP's own default of unlimited rounds. Set either way rather than
            // relying on that default. Off below the threshold takes neos-2624317-amur from a timeout to
            // 23s; it costs ~13% on the easy set models that solved regardless.
            MemorySegment maxRounds = SCIPBindings.allocateString(arena, "presolving/milp/maxrounds");
            SCIPBindings.setIntParamIfPresent(scip, maxRounds, nbConstraints <= PAPILO_THRESHOLD ? 0 : -1);
        }

        /**
         * Whether to hand the model to SCIP's concurrent portfolio, {@code SCIPsolveConcurrent}, rather than
         * to {@code SCIPsolve}. Off unless overridden, and only available at all in a TPI-enabled SCIP build.
         * <p>
         * The portfolio races differently configured sub-SCIPs against each other, and on some models that
         * wins. What it costs is the ability to stop: interrupting the solving thread stops a
         * {@code SCIPsolve} within milliseconds, whereas the sub-SCIPs of a concurrent solve only look at the
         * synchronisation store when they synchronise, which was measured minutes apart on a hard model.
         * Nothing in SCIP's exported API reaches them sooner. A solve that cannot be abandoned is the worse
         * default, so this trade has to be made deliberately.
         */
        protected boolean isConcurrent(final Optimisation.Options options) {
            return false;
        }

    }

    static final class Integration extends ExpressionsBasedModel.Integration<SolverSCIP> {

        /**
         * Must be false. SCIP handles variable bounds natively, not as constraint rows, so adjusting them would
         * mean rescaling the variables themselves.
         */
        private static final boolean ADJUSTED_BOUNDS = false;
        private static final boolean ADJUSTED_CONSTRAINTS = true;
        private static final boolean ADJUSTED_OBJECTIVE = true;

        Integration() {
            super();
        }

        @Override
        public SolverSCIP build(final ExpressionsBasedModel model) {

            Configurator configurator = model.getConfigurator(DEFAULT);

            int sense = model.getOptimisationSense() == Optimisation.Sense.MAX ? SCIPBindings.OBJSENSE_MAXIMIZE : SCIPBindings.OBJSENSE_MINIMIZE;

            List<Variable> variables = model.getVariables();
            List<Expression> constraints = model.constraints().toList();
            Expression objective = model.objective();

            int n = variables.size();
            int m = constraints.size();

            Arena arena = Arena.ofConfined();

            try {

                // Create SCIP instance
                MemorySegment scipPtr = arena.allocate(ValueLayout.ADDRESS);
                SCIPBindings.create(scipPtr);
                MemorySegment scip = scipPtr.get(ValueLayout.ADDRESS, 0);

                SCIPBindings.includeDefaultPlugins(scip);

                MemorySegment probName = SCIPBindings.allocateString(arena, "ojAlgo");
                SCIPBindings.createProbBasic(scip, probName);

                SCIPBindings.setObjsense(scip, sense);

                configurator.configure(model, scip, arena);

                // Create variables
                MemorySegment[] varPtrs = new MemorySegment[n];

                for (int v = 0; v < n; v++) {
                    Variable variable = variables.get(v);

                    double lb = variable.getLowerLimit(ADJUSTED_BOUNDS, NEGATIVE_INFINITY);
                    double ub = variable.getUpperLimit(ADJUSTED_BOUNDS, POSITIVE_INFINITY);

                    double obj = ZERO;
                    IntIndex key = new IntIndex(v);
                    if (objective.getLinearKeySet().contains(key)) {
                        obj = objective.doubleValue(key, ADJUSTED_OBJECTIVE);
                    }

                    int vartype;
                    if (variable.isBinary()) {
                        vartype = SCIPBindings.VARTYPE_BINARY;
                    } else if (variable.isInteger()) {
                        vartype = SCIPBindings.VARTYPE_INTEGER;
                    } else {
                        vartype = SCIPBindings.VARTYPE_CONTINUOUS;
                    }

                    MemorySegment varPtr = arena.allocate(ValueLayout.ADDRESS);
                    MemorySegment varName = SCIPBindings.allocateString(arena, variable.getName());

                    SCIPBindings.createVarBasic(scip, varPtr, varName, lb, ub, obj, vartype);

                    MemorySegment var = varPtr.get(ValueLayout.ADDRESS, 0);
                    SCIPBindings.addVar(scip, var);

                    varPtrs[v] = var;

                    SCIPBindings.releaseVar(scip, varPtr);
                }

                // Create constraints
                for (int c = 0; c < m; c++) {
                    Expression constraint = constraints.get(c);

                    double lhs = constraint.getLowerLimit(ADJUSTED_CONSTRAINTS, NEGATIVE_INFINITY);
                    double rhs = constraint.getUpperLimit(ADJUSTED_CONSTRAINTS, POSITIVE_INFINITY);

                    MemorySegment consPtr = arena.allocate(ValueLayout.ADDRESS);
                    MemorySegment consName = SCIPBindings.allocateString(arena, "c" + c);

                    if (constraint.isAnyQuadraticFactorNonZero()) {
                        SolverSCIP.createQuadraticConstraint(scip, arena, consPtr, consName, constraint, varPtrs, lhs, rhs, ADJUSTED_CONSTRAINTS);
                    } else {
                        SolverSCIP.createLinearConstraint(scip, arena, consPtr, consName, constraint, varPtrs, lhs, rhs, ADJUSTED_CONSTRAINTS);
                    }

                    MemorySegment cons = consPtr.get(ValueLayout.ADDRESS, 0);
                    SCIPBindings.addCons(scip, cons);
                    SCIPBindings.releaseCons(scip, consPtr);
                }

                // Quadratic objective terms
                if (objective.isAnyQuadraticFactorNonZero()) {
                    SolverSCIP.addQuadraticObjective(scip, arena, objective, varPtrs, ADJUSTED_OBJECTIVE);
                }

                boolean concurrent = configurator.isConcurrent(model.options) && SCIPBindings.hasSolveConcurrent() && !model.isAnyExpressionQuadratic();

                double objectiveScale = ADJUSTED_OBJECTIVE ? objective.getAdjustmentFactor() : ONE;
                double objectiveConstant = ExpressionsBasedModel.Integration.getObjectiveConstant(model);

                // The MIP gap, only if configured to be something other than the default – the absolute gap in the
                // objective's (scaled) units
                Optional<NumberContext> gap = model.options.getConfiguredGapTolerance();
                if (gap.isPresent()) {
                    SCIPBindings.setRealParam(scip, SCIPBindings.allocateString(arena, "limits/gap"), gap.get().getRelativeError());
                    SCIPBindings.setRealParam(scip, SCIPBindings.allocateString(arena, "limits/absgap"), gap.get().getAbsoluteError() * objectiveScale);
                }

                return new SolverSCIP(scip, varPtrs, n, arena, concurrent, model.options, objectiveScale, objectiveConstant);

            } catch (RuntimeException e) {
                arena.close();
                throw e;
            }
        }

        @Override
        public boolean isCapable(final ExpressionsBasedModel model) {
            return true;
        }

        @Override
        protected Optimisation.Sense getSolverSense() {
            return null;
        }

    }

    public static final ExpressionsBasedModel.Integration<SolverSCIP> INTEGRATION = new Integration();

    static final Configurator DEFAULT = new Configurator();

    private static void addQuadraticObjective(final MemorySegment scip, final Arena arena, final Expression objective, final MemorySegment[] varPtrs,
            final boolean adjusted) {

        // Add an auxiliary variable for the quadratic part: obj_aux with objective coefficient 1
        MemorySegment auxVarPtr = arena.allocate(ValueLayout.ADDRESS);
        MemorySegment auxVarName = SCIPBindings.allocateString(arena, "_qobj_");
        SCIPBindings.createVarBasic(scip, auxVarPtr, auxVarName, NEGATIVE_INFINITY, POSITIVE_INFINITY, ONE, SCIPBindings.VARTYPE_CONTINUOUS);
        MemorySegment auxVar = auxVarPtr.get(ValueLayout.ADDRESS, 0);
        SCIPBindings.addVar(scip, auxVar);
        SCIPBindings.releaseVar(scip, auxVarPtr);

        // Build quadratic constraint: obj_aux = x'Qx (with adjusted coefficients)
        // As: x'Qx - obj_aux = 0, i.e. lhs = 0, rhs = 0
        //
        // The coefficients are used as-is, for both senses. SCIP optimises c'x + obj_aux, and obj_aux is
        // pinned to x'Qx by this constraint, so c'x + obj_aux is the model objective whether SCIP is
        // minimising or maximising. Negating for MAX would pin obj_aux to -x'Qx and silently optimise
        // c'x - x'Qx instead.

        // Linear part: -1 * obj_aux (no linear terms from the quadratic part of the objective)
        MemorySegment[] linVars = { auxVar };
        double[] linCoefs = { -ONE };

        var quadKeys = objective.getQuadraticKeySet();
        List<MemorySegment> qv1 = new ArrayList<>();
        List<MemorySegment> qv2 = new ArrayList<>();
        List<Double> qc = new ArrayList<>();

        for (IntRowColumn key : quadKeys) {
            double val = objective.doubleValue(key, adjusted);
            if (val != ZERO) {
                qv1.add(varPtrs[key.row]);
                qv2.add(varPtrs[key.column]);
                qc.add(val);
            }
        }

        int nquad = qv1.size();
        MemorySegment qv1Seg = SCIPBindings.allocatePointerArray(arena, qv1.toArray(new MemorySegment[0]));
        MemorySegment qv2Seg = SCIPBindings.allocatePointerArray(arena, qv2.toArray(new MemorySegment[0]));
        double[] qcArr = new double[nquad];
        for (int i = 0; i < nquad; i++) {
            qcArr[i] = qc.get(i);
        }
        MemorySegment qcSeg = SCIPBindings.allocateDoubleArray(arena, qcArr);

        MemorySegment linVarsSeg = SCIPBindings.allocatePointerArray(arena, linVars);
        MemorySegment linCoefsSeg = SCIPBindings.allocateDoubleArray(arena, linCoefs);

        MemorySegment consPtr = arena.allocate(ValueLayout.ADDRESS);
        MemorySegment consName = SCIPBindings.allocateString(arena, "_qobj_cons_");
        SCIPBindings.createConsBasicQuadraticNonlinear(scip, consPtr, consName, 1, linVarsSeg, linCoefsSeg, nquad, qv1Seg, qv2Seg, qcSeg, ZERO, ZERO);

        MemorySegment cons = consPtr.get(ValueLayout.ADDRESS, 0);
        SCIPBindings.addCons(scip, cons);
        SCIPBindings.releaseCons(scip, consPtr);
    }

    private static void createLinearConstraint(final MemorySegment scip, final Arena arena, final MemorySegment consPtr, final MemorySegment consName,
            final Expression constraint, final MemorySegment[] varPtrs, final double lhs, final double rhs, final boolean adjusted) {

        var linearKeys = constraint.getLinearKeySet();
        int nvars = linearKeys.size();

        MemorySegment[] consVarPtrs = new MemorySegment[nvars];
        double[] consVals = new double[nvars];

        int idx = 0;
        for (IntIndex ckey : linearKeys) {
            consVarPtrs[idx] = varPtrs[ckey.index];
            consVals[idx] = constraint.doubleValue(ckey, adjusted);
            idx++;
        }

        MemorySegment consVarsSeg = SCIPBindings.allocatePointerArray(arena, consVarPtrs);
        MemorySegment consValsSeg = SCIPBindings.allocateDoubleArray(arena, consVals);

        SCIPBindings.createConsBasicLinear(scip, consPtr, consName, nvars, consVarsSeg, consValsSeg, lhs, rhs);
    }

    private static void createQuadraticConstraint(final MemorySegment scip, final Arena arena, final MemorySegment consPtr, final MemorySegment consName,
            final Expression constraint, final MemorySegment[] varPtrs, final double lhs, final double rhs, final boolean adjusted) {

        var linearKeys = constraint.getLinearKeySet();
        int nlin = linearKeys.size();

        MemorySegment[] linVars = new MemorySegment[nlin];
        double[] linCoefs = new double[nlin];

        int idx = 0;
        for (IntIndex key : linearKeys) {
            linVars[idx] = varPtrs[key.index];
            linCoefs[idx] = constraint.doubleValue(key, adjusted);
            idx++;
        }

        var quadKeys = constraint.getQuadraticKeySet();
        List<MemorySegment> qv1 = new ArrayList<>();
        List<MemorySegment> qv2 = new ArrayList<>();
        List<Double> qc = new ArrayList<>();

        for (IntRowColumn key : quadKeys) {
            double val = constraint.doubleValue(key, adjusted);
            if (val != ZERO) {
                qv1.add(varPtrs[key.row]);
                qv2.add(varPtrs[key.column]);
                qc.add(val);
            }
        }

        int nquad = qv1.size();

        MemorySegment linVarsSeg = SCIPBindings.allocatePointerArray(arena, linVars);
        MemorySegment linCoefsSeg = SCIPBindings.allocateDoubleArray(arena, linCoefs);
        MemorySegment qv1Seg = SCIPBindings.allocatePointerArray(arena, qv1.toArray(new MemorySegment[0]));
        MemorySegment qv2Seg = SCIPBindings.allocatePointerArray(arena, qv2.toArray(new MemorySegment[0]));
        double[] qcArr = new double[nquad];
        for (int i = 0; i < nquad; i++) {
            qcArr[i] = qc.get(i);
        }
        MemorySegment qcSeg = SCIPBindings.allocateDoubleArray(arena, qcArr);

        SCIPBindings.createConsBasicQuadraticNonlinear(scip, consPtr, consName, nlin, linVarsSeg, linCoefsSeg, nquad, qv1Seg, qv2Seg, qcSeg, lhs, rhs);
    }

    static Optimisation.State translate(final int status) {
        return switch (status) {
            case SCIPBindings.STATUS_OPTIMAL                                                                                                                                                                                                                                                                                                                                                                            ->
                Optimisation.State.OPTIMAL;
            case SCIPBindings.STATUS_INFEASIBLE                                                                                                                                                                                                                                                                                                                                                                         ->
                Optimisation.State.INFEASIBLE;
            case SCIPBindings.STATUS_UNBOUNDED                                                                                                                                                                                                                                                                                                                                                                          ->
                Optimisation.State.UNBOUNDED;
            case SCIPBindings.STATUS_INFORUNBD                                                                                                                                                                                                                                                                                                                                                                          ->
                Optimisation.State.INVALID;
            case SCIPBindings.STATUS_GAPLIMIT, SCIPBindings.STATUS_SOLLIMIT, SCIPBindings.STATUS_BESTSOLLIMIT, SCIPBindings.STATUS_TIMELIMIT,
                    SCIPBindings.STATUS_NODELIMIT, SCIPBindings.STATUS_TOTALNODELIMIT, SCIPBindings.STATUS_STALLNODELIMIT, SCIPBindings.STATUS_PRIMALLIMIT,
                    SCIPBindings.STATUS_DUALLIMIT, SCIPBindings.STATUS_RESTARTLIMIT, SCIPBindings.STATUS_USERINTERRUPT,
                    SCIPBindings.STATUS_TERMINATE                                                                                                                                                                                                                                                                                                                                                                       ->
                Optimisation.State.APPROXIMATE;
            case SCIPBindings.STATUS_UNKNOWN                                                                                                                                                                                                                                                                                                                                                                            ->
                Optimisation.State.UNEXPLORED;
            default                                                                                                                                                                                                                                                                                                                                                                                                     ->
                Optimisation.State.FAILED;
        };
    }

    private final Arena myArena;
    private final boolean myConcurrent;
    private final int myIterationsAbort;
    private final int myIterationsSuffice;
    private final int myNumVars;
    /**
     * The objective constant, in model units.
     */
    private final double myObjectiveConstant;
    /**
     * The objective's adjustment factor – the objective SCIP solves is scaled by this.
     */
    private final double myObjectiveScale;
    private final MemorySegment myScip;
    private final long myTimeAbortMillis;
    private final long myTimeSufficeMillis;
    private final MemorySegment[] myVarPtrs;

    SolverSCIP(final MemorySegment scip, final MemorySegment[] varPtrs, final int numVars, final Arena arena, final boolean concurrent,
            final Optimisation.Options options, final double objectiveScale, final double objectiveConstant) {
        super();
        myScip = scip;
        myVarPtrs = varPtrs;
        myNumVars = numVars;
        myArena = arena;
        myConcurrent = concurrent;
        myIterationsAbort = options.iterations_abort;
        myIterationsSuffice = options.iterations_suffice;
        myTimeAbortMillis = options.time_abort;
        myTimeSufficeMillis = options.time_suffice;
        myObjectiveScale = objectiveScale;
        myObjectiveConstant = objectiveConstant;
    }

    @Override
    public void dispose() {

        if (myScip != null && !myScip.equals(MemorySegment.NULL)) {
            MemorySegment scipPtr = myArena.allocate(ValueLayout.ADDRESS);
            scipPtr.set(ValueLayout.ADDRESS, 0, myScip);
            SCIPBindings.free(scipPtr);
        }
        myArena.close();
    }

    @Override
    public boolean fixVariable(final int index, final double value) {
        return this.updateRange(index, value, value);
    }

    @Override
    public double getDualMultiplier(final int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public double getReducedGradient(final int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optimisation.Result solve(final Result kickStarter) {

        long startMillis = System.currentTimeMillis();

        try (InterruptForwarder interrupter = InterruptForwarder.watch(() -> SCIPBindings.interruptSolve(myScip))) {
            if (myConcurrent) {
                SCIPBindings.solveConcurrent(myScip);
            } else {
                SCIPBindings.solve(myScip);
            }
        }

        int status = SCIPBindings.getStatus(myScip);
        Optimisation.State state = SolverSCIP.translate(status);

        if (!state.isFeasible() && myTimeSufficeMillis < myTimeAbortMillis) {
            long elapsed = System.currentTimeMillis() - startMillis;
            long remaining = myTimeAbortMillis - elapsed;
            if (remaining > 1000L) {

                SCIPBindings.freeTransform(myScip);

                try (Arena phaseArena = Arena.ofConfined()) {
                    MemorySegment timeLimit = SCIPBindings.allocateString(phaseArena, "limits/time");
                    SCIPBindings.setRealParam(myScip, timeLimit, remaining / 1000.0);
                    if (myIterationsSuffice < myIterationsAbort) {
                        MemorySegment nodeLimit = SCIPBindings.allocateString(phaseArena, "limits/nodes");
                        SCIPBindings.setLongintParam(myScip, nodeLimit, myIterationsAbort);
                    }
                }

                try (InterruptForwarder interrupter = InterruptForwarder.watch(() -> SCIPBindings.interruptSolve(myScip))) {
                    if (myConcurrent) {
                        SCIPBindings.solveConcurrent(myScip);
                    } else {
                        SCIPBindings.solve(myScip);
                    }
                }

                status = SCIPBindings.getStatus(myScip);
                state = SolverSCIP.translate(status);
            }
        }

        if (state.isFeasible() && state != Optimisation.State.UNBOUNDED) {

            MemorySegment sol = SCIPBindings.getBestSol(myScip);

            if (sol.equals(MemorySegment.NULL)) {
                return Optimisation.Result.of(state, new double[myNumVars]);
            }

            double objectiveValue = SCIPBindings.getSolOrigObj(myScip, sol) / myObjectiveScale + myObjectiveConstant;

            double[] solution = new double[myNumVars];
            for (int i = 0; i < myNumVars; i++) {
                solution[i] = SCIPBindings.getSolVal(myScip, sol, myVarPtrs[i]);
            }

            return Optimisation.Result.of(objectiveValue, state, solution);

        } else {

            return Optimisation.Result.of(state, new double[myNumVars]);
        }
    }

    @Override
    public boolean updateRange(final int index, final double lower, final double upper) {

        if (index < 0 || index >= myNumVars) {
            return false;
        }

        SCIPBindings.freeTransform(myScip);

        MemorySegment var = myVarPtrs[index];
        SCIPBindings.chgVarLb(myScip, var, lower);
        SCIPBindings.chgVarUb(myScip, var, upper);

        return true;
    }

}
