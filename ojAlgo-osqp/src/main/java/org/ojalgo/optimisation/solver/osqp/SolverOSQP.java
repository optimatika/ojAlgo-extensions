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
package org.ojalgo.optimisation.solver.osqp;

import static org.ojalgo.function.constant.PrimitiveMath.*;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.ojalgo.array.ArrayR064;
import org.ojalgo.matrix.store.R064CSC;
import org.ojalgo.matrix.store.R064CSC.Builder;
import org.ojalgo.optimisation.ConstraintsMetaData;
import org.ojalgo.optimisation.Expression;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.ModelEntity;
import org.ojalgo.optimisation.Optimisation;
import org.ojalgo.optimisation.UpdatableSolver;
import org.ojalgo.optimisation.Variable;
import org.ojalgo.structure.Access1D;
import org.ojalgo.structure.Structure1D.IntIndex;
import org.ojalgo.structure.Structure2D.IntRowColumn;
import org.ojalgo.type.context.NumberContext;
import org.ojalgo.type.keyvalue.EntryPair;

/**
 * OSQP solver integration for ojAlgo.
 * <p>
 * OSQP (Operator Splitting Quadratic Program solver) is an ADMM-based solver for convex QPs of the form
 *
 * <pre>
 * minimize    0.5 x'Px + q'x
 * subject to  l &lt;= Ax &lt;= u
 * </pre>
 *
 * There are no integer variables, and - since {@code P} only appears in the objective - no quadratic
 * constraints (QCQP) either. This integration uses Java's Foreign Function &amp; Memory API (FFM) to call the
 * OSQP (v1.0) C library directly.
 * <p>
 * OSQP has no notion of variable bounds; every row of {@code A} is either a translated model constraint or a
 * variable bound (a single-coefficient row, one per variable with limits), appended after the model
 * constraints.
 *
 * @see <a href="https://osqp.org">OSQP</a>
 */
public final class SolverOSQP implements Optimisation.Solver, UpdatableSolver {

    /**
     * Configuration options for the OSQP solver.
     * <p>
     * Subclass this to customize OSQP-specific settings. Set via
     * {@link ExpressionsBasedModel#setConfigurator(Object)} or
     * {@link Optimisation.Environment#setConfigurator(Object)}.
     */
    public static class Configurator {

        /**
         * The relative termination tolerance, for the primal and the dual residuals alike, unless a
         * feasibility tolerance is configured. OSQP's own default is 1e-3. This is the loosest value (with
         * polishing on) that the tests in this module pass with. All other tolerances are at OSQP's defaults.
         */
        private static final double EPS_REL = 2E-4;

        /**
         * Configure OSQP settings before solving.
         *
         * @param settings OSQP settings struct, already populated with OSQP's own defaults
         * @param options  the model's solver options
         */
        protected void configure(final MemorySegment settings, final Optimisation.Options options) {

            boolean verbose = options.logger_appender != null && options.logger_detailed;
            OSQPBindings.setVerbose(settings, verbose);

            // Polishing - a final direct solve over the active set, once ADMM has terminated as solved. Off by
            // default in OSQP.
            OSQPBindings.setPolishing(settings, true);

            int effectiveIterations = Math.min(options.iterations_suffice, options.iterations_abort);
            if (effectiveIterations > 0 && effectiveIterations < Integer.MAX_VALUE) {
                OSQPBindings.setMaxIter(settings, effectiveIterations);
            }

            long effectiveTimeMillis = Math.min(options.time_suffice, options.time_abort);
            if (effectiveTimeMillis > 0L) {
                OSQPBindings.setTimeLimit(settings, effectiveTimeMillis / 1_000.0);
            }

            // The feasibility tolerance, only if configured to be something other than the default - a
            // requirement to achieve. OSQP has no tolerance for primal feasibility alone - its termination
            // tolerances apply to the dual residual as well - but like NumberContext they have an absolute and
            // a relative part.
            Optional<NumberContext> feasibility = options.getConfiguredFeasibilityTolerance();
            if (feasibility.isPresent()) {
                OSQPBindings.setEpsAbs(settings, feasibility.get().getAbsoluteError());
                OSQPBindings.setEpsRel(settings, feasibility.get().getRelativeError());
            } else {
                OSQPBindings.setEpsRel(settings, EPS_REL);
            }
        }

    }

