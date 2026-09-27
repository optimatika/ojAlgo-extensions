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
package org.ojalgo.optimisation.solver.clarabel;

import static org.ojalgo.function.constant.PrimitiveMath.*;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

import org.ojalgo.array.ArrayR064;
import org.ojalgo.array.SparseArray;
import org.ojalgo.array.SparseArray.NonzeroView;
import org.ojalgo.matrix.decomposition.Cholesky;
import org.ojalgo.matrix.decomposition.Eigenvalue;
import org.ojalgo.matrix.store.ColumnsSupplier;
import org.ojalgo.matrix.store.MatrixStore;
import org.ojalgo.matrix.store.PhysicalStore;
import org.ojalgo.matrix.store.R064Store;
import org.ojalgo.optimisation.ConstraintsMetaData;
import org.ojalgo.optimisation.Expression;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.ModelEntity;
import org.ojalgo.optimisation.Optimisation;
import org.ojalgo.optimisation.Variable;
import org.ojalgo.structure.Structure1D.IntIndex;
import org.ojalgo.structure.Structure2D.IntRowColumn;
import org.ojalgo.type.FloatingPointReceptacle;
import org.ojalgo.type.keyvalue.EntryPair;

/**
 * Clarabel solver integration for ojAlgo using Panama FFI.
 * <p>
 * Clarabel is an interior-point solver for convex conic programs. This integration calls the Clarabel C
 * library directly via the Foreign Function &amp; Memory API, and covers LP, QP and (convex) QCQP - the
 * latter by translating quadratic constraints into second-order cones. Clarabel's semidefinite cones are not
 * used.
 *
 * @see <a href="https://github.com/oxfordcontrol/Clarabel.rs">Clarabel.rs</a>
 */
public final class SolverClarabel implements Optimisation.Solver {

    public static class Configurator {

        /**
         * The feasibility tolerance, for the primal and the dual residuals alike.
         */
        private static final double TOL_FEAS = 5E-9;
        /**
         * The absolute and relative duality gap tolerances.
         */
        private static final double TOL_GAP_ABS = 5E-9;
        private static final double TOL_GAP_REL = 1E-11;
        /**
         * The absolute and relative infeasibility detection tolerances.
         */
        private static final double TOL_INFEAS_ABS = 5E-9;
        private static final double TOL_INFEAS_REL = 1E-11;

        protected void configure(final MemorySegment settings, final Optimisation.Options options) {

            if (options.iterations_abort > 0L) {
                ClarabelBindings.setMaxIter(settings, Math.min(Integer.MAX_VALUE, options.iterations_abort));
            }

            if (options.time_abort > 0L) {
                ClarabelBindings.setTimeLimit(settings, options.time_abort / 1_000.0);
            }

            boolean verbose = options.logger_appender != null && options.logger_detailed;
            ClarabelBindings.setVerbose(settings, verbose);

            // Clarabel has no tolerance for primal feasibility alone - tol_feas applies to the dual residual as
            // well - so options.feasibility is not used.
            ClarabelBindings.setTolFeas(settings, TOL_FEAS);
            ClarabelBindings.setTolInfeasRel(settings, TOL_INFEAS_REL);
            ClarabelBindings.setTolInfeasAbs(settings, TOL_INFEAS_ABS);
            ClarabelBindings.setTolGapRel(settings, TOL_GAP_REL);
            ClarabelBindings.setTolGapAbs(settings, TOL_GAP_ABS);

            int parallelism = options.getParallelism();
            if (parallelism > 0) {
                ClarabelBindings.setMaxThreads(settings, parallelism);
            }
        }

    }

    static final class Integration extends ExpressionsBasedModel.Integration<SolverClarabel> {

        /**
         * Variable bounds are rows of A, with coefficient 1 (or the variable's adjustment factor if adjusted).
         */
        private static final boolean ADJUSTED_BOUNDS = false;
        /**
         * Applies to the quadratic constraints too.
         */
        private static final boolean ADJUSTED_CONSTRAINTS = true;
        private static final boolean ADJUSTED_OBJECTIVE = true;

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

