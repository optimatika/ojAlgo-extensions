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
package org.ojalgo.optimisation.solver.choco;

import static org.ojalgo.function.constant.PrimitiveMath.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.chocosolver.solver.Solution;
import org.chocosolver.solver.search.SearchState;
import org.chocosolver.solver.search.loop.monitors.NogoodFromRestarts;
import org.chocosolver.solver.search.restart.LubyCutoff;
import org.chocosolver.solver.search.restart.Restarter;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.search.strategy.strategy.AbstractStrategy;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.RealVar;
import org.chocosolver.solver.variables.Variable;
import org.ojalgo.function.special.MissingMath;
import org.ojalgo.optimisation.Expression;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.Optimisation;
import org.ojalgo.structure.Structure1D.IntIndex;

/**
 * Integration with the Choco constraint programming solver. Handles linear models with integer variables. The
 * objective is optimised at integer resolution.
 *
 * @see "https://choco-solver.org"
 */
public final class SolverChoco implements Optimisation.Solver {

    /**
     * Configuration options for the underlying solver. The default implementation is suitable for most cases,
     * but you can override it if you need to.
     * <ol>
     * <li>Define a subclass of this class and override/change the methods you need to.
     * <li>{@link Optimisation.Options#setConfigurator(Object)} to an instance of your subclass.
     * </ol>
     */
    public static class Configurator {

        protected org.chocosolver.solver.Model newModel(final Optimisation.Options options) {

            org.chocosolver.solver.Model delegate = new org.chocosolver.solver.Model("ojAlgo");

            long effectiveTimeMillis = Math.min(options.time_suffice, options.time_abort);
            if (effectiveTimeMillis > 0L && effectiveTimeMillis < Long.MAX_VALUE) {
                delegate.getSolver().limitTime(effectiveTimeMillis);
            }

            return delegate;
        }

        protected double precision() {
            return 1.0E-6;
        }

    }

    static final class Integration extends ExpressionsBasedModel.Integration<SolverChoco> {

        Integration() {
            super();
        }