    static final class Integration extends ExpressionsBasedModel.Integration<SolverOSQP> {

        private static final boolean ADJUSTED_BOUNDS = false;
        private static final boolean ADJUSTED_CONSTRAINTS = true;
        private static final boolean ADJUSTED_OBJECTIVE = true;

        Integration() {
            super();
        }

        @Override
        public SolverOSQP build(final ExpressionsBasedModel model) {

            Configurator configurator = model.getConfigurator(DEFAULT);

            boolean max = model.getOptimisationSense() == Optimisation.Sense.MAX;

            List<Variable> variables = model.getVariables();
            int n = variables.size();

            Expression objective = model.objective();
            List<Expression> constraints = model.constraints().toList();
            int numModelConstraints = constraints.size();

            // One row per variable with bounds, after the model's own constraints
            int[] boundRows = new int[n];
            int m = numModelConstraints;
            for (int v = 0; v < n; v++) {
                boundRows[v] = variables.get(v).isConstraint() ? m++ : -1;
            }

            double objectiveScale = ADJUSTED_OBJECTIVE ? objective.getAdjustmentFactor() : ONE;

            ConstraintsMetaData constraintsMetaData = ConstraintsMetaData.newEntityMap(m);
            constraintsMetaData.setMultiplierScale(objectiveScale);
            constraintsMetaData.setSignedRows(true);

            boolean hasQP = objective.isAnyQuadraticFactorNonZero();

            // q - linear part of the objective
            double[] q = new double[n];
            for (IntIndex key : objective.getLinearKeySet()) {
                double value = objective.doubleValue(key, ADJUSTED_OBJECTIVE);
                q[key.index] = max ? -value : value;
            }

            // P - quadratic part of the objective, upper triangular. A raw quadratic key (row,col) with
            // value v contributes v*x_row*x_col to the (0.5-free) objective; to get the standard 0.5 x'Px
            // form P needs P[row][col] = P[col][row] = v off the diagonal, and P[row][row] = 2v on it - see
            // ConvexSolver.buildData(), which builds its own Q the same way.
            R064CSC mtrxP;
            if (hasQP) {
                Builder builderP = R064CSC.newBuilder(n, n);
                for (IntRowColumn key : objective.getQuadraticKeySet()) {
                    double value = objective.doubleValue(key, ADJUSTED_OBJECTIVE);
                    if (max) {
                        value = -value;
                    }
                    int row = Math.min(key.row, key.column);
                    int col = Math.max(key.row, key.column);
                    builderP.add(row, col, row == col ? value + value : value);
                }
                mtrxP = builderP.build();
            } else {
                mtrxP = new R064CSC(n, n, 0);
            }

            // A & l/u - the model's own constraints, followed by one identity row per variable bound
            Builder builderA = R064CSC.newBuilder(m, n);
            double[] l = new double[m];
            double[] u = new double[m];

            for (int row = 0; row < numModelConstraints; row++) {
                Expression constraint = constraints.get(row);
                l[row] = OSQPBindings.clampInfinity(constraint.getLowerLimit(ADJUSTED_CONSTRAINTS, NEGATIVE_INFINITY));
                u[row] = OSQPBindings.clampInfinity(constraint.getUpperLimit(ADJUSTED_CONSTRAINTS, POSITIVE_INFINITY));
                for (IntIndex key : constraint.getLinearKeySet()) {
                    builderA.set(row, key.index, constraint.doubleValue(key, ADJUSTED_CONSTRAINTS));
                }
                constraintsMetaData.setEntry(row, constraint, constraint.getConstraintType(), false, ADJUSTED_CONSTRAINTS);
            }

            double[] boundScales = new double[n];
            for (int v = 0; v < n; v++) {
                int row = boundRows[v];
                if (row >= 0) {
                    Variable variable = variables.get(v);
                    boundScales[v] = ADJUSTED_BOUNDS ? variable.getAdjustmentFactor() : ONE;
                    l[row] = OSQPBindings.clampInfinity(variable.getLowerLimit(ADJUSTED_BOUNDS, NEGATIVE_INFINITY));
                    u[row] = OSQPBindings.clampInfinity(variable.getUpperLimit(ADJUSTED_BOUNDS, POSITIVE_INFINITY));
                    builderA.set(row, v, boundScales[v]);
                    constraintsMetaData.setEntry(row, variable, variable.getConstraintType(), false, ADJUSTED_BOUNDS);
                }
            }

            R064CSC mtrxA = builderA.build();

            Arena arena = Arena.ofConfined();

            try {

                MemorySegment pSeg = OSQPBindings.newCscMatrix(arena, n, n, mtrxP.capacity(), OSQPBindings.allocateLongArray(arena, mtrxP.pointers),
                        mtrxP.capacity() > 0 ? OSQPBindings.allocateLongArray(arena, mtrxP.indices) : MemorySegment.NULL,
                        mtrxP.capacity() > 0 ? OSQPBindings.allocateDoubleArray(arena, mtrxP.values) : MemorySegment.NULL);

                MemorySegment qSeg = OSQPBindings.allocateDoubleArray(arena, q);

                MemorySegment aSeg = OSQPBindings.newCscMatrix(arena, m, n, mtrxA.capacity(), OSQPBindings.allocateLongArray(arena, mtrxA.pointers),
                        mtrxA.capacity() > 0 ? OSQPBindings.allocateLongArray(arena, mtrxA.indices) : MemorySegment.NULL,
                        mtrxA.capacity() > 0 ? OSQPBindings.allocateDoubleArray(arena, mtrxA.values) : MemorySegment.NULL);

                MemorySegment lSeg = OSQPBindings.allocateDoubleArray(arena, l);
                MemorySegment uSeg = OSQPBindings.allocateDoubleArray(arena, u);

                MemorySegment settings = OSQPBindings.newDefaultSettings(arena);
                configurator.configure(settings, model.options);

                MemorySegment solverPtrOut = arena.allocate(ValueLayout.ADDRESS);

                long exitflag = OSQPBindings.setup(solverPtrOut, pSeg, qSeg, aSeg, lSeg, uSeg, m, n, settings);

                if (exitflag != 0L) {
                    throw new RuntimeException("Failed to set up OSQP problem: " + OSQPBindings.errorMessage(exitflag));
                }

                MemorySegment solver = OSQPBindings.getSolver(solverPtrOut);

                OSQPEntityMap entityMap = new OSQPEntityMap(n, constraints, constraintsMetaData);

                double objectiveConstant = ExpressionsBasedModel.Integration.getObjectiveConstant(model);

                return new SolverOSQP(solver, n, numModelConstraints, m, l, u, max, configurator, entityMap, constraintsMetaData, mtrxP, q, mtrxA, boundRows,
                        boundScales, objectiveScale, objectiveConstant);

            } finally {
                arena.close();
            }
        }

