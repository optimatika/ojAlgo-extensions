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
package org.ojalgo.optimisation.solver.clarabel;

import static org.ojalgo.function.constant.PrimitiveMath.*;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.ArrayList;
import java.util.List;

import org.ojalgo.array.SparseArray;
import org.ojalgo.array.SparseArray.NonzeroView;
import org.ojalgo.matrix.decomposition.Cholesky;
import org.ojalgo.matrix.store.ColumnsSupplier;
import org.ojalgo.matrix.store.MatrixStore;
import org.ojalgo.matrix.store.PhysicalStore;
import org.ojalgo.matrix.store.R064Store;
import org.ojalgo.optimisation.Expression;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.Optimisation;
import org.ojalgo.optimisation.Variable;
import org.ojalgo.structure.Structure1D.IntIndex;
import org.ojalgo.structure.Structure2D.IntRowColumn;
import org.ojalgo.type.FloatingPointReceptacle;

/**
 * Clarabel solver integration for ojAlgo using Panama FFI.
 * <p>
 * Clarabel is an interior-point solver for convex conic programs supporting LP, QP, SOCP, and SDP. This
 * integration calls the Clarabel C library directly via the Foreign Function &amp; Memory API.
 *
 * @see <a href="https://github.com/oxfordcontrol/Clarabel.rs">Clarabel.rs</a>
 */
public final class SolverClarabel implements Optimisation.Solver {

    public static class Configurator {

        protected void configure(final MemorySegment settings, final Optimisation.Options options) {

            if (options.iterations_abort > 0L) {
                ClarabelBindings.setMaxIter(settings, Math.min(Integer.MAX_VALUE, options.iterations_abort));
            }

            if (options.time_abort > 0L) {
                ClarabelBindings.setTimeLimit(settings, options.time_abort / 1_000.0);
            }

            boolean verbose = options.logger_appender != null && options.logger_detailed;
            ClarabelBindings.setVerbose(settings, verbose);

            double epsilon = options.feasibility.epsilon();
            ClarabelBindings.setTolFeas(settings, epsilon);

            double relativeError = options.feasibility.getRelativeError();
            double absoluteError = options.feasibility.getAbsoluteError();

            ClarabelBindings.setTolInfeasRel(settings, relativeError);
            ClarabelBindings.setTolInfeasAbs(settings, absoluteError);
            ClarabelBindings.setTolGapRel(settings, relativeError);
            ClarabelBindings.setTolGapAbs(settings, absoluteError);
        }

    }

    static final class Integration extends ExpressionsBasedModel.Integration<SolverClarabel> {

        Integration() {
            super();
        }

