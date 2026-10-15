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
package org.ojalgo.optimisation.solver.xpress;

import static org.ojalgo.function.constant.PrimitiveMath.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.ojalgo.array.ArrayR064;
import org.ojalgo.concurrent.InterruptForwarder;
import org.ojalgo.optimisation.ConstraintsMetaData;
import org.ojalgo.optimisation.Expression;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.ModelEntity;
import org.ojalgo.optimisation.Optimisation;
import org.ojalgo.optimisation.UpdatableSolver;
import org.ojalgo.optimisation.Variable;
import org.ojalgo.structure.Structure1D.IntIndex;
import org.ojalgo.structure.Structure2D.IntRowColumn;
import org.ojalgo.type.context.NumberContext;
import org.ojalgo.type.keyvalue.EntryPair;

import com.dashoptimization.XPRS;
import com.dashoptimization.XPRSconstants;
import com.dashoptimization.XPRSprob;

/**
 * FICO Xpress Optimizer integration, built on the {@link XPRSprob} matrix API ({@code com.dashoptimization}
 * package).
 *
 * @author apete
 */
public final class SolverXpress implements Optimisation.Solver, UpdatableSolver {

    public static class Configurator {

        protected void configure(final XPRSprob problem, final Optimisation.Options options) {

            problem.setIntControl(XPRSconstants.OUTPUTLOG, 0);
            problem.setLogFile(null);

            long effectiveTimeMillis = Math.min(options.time_suffice, options.time_abort);
            if (effectiveTimeMillis > 0L && effectiveTimeMillis < Long.MAX_VALUE) {
                long seconds = Math.max(1L, effectiveTimeMillis / 1000L);
                problem.setIntControl(XPRSconstants.MAXTIME, -(int) Math.min(seconds, Integer.MAX_VALUE));
            }

            int effectiveIterations = Math.min(options.iterations_suffice, options.iterations_abort);
            if (effectiveIterations > 0 && effectiveIterations < Integer.MAX_VALUE) {
                problem.setIntControl(XPRSconstants.BARITERLIMIT, effectiveIterations);
            }

            int parallelism = options.getParallelism();
            if (parallelism > 0) {
                problem.setIntControl(XPRSconstants.THREADS, parallelism);
            }

            // The feasibility tolerance, only if configured to be something other than the default – a
            // requirement on both the constraints and the integrality, within Xpress' allowed ranges
            Optional<NumberContext> feasibility = options.getConfiguredFeasibilityTolerance();
            if (feasibility.isPresent()) {
                double tolerance = feasibility.get().getAbsoluteError();
                problem.setDblControl(XPRSconstants.FEASTOL, Math.min(Math.max(tolerance, 1E-9), 1E-2));
                problem.setDblControl(XPRSconstants.MIPTOL, Math.min(Math.max(tolerance, 1E-9), 0.5));
            }
        }

        protected XPRSprob newProblem() {
            try {
                SolverXpress.ensureInitialised();
                return new XPRSprob(null);
            } catch (RuntimeException cause) {
                throw cause;
            } catch (Exception cause) {
                throw new RuntimeException(cause);
            }
        }

    }

    static final class Integration extends ExpressionsBasedModel.Integration<SolverXpress> {

        /**
         * A dual ray from the simplex is exact, up to rounding.
         */
        private static final double CERTIFICATE_NOISE = 1E-9;
        /**
         * Must be false. Xpress handles variable bounds natively, not as constraint rows, so adjusting them would
         * mean rescaling the variables themselves.
         */
        private static final boolean ADJUSTED_BOUNDS = false;
        private static final boolean ADJUSTED_CONSTRAINTS = false;
        private static final boolean ADJUSTED_OBJECTIVE = false;

        Integration() {
            super();
        }

