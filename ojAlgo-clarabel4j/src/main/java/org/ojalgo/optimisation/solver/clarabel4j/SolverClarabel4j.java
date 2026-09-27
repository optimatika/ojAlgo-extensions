package org.ojalgo.optimisation.solver.clarabel4j;

import static org.ojalgo.function.constant.PrimitiveMath.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

import org.ojalgo.array.ArrayR064;
import org.ojalgo.matrix.decomposition.Cholesky;
import org.ojalgo.matrix.decomposition.Eigenvalue;
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

import com.ustermetrics.clarabel4j.*;

/**
 * Integration wrapper around the Clarabel native solver (clarabel4j).
 * <p>
 * This class converts an ojAlgo {@link ExpressionsBasedModel} into the sparse CSC representations expected by
 * Clarabel and exposes the clarabel4j {@link com.ustermetrics.clarabel4j.Model} as an ojAlgo
 * {@link Optimisation.Solver}.
 */
public final class SolverClarabel4j implements Optimisation.Solver {

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

        protected void configure(final Optimisation.Options optimisationOptions, final Parameters.ParametersBuilder parametersBuilder) {

            // Iteration limit
            if (optimisationOptions.iterations_abort > 0L) {
                int maxIter = Math.min(Integer.MAX_VALUE, optimisationOptions.iterations_abort);
                parametersBuilder.maxIter(Integer.valueOf(maxIter));
            }

            // Time limit (ojAlgo stores ms; Clarabel expects seconds)
            if (optimisationOptions.time_abort > 0L) {
                double seconds = optimisationOptions.time_abort / 1_000.0;
                parametersBuilder.timeLimit(Double.valueOf(seconds));
            }

            // Verbosity: if the user has a logger configured and detailed logging is enabled,
            // turn on Clarabel's verbose output.
            boolean verbose = optimisationOptions.logger_appender != null && optimisationOptions.logger_detailed;
            parametersBuilder.verbose(Boolean.valueOf(verbose));

            // Clarabel has no tolerance for primal feasibility alone - tol_feas applies to the dual residual as
            // well - so options.feasibility is not used.
            parametersBuilder.tolFeas(Double.valueOf(TOL_FEAS));
            parametersBuilder.tolInfeasRel(Double.valueOf(TOL_INFEAS_REL));
            parametersBuilder.tolInfeasAbs(Double.valueOf(TOL_INFEAS_ABS));
            parametersBuilder.tolGapRel(Double.valueOf(TOL_GAP_REL));
            parametersBuilder.tolGapAbs(Double.valueOf(TOL_GAP_ABS));

            // The reduced ("almost") tolerances, by which Clarabel decides its almost solved and almost
            // infeasible statuses when the full ones are not reached, are left at Clarabel's defaults.

            int parallelism = optimisationOptions.getParallelism();
            if (parallelism > 0) {
                parametersBuilder.maxThreads(Integer.valueOf(parallelism));
            }
        }

