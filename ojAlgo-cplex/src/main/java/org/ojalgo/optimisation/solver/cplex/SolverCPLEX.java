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
package org.ojalgo.optimisation.solver.cplex;

import static org.ojalgo.function.constant.PrimitiveMath.*;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.ojalgo.array.ArrayR064;
import org.ojalgo.concurrent.InterruptForwarder;
import org.ojalgo.netio.BasicLogger;
import org.ojalgo.optimisation.ConstraintsMetaData;
import org.ojalgo.optimisation.Expression;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.ModelEntity;
import org.ojalgo.optimisation.Optimisation;
import org.ojalgo.optimisation.Optimisation.ConstraintType;
import org.ojalgo.optimisation.UpdatableSolver;
import org.ojalgo.optimisation.Variable;
import org.ojalgo.structure.Structure1D.IntIndex;
import org.ojalgo.structure.Structure2D.IntRowColumn;
import org.ojalgo.type.context.NumberContext;
import org.ojalgo.type.keyvalue.EntryPair;

import ilog.concert.IloException;
import ilog.concert.IloLQNumExpr;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumExpr;
import ilog.concert.IloRange;
import ilog.concert.IloNumVar;
import ilog.concert.IloNumVarType;
import ilog.concert.IloQuadNumExpr;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.IntParam;
import ilog.cplex.IloCplex.Status;

/**
 * Integration with IBM ILOG CPLEX. Handles LP, QP, QCQP and their mixed-integer variants.
 *
 * @see "https://www.ibm.com/products/ilog-cplex-optimization-studio"
 * @author apete
 */
public final class SolverCPLEX implements Optimisation.Solver, UpdatableSolver {

    public static class Configurator {

        /**
         * The optimality (reduced cost) tolerance.
         */
        private static final double OPTIMALITY_TOLERANCE = 5E-9;

        protected IloCplex newIloCplex(final Optimisation.Options options) throws IloException {

            IloCplex retVal = new IloCplex();

            if (options.logger_appender != null) {

                int displayLevel = options.logger_detailed ? 5 : 1;

                retVal.setParam(IntParam.MIPDisplay, displayLevel);
                retVal.setParam(IntParam.SimDisplay, displayLevel);
                retVal.setParam(IntParam.BarDisplay, displayLevel);

                retVal.setParam(IntParam.MIPInterval, 5);

                retVal.setOut(new OutputStream() {

                    @Override
                    public void write(final int b) throws IOException {
                        options.logger_appender.print((char) b);
                    }
                });

            } else {

                retVal.setOut(OutputStream.nullOutputStream());
            }

            long effectiveTimeMillis = Math.min(options.time_suffice, options.time_abort);
            if (effectiveTimeMillis > 0L && effectiveTimeMillis < Long.MAX_VALUE) {
                double seconds = effectiveTimeMillis / 1000.0;
                retVal.setParam(IloCplex.DoubleParam.TiLim, seconds);
            }

            int effectiveIterations = Math.min(options.iterations_suffice, options.iterations_abort);
            if (effectiveIterations > 0 && effectiveIterations < Integer.MAX_VALUE) {
                retVal.setParam(IntParam.ItLim, effectiveIterations);
                retVal.setParam(IloCplex.LongParam.BarItLim, effectiveIterations);
            }

            // The feasibility tolerance, only if configured to be something other than the default – a
            // requirement on both the constraints and the integrality, within CPLEX's allowed ranges
            Optional<NumberContext> feasibility = options.getConfiguredFeasibilityTolerance();
            if (feasibility.isPresent()) {
                double tolerance = feasibility.get().getAbsoluteError();
                retVal.setParam(IloCplex.DoubleParam.EpRHS, Math.min(Math.max(tolerance, 1E-9), 1E-1));
                retVal.setParam(IloCplex.DoubleParam.EpInt, Math.min(tolerance, 0.5));
            }
            retVal.setParam(IloCplex.DoubleParam.EpOpt, OPTIMALITY_TOLERANCE);

            int parallelism = options.getParallelism();
            if (parallelism > 0) {
                retVal.setParam(IntParam.Threads, parallelism);
            }

            return retVal;
        }

    }