        @Override
        public SolverXpress build(final ExpressionsBasedModel model) {

            try {

                Configurator configurator = model.getConfigurator(DEFAULT);

                XPRSprob problem = configurator.newProblem();

                configurator.configure(problem, model.options);

                boolean mip = model.isAnyVariableInteger();

                List<Variable> mVars = model.getVariables();
                int nbVars = mVars.size();

                double[] obj = new double[nbVars];
                double[] lb = new double[nbVars];
                double[] ub = new double[nbVars];

                for (int v = 0; v < nbVars; v++) {

                    Variable mVar = mVars.get(v);

                    lb[v] = mVar.getLowerLimit(ADJUSTED_BOUNDS, NEGATIVE_INFINITY);
                    ub[v] = mVar.getUpperLimit(ADJUSTED_BOUNDS, POSITIVE_INFINITY);
                }

                Expression mObj = model.objective();
                for (IntIndex key : mObj.getLinearKeySet()) {
                    obj[key.index] = mObj.doubleValue(key, ADJUSTED_OBJECTIVE);
                }

                // Columns are added without any of their own matrix data - all constraint coefficients are
                // supplied afterwards, row-wise, via addRows.
                problem.addCols(nbVars, 0, obj, new int[nbVars + 1], new int[0], new double[0], lb, ub);

                List<Expression> mConstrs = model.constraints().collect(Collectors.toList());
                int nbRows = mConstrs.size();

                int nbCoeffs = 0;
                for (Expression mConstr : mConstrs) {
                    nbCoeffs += mConstr.getLinearKeySet().size();
                }

                byte[] rowType = new byte[nbRows];
                double[] rowRHS = new double[nbRows];
                double[] rowRange = new double[nbRows];
                int[] rowStart = new int[nbRows + 1];
                int[] colIndex = new int[nbCoeffs];
                double[] coefValue = new double[nbCoeffs];

                List<EntryPair<ModelEntity<?>, ConstraintType>> constrInfo = new ArrayList<>();

                int cursor = 0;
                for (int r = 0; r < nbRows; r++) {

                    Expression mConstr = mConstrs.get(r);

                    rowStart[r] = cursor;

                    for (IntIndex key : mConstr.getLinearKeySet()) {
                        colIndex[cursor] = key.index;
                        coefValue[cursor] = mConstr.doubleValue(key, ADJUSTED_CONSTRAINTS);
                        cursor++;
                    }

                    double lower = mConstr.getLowerLimit(ADJUSTED_CONSTRAINTS, NEGATIVE_INFINITY);
                    double upper = mConstr.getUpperLimit(ADJUSTED_CONSTRAINTS, POSITIVE_INFINITY);

                    if (mConstr.isEqualityConstraint()) {
                        rowType[r] = (byte) 'E';
                        rowRHS[r] = lower;
                        constrInfo.add(EntryPair.of(mConstr, ConstraintType.EQUALITY));
                    } else if (mConstr.isLowerConstraint() && mConstr.isUpperConstraint()) {
                        rowType[r] = (byte) 'R';
                        rowRHS[r] = upper;
                        rowRange[r] = upper - lower;
                        constrInfo.add(EntryPair.of(mConstr, ConstraintType.RANGE));
                    } else if (mConstr.isLowerConstraint()) {
                        rowType[r] = (byte) 'G';
                        rowRHS[r] = lower;
                        constrInfo.add(EntryPair.of(mConstr, ConstraintType.LOWER));
                    } else if (mConstr.isUpperConstraint()) {
                        rowType[r] = (byte) 'L';
                        rowRHS[r] = upper;
                        constrInfo.add(EntryPair.of(mConstr, ConstraintType.UPPER));
                    } else {
                        rowType[r] = (byte) 'N';
                        constrInfo.add(EntryPair.of(mConstr, ConstraintType.NONE));
                    }
                }
                rowStart[nbRows] = cursor;

                problem.addRows(nbRows, nbCoeffs, rowType, rowRHS, rowRange, rowStart, colIndex, coefValue);

                if (mip) {

                    int nbInt = 0;
                    for (Variable mVar : mVars) {
                        if (mVar.isBinary() || mVar.isInteger()) {
                            nbInt++;
                        }
                    }

                    if (nbInt > 0) {

                        int[] colInd = new int[nbInt];
                        byte[] colType = new byte[nbInt];

                        int c = 0;
                        for (int v = 0; v < nbVars; v++) {
                            Variable mVar = mVars.get(v);
                            if (mVar.isBinary()) {
                                colInd[c] = v;
                                colType[c] = (byte) 'B';
                                c++;
                            } else if (mVar.isInteger()) {
                                colInd[c] = v;
                                colType[c] = (byte) 'I';
                                c++;
                            }
                        }

                        problem.chgColType(nbInt, colInd, colType);
                    }
                }

                problem.chgObjSense(model.getOptimisationSense() == Optimisation.Sense.MAX ? XPRSconstants.OBJ_MAXIMIZE : XPRSconstants.OBJ_MINIMIZE);

                if (mObj.isAnyQuadraticFactorNonZero()) {

                    // One triangle of Q, where the quadratic part is x'Qx/2. The keys (i,j) and (j,i) are the
                    // same element, and are added.
                    Map<IntRowColumn, Double> triangle = new LinkedHashMap<>();
                    for (IntRowColumn key : mObj.getQuadraticKeySet()) {
                        double value = mObj.doubleValue(key, ADJUSTED_OBJECTIVE);
                        IntRowColumn element = new IntRowColumn(Math.min(key.row, key.column), Math.max(key.row, key.column));
                        triangle.merge(element, key.row == key.column ? value + value : value, Double::sum);
                    }

                    int nqtr = triangle.size();

                    int[] mqcol1 = new int[nqtr];
                    int[] mqcol2 = new int[nqtr];
                    double[] dqe = new double[nqtr];

                    int q = 0;
                    for (Entry<IntRowColumn, Double> element : triangle.entrySet()) {
                        mqcol1[q] = element.getKey().row;
                        mqcol2[q] = element.getKey().column;
                        dqe[q] = element.getValue().doubleValue();
                        q++;
                    }

                    problem.addQMatrix(-1, nqtr, mqcol1, mqcol2, dqe);
                }

                double objectiveScale = ADJUSTED_OBJECTIVE ? mObj.getAdjustmentFactor() : ONE;
                double objectiveConstant = ExpressionsBasedModel.Integration.getObjectiveConstant(model);

                // The MIP gap, only if configured to be something other than the default – the absolute gap in the
                // objective's (scaled) units
                Optional<NumberContext> gap = model.options.getConfiguredGapTolerance();
                if (gap.isPresent()) {
                    problem.setDblControl(XPRSconstants.MIPRELSTOP, gap.get().getRelativeError());
                    problem.setDblControl(XPRSconstants.MIPABSSTOP, gap.get().getAbsoluteError() * objectiveScale);
                }

                XpressEntityMap entityMap = new XpressEntityMap(nbVars, constrInfo, objectiveScale, ADJUSTED_CONSTRAINTS);

                return new SolverXpress(problem, nbVars, mip, entityMap, model.options, objectiveScale, objectiveConstant);

            } catch (RuntimeException cause) {
                throw cause;
            } catch (Exception cause) {
                throw new RuntimeException(cause);
            }
        }