        @Override
        public SolverChoco build(final ExpressionsBasedModel model) {

            Configurator configurator = model.getConfigurator(DEFAULT);

            org.chocosolver.solver.Model delegate = configurator.newModel(model.options);
            double precision = configurator.precision();

            List<org.ojalgo.optimisation.Variable> modVars = model.getVariables();
            int nbVars = modVars.size();

            Variable[] delegateVars = new Variable[nbVars];
            boolean[] isIntVar = new boolean[nbVars];
            boolean[] artificialLB = new boolean[nbVars];
            boolean[] artificialUB = new boolean[nbVars];
            double[] effectiveLB = new double[nbVars];
            double[] effectiveUB = new double[nbVars];

            for (int i = 0; i < nbVars; i++) {

                org.ojalgo.optimisation.Variable modVar = modVars.get(i);

                String name = modVar.getName();
                double lowerLimit = modVar.getLowerLimit(false, NEGATIVE_INFINITY);
                double upperLimit = modVar.getUpperLimit(false, POSITIVE_INFINITY);

                if (modVar.isInteger()) {

                    int lb = SolverChoco.toLowerIntBound(lowerLimit);
                    int ub = SolverChoco.toUpperIntBound(upperLimit);
                    if (ub < lb) {
                        ub = lb;
                    }

                    artificialLB[i] = lb == IntVar.MIN_INT_BOUND;
                    artificialUB[i] = ub == IntVar.MAX_INT_BOUND;
                    effectiveLB[i] = lb;
                    effectiveUB[i] = ub;

                    delegateVars[i] = delegate.intVar(name, lb, ub);
                    isIntVar[i] = true;

                } else {

                    double lb = Double.isFinite(lowerLimit) ? lowerLimit : -REAL_ARTIFICIAL_BOUND;
                    double ub = Double.isFinite(upperLimit) ? upperLimit : REAL_ARTIFICIAL_BOUND;

                    artificialLB[i] = !Double.isFinite(lowerLimit);
                    artificialUB[i] = !Double.isFinite(upperLimit);
                    effectiveLB[i] = lb;
                    effectiveUB[i] = ub;

                    delegateVars[i] = delegate.realVar(name, lb, ub, precision);
                    isIntVar[i] = false;
                }
            }

            model.constraints().forEach(mConstr -> SolverChoco.addConstraint(delegate, mConstr, delegateVars, isIntVar));

            Expression modObj = model.objective();
            Set<IntIndex> objKeys = modObj.getLinearKeySet();
            int nbObjTerms = objKeys.size();

            // To evaluate the objective function at the solution
            double[] objectiveCoefficients = new double[nbVars];
            for (IntIndex key : objKeys) {
                objectiveCoefficients[key.index] = modObj.doubleValue(key, false);
            }

            boolean allObjVarsInteger = true;
            boolean artificialObjLB = false;
            boolean artificialObjUB = false;
            double rawObjMin = 0.0;
            double rawObjMax = 0.0;

            for (IntIndex key : objKeys) {
                double rawCoeff = modObj.doubleValue(key, false);
                allObjVarsInteger &= isIntVar[key.index];

                double lo = rawCoeff * effectiveLB[key.index];
                double hi = rawCoeff * effectiveUB[key.index];
                rawObjMin += Math.min(lo, hi);
                rawObjMax += Math.max(lo, hi);

                if (rawCoeff > 0) {
                    artificialObjLB |= artificialLB[key.index];
                    artificialObjUB |= artificialUB[key.index];
                } else if (rawCoeff < 0) {
                    artificialObjLB |= artificialUB[key.index];
                    artificialObjUB |= artificialLB[key.index];
                }
            }

            double objScale = 1.0;
            if (nbObjTerms > 0 && !modObj.isInteger()) {
                BigDecimal integerStep = modObj.getIntegerStep();
                if (integerStep != null && integerStep.signum() > 0) {
                    double candidateScale = MissingMath.divide(BigDecimal.ONE, integerStep).doubleValue();
                    objScale = candidateScale <= 10_000.0 ? candidateScale : 10_000.0;
                } else {
                    objScale = 10_000.0;
                }
            }

            int[] intObjCoeffs = new int[nbObjTerms + 1];
            IntVar[] intObjVars = new IntVar[nbObjTerms + 1];

            Variable[] mixedObjVars = new Variable[nbObjTerms + 1];
            double[] mixedObjCoeffs = new double[nbObjTerms + 1];

            int t = 0;
            for (IntIndex key : objKeys) {

                double coefficient = modObj.doubleValue(key, false) * objScale;

                intObjCoeffs[t] = (int) Math.round(coefficient);
                intObjVars[t] = isIntVar[key.index] ? (IntVar) delegateVars[key.index] : null;

                mixedObjVars[t] = delegateVars[key.index];
                mixedObjCoeffs[t] = coefficient;

                t++;
            }

            double objMin = rawObjMin * objScale;
            double objMax = rawObjMax * objScale;

            int objLB = SolverChoco.toLowerIntBound(objMin);
            int objUB = SolverChoco.toUpperIntBound(objMax);
            if (objUB < objLB) {
                objUB = objLB;
            }
            if ((long) objUB - (long) objLB > Integer.MAX_VALUE - 2L) {
                int halfRange = (Integer.MAX_VALUE - 2) / 2;
                objLB = Math.max(objLB, -halfRange);
                objUB = Math.min(objUB, halfRange);
            }

            IntVar objVar = delegate.intVar("objective", objLB, objUB);

            if (nbObjTerms > 0) {
                if (allObjVarsInteger) {
                    intObjVars[nbObjTerms] = objVar;
                    intObjCoeffs[nbObjTerms] = -1;
                    delegate.scalar(intObjVars, intObjCoeffs, "=", 0).post();
                } else {
                    mixedObjVars[nbObjTerms] = objVar;
                    mixedObjCoeffs[nbObjTerms] = -1.0;
                    delegate.scalar(mixedObjVars, mixedObjCoeffs, "=", 0.0).post();
                }
            } else {
                delegate.arithm(objVar, "=", 0).post();
            }

            boolean maximise = model.getOptimisationSense() == Optimisation.Sense.MAX;

            List<IntVar> intVars = new ArrayList<>();
            List<RealVar> realVars = new ArrayList<>();
            for (int i = 0; i < nbVars; i++) {
                if (isIntVar[i]) {
                    intVars.add((IntVar) delegateVars[i]);
                } else {
                    realVars.add((RealVar) delegateVars[i]);
                }
            }
            if (intVars.size() > 0) {

                // Decide the integer variables, then the continuous ones, and then the objective variable (if
                // the others have not already instantiated it) – a solution needs them all instantiated.
                List<AbstractStrategy<?>> strategies = new ArrayList<>();
                strategies.add(Search.activityBasedSearch(intVars.toArray(new IntVar[intVars.size()])));
                if (realVars.size() > 0) {
                    strategies.add(Search.realVarSearch(realVars.toArray(new RealVar[realVars.size()])));
                }
                strategies.add(Search.inputOrderLBSearch(objVar));

                org.chocosolver.solver.Solver solver = delegate.getSolver();
                solver.setSearch(strategies.toArray(new AbstractStrategy<?>[strategies.size()]));
                solver.addRestarter(new Restarter(new LubyCutoff(100), limit -> solver.getMeasures().getFailCount() >= limit, Integer.MAX_VALUE, false));
                if (realVars.size() == 0) {
                    // Only handles decisions on integer variables
                    solver.plugMonitor(new NogoodFromRestarts(delegate));
                }
            }

            double objectiveConstant = ExpressionsBasedModel.Integration.getObjectiveConstant(model);

            return new SolverChoco(delegate, delegateVars, isIntVar, objVar, maximise, model.options, artificialLB, artificialUB, artificialObjLB,
                    artificialObjUB, precision, objectiveCoefficients, objectiveConstant);
        }

