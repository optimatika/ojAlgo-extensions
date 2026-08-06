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
package org.ojalgo.optimisation.solver.highs;

import static org.ojalgo.function.constant.PrimitiveMath.*;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.ojalgo.array.ArrayR064;
import org.ojalgo.matrix.store.R064CSC;
import org.ojalgo.matrix.store.R064CSC.Builder;
import org.ojalgo.optimisation.Expression;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.ModelEntity;
import org.ojalgo.optimisation.Optimisation;
import org.ojalgo.optimisation.UpdatableSolver;
import org.ojalgo.optimisation.Variable;
import org.ojalgo.structure.Access1D;
import org.ojalgo.structure.Structure1D.IntIndex;
import org.ojalgo.structure.Structure2D.IntRowColumn;
import org.ojalgo.type.keyvalue.EntryPair;

/**
 * HiGHS solver integration for ojAlgo.
 * <p>
 * HiGHS is a high-performance open-source solver for linear programming (LP), mixed-integer programming
 * (MIP), and quadratic programming (QP). This integration uses Java's Foreign Function &amp; Memory API (FFM)
 * to directly call the HiGHS C library.
 *
 * @see <a href="https://highs.dev">HiGHS</a>
 */
public final class SolverHiGHS implements Optimisation.Solver, UpdatableSolver {

    /**
     * Configuration options for the HiGHS solver.
     * <p>
     * Subclass this to customize HiGHS-specific settings. Set via
     * {@link ExpressionsBasedModel#setConfigurator(Object)} or
     * {@link Optimisation.Environment#setConfigurator(Object)}.
     */
    public static class Configurator {

        /**
         * Configure HiGHS options before solving.
         *
         * @param highs   HiGHS instance pointer
         * @param arena   arena for allocating option strings
         * @param options ojAlgo optimisation options
         */
        protected void configure(final ExpressionsBasedModel model, final MemorySegment highs, final Arena arena) {

            int nbVars = model.countVariables();

            Optimisation.Options options = model.options;

            MemorySegment outputFlag = HiGHSBindings.allocateString(arena, "output_flag");
            boolean verbose = options.logger_appender != null && options.logger_detailed;
            HiGHSBindings.setBoolOptionValue(highs, outputFlag, verbose ? 1 : 0);

            if (options.time_abort > 0L) {
                MemorySegment timeLimit = HiGHSBindings.allocateString(arena, "time_limit");
                double seconds = options.time_abort / 1000.0;
                HiGHSBindings.setDoubleOptionValue(highs, timeLimit, seconds);
            }

            if (options.iterations_abort > 0L) {
                MemorySegment simplexIterLimit = HiGHSBindings.allocateString(arena, "simplex_iteration_limit");
                int limit = Math.min(options.iterations_abort, Integer.MAX_VALUE);
                HiGHSBindings.setIntOptionValue(highs, simplexIterLimit, limit);

                MemorySegment ipmIterLimit = HiGHSBindings.allocateString(arena, "ipm_iteration_limit");
                HiGHSBindings.setIntOptionValue(highs, ipmIterLimit, limit);
            }

            int parallelism = options.getParallelism();
            if (parallelism >= 4 && nbVars > 1_000) {

                MemorySegment threads = HiGHSBindings.allocateString(arena, "threads");
                HiGHSBindings.setIntOptionValue(highs, threads, Math.min(8, parallelism));

                MemorySegment parallel = HiGHSBindings.allocateString(arena, "parallel");
                MemorySegment on = HiGHSBindings.allocateString(arena, "on");
                HiGHSBindings.setStringOptionValue(highs, parallel, on);
            }

            //            double feasTol = options.feasibility.epsilon();
            //            if (feasTol > ZERO && feasTol < ONE) {
            //                MemorySegment primalFeasTol = HiGHSBindings.allocateString(arena, "primal_feasibility_tolerance");
            //                HiGHSBindings.setDoubleOptionValue(highs, primalFeasTol, feasTol);
            //
            //                MemorySegment dualFeasTol = HiGHSBindings.allocateString(arena, "dual_feasibility_tolerance");
            //                HiGHSBindings.setDoubleOptionValue(highs, dualFeasTol, feasTol);
            //            }
        }

    }