    static final class CPLEXEntityMap implements ExpressionsBasedModel.EntityMap {

        private final ConstraintsMetaData myConstraints;
        private final int myNumEqualityConstraints;
        private final int myNumInequalityConstraints;
        private final int myNumVars;

        CPLEXEntityMap(final int numVars, final List<EntryPair<ModelEntity<?>, ConstraintType>> constraints, final double objectiveScale,
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

    static final class Integration extends ExpressionsBasedModel.Integration<SolverCPLEX> {

        /**
         * A Farkas proof is exact, up to rounding.
         */
        private static final double CERTIFICATE_NOISE = 1E-9;
        /**
         * Must be false. CPLEX handles variable bounds natively, not as constraint rows, so adjusting them
         * would mean rescaling the variables themselves.
         */
        private static final boolean ADJUSTED_BOUNDS = false;
        private static final boolean ADJUSTED_CONSTRAINTS = false;
        private static final boolean ADJUSTED_OBJECTIVE = false;

        Integration() {
            super();
        }

        @Override
        public SolverCPLEX build(final ExpressionsBasedModel model) {

            try {

                Configurator configurator = model.getConfigurator(DEFAULT);

                IloCplex solver = configurator.newIloCplex(model.options);

                boolean mip = model.isAnyVariableInteger();

                List<Variable> mVars = model.getVariables();
                int nbVars = mVars.size();

                IloNumVar[] sVars = new IloNumVar[nbVars];

                for (int i = 0; i < nbVars; i++) {

                    Variable mVar = mVars.get(i);

                    IloNumVarType type = IloNumVarType.Float;
                    if (mip) {
                        if (mVar.isBinary()) {
                            type = IloNumVarType.Bool;
                        } else if (mVar.isInteger()) {
                            type = IloNumVarType.Int;
                        }
                    }
                    double lb = mVar.getLowerLimit(ADJUSTED_BOUNDS, NEGATIVE_INFINITY);
                    double ub = mVar.getUpperLimit(ADJUSTED_BOUNDS, POSITIVE_INFINITY);
                    String name = mVar.getName();

                    solver.add(sVars[i] = solver.numVar(lb, ub, type, name));
                }

                List<ilog.concert.IloRange> ranges = new ArrayList<>();
                List<EntryPair<ModelEntity<?>, ConstraintType>> constrInfo = new ArrayList<>();

                List<Expression> mConstrs = model.constraints().collect(Collectors.toList());
                for (Expression mConstr : mConstrs) {

                    IloNumExpr sConstr = this.buildExpression(mConstr, solver, sVars, ADJUSTED_CONSTRAINTS);

                    if (mConstr.isEqualityConstraint()) {
                        ranges.add(solver.addEq(mConstr.getLowerLimit(ADJUSTED_CONSTRAINTS, ZERO), sConstr));
                        constrInfo.add(EntryPair.of(mConstr, ConstraintType.EQUALITY));
                    } else if (mConstr.isLowerConstraint() && mConstr.isUpperConstraint()) {
                        double lb = mConstr.getLowerLimit(ADJUSTED_CONSTRAINTS, NEGATIVE_INFINITY);
                        double ub = mConstr.getUpperLimit(ADJUSTED_CONSTRAINTS, POSITIVE_INFINITY);
                        IloRange range = solver.range(lb, sConstr, ub);
                        solver.add(range);
                        ranges.add(range);
                        constrInfo.add(EntryPair.of(mConstr, ConstraintType.RANGE));
                    } else if (mConstr.isLowerConstraint()) {
                        ranges.add(solver.addLe(mConstr.getLowerLimit(ADJUSTED_CONSTRAINTS, NEGATIVE_INFINITY), sConstr));
                        constrInfo.add(EntryPair.of(mConstr, ConstraintType.LOWER));
                    } else if (mConstr.isUpperConstraint()) {
                        ranges.add(solver.addGe(mConstr.getUpperLimit(ADJUSTED_CONSTRAINTS, POSITIVE_INFINITY), sConstr));
                        constrInfo.add(EntryPair.of(mConstr, ConstraintType.UPPER));
                    }
                }

                Expression mObj = model.objective();
                IloNumExpr sObj = this.buildExpression(mObj, solver, sVars, ADJUSTED_OBJECTIVE);

                if (model.getOptimisationSense() == Optimisation.Sense.MAX) {
                    solver.addMaximize(sObj);
                } else {
                    solver.addMinimize(sObj);
                }

                double objectiveScale = ADJUSTED_OBJECTIVE ? mObj.getAdjustmentFactor() : ONE;
                double objectiveConstant = ExpressionsBasedModel.Integration.getObjectiveConstant(model);

                // The MIP gap, only if configured to be something other than the default – the absolute gap in the
                // objective's (scaled) units
                Optional<NumberContext> gap = model.options.getConfiguredGapTolerance();
                if (gap.isPresent()) {
                    solver.setParam(IloCplex.DoubleParam.EpGap, gap.get().getRelativeError());
                    solver.setParam(IloCplex.DoubleParam.EpAGap, gap.get().getAbsoluteError() * objectiveScale);
                }

                CPLEXEntityMap entityMap = new CPLEXEntityMap(nbVars, constrInfo, objectiveScale, ADJUSTED_CONSTRAINTS);

                return new SolverCPLEX(solver, sVars, ranges.toArray(new ilog.concert.IloRange[0]), entityMap, model.options, objectiveScale,
                        objectiveConstant);

            } catch (IloException cause) {
                throw new RuntimeException(cause);
            }
        }

        /**
         * CPLEX can handle anything/everything ExpressionsBasedModel can model.
         *
         * @see org.ojalgo.optimisation.Optimisation.Integration#isCapable(org.ojalgo.optimisation.Optimisation.Model)
         */
        @Override
        public boolean isCapable(final ExpressionsBasedModel model) {
            return true;
        }

        private void copyLinear(final Expression source, final IloNumVar[] variables, final IloLinearNumExpr destination, final boolean adjusted)
                throws IloException {
            for (IntIndex key : source.getLinearKeySet()) {
                destination.addTerm(source.doubleValue(key, adjusted), variables[key.index]);
            }
        }

        private void copyQuadratic(final Expression source, final IloNumVar[] variables, final IloQuadNumExpr destination, final boolean adjusted)
                throws IloException {
            for (IntRowColumn key : source.getQuadraticKeySet()) {
                destination.addTerm(source.doubleValue(key, adjusted), variables[key.row], variables[key.column]);
            }
        }

        /**
         * An INFEASIBLE result with a Farkas proof gets the reduced gradient of the infeasibility
         * certificate, see
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

        IloNumExpr buildExpression(final Expression expression, final IloCplex cplex, final IloNumVar[] variables, final boolean adjusted) throws IloException {

            if (expression.isFunctionQuadratic()) {

                IloLQNumExpr tmpIloLQNumExpr = cplex.lqNumExpr();

                this.copyQuadratic(expression, variables, tmpIloLQNumExpr, adjusted);
                this.copyLinear(expression, variables, tmpIloLQNumExpr, adjusted);

                return tmpIloLQNumExpr;

            } else if (expression.isFunctionPureQuadratic()) {

                IloQuadNumExpr tmpIloQuadNumExpr = cplex.quadNumExpr();

                this.copyQuadratic(expression, variables, tmpIloQuadNumExpr, adjusted);

                return tmpIloQuadNumExpr;

            } else if (expression.isFunctionLinear()) {

                IloLinearNumExpr tmpIloLinearNumExpr = cplex.linearNumExpr();

                this.copyLinear(expression, variables, tmpIloLinearNumExpr, adjusted);

                return tmpIloLinearNumExpr;

            } else {

                return cplex.linearNumExpr();
            }
        }

    }

    public static final ExpressionsBasedModel.Integration<SolverCPLEX> INTEGRATION = new Integration();

    static final Configurator DEFAULT = new Configurator();

    static Optimisation.State translate(final IloCplex.Status status) {
        if (status.equals(Status.Bounded)) {
            return State.VALID;
        } else if (status.equals(Status.Error)) {
            return State.FAILED;
        } else if (status.equals(Status.Feasible)) {
            return State.FEASIBLE;
        } else if (status.equals(Status.Infeasible)) {
            return State.INFEASIBLE;
        } else if (status.equals(Status.InfeasibleOrUnbounded)) {
            return State.INVALID;
        } else if (status.equals(Status.Optimal)) {
            return State.OPTIMAL;
        } else if (status.equals(Status.Unbounded)) {
            return State.UNBOUNDED;
        } else if (status.equals(Status.Unknown)) {
            return State.UNEXPLORED;
        } else {
            return State.FAILED;
        }
    }

    private IloCplex.Aborter myAborter = null;
    private final ilog.concert.IloRange[] myConstraints;
    private final CPLEXEntityMap myEntityMap;
    private final int myIterationsAbort;
    private final int myIterationsSuffice;
    /**
     * The objective constant, in model units.
     */
    private final double myObjectiveConstant;
    /**
     * The objective's adjustment factor – the objective CPLEX solves is scaled by this.
     */
    private final double myObjectiveScale;
    private final IloCplex mySolver;
    private final long myTimeAbortMillis;
    private final long myTimeSufficeMillis;
    private final IloNumVar[] myVariables;

    SolverCPLEX(final IloCplex solver, final IloNumVar[] variables, final ilog.concert.IloRange[] constraints, final CPLEXEntityMap entityMap,
            final Optimisation.Options options, final double objectiveScale, final double objectiveConstant) {
        super();
        myConstraints = constraints;
        myEntityMap = entityMap;
        myIterationsAbort = options.iterations_abort;
        myIterationsSuffice = options.iterations_suffice;
        mySolver = solver;
        myTimeAbortMillis = options.time_abort;
        myTimeSufficeMillis = options.time_suffice;
        myVariables = variables;
        myObjectiveScale = objectiveScale;
        myObjectiveConstant = objectiveConstant;
    }

    @Override
    public void dispose() {

        UpdatableSolver.super.dispose();

        if (mySolver != null) {
            mySolver.end();
        }
    }

    @Override
    public boolean fixVariable(final int index, final double value) {
        return this.updateRange(index, value, value);
    }

    @Override
    public double getDualMultiplier(final int index) {
        try {
            return mySolver.getDual(myConstraints[index]);
        } catch (IloException cause) {
            throw new RuntimeException(cause);
        }
    }

    @Override
    public Optional<ExpressionsBasedModel.EntityMap> getEntityMap() {
        return Optional.of(myEntityMap);
    }

    @Override
    public double getReducedGradient(final int index) {
        try {
            return mySolver.getReducedCost(myVariables[index]);
        } catch (IloException cause) {
            throw new RuntimeException(cause);
        }
    }

    /**
     * For a continuous model, without quadratic constraints, the dual values and the reduced gradient are
     * CPLEX's own: the rates of change of the objective function value with the constraint limits and the
     * variable bounds, as described at {@link Optimisation.Result#getDualSolution()}. If the state is
     * INFEASIBLE the dual values are instead CPLEX's Farkas proof, signed as an infeasibility certificate
     * whatever the sense. There is one only if a simplex optimiser, not presolve, proved the infeasibility.
     */
    @Override
    public Optimisation.Result solve(final Result kickStarter) {

        double retValue = NaN;
        Optimisation.State retState = Optimisation.State.UNEXPLORED;
        double[] retSolution = new double[myVariables.length];

        try {

            long startMillis = System.currentTimeMillis();

            IloCplex.Aborter aborter = this.aborter();

            // Latched from the last solve, if that one was aborted. The interrupt that caused it may since
            // have been dealt with, and this solve is entitled to run.
            aborter.clear();

            boolean solved;
            try (InterruptForwarder interrupter = InterruptForwarder.watch(aborter::abort)) {
                solved = mySolver.solve();
            }

            if (solved) {

                for (int i = 0; i < myVariables.length; i++) {
                    retSolution[i] = mySolver.getValue(myVariables[i]);
                }

                retValue = mySolver.getObjValue() / myObjectiveScale + myObjectiveConstant;
            }

            retState = SolverCPLEX.translate(mySolver.getStatus());

            if (!retState.isFeasible() && myTimeSufficeMillis < myTimeAbortMillis) {
                long elapsed = System.currentTimeMillis() - startMillis;
                long remaining = myTimeAbortMillis - elapsed;
                if (remaining > 1000L) {
                    mySolver.setParam(IloCplex.DoubleParam.TiLim, remaining / 1000.0);
                    if (myIterationsSuffice < myIterationsAbort) {
                        mySolver.setParam(IntParam.ItLim, myIterationsAbort);
                        mySolver.setParam(IloCplex.LongParam.BarItLim, myIterationsAbort);
                    }

                    aborter.clear();
                    try (InterruptForwarder interrupter = InterruptForwarder.watch(aborter::abort)) {
                        solved = mySolver.solve();
                    }

                    if (solved) {
                        for (int i = 0; i < myVariables.length; i++) {
                            retSolution[i] = mySolver.getValue(myVariables[i]);
                        }
                        retValue = mySolver.getObjValue() / myObjectiveScale + myObjectiveConstant;
                    }

                    retState = SolverCPLEX.translate(mySolver.getStatus());
                }
            }

            if (retState.isOptimal() && !mySolver.isMIP() && !mySolver.isQC()) {

                double[] duals = myConstraints.length > 0 ? mySolver.getDuals(myConstraints) : new double[0];

                double[] reducedGradient = mySolver.getReducedCosts(myVariables);
                for (int j = 0; j < reducedGradient.length; j++) {
                    reducedGradient[j] /= myObjectiveScale;
                }

                return Optimisation.Result.of(retValue, retState, retSolution).withDualValues(myEntityMap.getConstraintsMetaData(), () -> ArrayR064.wrap(duals))
                        .withReducedGradient(() -> ArrayR064.wrap(reducedGradient));
            }

            if (retState == State.INFEASIBLE && !mySolver.isMIP() && !mySolver.isQC() && myConstraints.length > 0) {

                double[] farkas = new double[myConstraints.length];
                try {
                    mySolver.dualFarkas(myConstraints, farkas);
                    return Optimisation.Result.of(retValue, retState, retSolution).withDualValues(myEntityMap.getConstraintsMetaData(),
                            () -> ArrayR064.wrap(farkas));
                } catch (IloException ignore) {
                }
            }

        } catch (IloException cause) {
            BasicLogger.error(cause, "CPLEX solve failed!");
            retValue = NaN;
            retState = Optimisation.State.FAILED;
        }

        return Optimisation.Result.of(retValue, retState, retSolution);
    }

    @Override
    public boolean updateRange(final int index, final double lower, final double upper) {

        if (index < 0 || index >= myVariables.length) {
            return false;
        }

        try {
            myVariables[index].setLB(lower);
            myVariables[index].setUB(upper);
            return true;
        } catch (IloException cause) {
            return false;
        }
    }

    /**
     * CPLEX's own way of being stopped from elsewhere, which is what makes an interrupt of the solving thread
     * reach it. Registered once and kept – an aborter belongs to the {@link IloCplex} instance rather than to
     * one solve.
     */
    private IloCplex.Aborter aborter() throws IloException {

        if (myAborter == null) {
            myAborter = mySolver.use(new IloCplex.Aborter());
        }

        return myAborter;
    }
}