        @Override
        public boolean isCapable(final ExpressionsBasedModel model) {
            return model.isAnyVariableInteger() && !model.isAnyExpressionQuadratic();
        }

        @Override
        protected Optimisation.Sense getSolverSense() {
            return null;
        }

    }

    public static final ExpressionsBasedModel.Integration<SolverChoco> INTEGRATION = new Integration();

    static final Configurator DEFAULT = new Configurator();

    static final double REAL_ARTIFICIAL_BOUND = 1.0E15;

    static void addConstraint(final org.chocosolver.solver.Model delegate, final Expression mConstr, final Variable[] delegateVars, final boolean[] isIntVar) {

        Set<IntIndex> keys = mConstr.getLinearKeySet();
        int nbTerms = keys.size();

        if (nbTerms <= 0) {
            return;
        }

        boolean useIntPropagator = true;
        for (IntIndex key : keys) {
            if (!isIntVar[key.index]) {
                useIntPropagator = false;
                break;
            }
            double coeff = mConstr.doubleValue(key, false);
            if (Math.abs(coeff - Math.round(coeff)) > 1e-8) {
                useIntPropagator = false;
                break;
            }
        }

        if (useIntPropagator) {

            IntVar[] termVars = new IntVar[nbTerms];
            int[] termCoeffs = new int[nbTerms];

            int t = 0;
            for (IntIndex key : keys) {
                termVars[t] = (IntVar) delegateVars[key.index];
                termCoeffs[t] = (int) Math.round(mConstr.doubleValue(key, false));
                t++;
            }

            if (mConstr.isEqualityConstraint()) {
                int rhs = (int) Math.round(mConstr.getLowerLimit(false, ZERO));
                delegate.scalar(termVars, termCoeffs, "=", rhs).post();
            } else {
                if (mConstr.isLowerConstraint()) {
                    double rawRhs = mConstr.getLowerLimit(false, NEGATIVE_INFINITY);
                    if (Double.isFinite(rawRhs)) {
                        delegate.scalar(termVars, termCoeffs, ">=", (int) Math.ceil(rawRhs - 1e-9)).post();
                    }
                }
                if (mConstr.isUpperConstraint()) {
                    double rawRhs = mConstr.getUpperLimit(false, POSITIVE_INFINITY);
                    if (Double.isFinite(rawRhs)) {
                        delegate.scalar(termVars, termCoeffs, "<=", (int) Math.floor(rawRhs + 1e-9)).post();
                    }
                }
            }

        } else {

            Variable[] termVars = new Variable[nbTerms];
            double[] termCoeffs = new double[nbTerms];

            int t = 0;
            for (IntIndex key : keys) {
                termVars[t] = delegateVars[key.index];
                termCoeffs[t] = mConstr.doubleValue(key, false);
                t++;
            }

            if (mConstr.isEqualityConstraint()) {
                double rhs = mConstr.getLowerLimit(false, ZERO);
                delegate.scalar(termVars, termCoeffs, "=", rhs).post();
            } else {
                if (mConstr.isLowerConstraint()) {
                    double rhs = mConstr.getLowerLimit(false, NEGATIVE_INFINITY);
                    delegate.scalar(termVars, termCoeffs, ">=", rhs).post();
                }
                if (mConstr.isUpperConstraint()) {
                    double rhs = mConstr.getUpperLimit(false, POSITIVE_INFINITY);
                    delegate.scalar(termVars, termCoeffs, "<=", rhs).post();
                }
            }
        }
    }