    /**
     * Maps solver variable/constraint indices to model entities.
     */
    static final class HiGHSEntityMap implements ExpressionsBasedModel.EntityMap {

        private final List<EntryPair<ModelEntity<?>, Optimisation.ConstraintType>> myConstraints;
        private final int myNumEqualityConstraints;
        private final int myNumInequalityConstraints;
        private final int myNumModelVars;
        private final int[] mySolverToModel;

        HiGHSEntityMap(final int numModelVars, final int numConstraints, final ExpressionsBasedModel model, final List<Variable> freeVariables,
                final List<Expression> constraints) {
            myNumModelVars = numModelVars;

            mySolverToModel = new int[numModelVars];
            for (int i = 0; i < numModelVars; i++) {
                mySolverToModel[i] = model.indexOf(freeVariables.get(i));
            }

            myConstraints = new ArrayList<>(numConstraints);
            int equalityCount = 0;
            int inequalityCount = 0;
            for (Expression constraint : constraints) {
                Optimisation.ConstraintType type = constraint.getConstraintType();
                myConstraints.add(EntryPair.of(constraint, type));
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
            return myConstraints.get(idc);
        }

        @Override
        public EntryPair<ModelEntity<?>, Optimisation.ConstraintType> getSlack(final int ids) {
            return null;
        }

        @Override
        public int indexOf(final int solverIndex) {
            if (solverIndex >= 0 && solverIndex < mySolverToModel.length) {
                return mySolverToModel[solverIndex];
            }
            return -1;
        }

        @Override
        public boolean isNegated(final int solverIndex) {
            return false;
        }

    }

    static final class Integration extends ExpressionsBasedModel.Integration<SolverHiGHS> {

        private static final boolean ADJUSTED_BOUNDS = false;
        private static final boolean ADJUSTED_CONSTRAINTS = true;
        private static final boolean ADJUSTED_OBJECTIVE = true;

        Integration() {
            super();
        }