        /**
         * OSQP is a convex QP solver - no integer variables, and (since {@code P} is only in the objective)
         * no quadratic constraints either. Doesn't work very well for pure LP either.
         */
        @Override
        public boolean isCapable(final ExpressionsBasedModel model) {
            return !model.isAnyVariableInteger() && !model.isAnyConstraintQuadratic() && model.objective().isAnyQuadraticFactorNonZero();
        }

        @Override
        protected Optimisation.Sense getSolverSense() {
            // OSQP only ever minimises; MAX models are negated in build()/solve() above.
            return Optimisation.Sense.MIN;
        }

    }

    /**
     * Maps solver variable/constraint indices to model entities.
     * <p>
     * All model variables are passed through to OSQP 1:1 (no free/fixed distinction, no presolve), so
     * {@link #indexOf(int)} is a straight identity map. The model's own constraint expressions come first,
     * followed by one row per variable with bounds (mapped to the variable).
     */
    static final class OSQPEntityMap implements ExpressionsBasedModel.EntityMap {

        private final ConstraintsMetaData myConstraints;
        private final int myNumEqualityConstraints;
        private final int myNumInequalityConstraints;
        private final int myNumModelVars;

        OSQPEntityMap(final int numModelVars, final List<Expression> constraints, final ConstraintsMetaData constraintsMetaData) {

            myNumModelVars = numModelVars;

            myConstraints = constraintsMetaData;
            int equalityCount = 0;
            int inequalityCount = 0;
            for (Expression constraint : constraints) {
                Optimisation.ConstraintType type = constraint.getConstraintType();
                if (type == Optimisation.ConstraintType.EQUALITY) {
                    equalityCount++;
                } else if (type != Optimisation.ConstraintType.NONE) {
                    inequalityCount++;
                }
            }
            myNumEqualityConstraints = equalityCount;
            myNumInequalityConstraints = inequalityCount;
        }