        @Override
        public boolean isCapable(final ExpressionsBasedModel model) {
            return !model.isAnyConstraintQuadratic();
        }

        /**
         * An INFEASIBLE result with a dual ray gets the reduced gradient of the infeasibility certificate, see
         * {@link ExpressionsBasedModel.Integration#toInfeasibilityCertificate(Optimisation.Result, ExpressionsBasedModel, double)}.
         */
        @Override
        public Result toModelState(final Result solverState, final ExpressionsBasedModel model) {
            if (solverState.getState() == State.INFEASIBLE && solverState.getDualSolution().isPresent()) {
                return ExpressionsBasedModel.Integration.toInfeasibilityCertificate(solverState, model, CERTIFICATE_NOISE);
            }
            return solverState;
        }

        @Override
        protected Optimisation.Sense getSolverSense() {
            return null;
        }

    }

    static final class XpressEntityMap implements ExpressionsBasedModel.EntityMap {

        private final ConstraintsMetaData myConstraints;
        private final int myNumEqualityConstraints;
        private final int myNumInequalityConstraints;
        private final int myNumVars;

        XpressEntityMap(final int numVars, final List<EntryPair<ModelEntity<?>, ConstraintType>> constraints, final double objectiveScale,
                final boolean adjusted) {
            myNumVars = numVars;
            myConstraints = ConstraintsMetaData.newEntityMap(constraints.size());
            myConstraints.setMultiplierScale(objectiveScale);
            int eq = 0;
            int ineq = 0;
            for (int i = 0; i < constraints.size(); i++) {
                EntryPair<ModelEntity<?>, ConstraintType> entry = constraints.get(i);
                myConstraints.setEntry(i, entry.left(), entry.right(), false, adjusted);
                if (entry.getValue() == ConstraintType.EQUALITY) {
                    eq++;
                } else {
                    ineq++;
                }
            }
            myNumEqualityConstraints = eq;
            myNumInequalityConstraints = ineq;
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
            return myNumVars;
        }

        @Override
        public int countSlackVariables() {
            return 0;
        }

        @Override
        public int countVariables() {
            return myNumVars;
        }