        @Override
        public SolverClarabel build(final ExpressionsBasedModel model) {

            Configurator configurator = model.getConfigurator(DEFAULT);

            List<Variable> variables = model.getVariables();
            int nbProbVars = variables.size();

            List<int[]> coneSpecs = new ArrayList<>();

            int aRowDim = 0;
            int aColDim = nbProbVars;
            ColumnsSupplier<Double> aColumns = R064Store.FACTORY.makeColumnsSupplier(Integer.MAX_VALUE);
            FloatingPointReceptacle bValues = new FloatingPointReceptacle();

            // Linear equality constraints -> ZeroCone
            Expression[] linEquExprs = model.constraints().filter(expr -> !expr.isAnyQuadraticFactorNonZero()).filter(Expression::isEqualityConstraint)
                    .toArray(Expression[]::new);

            if (linEquExprs.length > 0) {
                int count = 0;
                for (Expression expression : linEquExprs) {
                    SolverClarabel.ensureColumns(aColumns, aColDim);
                    for (IntIndex key : expression.getLinearKeySet()) {
                        aColumns.add(aRowDim, key.index, expression.doubleValue(key, ADJUSTED));
                    }
                    bValues.append(expression.getUpperLimit(ADJUSTED, POSITIVE_INFINITY));
                    aRowDim++;
                    count++;
                }
                coneSpecs.add(new int[]{ ClarabelBindings.CONE_ZERO, count });
            }

            // Linear inequality constraints + variable bounds -> NonnegativeCone
            Expression[] linInqExprs = model.constraints().filter(constr -> !constr.isAnyQuadraticFactorNonZero())
                    .filter(constr -> !constr.isEqualityConstraint()).toArray(Expression[]::new);

            int nnCount = 0;
            for (Expression expression : linInqExprs) {
                double lb = expression.getLowerLimit(ADJUSTED, NEGATIVE_INFINITY);
                double ub = expression.getUpperLimit(ADJUSTED, POSITIVE_INFINITY);

                SolverClarabel.ensureColumns(aColumns, aColDim);

                if (ub < POSITIVE_INFINITY) {
                    for (IntIndex key : expression.getLinearKeySet()) {
                        aColumns.add(aRowDim, key.index, expression.doubleValue(key, ADJUSTED));
                    }
                    bValues.append(ub);
                    aRowDim++;
                    nnCount++;
                }
                if (lb > NEGATIVE_INFINITY) {
                    for (IntIndex key : expression.getLinearKeySet()) {
                        aColumns.add(aRowDim, key.index, -expression.doubleValue(key, ADJUSTED));
                    }
                    bValues.append(-lb);
                    aRowDim++;
                    nnCount++;
                }
            }

            for (Variable variable : variables) {
                int col = model.indexOf(variable);
                double lb = variable.getLowerLimit(ADJUSTED, NEGATIVE_INFINITY);
                double ub = variable.getUpperLimit(ADJUSTED, POSITIVE_INFINITY);
                double val = variable.getAdjustmentFactor();

                SolverClarabel.ensureColumns(aColumns, aColDim);

                if (ub < POSITIVE_INFINITY) {
                    aColumns.add(aRowDim, col, val);
                    bValues.append(ub);
                    aRowDim++;
                    nnCount++;
                }
                if (lb > NEGATIVE_INFINITY) {
                    aColumns.add(aRowDim, col, -val);
                    bValues.append(-lb);
                    aRowDim++;
                    nnCount++;
                }
            }
            if (nnCount > 0) {
                coneSpecs.add(new int[]{ ClarabelBindings.CONE_NONNEGATIVE, nnCount });
            }

            // Quadratic constraints -> SecondOrderCone
            Expression[] quadExprs = model.constraints().filter(Expression::isAnyQuadraticFactorNonZero).toArray(Expression[]::new);
            for (Expression expression : quadExprs) {
                int socRows = SolverClarabel.addSecondOrderCone(expression, aColumns, bValues, aRowDim, aColDim, model);
                if (socRows > 0) {
                    coneSpecs.add(new int[]{ ClarabelBindings.CONE_SECOND_ORDER, socRows });
                    aRowDim += socRows;
                }
            }

            boolean max = model.getOptimisationSense() == Optimisation.Sense.MAX;

            Expression mObj = model.objective();
            double[] q = new double[nbProbVars];
            for (IntIndex key : mObj.getLinearKeySet()) {
                double val = mObj.doubleValue(key, ADJUSTED);
                q[key.index] = max ? -val : val;
            }

            // Build P (upper triangle of objective Hessian) in CSC
            ColumnsSupplier<Double> pColumns = R064Store.FACTORY.makeColumnsSupplier(Integer.MAX_VALUE);
            for (IntRowColumn key : mObj.getQuadraticKeySet()) {
                double val = mObj.doubleValue(key, ADJUSTED);
                if (key.row <= key.column) {
                    SolverClarabel.ensureColumns(pColumns, nbProbVars);
                    pColumns.add(key.row, key.column, max ? -val : val);
                    pColumns.add(key.column, key.row, max ? -val : val);
                }
            }

            Arena arena = Arena.ofConfined();

            try {
                // Allocate and init P CSC matrix
                MemorySegment pSeg = ClarabelBindings.allocateCscMatrix(arena);
                SolverClarabel.initCscMatrix(arena, pSeg, nbProbVars, nbProbVars, pColumns);

                // Allocate q vector
                MemorySegment qSeg = ClarabelBindings.allocateDoubleArray(arena, q);

                // Allocate and init A CSC matrix
                MemorySegment aSeg = ClarabelBindings.allocateCscMatrix(arena);
                SolverClarabel.ensureColumns(aColumns, aColDim);
                SolverClarabel.initCscMatrix(arena, aSeg, aRowDim, aColDim, aColumns);

                // Allocate b vector
                double[] bArr = bValues.toDoubles();
                MemorySegment bSeg = ClarabelBindings.allocateDoubleArray(arena, bArr);

                // Allocate cone array
                int nCones = coneSpecs.size();
                MemorySegment conesSeg = arena.allocate(ClarabelBindings.CONE_LAYOUT, nCones);
                for (int i = 0; i < nCones; i++) {
                    int[] spec = coneSpecs.get(i);
                    MemorySegment slot = conesSeg.asSlice(i * ClarabelBindings.CONE_LAYOUT.byteSize(), ClarabelBindings.CONE_LAYOUT.byteSize());
                    slot.set(ValueLayout.JAVA_INT, 0, spec[0]);
                    slot.set(ValueLayout.JAVA_LONG, 8, (long) spec[1]);
                }

                // Get default settings and configure
                MemorySegment settings = ClarabelBindings.defaultSettings(arena);
                configurator.configure(settings, model.options);

                // Create solver
                MemorySegment solver = ClarabelBindings.solverNew(pSeg, qSeg, aSeg, bSeg, nCones, conesSeg, settings);

                return new SolverClarabel(solver, max, nbProbVars, arena);

            } catch (RuntimeException e) {
                arena.close();
                throw e;
            }
        }