            // The model entity (and constraint type) of each dual value, and where to find it among Clarabel's
            // dual variables: the first row and the number of rows to sum.
            List<EntryPair<ModelEntity<?>, ConstraintType>> dualEntries = new ArrayList<>();
            List<int[]> dualRows = new ArrayList<>();
            BitSet dualAdjusted = new BitSet();

            // Each variable's bound rows (-1 if none), and their coefficient. The "upper" row is the one where
            // the variable has a positive coefficient - its upper bound, or its level if it is fixed.
            int[] upperRows = new int[nbProbVars];
            int[] lowerRows = new int[nbProbVars];
            double[] boundScales = new double[nbProbVars];
            Arrays.fill(upperRows, -1);
            Arrays.fill(lowerRows, -1);

            // Linear equality constraints and fixed variables -> ZeroCone. (Fixed variables as a pair of
            // opposing inequalities would leave no interior for the interior point method.)
            Expression[] linEquExprs = model.constraints().filter(expr -> !expr.isAnyQuadraticFactorNonZero()).filter(Expression::isEqualityConstraint)
                    .toArray(Expression[]::new);

            int zeroCount = 0;
            for (Expression expression : linEquExprs) {
                SolverClarabel.ensureColumns(aColumns, aColDim);
                for (IntIndex key : expression.getLinearKeySet()) {
                    aColumns.add(aRowDim, key.index, expression.doubleValue(key, ADJUSTED_CONSTRAINTS));
                }
                bValues.append(expression.getUpperLimit(ADJUSTED_CONSTRAINTS, POSITIVE_INFINITY));
                dualAdjusted.set(dualEntries.size(), ADJUSTED_CONSTRAINTS);
                SolverClarabel.addDual(dualEntries, dualRows, expression, ConstraintType.EQUALITY, aRowDim, 1);
                aRowDim++;
                zeroCount++;
            }
            for (Variable variable : variables) {
                if (variable.isEqualityConstraint()) {
                    int col = model.indexOf(variable);
                    double val = ADJUSTED_BOUNDS ? variable.getAdjustmentFactor() : ONE;
                    SolverClarabel.ensureColumns(aColumns, aColDim);
                    aColumns.add(aRowDim, col, val);
                    bValues.append(variable.getUpperLimit(ADJUSTED_BOUNDS, POSITIVE_INFINITY));
                    dualAdjusted.set(dualEntries.size(), ADJUSTED_BOUNDS);
                    SolverClarabel.addDual(dualEntries, dualRows, variable, ConstraintType.EQUALITY, aRowDim, 1);
                    boundScales[col] = val;
                    upperRows[col] = aRowDim;
                    aRowDim++;
                    zeroCount++;
                }
            }
            if (zeroCount > 0) {
                coneSpecs.add(new int[] { ClarabelBindings.CONE_ZERO, zeroCount });
            }

            // Linear inequality constraints + variable bounds -> NonnegativeCone
            Expression[] linInqExprs = model.constraints().filter(constr -> !constr.isAnyQuadraticFactorNonZero())
                    .filter(constr -> !constr.isEqualityConstraint()).toArray(Expression[]::new);

            int nnCount = 0;
            for (Expression expression : linInqExprs) {
                double lb = expression.getLowerLimit(ADJUSTED_CONSTRAINTS, NEGATIVE_INFINITY);
                double ub = expression.getUpperLimit(ADJUSTED_CONSTRAINTS, POSITIVE_INFINITY);

                SolverClarabel.ensureColumns(aColumns, aColDim);

                if (ub < POSITIVE_INFINITY) {
                    for (IntIndex key : expression.getLinearKeySet()) {
                        aColumns.add(aRowDim, key.index, expression.doubleValue(key, ADJUSTED_CONSTRAINTS));
                    }
                    bValues.append(ub);
                    dualAdjusted.set(dualEntries.size(), ADJUSTED_CONSTRAINTS);
                    SolverClarabel.addDual(dualEntries, dualRows, expression, ConstraintType.UPPER, aRowDim, 1);
                    aRowDim++;
                    nnCount++;
                }
                if (lb > NEGATIVE_INFINITY) {
                    for (IntIndex key : expression.getLinearKeySet()) {
                        aColumns.add(aRowDim, key.index, -expression.doubleValue(key, ADJUSTED_CONSTRAINTS));
                    }
                    bValues.append(-lb);
                    dualAdjusted.set(dualEntries.size(), ADJUSTED_CONSTRAINTS);
                    SolverClarabel.addDual(dualEntries, dualRows, expression, ConstraintType.LOWER, aRowDim, 1);
                    aRowDim++;
                    nnCount++;
                }
            }