        @Override
        public SolverHiGHS build(final ExpressionsBasedModel model) {

            Configurator configurator = model.getConfigurator(DEFAULT);

            int sense = model.getOptimisationSense() == Optimisation.Sense.MAX ? HiGHSBindings.SENSE_MAXIMIZE : HiGHSBindings.SENSE_MINIMIZE;

            List<Variable> variables = model.getVariables();

            Expression objective = model.objective();

            List<Expression> constraints = model.constraints().toList();

            int m = constraints.size();
            int n = variables.size();

            double[] colCost = new double[n];
            double[] colLower = new double[n];
            double[] colUpper = new double[n];
            int[] integrality = new int[n];

            boolean hasQP = objective.isAnyQuadraticFactorNonZero();
            boolean hasMIP = false;

            for (int v = 0; v < n; v++) {
                Variable variable = variables.get(v);

                colLower[v] = variable.getLowerLimit(ADJUSTED_BOUNDS, NEGATIVE_INFINITY);
                colUpper[v] = variable.getUpperLimit(ADJUSTED_BOUNDS, POSITIVE_INFINITY);

                if (variable.isInteger()) {
                    integrality[v] = HiGHSBindings.VAR_TYPE_INTEGER;
                    hasMIP = true;
                } else {
                    integrality[v] = HiGHSBindings.VAR_TYPE_CONTINUOUS;
                }
            }

            for (IntIndex key : objective.getLinearKeySet()) {
                colCost[key.index] = objective.doubleValue(key, ADJUSTED_OBJECTIVE);
            }

            R064CSC mtrxQ = null;
            if (hasQP) {

                Builder builderQ = R064CSC.newBuilder(n, n);

                for (IntRowColumn key : objective.getQuadraticKeySet()) {
                    double value = objective.doubleValue(key, ADJUSTED_OBJECTIVE);
                    if (key.row >= key.column) {
                        builderQ.add(key.row, key.column, value);
                    }
                    if (key.column >= key.row) {
                        builderQ.add(key.column, key.row, value);
                    }
                }

                mtrxQ = builderQ.build();
            }

            Builder builderA = R064CSC.newBuilder(m, n);
            double[] rowLower = new double[m];
            double[] rowUpper = new double[m];
            for (int row = 0; row < m; row++) {
                Expression constraint = constraints.get(row);
                rowLower[row] = constraint.getLowerLimit(ADJUSTED_CONSTRAINTS, NEGATIVE_INFINITY);
                rowUpper[row] = constraint.getUpperLimit(ADJUSTED_CONSTRAINTS, POSITIVE_INFINITY);
                for (IntIndex key : constraint.getLinearKeySet()) {
                    builderA.set(row, key.index, constraint.doubleValue(key, ADJUSTED_CONSTRAINTS));
                }
            }

            R064CSC mtrxA = builderA.build();

            MemorySegment highs = HiGHSBindings.create();
            Arena arena = Arena.ofConfined();

            try {

                configurator.configure(model, highs, arena);

                MemorySegment colCostSeg = HiGHSBindings.allocateDoubleArray(arena, colCost);
                MemorySegment colLowerSeg = HiGHSBindings.allocateDoubleArray(arena, colLower);
                MemorySegment colUpperSeg = HiGHSBindings.allocateDoubleArray(arena, colUpper);
                MemorySegment rowLowerSeg = HiGHSBindings.allocateDoubleArray(arena, rowLower);
                MemorySegment rowUpperSeg = HiGHSBindings.allocateDoubleArray(arena, rowUpper);
                MemorySegment aStartSeg = HiGHSBindings.allocateIntArray(arena, mtrxA.pointers);
                MemorySegment aIndexSeg = HiGHSBindings.allocateIntArray(arena, mtrxA.indices);
                MemorySegment aValueSeg = HiGHSBindings.allocateDoubleArray(arena, mtrxA.values);

                int status;
                if (hasMIP) {
                    MemorySegment integralitySeg = HiGHSBindings.allocateIntArray(arena, integrality);
                    status = HiGHSBindings.passMip(highs, n, m, mtrxA.capacity(), HiGHSBindings.MATRIX_FORMAT_COLWISE, sense, ZERO, colCostSeg, colLowerSeg,
                            colUpperSeg, rowLowerSeg, rowUpperSeg, aStartSeg, aIndexSeg, aValueSeg, integralitySeg);
                } else {
                    status = HiGHSBindings.passLp(highs, n, m, mtrxA.capacity(), HiGHSBindings.MATRIX_FORMAT_COLWISE, sense, ZERO, colCostSeg, colLowerSeg,
                            colUpperSeg, rowLowerSeg, rowUpperSeg, aStartSeg, aIndexSeg, aValueSeg);
                }

                if (status == HiGHSBindings.STATUS_ERROR) {
                    HiGHSBindings.destroy(highs);
                    throw new RuntimeException("Failed to pass model to HiGHS, status: " + status);
                }

                if (hasQP && mtrxQ != null) {

                    MemorySegment qStartSeg = HiGHSBindings.allocateIntArray(arena, mtrxQ.pointers);
                    MemorySegment qIndexSeg = HiGHSBindings.allocateIntArray(arena, mtrxQ.indices);
                    MemorySegment qValueSeg = HiGHSBindings.allocateDoubleArray(arena, mtrxQ.values);

                    status = HiGHSBindings.passHessian(highs, n, mtrxQ.capacity(), HiGHSBindings.HESSIAN_FORMAT_TRIANGULAR, qStartSeg, qIndexSeg, qValueSeg);

                    if (status == HiGHSBindings.STATUS_ERROR) {
                        HiGHSBindings.destroy(highs);
                        throw new RuntimeException("Failed to pass Hessian to HiGHS, status: " + status);
                    }
                }
            } finally {
                arena.close();
            }

            HiGHSEntityMap entityMap = new HiGHSEntityMap(n, m, model, variables, constraints);

            return new SolverHiGHS(highs, n, m, entityMap);
        }