        @Override
        public int countAdditionalConstraints() {
            return 0;
        }

        @Override
        public int countEqualityConstraints() {
            return myNumEqualityConstraints;
        }

        @Override
        public int countInequalityConstraints() {
            return myNumInequalityConstraints;
        }

        @Override
        public int countModelVariables() {
            return myNumModelVars;
        }

        @Override
        public int countSlackVariables() {
            return 0;
        }

        @Override
        public int countVariables() {
            return myNumModelVars;
        }

        @Override
        public EntryPair<ModelEntity<?>, Optimisation.ConstraintType> getConstraint(final int idc) {
            return myConstraints.getEntry(idc);
        }

        @Override
        public EntryPair<ModelEntity<?>, Optimisation.ConstraintType> getSlack(final int ids) {
            return null;
        }

        @Override
        public int indexOf(final int solverIndex) {
            if (solverIndex >= 0 && solverIndex < myNumModelVars) {
                return solverIndex;
            }
            return -1;
        }

        @Override
        public boolean isNegated(final int solverIndex) {
            return false;
        }

    }

    public static final ExpressionsBasedModel.Integration<SolverOSQP> INTEGRATION = new Integration();

    static final Configurator DEFAULT = new Configurator();

    /**
     * Translate an OSQP {@code status_val} to an ojAlgo optimisation state.
     */
    static Optimisation.State translate(final long statusVal) {
        if (statusVal == OSQPBindings.OSQP_SOLVED) {
            return Optimisation.State.OPTIMAL;
        } else if (statusVal == OSQPBindings.OSQP_SOLVED_INACCURATE) {
            return Optimisation.State.APPROXIMATE;
        } else if (statusVal == OSQPBindings.OSQP_PRIMAL_INFEASIBLE || statusVal == OSQPBindings.OSQP_PRIMAL_INFEASIBLE_INACCURATE) {
            return Optimisation.State.INFEASIBLE;
        } else if (statusVal == OSQPBindings.OSQP_DUAL_INFEASIBLE || statusVal == OSQPBindings.OSQP_DUAL_INFEASIBLE_INACCURATE) {
            return Optimisation.State.UNBOUNDED;
        } else if (statusVal == OSQPBindings.OSQP_MAX_ITER_REACHED || statusVal == OSQPBindings.OSQP_TIME_LIMIT_REACHED
                || statusVal == OSQPBindings.OSQP_SIGINT) {
            return Optimisation.State.APPROXIMATE;
        } else if (statusVal == OSQPBindings.OSQP_NON_CVX) {
            return Optimisation.State.INVALID;
        } else if (statusVal == OSQPBindings.OSQP_UNSOLVED) {
            return Optimisation.State.UNEXPLORED;
        } else {
            return Optimisation.State.FAILED;
        }
    }

    /**
     * The constraint matrix, the model's constraints followed by the variable-bound rows.
     */
    private final R064CSC myA;
    /**
     * For each variable, the row of its bound constraint, or -1 if it has none.
     */
    private final int[] myBoundRows;
    /**
     * For each variable with a bound row, the coefficient of that row: the variable's adjustment factor if
     * the bounds are adjusted, or else 1.
     */
    private final double[] myBoundScales;
    private final Configurator myConfigurator;
    /**
     * Replaced, not modified, when a bound row's type changes – results already returned refer to it.
     */
    private ConstraintsMetaData myConstraintsMetaData;
    /**
     * Cached full dual vector (length {@link #myNumRows}) from the most recent solve, in OSQP's convention -
     * rows {@code [0, myNumModelConstraints)} are the model's own constraint duals, the rest the
     * variable-bound duals.
     */
    private double[] myDualY = null;
    private final OSQPEntityMap myEntityMap;
    private final double[] myLower;
    private final boolean myMaximisation;
    private final int myNumModelConstraints;
    private final int myNumRows;
    private final int myNumVars;
    /**
     * The objective constant, in model units and the model's sense.
     */
    private final double myObjectiveConstant;
    /**
     * The objective's adjustment factor – the objective OSQP solves is scaled by this.
     */
    private final double myObjectiveScale;
    /**
     * The quadratic part of the (minimisation form) objective, upper triangular.
     */
    private final R064CSC myP;
    /**
     * The linear part of the (minimisation form) objective.
     */
    private final double[] myQ;
    /**
     * Cached reduced gradient from the most recent solve, in model units and the model's sense.
     */
    private double[] myReducedGradient = null;
    private final MemorySegment mySolver;
    private final double[] myUpper;