            for (Variable variable : variables) {
                if (variable.isEqualityConstraint()) {
                    continue;
                }
                int col = model.indexOf(variable);
                double lb = variable.getLowerLimit(ADJUSTED_BOUNDS, NEGATIVE_INFINITY);
                double ub = variable.getUpperLimit(ADJUSTED_BOUNDS, POSITIVE_INFINITY);
                double val = ADJUSTED_BOUNDS ? variable.getAdjustmentFactor() : ONE;

                SolverClarabel.ensureColumns(aColumns, aColDim);

                boundScales[col] = val;

                if (ub < POSITIVE_INFINITY) {
                    aColumns.add(aRowDim, col, val);
                    bValues.append(ub);
                    dualAdjusted.set(dualEntries.size(), ADJUSTED_BOUNDS);
                    SolverClarabel.addDual(dualEntries, dualRows, variable, ConstraintType.UPPER, aRowDim, 1);
                    upperRows[col] = aRowDim;
                    aRowDim++;
                    nnCount++;
                }
                if (lb > NEGATIVE_INFINITY) {
                    aColumns.add(aRowDim, col, -val);
                    bValues.append(-lb);
                    dualAdjusted.set(dualEntries.size(), ADJUSTED_BOUNDS);
                    SolverClarabel.addDual(dualEntries, dualRows, variable, ConstraintType.LOWER, aRowDim, 1);
                    lowerRows[col] = aRowDim;
                    aRowDim++;
                    nnCount++;
                }
            }
            if (nnCount > 0) {
                coneSpecs.add(new int[] { ClarabelBindings.CONE_NONNEGATIVE, nnCount });
            }

            // Quadratic constraints -> SecondOrderCone
            Expression[] quadExprs = model.constraints().filter(Expression::isAnyQuadraticFactorNonZero).toArray(Expression[]::new);
            for (Expression expression : quadExprs) {
                int socRows = SolverClarabel.addSecondOrderCone(expression, aColumns, bValues, aRowDim, aColDim);
                if (socRows > 0) {
                    coneSpecs.add(new int[] { ClarabelBindings.CONE_SECOND_ORDER, socRows });
                    // The constraint's multiplier is the sum of the cone's first two dual variables
                    ConstraintType type = expression.isUpperLimitSet() ? ConstraintType.UPPER : ConstraintType.LOWER;
                    dualAdjusted.set(dualEntries.size(), ADJUSTED_CONSTRAINTS);
                    SolverClarabel.addDual(dualEntries, dualRows, expression, type, aRowDim, 2);
                    aRowDim += socRows;
                }
            }

            boolean max = model.getOptimisationSense() == Optimisation.Sense.MAX;

            Expression mObj = model.objective();
            double objectiveScale = ADJUSTED_OBJECTIVE ? mObj.getAdjustmentFactor() : ONE;

            double[] q = new double[nbProbVars];
            for (IntIndex key : mObj.getLinearKeySet()) {
                double val = mObj.doubleValue(key, ADJUSTED_OBJECTIVE);
                q[key.index] = max ? -val : val;
            }

            // P - quadratic part of the objective, upper triangular. A quadratic key (row,col) with value v
            // contributes v*x_row*x_col to the objective; in the 0.5 x'Px form that is P[row][col] = v off the
            // diagonal (in the upper triangle, whatever the key's order), and P[row][row] = 2v on it.
            ColumnsSupplier<Double> pColumns = R064Store.FACTORY.makeColumnsSupplier(Integer.MAX_VALUE);
            SolverClarabel.ensureColumns(pColumns, nbProbVars);
            for (IntRowColumn key : mObj.getQuadraticKeySet()) {
                double val = mObj.doubleValue(key, ADJUSTED_OBJECTIVE);
                if (max) {
                    val = -val;
                }
                int row = Math.min(key.row, key.column);
                int col = Math.max(key.row, key.column);
                pColumns.add(row, col, row == col ? val + val : val);
            }

