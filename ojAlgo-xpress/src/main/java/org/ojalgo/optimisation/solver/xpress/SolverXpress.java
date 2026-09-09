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
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.ojalgo.array.ArrayR064;
import org.ojalgo.concurrent.InterruptForwarder;
import org.ojalgo.optimisation.Expression;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.ModelEntity;
import org.ojalgo.optimisation.Optimisation;
import org.ojalgo.optimisation.UpdatableSolver;
import org.ojalgo.optimisation.Variable;
import org.ojalgo.structure.Structure1D.IntIndex;
import org.ojalgo.structure.Structure2D.IntRowColumn;
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

                    var quadKeys = mObj.getQuadraticKeySet();
                    int nqtr = quadKeys.size();

                    int[] mqcol1 = new int[nqtr];
                    int[] mqcol2 = new int[nqtr];
                    double[] dqe = new double[nqtr];

                    int q = 0;
                    for (IntRowColumn key : quadKeys) {
                        mqcol1[q] = key.row;
                        mqcol2[q] = key.column;
                        double value = mObj.doubleValue(key, ADJUSTED_OBJECTIVE);
                        dqe[q] = key.row == key.column ? value + value : value;
                        q++;
                    }

                    problem.addQMatrix(-1, nqtr, mqcol1, mqcol2, dqe);
                }

                XpressEntityMap entityMap = new XpressEntityMap(nbVars, constrInfo);

                return new SolverXpress(problem, nbVars, mip, entityMap, model.options);

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

        @Override
        protected Optimisation.Sense getSolverSense() {
            return null;
        }

    }

    static final class XpressEntityMap implements ExpressionsBasedModel.EntityMap {

        private final List<EntryPair<ModelEntity<?>, ConstraintType>> myConstraints;
        private final int myNumEqualityConstraints;
        private final int myNumInequalityConstraints;
        private final int myNumVars;

        XpressEntityMap(final int numVars, final List<EntryPair<ModelEntity<?>, ConstraintType>> constraints) {
            myNumVars = numVars;
            myConstraints = constraints;
            int eq = 0;
            int ineq = 0;
            for (EntryPair<ModelEntity<?>, ConstraintType> entry : constraints) {
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
            return myConstraints.get(idc);
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

    static Optimisation.State translateMIP(final int status) {
        switch (status) {
            case 1:
                // Not loaded
                return State.UNEXPLORED;
            case 2:
                // LP not optimal
                return State.UNEXPLORED;
            case 3:
                // LP optimal, but no integer solution found (yet)
                return State.UNEXPLORED;
            case 4:
                // Search complete, no integer solution found
                return State.INFEASIBLE;
            case 5:
                // Integer solution found, but not proven optimal
                return State.FEASIBLE;
            case 6:
                return State.OPTIMAL;
            default:
                return State.FAILED;
        }
    }

    private final XpressEntityMap myEntityMap;
    private final int myIterationsAbort;
    private final int myIterationsSuffice;
    private final boolean myMIP;
    private final int myNumberOfVariables;
    private final XPRSprob myProblem;
    private final long myTimeAbortMillis;
    private final long myTimeSufficeMillis;

    SolverXpress(final XPRSprob problem, final int numberOfVariables, final boolean mip, final XpressEntityMap entityMap, final Optimisation.Options options) {

        super();

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

                retSolution = myProblem.getSolution();
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