        /**
         * HiGHS supports LP, MIP, and QP but not Quadratic constraints (QCQP) or Mixed-integer quadratic
         * programming (MIQP).
         */
        @Override
        public boolean isCapable(final ExpressionsBasedModel model) {

            if (!model.isAnyExpressionQuadratic()) {
                return true;
            }

            if (model.isAnyConstraintQuadratic()) {
                return false; // QCQP
            } else if (model.isAnyVariableInteger()) {
                return false; // MIQP
            }

            return true;
        }

        @Override
        protected Optimisation.Sense getSolverSense() {
            return null;
        }

    }

    public static final ExpressionsBasedModel.Integration<SolverHiGHS> INTEGRATION = new Integration();

    static final Configurator DEFAULT = new Configurator();

    /**
     * Translate HiGHS model status to ojAlgo optimisation state.
     */
    static Optimisation.State translate(final int modelStatus) {
        return switch (modelStatus) {
            case HiGHSBindings.MODEL_STATUS_OPTIMAL                                                                                                                                                                                    ->
                Optimisation.State.OPTIMAL;
            case HiGHSBindings.MODEL_STATUS_INFEASIBLE                                                                                                                                                                                 ->
                Optimisation.State.INFEASIBLE;
            case HiGHSBindings.MODEL_STATUS_UNBOUNDED                                                                                                                                                                                  ->
                Optimisation.State.UNBOUNDED;
            case HiGHSBindings.MODEL_STATUS_UNBOUNDED_OR_INFEASIBLE                                                                                                                                                                    ->
                Optimisation.State.INVALID;
            case HiGHSBindings.MODEL_STATUS_OBJECTIVE_BOUND, HiGHSBindings.MODEL_STATUS_OBJECTIVE_TARGET, HiGHSBindings.MODEL_STATUS_TIME_LIMIT,
                    HiGHSBindings.MODEL_STATUS_ITERATION_LIMIT,
                    HiGHSBindings.MODEL_STATUS_SOLUTION_LIMIT                                                                                                                                                                          ->
                Optimisation.State.APPROXIMATE;
            case HiGHSBindings.MODEL_STATUS_MODEL_EMPTY,
                    HiGHSBindings.MODEL_STATUS_NOTSET                                                                                                                                                                                  ->
                Optimisation.State.UNEXPLORED;
            case HiGHSBindings.MODEL_STATUS_INTERRUPT                                                                                                                                                                                  ->
                Optimisation.State.APPROXIMATE;
            default                                                                                                                                                                                                                    ->
                Optimisation.State.FAILED;
        };
    }

    private double[] myColDual;
    private final HiGHSEntityMap myEntityMap;
    private final MemorySegment myHighs;
    private final int myNumCol;
    private final int myNumRow;
    private double[] myRowDual;

    SolverHiGHS(final MemorySegment highs, final int numCol, final int numRow, final HiGHSEntityMap entityMap) {
        super();
        myHighs = highs;
        myNumCol = numCol;
        myNumRow = numRow;
        myEntityMap = entityMap;
    }

    @Override
    public void dispose() {
        if (myHighs != null && !myHighs.equals(MemorySegment.NULL)) {
            HiGHSBindings.destroy(myHighs);
        }
    }

    @Override
    public boolean fixVariable(final int index, final double value) {
        return this.updateRange(index, value, value);
    }

    @Override
    public double getDualMultiplier(final int index) {

        if (myRowDual == null) {
            throw new IllegalStateException();
        }
        return myRowDual[index];
    }

    @Override
    public Optional<ExpressionsBasedModel.EntityMap> getEntityMap() {
        return Optional.of(myEntityMap);
    }

    @Override
    public double getReducedGradient(final int index) {

        if (myColDual == null) {
            throw new IllegalStateException();
        }
        return myColDual[index];
    }