        /**
         * Configure the Clarabel model using ojAlgo {@link Optimisation.Options}.
         * <p>
         * This implementation maps a small subset of generic options (verbosity and simple limits) into
         * Clarabel {@link Parameters} using its builder, and wires Clarabel's textual output into ojAlgo's
         * logging when a logger is present.
         */
        final void configure(final com.ustermetrics.clarabel4j.Model model, final Options options) {

            Parameters.ParametersBuilder builder = Parameters.builder();

            this.configure(options, builder);

            Parameters parameters = builder.build();

            model.setParameters(parameters);

            //  model.setOutput(output);
        }

    }

    static final class Integration extends ExpressionsBasedModel.Integration<SolverClarabel4j> {

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
        public SolverClarabel4j build(final ExpressionsBasedModel model) {

            Configurator configurator = model.getConfigurator(DEFAULT);

            List<Variable> variables = model.getVariables();

            // The number of model variables present in the solver problem
            int nbProbVars = variables.size();

            List<Cone> cones = new ArrayList<>();

            MatrixBuilder bldrA = new MatrixBuilder(0, nbProbVars);
            FloatingPointReceptacle bldrB = new FloatingPointReceptacle();

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

            int row = 0;

            // Linear equality constraints and fixed variables -> ZeroCone. (Fixed variables as a pair of
            // opposing inequalities would leave no interior for the interior point method.)
            Expression[] linEquExprs = model.constraints().filter(expr -> !expr.isAnyQuadraticFactorNonZero()).filter(Expression::isEqualityConstraint)
                    .toArray(Expression[]::new);

            for (Expression expression : linEquExprs) {
                for (IntIndex key : expression.getLinearKeySet()) {
                    bldrA.add(row, key.index, expression.doubleValue(key, ADJUSTED_CONSTRAINTS));
                }
                bldrB.append(expression.getUpperLimit(ADJUSTED_CONSTRAINTS, POSITIVE_INFINITY));
                dualAdjusted.set(dualEntries.size(), ADJUSTED_CONSTRAINTS);
                SolverClarabel4j.addDual(dualEntries, dualRows, expression, ConstraintType.EQUALITY, row, 1);
                row++;
            }
            for (Variable variable : variables) {
                if (variable.isEqualityConstraint()) {
                    int col = model.indexOf(variable);
                    double val = ADJUSTED_BOUNDS ? variable.getAdjustmentFactor() : ONE;
                    bldrA.add(row, col, val);
                    bldrB.append(variable.getUpperLimit(ADJUSTED_BOUNDS, POSITIVE_INFINITY));
                    dualAdjusted.set(dualEntries.size(), ADJUSTED_BOUNDS);
                    SolverClarabel4j.addDual(dualEntries, dualRows, variable, ConstraintType.EQUALITY, row, 1);
                    boundScales[col] = val;
                    upperRows[col] = row;
                    row++;
                }
            }
            if (row > 0) {
                cones.add(new ZeroCone(row));
            }

            // Linear inequalities and variable bounds -> NonnegativeCone
            int nnStart = row;

            Expression[] linInqExprs = model.constraints().filter(constr -> !constr.isAnyQuadraticFactorNonZero())
                    .filter(constr -> !constr.isEqualityConstraint()).toArray(Expression[]::new);

            for (Expression expression : linInqExprs) {

                double lb = expression.getLowerLimit(ADJUSTED_CONSTRAINTS, NEGATIVE_INFINITY);
                double ub = expression.getUpperLimit(ADJUSTED_CONSTRAINTS, POSITIVE_INFINITY);

                if (ub < POSITIVE_INFINITY) {
                    for (IntIndex key : expression.getLinearKeySet()) {
                        bldrA.add(row, key.index, expression.doubleValue(key, ADJUSTED_CONSTRAINTS));
                    }
                    bldrB.append(ub);
                    dualAdjusted.set(dualEntries.size(), ADJUSTED_CONSTRAINTS);
                    SolverClarabel4j.addDual(dualEntries, dualRows, expression, ConstraintType.UPPER, row, 1);
                    row++;
                }

                if (lb > NEGATIVE_INFINITY) {
                    for (IntIndex key : expression.getLinearKeySet()) {
                        bldrA.add(row, key.index, -expression.doubleValue(key, ADJUSTED_CONSTRAINTS));
                    }
                    bldrB.append(-lb);
                    dualAdjusted.set(dualEntries.size(), ADJUSTED_CONSTRAINTS);
                    SolverClarabel4j.addDual(dualEntries, dualRows, expression, ConstraintType.LOWER, row, 1);
                    row++;
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
                boundScales[col] = val;

                if (ub < POSITIVE_INFINITY) {
                    bldrA.add(row, col, val);
                    bldrB.append(ub);
                    dualAdjusted.set(dualEntries.size(), ADJUSTED_BOUNDS);
                    SolverClarabel4j.addDual(dualEntries, dualRows, variable, ConstraintType.UPPER, row, 1);
                    upperRows[col] = row;
                    row++;
                }

                if (lb > NEGATIVE_INFINITY) {
                    bldrA.add(row, col, -val);
                    bldrB.append(-lb);
                    dualAdjusted.set(dualEntries.size(), ADJUSTED_BOUNDS);
                    SolverClarabel4j.addDual(dualEntries, dualRows, variable, ConstraintType.LOWER, row, 1);
                    lowerRows[col] = row;
                    row++;
                }
            }

            if (row > nnStart) {
                cones.add(new NonnegativeCone(row - nnStart));
            }

            // Quadratic constraints -> SecondOrderCone
            Expression[] quadExprs = model.constraints().filter(Expression::isAnyQuadraticFactorNonZero).toArray(Expression[]::new);

            for (Expression expression : quadExprs) {
                int socRows = SolverClarabel4j.addSecondOrderCone(expression, bldrA, bldrB, row);
                if (socRows > 0) {
                    cones.add(new SecondOrderCone(socRows));
                    // The constraint's multiplier is the sum of the cone's first two dual variables
                    ConstraintType type = expression.isUpperLimitSet() ? ConstraintType.UPPER : ConstraintType.LOWER;
                    dualAdjusted.set(dualEntries.size(), ADJUSTED_CONSTRAINTS);
                    SolverClarabel4j.addDual(dualEntries, dualRows, expression, type, row, 2);
                    row += socRows;
                }
            }

            ExponentialCone exponentialCone = SolverClarabel4j.makeExponentialCone(model);
            if (exponentialCone != null) {
                cones.add(exponentialCone);
            }

            PowerCone powerCone = SolverClarabel4j.makePowerCone(model);
            if (powerCone != null) {
                cones.add(powerCone);
            }

            GenPowerCone genPowerCone = SolverClarabel4j.makeGenPowerCone(model);
            if (genPowerCone != null) {
                cones.add(genPowerCone);
            }

            int m = row;
            int n = nbProbVars;

            boolean max = model.getOptimisationSense() == Optimisation.Sense.MAX;

            Expression mObj = model.objective();
            double objectiveScale = ADJUSTED_OBJECTIVE ? mObj.getAdjustmentFactor() : ONE;

            double[] q = new double[n];
            for (IntIndex key : mObj.getLinearKeySet()) {
                double val = mObj.doubleValue(key, ADJUSTED_OBJECTIVE);
                q[key.index] = max ? -val : val;
            }

            // P - the (symmetric) Hessian of the objective, of which Clarabel uses the upper triangle. A
            // quadratic key (row,col) with value v contributes v*x_row*x_col to the objective; in the 0.5 x'Px
            // form that is P[row][col] = P[col][row] = v off the diagonal, and P[row][row] = 2v on it.
            MatrixBuilder bldrP = new MatrixBuilder(n, n);
            for (IntRowColumn key : mObj.getQuadraticKeySet()) {
                double val = mObj.doubleValue(key, ADJUSTED_OBJECTIVE);
                bldrP.add(key.row, key.column, max ? -val : val);
                bldrP.add(key.column, key.row, max ? -val : val);
            }
            Matrix mtrxP = bldrP.build();

            Matrix mtrxA = null;
            double[] vctrB = null;
            if (m > 0) {
                mtrxA = bldrA.build();
                vctrB = bldrB.toDoubles();
            }

            ConstraintsMetaData constraintsMetaData = ConstraintsMetaData.newEntityMap(dualEntries.size());
            constraintsMetaData.setMultiplierScale(objectiveScale);
            for (int i = 0; i < dualEntries.size(); i++) {
                EntryPair<ModelEntity<?>, ConstraintType> entry = dualEntries.get(i);
                constraintsMetaData.setEntry(i, entry.left(), entry.right(), false, dualAdjusted.get(i));
            }

            double objectiveConstant = ExpressionsBasedModel.Integration.getObjectiveConstant(model);

            com.ustermetrics.clarabel4j.Model clarabelModel = new com.ustermetrics.clarabel4j.Model();

            configurator.configure(clarabelModel, model.options);

            if (m == 0) {
                clarabelModel.setup(mtrxP, q);
            } else {
                clarabelModel.setup(mtrxP, q, mtrxA, vctrB, cones);
            }

            return new SolverClarabel4j(clarabelModel, max, nbProbVars, constraintsMetaData, dualRows.toArray(new int[dualRows.size()][]), upperRows,
                    lowerRows, boundScales, objectiveScale, objectiveConstant);
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

    public static final ExpressionsBasedModel.Integration<SolverClarabel4j> INTEGRATION = new Integration();

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
    private static int addSecondOrderCone(final Expression constraint, final MatrixBuilder bldrA, final FloatingPointReceptacle bldrB, final int row) {

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

        MatrixStore<Double> mtrxM = SolverClarabel4j.factorise(qStore, constraint);
        int mRows = mtrxM.getRowDim();

        // The two rows carrying t = bound - c'x. Both get the same linear coefficients; they differ only
        // in the constant, which is where the +1 / -1 of the reformulation goes.
        for (IntIndex key : constraint.getLinearKeySet()) {
            double val = constraint.doubleValue(key, Integration.ADJUSTED_CONSTRAINTS);
            if (negate) {
                val = -val;
            }
            bldrA.add(row, key.index, val);
            bldrA.add(row + 1, key.index, val);
        }
        bldrB.append(bound + ONE);
        bldrB.append(bound - ONE);

        for (int i = 0; i < mRows; i++) {
            for (int j = 0; j < dim; j++) {
                double mij = mtrxM.doubleValue(i, j);
                if (mij != ZERO) {
                    bldrA.add(row + 2 + i, varIdx.get(j).intValue(), TWO * mij);
                }
            }
            bldrB.append(ZERO);
        }

        return 2 + mRows;
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

        return SolverClarabel4j.factoriseSemiDefinite(qStore, constraint);
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

    static ExponentialCone makeExponentialCone(final ExpressionsBasedModel model) {
        return null;
    }

    static GenPowerCone makeGenPowerCone(final ExpressionsBasedModel model) {
        return null;
    }

    static PowerCone makePowerCone(final ExpressionsBasedModel model) {
        return null;
    }

    static Optimisation.State translate(final Status status) {

        if (status == null) {
            return Optimisation.State.FAILED;
        }

        return switch (status) {
            case SOLVED                                                                                          -> Optimisation.State.OPTIMAL;
            case ALMOST_SOLVED, ALMOST_PRIMAL_INFEASIBLE, ALMOST_DUAL_INFEASIBLE                                 -> Optimisation.State.APPROXIMATE;
            case PRIMAL_INFEASIBLE                                                                               -> Optimisation.State.INFEASIBLE;
            case DUAL_INFEASIBLE                                                                                 -> Optimisation.State.UNBOUNDED;
            case NUMERICAL_ERROR, MAX_ITERATIONS, MAX_TIME, INSUFFICIENT_PROGRESS, CLARABEL_CALL_BACK_TERMINATED -> Optimisation.State.FAILED;
            case UNSOLVED                                                                                        -> Optimisation.State.UNEXPLORED;
        };
    }

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
     * For each variable, the row of its lower bound, or -1 if it has none.
     */
    private final int[] myLowerRows;
    private final boolean myMaximisation;
    private final com.ustermetrics.clarabel4j.Model myModel;
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
     * For each variable, the row of its upper bound (or its level, if it is fixed), or -1 if it has none.
     */
    private final int[] myUpperRows;

    SolverClarabel4j(final com.ustermetrics.clarabel4j.Model model, final boolean maximisation, final int numberOfVariables,
            final ConstraintsMetaData constraintsMetaData, final int[][] dualRows, final int[] upperRows, final int[] lowerRows, final double[] boundScales,
            final double objectiveScale, final double objectiveConstant) {
        super();
        myModel = model;
        myMaximisation = maximisation;
        myNumberOfVariables = numberOfVariables;
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
        Optimisation.Solver.super.dispose();
        try {
            if (myModel != null) {
                myModel.close();
            }
        } catch (Exception ignore) {
        }
    }

    @Override
    public Optimisation.Result solve(final Result kickStarter) {

        Status status = myModel.optimize();

        Optimisation.State state = SolverClarabel4j.translate(status);

        if (state.isFeasible() && state != Optimisation.State.UNBOUNDED) {

            double obj = myModel.objVal() / myObjectiveScale;
            if (myMaximisation) {
                obj = -obj;
            }
            obj += myObjectiveConstant;

            double[] x = myModel.x();
            double[] z = myModel.z();

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

}