    SolverOSQP(final MemorySegment solver, final int numVars, final int numModelConstraints, final int numRows, final double[] lower, final double[] upper,
            final boolean maximisation, final Configurator configurator, final OSQPEntityMap entityMap, final ConstraintsMetaData constraintsMetaData,
            final R064CSC mtrxP, final double[] q, final R064CSC mtrxA, final int[] boundRows, final double[] boundScales, final double objectiveScale,
            final double objectiveConstant) {

        super();

        mySolver = solver;
        myNumVars = numVars;
        myNumModelConstraints = numModelConstraints;
        myNumRows = numRows;
        myLower = lower;
        myUpper = upper;
        myMaximisation = maximisation;
        myConfigurator = configurator;
        myEntityMap = entityMap;
        myConstraintsMetaData = constraintsMetaData;
        myP = mtrxP;
        myQ = q;
        myA = mtrxA;
        myBoundRows = boundRows;
        myBoundScales = boundScales;
        myObjectiveScale = objectiveScale;
        myObjectiveConstant = objectiveConstant;
    }

    @Override
    public void dispose() {
        if (mySolver != null && !mySolver.equals(MemorySegment.NULL)) {
            OSQPBindings.cleanup(mySolver);
        }
    }

    @Override
    public boolean fixVariable(final int index, final double value) {
        return this.updateRange(index, value, value);
    }

    @Override
    public double getDualMultiplier(final int index) {
        if (myDualY == null) {
            throw new IllegalStateException();
        }
        return myDualY[index];
    }

    @Override
    public Optional<ExpressionsBasedModel.EntityMap> getEntityMap() {
        return Optional.of(myEntityMap);
    }

    @Override
    public double getReducedGradient(final int index) {
        if (myReducedGradient == null) {
            throw new IllegalStateException();
        }
        return myReducedGradient[index];
    }

    @Override
    public Optimisation.Result solve(final Result kickStarter) {

        if (kickStarter != null && kickStarter.size() == myNumVars) {
            try (Arena arena = Arena.ofConfined()) {
                double[] x0 = new double[myNumVars];
                for (int i = 0; i < myNumVars; i++) {
                    x0[i] = kickStarter.doubleValue(i);
                }
                MemorySegment xSeg = OSQPBindings.allocateDoubleArray(arena, x0);
                OSQPBindings.warmStart(mySolver, xSeg, MemorySegment.NULL);
            } catch (RuntimeException cause) {
                // Best-effort only - a bad warm start is not fatal, OSQP will just cold-start instead.
            }
        }

        long exitflag = OSQPBindings.solve(mySolver);

        MemorySegment info = OSQPBindings.getInfo(mySolver);
        long statusVal = OSQPBindings.getStatusVal(info);
        Optimisation.State state = SolverOSQP.translate(statusVal);

        if (exitflag != 0L) {
            // An error during the solve - such as a failed polish - leaves the status as it was but skips
            // storing the solution, so there is no solution to return whatever the status says.
            myDualY = null;
            myReducedGradient = null;
            return Optimisation.Result.of(Optimisation.State.FAILED, new double[myNumVars]);
        }

        if (!state.isFeasible()) {
            myDualY = null;
            myReducedGradient = null;
            return Optimisation.Result.of(state, new double[myNumVars]);
        }

        double objectiveValue = OSQPBindings.getObjVal(info) / myObjectiveScale;
        if (myMaximisation) {
            objectiveValue = -objectiveValue;
        }
        objectiveValue += myObjectiveConstant;

        double[] solution = new double[myNumVars];
        myDualY = new double[myNumRows];

        MemorySegment solutionSeg = OSQPBindings.getSolution(mySolver);
        MemorySegment xPtr = OSQPBindings.getSolutionX(solutionSeg, myNumVars);
        MemorySegment yPtr = OSQPBindings.getSolutionY(solutionSeg, myNumRows);

        MemorySegment.copy(xPtr, ValueLayout.JAVA_DOUBLE, 0, solution, 0, myNumVars);
        MemorySegment.copy(yPtr, ValueLayout.JAVA_DOUBLE, 0, myDualY, 0, myNumRows);

        myReducedGradient = this.computeReducedGradient(solution, myDualY);

        double[] dualSnapshot = myDualY;
        double[] reducedGradientSnapshot = myReducedGradient;
        Supplier<Access1D<?>> reducedGradient = () -> ArrayR064.wrap(reducedGradientSnapshot);

        return Optimisation.Result.of(objectiveValue, state, solution).withReducedGradient(reducedGradient).withDualValues(myConstraintsMetaData,
                () -> ArrayR064.wrap(dualSnapshot));
    }