        @Override
        public EntryPair<ModelEntity<?>, ConstraintType> getConstraint(final int idc) {
            return myConstraints.getEntry(idc);
        }

        @Override
        public EntryPair<ModelEntity<?>, ConstraintType> getSlack(final int ids) {
            return null;
        }

        @Override
        public int indexOf(final int solverIndex) {
            return solverIndex;
        }

        @Override
        public boolean isNegated(final int solverIndex) {
            return false;
        }

        ConstraintsMetaData getConstraintsMetaData() {
            return myConstraints;
        }

    }

    public static final ExpressionsBasedModel.Integration<SolverXpress> INTEGRATION = new Integration();

    static final Configurator DEFAULT = new Configurator();

    private static final AtomicBoolean INITIALISED = new AtomicBoolean(false);

    /**
     * {@link XPRS#init()} must be called exactly once per JVM, before any {@link XPRSprob} is created. Done
     * lazily - rather than in a static initialiser - so merely loading this class (e.g. while a test runner
     * discovers {@code @Disabled} tests) does not require a working Xpress installation.
     */
    private static void ensureInitialised() {
        if (INITIALISED.compareAndSet(false, true)) {
            XPRS.init();
        }
    }

    static Optimisation.State translateLP(final int status) {
        switch (status) {
            case 1:
                return State.OPTIMAL;
            case 2:
                return State.INFEASIBLE;
            case 3:
                // Cutoff - stopped because the objective limit was reached
                return State.APPROXIMATE;
            case 4:
                // Unfinished
                return State.UNEXPLORED;
            case 5:
                return State.UNBOUNDED;
            case 6:
                // Cutoff in dual
                return State.APPROXIMATE;
            default:
                return State.FAILED;
        }
    }

    /**
     * MIP_UNBOUNDED means that the LP relaxation is unbounded, and then the MIP is either unbounded or
     * infeasible – {@link State#INVALID}.
     */
    static Optimisation.State translateMIP(final int status) {
        switch (status) {
            case XPRSconstants.MIP_NOT_LOADED:
            case XPRSconstants.MIP_LP_NOT_OPTIMAL:
            case XPRSconstants.MIP_LP_OPTIMAL:
            case XPRSconstants.MIP_NO_SOL_FOUND:
                return State.UNEXPLORED;
            case XPRSconstants.MIP_SOLUTION:
                return State.FEASIBLE;
            case XPRSconstants.MIP_INFEAS:
                return State.INFEASIBLE;
            case XPRSconstants.MIP_OPTIMAL:
                return State.OPTIMAL;
            case XPRSconstants.MIP_UNBOUNDED:
                return State.INVALID;
            default:
                return State.FAILED;
        }
    }

    private final XpressEntityMap myEntityMap;
    private final int myIterationsAbort;
    private final int myIterationsSuffice;
    private final boolean myMIP;
    private final int myNumberOfVariables;
    /**
     * The objective constant, in model units.
     */
    private final double myObjectiveConstant;
    /**
     * The objective's adjustment factor – the objective Xpress solves is scaled by this.
     */
    private final double myObjectiveScale;
    private final XPRSprob myProblem;
    private final long myTimeAbortMillis;
    private final long myTimeSufficeMillis;

    SolverXpress(final XPRSprob problem, final int numberOfVariables, final boolean mip, final XpressEntityMap entityMap, final Optimisation.Options options,
            final double objectiveScale, final double objectiveConstant) {

        super();

        myObjectiveScale = objectiveScale;
        myObjectiveConstant = objectiveConstant;

        myProblem = problem;
        myNumberOfVariables = numberOfVariables;
        myMIP = mip;
        myEntityMap = entityMap;
        myTimeSufficeMillis = options.time_suffice;
        myTimeAbortMillis = options.time_abort;
        myIterationsSuffice = options.iterations_suffice;
        myIterationsAbort = options.iterations_abort;
    }

    @Override
    public void dispose() {
        UpdatableSolver.super.dispose();
        if (myProblem != null) {
            myProblem.destroyProb();
        }
    }

    @Override
    public boolean fixVariable(final int index, final double value) {
        return this.updateRange(index, value, value);
    }

    @Override
    public double getDualMultiplier(final int index) {
        return myProblem.getDual(index);
    }

    @Override
    public Optional<ExpressionsBasedModel.EntityMap> getEntityMap() {
        return Optional.of(myEntityMap);
    }

    @Override
    public double getReducedGradient(final int index) {
        return myProblem.getRedCost(index);
    }