    @Override
    public Optimisation.Result solve(final Result kickStarter) {

        int runStatus = HiGHSBindings.run(myHighs);

        int modelStatus = HiGHSBindings.getModelStatus(myHighs);
        Optimisation.State state = SolverHiGHS.translate(modelStatus);

        if (state.isFeasible()) {

            double objectiveValue = HiGHSBindings.getObjectiveValue(myHighs);

            double[] solution = new double[myNumCol];
            myColDual = new double[myNumCol];
            myRowDual = new double[myNumRow];

            try (Arena arena = Arena.ofConfined()) {
                MemorySegment colValue = arena.allocate(myNumCol * Double.BYTES);
                MemorySegment colDual = arena.allocate(myNumCol * Double.BYTES);
                MemorySegment rowValue = arena.allocate(myNumRow * Double.BYTES);
                MemorySegment rowDual = arena.allocate(myNumRow * Double.BYTES);

                HiGHSBindings.getSolution(myHighs, colValue, colDual, rowValue, rowDual);
                HiGHSBindings.copyToJavaArray(colValue, solution);
                HiGHSBindings.copyToJavaArray(colDual, myColDual);
                HiGHSBindings.copyToJavaArray(rowDual, myRowDual);
            }

            // HiGHS was given the model's actual sense (MIN or MAX), so myColDual is already in the
            // model's optimisation-sense convention and indexed by model variable (no presolve mapping
            // is applied between model and HiGHS — all model variables are passed through).
            double[] colDualSnapshot = myColDual;
            Supplier<Access1D<?>> reducedGradient = () -> ArrayR064.wrap(colDualSnapshot);

            return Optimisation.Result.of(objectiveValue, state, solution).withReducedGradient(reducedGradient);

        } else {
            myColDual = null;
            myRowDual = null;
            return Optimisation.Result.of(state, new double[myNumCol]);
        }
    }

    /**
     * Masked bulk-update: change the bounds of the columns flagged in {@code mask} (HiGHS
     * {@code changeColsBoundsByMask}). The bound arrays are full length; only the entries with
     * {@code mask[i] == true} are applied.
     * <p>
     * <b>Use this when</b> the caller already maintains full-length bound vectors plus a parallel "changed"
     * flag — common in incremental update patterns where it is cheaper to flip a boolean than to compact a
     * sparse index list. HiGHS still walks all {@code numCol} mask entries, so for very sparse updates
     * {@link #updateBounds(int[], double[], double[])} is faster.
     * <p>
     * All three arrays must be of length {@code numCol} and aligned with the solver's internal column
     * ordering.
     * <p>
     * Compare with:
     * <ul>
     * <li>{@link #updateBounds(double[], double[])} — equivalent to an all-true mask, but skips the mask
     * marshalling.</li>
     * <li>{@link #updateBounds(int[], double[], double[])} — same selective semantics expressed as a
     * compacted index list; preferable when the number of changed columns is much smaller than
     * {@code numCol}.</li>
     * </ul>
     *
     * @param mask  selection mask of length {@code numCol} ({@code true} = update column {@code i})
     * @param lower new lower bounds, length {@code numCol}
     * @param upper new upper bounds, length {@code numCol}
     * @return {@code true} if successfully applied
     */
    public boolean updateBounds(final boolean[] mask, final double[] lower, final double[] upper) {

        if (mask.length != myNumCol || lower.length != myNumCol || upper.length != myNumCol) {
            return false;
        }
        if (myNumCol == 0) {
            return true;
        }
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment maskSeg = arena.allocate(ValueLayout.JAVA_INT, myNumCol);
            for (int i = 0; i < myNumCol; i++) {
                maskSeg.setAtIndex(ValueLayout.JAVA_INT, i, mask[i] ? 1 : 0);
            }
            MemorySegment lowerSeg = HiGHSBindings.allocateDoubleArray(arena, lower);
            MemorySegment upperSeg = HiGHSBindings.allocateDoubleArray(arena, upper);
            if (HiGHSBindings.changeColsBoundsByMask(myHighs, maskSeg, lowerSeg, upperSeg) != HiGHSBindings.STATUS_OK) {
                return false;
            }
        }
        myColDual = null;
        myRowDual = null;
        return true;
    }