    @Override
    public boolean updateRange(final int index, final double lower, final double upper) {

        if (index < 0 || index >= myNumVars || myBoundRows[index] < 0) {
            return false;
        }

        int row = myBoundRows[index];
        myLower[row] = OSQPBindings.clampInfinity(lower * myBoundScales[index]);
        myUpper[row] = OSQPBindings.clampInfinity(upper * myBoundScales[index]);
        myConstraintsMetaData = myConstraintsMetaData.withLimits(row, lower, upper);

        try (Arena arena = Arena.ofConfined()) {
            MemorySegment lSeg = OSQPBindings.allocateDoubleArray(arena, myLower);
            MemorySegment uSeg = OSQPBindings.allocateDoubleArray(arena, myUpper);
            if (OSQPBindings.updateDataVec(mySolver, MemorySegment.NULL, lSeg, uSeg) != 0L) {
                return false;
            }
        }

        myDualY = null;
        myReducedGradient = null;
        return true;
    }

    /**
     * Re-applies {@code options} (via this instance's {@link Configurator}) to the already-built OSQP solver.
     * Settings that only take effect at setup time - {@code scaling}, {@code rho_is_vec}, {@code sigma} and
     * the adaptive-rho parameters - are ignored by OSQP for this call; {@code rho} itself needs
     * {@code osqp_update_rho} rather than this, and is not touched by the {@link Configurator} anyway.
     *
     * @return {@code true} if successfully applied
     */
    public boolean updateSettings(final Optimisation.Options options) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment settings = OSQPBindings.newDefaultSettings(arena);
            myConfigurator.configure(settings, options);
            return OSQPBindings.updateSettings(mySolver, settings) == 0L;
        }
    }

    /**
     * The reduced gradient, in the bounded-variable sense, in model units and the model's sense: the gradient
     * of the Lagrangian, {@code Px + q + A'y}, without the variable-bound rows, so that at an optimum it is
     * non-zero only for variables at an active bound. (The bound multipliers are also reported as dual
     * values, with the variables.)
     */
    private double[] computeReducedGradient(final double[] x, final double[] y) {

        double[] rowMultipliers = Arrays.copyOf(y, y.length);
        for (int j = 0; j < myNumVars; j++) {
            if (myBoundRows[j] >= 0) {
                rowMultipliers[myBoundRows[j]] = ZERO;
            }
        }

        double[] gradient = new double[myNumVars];
        double[] yA = new double[myNumVars];
        R064CSC.multiplySymmetric(gradient, myP, x);
        R064CSC.multiply(yA, rowMultipliers, myA);

        double scale = myMaximisation ? -ONE / myObjectiveScale : ONE / myObjectiveScale;
        for (int j = 0; j < myNumVars; j++) {
            gradient[j] = (gradient[j] + myQ[j] + yA[j]) * scale;
        }

        return gradient;
    }

}