        @Override
        public boolean isCapable(final ExpressionsBasedModel model) {
            return !model.isAnyVariableInteger();
        }

        @Override
        protected Optimisation.Sense getSolverSense() {
            return Optimisation.Sense.MIN;
        }

    }

    public static final ExpressionsBasedModel.Integration<SolverClarabel> INTEGRATION = new Integration();

    private static final boolean ADJUSTED = true;

    static final Configurator DEFAULT = new Configurator();

    private static int addSecondOrderCone(final Expression constraint, final ColumnsSupplier<Double> aColumns,
            final FloatingPointReceptacle bValues, int aRowDim, final int aColDim, final ExpressionsBasedModel model) {

        double ub = constraint.getUpperLimit(ADJUSTED, POSITIVE_INFINITY);
        double lb = constraint.getLowerLimit(ADJUSTED, NEGATIVE_INFINITY);

        double bound;
        boolean negate;
        if (ub < POSITIVE_INFINITY) {
            bound = ub;
            negate = false;
        } else if (lb > NEGATIVE_INFINITY) {
            bound = lb;
            negate = true;
        } else {
            return 0;
        }

        List<Integer> varIdx = new ArrayList<>();
        for (IntRowColumn key : constraint.getQuadraticKeySet()) {
            if (!varIdx.contains(key.row)) {
                varIdx.add(key.row);
            }
            if (!varIdx.contains(key.column)) {
                varIdx.add(key.column);
            }
        }

        int dim = varIdx.size();
        if (dim == 0) {
            return 0;
        }

        PhysicalStore<Double> qStore = R064Store.FACTORY.make(dim, dim);
        for (IntRowColumn key : constraint.getQuadraticKeySet()) {
            int i = varIdx.indexOf(key.row);
            int j = varIdx.indexOf(key.column);
            double val = HALF * constraint.doubleValue(key, ADJUSTED);
            if (negate) {
                val = -val;
            }
            qStore.add(i, j, val);
            qStore.add(j, i, val);
        }

        Cholesky<Double> cholesky = Cholesky.R064.make(qStore);
        if (!cholesky.compute(qStore)) {
            return 0;
        }
        MatrixStore<Double> lStore = cholesky.getL();

        bValues.append(Math.sqrt(negate ? -bound : bound));

        SolverClarabel.ensureColumns(aColumns, aColDim);

        int rowsInA = 0;
        for (int i = 0; i < dim; i++) {
            int row = aRowDim + 1 + rowsInA;
            for (int j = 0; j < dim; j++) {
                double lij = lStore.doubleValue(i, j);
                if (lij != ZERO) {
                    int col = varIdx.get(j);
                    aColumns.add(row, col, lij);
                }
            }
            bValues.append(ZERO);
            rowsInA++;
        }

        return 1 + rowsInA;
    }

    private static void ensureColumns(final ColumnsSupplier<Double> columns, final int required) {
        if (columns.getColDim() < required) {
            columns.addColumns(required - columns.getColDim());
        }
    }