    static int toLowerIntBound(final double value) {
        if (value <= IntVar.MIN_INT_BOUND) {
            return IntVar.MIN_INT_BOUND;
        } else if (value >= IntVar.MAX_INT_BOUND) {
            return IntVar.MAX_INT_BOUND;
        } else {
            return (int) Math.ceil(value);
        }
    }

    static int toUpperIntBound(final double value) {
        if (value >= IntVar.MAX_INT_BOUND) {
            return IntVar.MAX_INT_BOUND;
        } else if (value <= IntVar.MIN_INT_BOUND) {
            return IntVar.MIN_INT_BOUND;
        } else {
            return (int) Math.floor(value);
        }
    }

    private final boolean[] myArtificialLB;
    private final boolean myArtificialObjLB;
    private final boolean myArtificialObjUB;
    private final boolean[] myArtificialUB;
    private final org.chocosolver.solver.Model myDelegateModel;
    private final Variable[] myDelegateVariables;
    private final boolean[] myIsIntVar;
    private final boolean myMaximise;
    /**
     * The objective function's linear coefficients (unadjusted, all variables).
     */
    private final double[] myObjectiveCoefficients;
    /**
     * The objective constant, in model units.
     */
    private final double myObjectiveConstant;
    private final IntVar myObjectiveVariable;
    private final double myPrecision;
    private final long myTimeAbortMillis;
    private final long myTimeSufficeMillis;

    SolverChoco(final org.chocosolver.solver.Model model, final Variable[] variables, final boolean[] isIntVar, final IntVar objective, final boolean maximise,
            final Optimisation.Options options, final boolean[] artificialLB, final boolean[] artificialUB, final boolean artificialObjLB,
            final boolean artificialObjUB, final double precision, final double[] objectiveCoefficients, final double objectiveConstant) {

        super();

        myArtificialLB = artificialLB;
        myArtificialObjLB = artificialObjLB;
        myArtificialObjUB = artificialObjUB;
        myArtificialUB = artificialUB;
        myDelegateModel = model;
        myDelegateVariables = variables;
        myIsIntVar = isIntVar;
        myObjectiveVariable = objective;
        myMaximise = maximise;
        myObjectiveCoefficients = objectiveCoefficients;
        myObjectiveConstant = objectiveConstant;
        myPrecision = precision;
        myTimeAbortMillis = options.time_abort;
        myTimeSufficeMillis = options.time_suffice;
    }