    /**
     * For a continuous model the dual values and the reduced gradient are Xpress' own: the rates of change of
     * the objective function value with the constraint limits and the variable bounds, as described at
     * {@link Optimisation.Result#getDualSolution()}. If the state is INFEASIBLE the dual values are instead
     * Xpress' dual ray, signed as an infeasibility certificate whatever the sense. There is one only if the
     * dual simplex, not presolve, proved the infeasibility.
     */
    @Override
    public Result solve(final Result kickStarter) {

        State retState = State.UNEXPLORED;
        double retValue = NaN;
        double[] retSolution = new double[myNumberOfVariables];

        try {

            long startMillis = System.currentTimeMillis();

            try (InterruptForwarder interrupter = InterruptForwarder.watch(() -> myProblem.interrupt(0))) {
                if (myMIP) {
                    myProblem.mipOptimize("");
                } else {
                    myProblem.lpOptimize("");
                }
            }

            if (myMIP) {
                retState = SolverXpress.translateMIP(myProblem.getIntAttrib(XPRSconstants.MIPSTATUS));
            } else {
                retState = SolverXpress.translateLP(myProblem.getIntAttrib(XPRSconstants.LPSTATUS));
            }

            if (!retState.isFeasible() && myTimeSufficeMillis < myTimeAbortMillis) {
                long elapsed = System.currentTimeMillis() - startMillis;
                long remaining = myTimeAbortMillis - elapsed;
                if (remaining > 1000L) {
                    long seconds = Math.max(1L, remaining / 1000L);
                    myProblem.setIntControl(XPRSconstants.MAXTIME, -(int) Math.min(seconds, Integer.MAX_VALUE));
                    if (myIterationsSuffice < myIterationsAbort) {
                        myProblem.setIntControl(XPRSconstants.BARITERLIMIT, myIterationsAbort);
                    }

                    try (InterruptForwarder interrupter = InterruptForwarder.watch(() -> myProblem.interrupt(0))) {
                        if (myMIP) {
                            myProblem.mipOptimize("");
                        } else {
                            myProblem.lpOptimize("");
                        }
                    }

                    if (myMIP) {
                        retState = SolverXpress.translateMIP(myProblem.getIntAttrib(XPRSconstants.MIPSTATUS));
                    } else {
                        retState = SolverXpress.translateLP(myProblem.getIntAttrib(XPRSconstants.LPSTATUS));
                    }
                }
            }

            if (retState.isFeasible()) {

                retValue = myMIP ? myProblem.getDblAttrib(XPRSconstants.MIPOBJVAL) : myProblem.getDblAttrib(XPRSconstants.LPOBJVAL);
                retValue = retValue / myObjectiveScale + myObjectiveConstant;

                retSolution = myProblem.getSolution();

                if (!myMIP && retState.isOptimal()) {

                    double[] duals = myProblem.getDuals();

                    double[] reducedGradient = myProblem.getRedCosts();
                    for (int j = 0; j < reducedGradient.length; j++) {
                        reducedGradient[j] /= myObjectiveScale;
                    }

                    return new Result(retState, retValue, ArrayR064.wrap(retSolution)).withDualValues(myEntityMap.getConstraintsMetaData(),
                            () -> ArrayR064.wrap(duals)).withReducedGradient(() -> ArrayR064.wrap(reducedGradient));
                }
            }

            if (!myMIP && retState == State.INFEASIBLE && myEntityMap.getConstraintsMetaData().size() > 0) {

                double[] ray = new double[myEntityMap.getConstraintsMetaData().size()];
                boolean available;
                try {
                    available = myProblem.getDualRay(ray);
                } catch (Exception cause) {
                    available = false;
                }
                if (available) {
                    return new Result(retState, retValue, ArrayR064.wrap(retSolution)).withDualValues(myEntityMap.getConstraintsMetaData(),
                            () -> ArrayR064.wrap(ray));
                }
            }

        } catch (RuntimeException cause) {
            throw cause;
        } catch (Exception cause) {
            throw new RuntimeException(cause);
        }

        return new Result(retState, retValue, ArrayR064.wrap(retSolution));
    }

    @Override
    public boolean updateRange(final int index, final double lower, final double upper) {

        if (index < 0 || index >= myNumberOfVariables) {
            return false;
        }

        try {
            myProblem.chgBounds(index, lower, upper);
            return true;
        } catch (Exception cause) {
            return false;
        }
    }

}