            ConstraintsMetaData constraintsMetaData = ConstraintsMetaData.newEntityMap(dualEntries.size());
            constraintsMetaData.setMultiplierScale(objectiveScale);
            for (int i = 0; i < dualEntries.size(); i++) {
                EntryPair<ModelEntity<?>, ConstraintType> entry = dualEntries.get(i);
                constraintsMetaData.setEntry(i, entry.left(), entry.right(), false, dualAdjusted.get(i));
            }

            double objectiveConstant = ExpressionsBasedModel.Integration.getObjectiveConstant(model);

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
                    slot.set(ValueLayout.JAVA_LONG, 8, spec[1]);
                }

                // Get default settings and configure
                MemorySegment settings = ClarabelBindings.defaultSettings(arena);
                configurator.configure(settings, model.options);

                // Create solver
                MemorySegment solver = ClarabelBindings.solverNew(pSeg, qSeg, aSeg, bSeg, nCones, conesSeg, settings);

                return new SolverClarabel(solver, max, nbProbVars, arena, constraintsMetaData, dualRows.toArray(new int[dualRows.size()][]), upperRows,
                        lowerRows, boundScales, objectiveScale, objectiveConstant);

            } catch (RuntimeException e) {
                arena.close();
                throw e;
            }
        }

        /**
         * Clarabel handles any continuous convex model - LP, QP and QCQP - but not integer variables.
         * <p>
         * Convexity of a quadratic constraint cannot be determined without factorising its quadratic form,
         * which is too expensive to do here. It is checked when the solver is built, and a non-convex
         * constraint is reported by {@link #build(ExpressionsBasedModel)} throwing.
         */
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

    static final Configurator DEFAULT = new Configurator();

    private static void addDual(final List<EntryPair<ModelEntity<?>, ConstraintType>> entries, final List<int[]> rows, final ModelEntity<?> entity,
            final ConstraintType type, final int row, final int nbRows) {
        entries.add(EntryPair.of(entity, type));
        rows.add(new int[] { row, nbRows });
    }

    /**
     * Model a quadratic constraint x'Qx + c'x &lt;= bound as a second-order cone.
     * <p>
     * With Q factorised as M'M (so x'Qx = ||Mx||²) and t = bound - c'x, the constraint ||Mx||² &lt;= t is
     * equivalent to ||(2Mx, t-1)||₂ &lt;= t+1. Clarabel expects s = b - Ax in the cone, with s₁ &gt;=
     * ||s₂..||, which gives these rows:
     * <ol>
     * <li>s₁ = (bound + 1) - c'x
     * <li>s₂ = (bound - 1) - c'x
     * <li>s₃.. = -2Mx
     * </ol>
     * Note that this also enforces t &gt;= 0, as it must, since x'Qx is non-negative for convex Q.
     *
     * @return the number of rows added (the cone dimension)
     */
    private static int addSecondOrderCone(final Expression constraint, final ColumnsSupplier<Double> aColumns, final FloatingPointReceptacle bValues,
            final int aRowDim, final int aColDim) {

        double ub = constraint.getUpperLimit(Integration.ADJUSTED_CONSTRAINTS, POSITIVE_INFINITY);
        double lb = constraint.getLowerLimit(Integration.ADJUSTED_CONSTRAINTS, NEGATIVE_INFINITY);

        boolean hasUpper = ub < POSITIVE_INFINITY;
        boolean hasLower = lb > NEGATIVE_INFINITY;

        if (!hasUpper && !hasLower) {
            return 0;
        }

        if (hasUpper && hasLower) {
            throw new IllegalArgumentException("Clarabel needs convex quadratic constraints, and '" + constraint.getName()
                    + "' is two-sided (or an equality) - the region between two quadratic limits is not convex");
        }

        // Model as x'Qx + c'x <= bound, negating the coefficients of a lower-limited constraint
        boolean negate = !hasUpper;
        double bound = negate ? -lb : ub;

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
            double val = HALF * constraint.doubleValue(key, Integration.ADJUSTED_CONSTRAINTS);
            if (negate) {
                val = -val;
            }
            qStore.add(i, j, val);
            qStore.add(j, i, val);
        }

        MatrixStore<Double> mtrxM = SolverClarabel.factorise(qStore, constraint);
        int mRows = mtrxM.getRowDim();

        SolverClarabel.ensureColumns(aColumns, aColDim);

        // The two rows carrying t = bound - c'x. Both get the same linear coefficients; they differ only
        // in the constant, which is where the +1 / -1 of the reformulation goes.
        for (IntIndex key : constraint.getLinearKeySet()) {
            double val = constraint.doubleValue(key, Integration.ADJUSTED_CONSTRAINTS);
            if (negate) {
                val = -val;
            }
            aColumns.add(aRowDim, key.index, val);
            aColumns.add(aRowDim + 1, key.index, val);
        }
        bValues.append(bound + ONE);
        bValues.append(bound - ONE);

        for (int i = 0; i < mRows; i++) {
            int row = aRowDim + 2 + i;
            for (int j = 0; j < dim; j++) {
                double mij = mtrxM.doubleValue(i, j);
                if (mij != ZERO) {
                    int col = varIdx.get(j).intValue();
                    aColumns.add(row, col, TWO * mij);
                }
            }
            bValues.append(ZERO);
        }

        return 2 + mRows;
    }

    private static void ensureColumns(final ColumnsSupplier<Double> columns, final int required) {
        if (columns.getColDim() < required) {
            columns.addColumns(required - columns.getColDim());
        }
    }

    /**
     * Factorise a symmetric positive semi-definite Q as M'M, so that x'Qx = ||Mx||².
     * <p>
     * Cholesky does this in roughly n³/3 operations and doubles as the convexity test - it succeeds exactly
     * when Q is positive definite. Since Q = LL' = R'R, the R factor is the M wanted here.
     *
     * @return a matrix M with M'M = Q
     */
    private static MatrixStore<Double> factorise(final PhysicalStore<Double> qStore, final Expression constraint) {

        Cholesky<Double> cholesky = Cholesky.R064.make(qStore);

        if (cholesky.decompose(qStore) && cholesky.isSPD()) {
            return cholesky.getR();
        }

        return SolverClarabel.factoriseSemiDefinite(qStore, constraint);
    }

    /**
     * The fallback for when Cholesky fails, which happens both for a positive <em>semi</em>-definite Q - a
     * perfectly valid convex constraint - and for an indefinite, genuinely non-convex, one. Telling those two
     * apart needs the eigenvalues, so that cost is only paid here, once Cholesky has already declined. Zero
     * eigenvalues simply contribute no rows, and a negative one is reported.
     *
     * @return a matrix M with M'M = Q, having one row per strictly positive eigenvalue
     */
    private static MatrixStore<Double> factoriseSemiDefinite(final PhysicalStore<Double> qStore, final Expression constraint) {

        int dim = qStore.getColDim();

        Eigenvalue<Double> evd = Eigenvalue.R064.make(qStore, true);

        if (!evd.decompose(qStore)) {
            throw new IllegalArgumentException("Failed to factorise the quadratic form of constraint '" + constraint.getName() + "'");
        }

        MatrixStore<Double> mtrxD = evd.getD();
        MatrixStore<Double> mtrxV = evd.getV();

        double largest = ZERO;
        for (int i = 0; i < dim; i++) {
            largest = Math.max(largest, Math.abs(mtrxD.doubleValue(i, i)));
        }
        double tolerance = dim * Math.max(largest, ONE) * MACHINE_EPSILON;

        List<Integer> positive = new ArrayList<>();
        for (int i = 0; i < dim; i++) {
            double eigenvalue = mtrxD.doubleValue(i, i);
            if (eigenvalue < -tolerance) {
                throw new IllegalArgumentException("Clarabel needs convex quadratic constraints, and '" + constraint.getName()
                        + "' has an indefinite quadratic form (eigenvalue " + eigenvalue + ")");
            }
            if (eigenvalue > tolerance) {
                positive.add(i);
            }
        }

        PhysicalStore<Double> retVal = R064Store.FACTORY.make(positive.size(), dim);
        for (int r = 0; r < positive.size(); r++) {
            int i = positive.get(r).intValue();
            double scale = Math.sqrt(mtrxD.doubleValue(i, i));
            for (int j = 0; j < dim; j++) {
                retVal.set(r, j, scale * mtrxV.doubleValue(j, i));
            }
        }

        return retVal;
    }

    private static void initCscMatrix(final Arena arena, final MemorySegment cscSeg, final int rows, final int cols, final ColumnsSupplier<Double> columns) {

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
            case ClarabelBindings.STATUS_SOLVED                                                                                                                                                       ->
                Optimisation.State.OPTIMAL;
            case ClarabelBindings.STATUS_ALMOST_SOLVED, ClarabelBindings.STATUS_ALMOST_PRIMAL_INFEASIBLE, ClarabelBindings.STATUS_ALMOST_DUAL_INFEASIBLE,
                    ClarabelBindings.STATUS_CALLBACK_TERMINATED                                                                                                                                       ->
                Optimisation.State.APPROXIMATE;
            case ClarabelBindings.STATUS_PRIMAL_INFEASIBLE                                                                                                                                            ->
                Optimisation.State.INFEASIBLE;
            case ClarabelBindings.STATUS_DUAL_INFEASIBLE                                                                                                                                              ->
                Optimisation.State.UNBOUNDED;
            case ClarabelBindings.STATUS_UNSOLVED                                                                                                                                                     ->
                Optimisation.State.UNEXPLORED;
            default                                                                                                                                                                                   ->
                Optimisation.State.FAILED;
        };
    }

    private final Arena myArena;
    /**
     * For each variable, the coefficient of its bound rows: the variable's adjustment factor if the bounds are
     * adjusted, or else 1.
     */
    private final double[] myBoundScales;
    private final ConstraintsMetaData myConstraintsMetaData;
    /**
     * For each dual value (entry in {@link #myConstraintsMetaData}): the first of Clarabel's dual variables, and
     * the number of them to sum.
     */
    private final int[][] myDualRows;
    /**
     * The upcall stub Clarabel calls to ask whether it should stop. Kept in a field because the automatic
     * arena it lives in frees it once nothing refers to it any more.
     */
    private MemorySegment myInterruptCallback = null;
    /**
     * For each variable, the row of its lower bound, or -1 if it has none.
     */
    private final int[] myLowerRows;
    private final boolean myMaximisation;
    private final int myNumberOfVariables;
    /**
     * The objective constant, in model units and the model's sense.
     */
    private final double myObjectiveConstant;
    /**
     * The objective's adjustment factor – the objective Clarabel solves is scaled by this.
     */
    private final double myObjectiveScale;
    /**
     * The thread inside {@link #solve(Result)}, whose interrupt flag the callback reports.
     */
    private volatile Thread mySolving = null;
    private final MemorySegment mySolver;
    /**
     * For each variable, the row of its upper bound (or its level, if it is fixed), or -1 if it has none.
     */
    private final int[] myUpperRows;

    SolverClarabel(final MemorySegment solver, final boolean maximisation, final int numberOfVariables, final Arena arena,
            final ConstraintsMetaData constraintsMetaData, final int[][] dualRows, final int[] upperRows, final int[] lowerRows, final double[] boundScales,
            final double objectiveScale, final double objectiveConstant) {
        super();
        mySolver = solver;
        myMaximisation = maximisation;
        myNumberOfVariables = numberOfVariables;
        myArena = arena;
        myConstraintsMetaData = constraintsMetaData;
        myDualRows = dualRows;
        myUpperRows = upperRows;
        myLowerRows = lowerRows;
        myBoundScales = boundScales;
        myObjectiveScale = objectiveScale;
        myObjectiveConstant = objectiveConstant;
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

        mySolving = Thread.currentThread();
        this.enableInterrupt();

        try {
            ClarabelBindings.solverSolve(mySolver);
        } finally {
            mySolving = null;
        }

        MemorySegment solution = ClarabelBindings.solverSolution(myArena, mySolver);

        int status = ClarabelBindings.solutionStatus(solution);
        Optimisation.State state = SolverClarabel.translate(status);

        if (state.isFeasible() && state != Optimisation.State.UNBOUNDED) {

            double obj = ClarabelBindings.solutionObjVal(solution) / myObjectiveScale;
            if (myMaximisation) {
                obj = -obj;
            }
            obj += myObjectiveConstant;

            MemorySegment xPtr = ClarabelBindings.solutionX(solution);
            long xLen = ClarabelBindings.solutionXLength(solution);

            double[] x = xPtr.reinterpret(xLen * ValueLayout.JAVA_DOUBLE.byteSize()).toArray(ValueLayout.JAVA_DOUBLE);

            if (x.length > myNumberOfVariables) {
                double[] trimmed = new double[myNumberOfVariables];
                System.arraycopy(x, 0, trimmed, 0, myNumberOfVariables);
                x = trimmed;
            }

            MemorySegment zPtr = ClarabelBindings.solutionZ(solution);
            long zLen = ClarabelBindings.solutionZLength(solution);

            double[] z = zPtr.reinterpret(zLen * ValueLayout.JAVA_DOUBLE.byteSize()).toArray(ValueLayout.JAVA_DOUBLE);

            double[] duals = new double[myDualRows.length];
            for (int i = 0; i < duals.length; i++) {
                int[] rows = myDualRows[i];
                for (int r = rows[0], limit = rows[0] + rows[1]; r < limit; r++) {
                    duals[i] += z[r];
                }
            }

            double[] reducedGradient = this.computeReducedGradient(z);

            return Optimisation.Result.of(obj, state, x).withDualValues(myConstraintsMetaData, () -> ArrayR064.wrap(duals))
                    .withReducedGradient(() -> ArrayR064.wrap(reducedGradient));

        } else {

            return Optimisation.Result.of(state, new double[myNumberOfVariables]);
        }
    }

    /**
     * The reduced gradient, in the bounded-variable sense, in model units and the model's sense. Clarabel's
     * solution satisfies Px + q + A'z = 0, so the objective's gradient plus the constraint rows' terms equals
     * minus the variable-bound rows' terms – non-zero only for variables at an active bound.
     */
    private double[] computeReducedGradient(final double[] z) {

        double scale = myMaximisation ? -ONE / myObjectiveScale : ONE / myObjectiveScale;

        double[] retVal = new double[myNumberOfVariables];
        for (int j = 0; j < myNumberOfVariables; j++) {
            double lowerDual = myLowerRows[j] >= 0 ? z[myLowerRows[j]] : ZERO;
            double upperDual = myUpperRows[j] >= 0 ? z[myUpperRows[j]] : ZERO;
            retVal[j] = myBoundScales[j] * (lowerDual - upperDual) * scale;
        }
        return retVal;
    }

    /**
     * Hands Clarabel a callback that lets an interrupt of the solving thread stop the solve. Clarabel asks it
     * once per iteration, from the thread doing the solving, so nothing needs to watch that thread from
     * outside.
     * <p>
     * Registered once and left in place. Silently skipped on a Clarabel too old to have the callback, since
     * there is no way to stop such a build and failing the solve over it would be worse.
     */
    private void enableInterrupt() {

        if (myInterruptCallback != null || !ClarabelBindings.hasTerminationCallback()) {
            return;
        }

        myInterruptCallback = ClarabelBindings.setTerminationCallback(mySolver, this::isStopWanted);
    }

    /**
     * Asked by Clarabel, once per iteration, while solving.
     */
    private boolean isStopWanted() {
        Thread solving = mySolving;
        return solving != null && solving.isInterrupted();
    }

}