    /**
     * Bulk-update the bounds of <em>all</em> variables in a single native call (HiGHS
     * {@code changeColsBoundsByRange} over the full column range {@code [0, numCol - 1]}).
     * <p>
     * <b>Use this when</b> the caller maintains complete bound vectors and wants to push a fresh snapshot —
     * e.g. a branch-and-bound master that recomputes all bounds per node. This is the cheapest of the three
     * overloads: no index/mask marshalling, just two {@code memcpy}s and one contiguous range walk on the
     * HiGHS side.
     * <p>
     * Compare with:
     * <ul>
     * <li>{@link #updateBounds(int[], double[], double[])} — sparse: pass only the indices that change.
     * Faster when {@code k << numCol}, since HiGHS only visits the listed columns.</li>
     * <li>{@link #updateBounds(boolean[], double[], double[])} — masked: pass a flag array of length
     * {@code numCol}. Convenient when bound vectors are already full-length and a "changed" flag is
     * maintained alongside them.</li>
     * </ul>
     * Both arrays must be of length {@code numCol} and aligned with the solver's internal column ordering (as
     * defined by the {@link HiGHSEntityMap}).
     *
     * @param lower new lower bounds for all columns
     * @param upper new upper bounds for all columns
     * @return {@code true} if successfully applied
     */
    public boolean updateBounds(final double[] lower, final double[] upper) {

        if (lower.length != myNumCol || upper.length != myNumCol) {
            return false;
        }
        if (myNumCol == 0) {
            return true;
        }
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment lowerSeg = HiGHSBindings.allocateDoubleArray(arena, lower);
            MemorySegment upperSeg = HiGHSBindings.allocateDoubleArray(arena, upper);
            if (HiGHSBindings.changeColsBoundsByRange(myHighs, 0, myNumCol - 1, lowerSeg, upperSeg) != HiGHSBindings.STATUS_OK) {
                return false;
            }
        }
        myColDual = null;
        myRowDual = null;
        return true;
    }

    /**
     * Sparse bulk-update: change the bounds of the columns listed in {@code indices} (HiGHS
     * {@code changeColsBoundsBySet}).
     * <p>
     * <b>Use this when</b> only a small subset of variables change between solves — e.g. a B&amp;B node that
     * tightens bounds on a handful of branching variables. Cost scales with {@code indices.length}, not
     * {@code numCol}, so it dominates the full-range form when {@code k << numCol}.
     * <p>
     * All three arrays must have the same length. Indices must be valid column indices in
     * {@code [0, numCol)}; HiGHS will sort them internally if not already ascending.
     * <p>
     * Compare with:
     * <ul>
     * <li>{@link #updateBounds(double[], double[])} — cheapest when essentially all columns change.</li>
     * <li>{@link #updateBounds(boolean[], double[], double[])} — same selective semantics, but selection is
     * expressed as a mask over full-length bound arrays instead of a compacted index array.</li>
     * </ul>
     *
     * @param indices solver-internal column indices to update
     * @param lower   new lower bounds, parallel to {@code indices}
     * @param upper   new upper bounds, parallel to {@code indices}
     * @return {@code true} if successfully applied
     */
    public boolean updateBounds(final int[] indices, final double[] lower, final double[] upper) {

        int k = indices.length;
        if (lower.length != k || upper.length != k) {
            return false;
        }
        if (k == 0) {
            return true;
        }
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment setSeg = HiGHSBindings.allocateIntArray(arena, indices);
            MemorySegment lowerSeg = HiGHSBindings.allocateDoubleArray(arena, lower);
            MemorySegment upperSeg = HiGHSBindings.allocateDoubleArray(arena, upper);
            if (HiGHSBindings.changeColsBoundsBySet(myHighs, k, setSeg, lowerSeg, upperSeg) != HiGHSBindings.STATUS_OK) {
                return false;
            }
        }
        myColDual = null;
        myRowDual = null;
        return true;
    }

    @Override
    public boolean updateRange(final int index, final double lower, final double upper) {

        if (index < 0 || index >= myNumCol) {
            return false;
        }
        if (HiGHSBindings.changeColBounds(myHighs, index, lower, upper) != HiGHSBindings.STATUS_OK) {
            return false;
        }
        myColDual = null;
        myRowDual = null;
        return true;
    }

}
