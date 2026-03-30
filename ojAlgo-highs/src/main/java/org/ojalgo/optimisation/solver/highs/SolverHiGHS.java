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
package org.ojalgo.optimisation.solver.highs;

import static org.ojalgo.function.constant.PrimitiveMath.*;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.ojalgo.array.ArrayR064;
import org.ojalgo.matrix.store.R064CSC;
import org.ojalgo.matrix.store.R064CSC.Builder;
import org.ojalgo.optimisation.Expression;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.ModelEntity;
import org.ojalgo.optimisation.Optimisation;
import org.ojalgo.optimisation.UpdatableSolver;
import org.ojalgo.optimisation.Variable;
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
     * {@link Optimisation.Options#setConfigurator(Object)} or
     * {@link OptimisationEnvironment#setConfigurator(Object)}.
     */
    public static class Configurator {

        /**
         * Configure HiGHS options before solving.
         *
         * @param highs   HiGHS instance pointer
         * @param arena   arena for allocating option strings
         * @param options ojAlgo optimisation options
         */
        protected void configure(final MemorySegment highs, final Arena arena, final Optimisation.Options options) {

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

        Integration() {
            super();
        }

        @Override
        public SolverHiGHS build(final ExpressionsBasedModel model) {

            Configurator configurator = model.getConfigurator(DEFAULT);

            int sense = model.getOptimisationSense() == Optimisation.Sense.MAX ? HiGHSBindings.SENSE_MAXIMIZE : HiGHSBindings.SENSE_MINIMIZE;

            List<Variable> freeVariables = model.getFreeVariables();
            Set<IntIndex> fixedVariables = model.getFixedVariables();

            Expression objective = model.objective().compensate(fixedVariables);

            List<Expression> constraints = model.constraints().map(c -> c.compensate(fixedVariables)).toList();

            int m = constraints.size();
            int n = freeVariables.size();

            double[] colCost = new double[n];
            double[] colLower = new double[n];
            double[] colUpper = new double[n];
            int[] integrality = new int[n];

            boolean hasQP = objective.isAnyQuadraticFactorNonZero();
            boolean hasMIP = false;

            for (int v = 0; v < n; v++) {
                Variable variable = freeVariables.get(v);

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
                int col = model.indexOfFreeVariable(key.index);
                if (col >= 0) {
                    colCost[col] = objective.doubleValue(key, ADJUSTED_OBJECTIVE);
                }
            }

            R064CSC mtrxQ = null;
            if (hasQP) {

                Builder builderQ = R064CSC.newBuilder(n, n);

                for (IntRowColumn key : objective.getQuadraticKeySet()) {
                    int row = model.indexOfFreeVariable(key.row);
                    int col = model.indexOfFreeVariable(key.column);
                    if (row >= 0 && row < n && col >= 0 && col < n) {
                        double value = objective.doubleValue(key, ADJUSTED_OBJECTIVE);
                        if (row >= col) {
                            builderQ.add(row, col, value);
                        }
                        if (col >= row) {
                            builderQ.add(col, row, value);
                        }
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
                    int col = model.indexOfFreeVariable(key.index);
                    if (col >= 0 && col < n) {
                        builderA.set(row, col, constraint.doubleValue(key, ADJUSTED_CONSTRAINTS));
                    }
                }
            }

            R064CSC mtrxA = builderA.build();

            MemorySegment highs = HiGHSBindings.create();
            Arena arena = Arena.ofConfined();

            try {

                configurator.configure(highs, arena, model.options);

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

                if (status != HiGHSBindings.STATUS_OK) {
                    HiGHSBindings.destroy(highs);
                    throw new RuntimeException("Failed to pass model to HiGHS, status: " + status);
                }

                if (hasQP && mtrxQ != null) {

                    MemorySegment qStartSeg = HiGHSBindings.allocateIntArray(arena, mtrxQ.pointers);
                    MemorySegment qIndexSeg = HiGHSBindings.allocateIntArray(arena, mtrxQ.indices);
                    MemorySegment qValueSeg = HiGHSBindings.allocateDoubleArray(arena, mtrxQ.values);

                    status = HiGHSBindings.passHessian(highs, n, mtrxQ.capacity(), HiGHSBindings.HESSIAN_FORMAT_TRIANGULAR, qStartSeg, qIndexSeg, qValueSeg);

                    if (status != HiGHSBindings.STATUS_OK) {
                        HiGHSBindings.destroy(highs);
                        throw new RuntimeException("Failed to pass Hessian to HiGHS, status: " + status);
                    }
                }
            } finally {
                arena.close();
            }

            HiGHSEntityMap entityMap = new HiGHSEntityMap(n, m, model, freeVariables, constraints);

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
        public Result toModelState(final Result solverState, final ExpressionsBasedModel model) {
            return ExpressionsBasedModel.Integration.expandFreeToFull(solverState, model, ArrayR064.FACTORY, solverState.getReducedGradient());
        }

        @Override
        public Result toSolverState(final Result modelState, final ExpressionsBasedModel model) {
            return ExpressionsBasedModel.Integration.reduceFullToFree(modelState, model, ArrayR064.FACTORY);
        }

    }

    public static final ExpressionsBasedModel.Integration<SolverHiGHS> INTEGRATION = new Integration();
    private static final boolean ADJUSTED_BOUNDS = false;
    private static final boolean ADJUSTED_CONSTRAINTS = true;

    private static final boolean ADJUSTED_OBJECTIVE = true;

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
        if (index < 0 || index >= myNumCol) {
            return false;
        }
        int status = HiGHSBindings.changeColBounds(myHighs, index, value, value);
        if (status == HiGHSBindings.STATUS_OK) {
            HiGHSBindings.clearSolver(myHighs);
            myColDual = null;
            myRowDual = null;
            return true;
        }
        return false;
    }

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

            return Optimisation.Result.of(objectiveValue, state, solution);

        } else {
            myColDual = null;
            myRowDual = null;
            return Optimisation.Result.of(state, new double[myNumCol]);
        }
    }

    @Override
    public boolean updateRange(final int index, final double lower, final double upper) {
        if (index < 0 || index >= myNumCol) {
            return false;
        }
        int status = HiGHSBindings.changeColBounds(myHighs, index, lower, upper);
        if (status == HiGHSBindings.STATUS_OK) {
            HiGHSBindings.clearSolver(myHighs);
            myColDual = null;
            myRowDual = null;
            return true;
        }
        return false;
    }

}