    @Override
    public void dispose() {
        Solver.super.dispose();
    }

    @Override
    public Result solve(final Result kickStarter) {

        long startMillis = System.currentTimeMillis();

        org.chocosolver.solver.Solver delegateSolver = myDelegateModel.getSolver();

        Solution solution = delegateSolver.findOptimalSolution(myObjectiveVariable, myMaximise);

        Optimisation.State retState;
        double retValue = NaN;
        double[] retSolution = new double[myDelegateVariables.length];

        if (solution != null) {

            int objValue = solution.getIntVal(myObjectiveVariable);

            if (this.isUnbounded(objValue)) {
                return Optimisation.Result.of(Optimisation.State.UNBOUNDED, retSolution);
            }

            this.extractSolution(solution, retSolution);

            if (this.isUnbounded(retSolution)) {
                return Optimisation.Result.of(Optimisation.State.UNBOUNDED, retSolution);
            }

            retState = delegateSolver.isObjectiveOptimal() ? Optimisation.State.OPTIMAL : Optimisation.State.FEASIBLE;
            retValue = this.evaluate(retSolution);

            return Optimisation.Result.of(retValue, retState, retSolution);

        } else if (delegateSolver.getSearchState() == SearchState.STOPPED) {
            retState = Optimisation.State.FAILED;

            if (myTimeSufficeMillis < myTimeAbortMillis) {
                long elapsed = System.currentTimeMillis() - startMillis;
                long remaining = myTimeAbortMillis - elapsed;
                if (remaining > 1000L) {
                    delegateSolver.limitTime(remaining);

                    solution = delegateSolver.findOptimalSolution(myObjectiveVariable, myMaximise);

                    if (solution != null) {
                        this.extractSolution(solution, retSolution);
                        retState = delegateSolver.isObjectiveOptimal() ? Optimisation.State.OPTIMAL : Optimisation.State.FEASIBLE;
                        retValue = this.evaluate(retSolution);
                        return Optimisation.Result.of(retValue, retState, retSolution);
                    }
                }
            }

        } else {
            retState = Optimisation.State.INFEASIBLE;
        }

        return Optimisation.Result.of(retState);
    }

    /**
     * The objective function value, in model units and including the constant.
     */
    private double evaluate(final double[] solution) {
        double retVal = myObjectiveConstant;
        for (int j = 0; j < solution.length; j++) {
            retVal += myObjectiveCoefficients[j] * solution[j];
        }
        return retVal;
    }

    private void extractSolution(final Solution solution, final double[] target) {
        for (int i = 0; i < target.length; i++) {
            if (myIsIntVar[i]) {
                target[i] = solution.getIntVal((IntVar) myDelegateVariables[i]);
            } else {
                double[] bounds = solution.getRealBounds((RealVar) myDelegateVariables[i]);
                target[i] = (bounds[0] + bounds[1]) / 2.0;
            }
        }
    }

    private boolean isUnbounded(final double[] solution) {
        for (int i = 0; i < solution.length; i++) {
            if (myIsIntVar[i]) {
                int val = (int) solution[i];
                if (myArtificialUB[i] && val >= IntVar.MAX_INT_BOUND || myArtificialLB[i] && val <= IntVar.MIN_INT_BOUND) {
                    return true;
                }
            } else {
                if (myArtificialUB[i] && solution[i] >= REAL_ARTIFICIAL_BOUND - myPrecision
                        || myArtificialLB[i] && solution[i] <= -REAL_ARTIFICIAL_BOUND + myPrecision) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isUnbounded(final int objValue) {
        if (!myMaximise && myArtificialObjLB && objValue <= myObjectiveVariable.getLB()) {
            return true;
        }
        return myMaximise && myArtificialObjUB && objValue >= myObjectiveVariable.getUB();
    }

}