    private static void initCscMatrix(final Arena arena, final MemorySegment cscSeg, final int rows, final int cols,
            final ColumnsSupplier<Double> columns) {

        int nnz = 0;
        for (int c = 0; c < cols; c++) {
            if (c < columns.getColDim()) {
                SparseArray<Double> column = columns.getColumn(c);
                for (NonzeroView<Double> ignored : column.nonzeros()) {
                    nnz++;
                }
            }
        }

        long[] colPtr = new long[cols + 1];
        long[] rowIdx = new long[nnz];
        double[] values = new double[nnz];

        int cursor = 0;
        for (int c = 0; c < cols; c++) {
            colPtr[c] = cursor;
            if (c < columns.getColDim()) {
                SparseArray<Double> column = columns.getColumn(c);
                for (NonzeroView<Double> nz : column.nonzeros()) {
                    rowIdx[cursor] = nz.index();
                    values[cursor] = nz.doubleValue();
                    cursor++;
                }
            }
        }
        colPtr[cols] = cursor;

        MemorySegment colPtrSeg = ClarabelBindings.allocateLongArray(arena, colPtr);
        MemorySegment rowIdxSeg = ClarabelBindings.allocateLongArray(arena, rowIdx);
        MemorySegment valuesSeg = ClarabelBindings.allocateDoubleArray(arena, values);

        ClarabelBindings.cscMatrixInit(cscSeg, rows, cols, colPtrSeg, rowIdxSeg, valuesSeg);
    }

    static Optimisation.State translate(final int status) {
        return switch (status) {
            case ClarabelBindings.STATUS_SOLVED -> Optimisation.State.OPTIMAL;
            case ClarabelBindings.STATUS_ALMOST_SOLVED, ClarabelBindings.STATUS_ALMOST_PRIMAL_INFEASIBLE,
                    ClarabelBindings.STATUS_ALMOST_DUAL_INFEASIBLE -> Optimisation.State.APPROXIMATE;
            case ClarabelBindings.STATUS_PRIMAL_INFEASIBLE -> Optimisation.State.INFEASIBLE;
            case ClarabelBindings.STATUS_DUAL_INFEASIBLE -> Optimisation.State.UNBOUNDED;
            case ClarabelBindings.STATUS_UNSOLVED -> Optimisation.State.UNEXPLORED;
            default -> Optimisation.State.FAILED;
        };
    }

    private final Arena myArena;
    private final boolean myMaximisation;
    private final int myNumberOfVariables;
    private final MemorySegment mySolver;

    SolverClarabel(final MemorySegment solver, final boolean maximisation, final int numberOfVariables, final Arena arena) {
        super();
        mySolver = solver;
        myMaximisation = maximisation;
        myNumberOfVariables = numberOfVariables;
        myArena = arena;
    }

    @Override
    public void dispose() {
        if (mySolver != null && !mySolver.equals(MemorySegment.NULL)) {
            ClarabelBindings.solverFree(mySolver);
        }
        myArena.close();
    }

    @Override
    public Optimisation.Result solve(final Result kickStarter) {

        ClarabelBindings.solverSolve(mySolver);

        MemorySegment solution = ClarabelBindings.solverSolution(myArena, mySolver);

        int status = ClarabelBindings.solutionStatus(solution);
        Optimisation.State state = SolverClarabel.translate(status);

        if (state.isFeasible() && state != Optimisation.State.UNBOUNDED) {

            double obj = ClarabelBindings.solutionObjVal(solution);
            if (myMaximisation) {
                obj = -obj;
            }

            MemorySegment xPtr = ClarabelBindings.solutionX(solution);
            long xLen = ClarabelBindings.solutionXLength(solution);

            int n = (int) Math.min(xLen, myNumberOfVariables);
            double[] x = xPtr.reinterpret(xLen * ValueLayout.JAVA_DOUBLE.byteSize()).toArray(ValueLayout.JAVA_DOUBLE);

            if (x.length > myNumberOfVariables) {
                double[] trimmed = new double[myNumberOfVariables];
                System.arraycopy(x, 0, trimmed, 0, myNumberOfVariables);
                x = trimmed;
            }

            return Optimisation.Result.of(obj, state, x);

        } else {

            return Optimisation.Result.of(state, new double[myNumberOfVariables]);
        }
    }

}
