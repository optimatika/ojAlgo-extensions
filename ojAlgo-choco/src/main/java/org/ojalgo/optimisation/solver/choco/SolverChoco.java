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

import java.util.List;
import java.util.Set;

import org.chocosolver.solver.Solution;
import org.chocosolver.solver.search.SearchState;
import org.chocosolver.solver.variables.IntVar;
import org.ojalgo.optimisation.Expression;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.Optimisation;
import org.ojalgo.optimisation.Variable;
import org.ojalgo.structure.Structure1D.IntIndex;

/**
 * Choco-solver is a constraint programming (CP) solver. It only works with integer (finite domain) variables,
 * and its scalar/linear constraints only accept <code>int</code> coefficients and bounds.
 * <p>
 * This means:
 * <ol>
 * <li>Models with no integer variables at all are not accepted -
 * {@link Integration#isCapable(ExpressionsBasedModel)} requires
 * {@link ExpressionsBasedModel#isAnyVariableInteger()}.
 * <li>Quadratic expressions are not supported - {@link Integration#isCapable(ExpressionsBasedModel)} also
 * requires {@code !model.isAnyExpressionQuadratic()}.
 * <li>Variable bounds are rounded to the nearest feasible integer: the lower bound is rounded up
 * ({@link Math#ceil(double)}) and the upper bound is rounded down ({@link Math#floor(double)}).
 * <li>Constraint and objective coefficients - as well as constraint right-hand sides - are rounded to the
 * nearest {@code int} ({@link Math#round(double)}). Models with fractional (non-integer) coefficients or
 * limits will therefore not be translated exactly - this is a fundamental limitation of Choco's scalar
 * constraint API, not something this integration can work around. Only use this integration with models that
 * already have (or are close enough to) integer data.
 * </ol>
 * The objective is modelled as an extra {@link IntVar}, bounded by the min/max the weighted sum of the
 * decision variables' domains can attain, constrained to equal that weighted sum via
 * {@link org.chocosolver.solver.Model#scalar(IntVar[], int[], String, IntVar)}, and then optimized with
 * {@link org.chocosolver.solver.search.IResolutionHelper#findOptimalSolution(IntVar, boolean, org.chocosolver.util.criteria.Criterion...)}.
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

    }

    static final class Integration extends ExpressionsBasedModel.Integration<SolverChoco> {

        private static final boolean ADJUSTED_BOUNDS = false;
        private static final boolean ADJUSTED_CONSTRAINTS = false;
        private static final boolean ADJUSTED_OBJECTIVE = false;

        Integration() {
            super();
        }

        @Override
        public SolverChoco build(final ExpressionsBasedModel model) {

            Configurator configurator = model.getConfigurator(DEFAULT);

            org.chocosolver.solver.Model delegate = configurator.newModel(model.options);

            List<Variable> modVars = model.getVariables();
            int nbVars = modVars.size();

            IntVar[] delegateVars = new IntVar[nbVars];

            for (int i = 0; i < nbVars; i++) {

                Variable modVar = modVars.get(i);

                String name = modVar.getName();
                double lowerLimit = modVar.getLowerLimit(ADJUSTED_BOUNDS, NEGATIVE_INFINITY);
                double upperLimit = modVar.getUpperLimit(ADJUSTED_BOUNDS, POSITIVE_INFINITY);

                int lb = SolverChoco.toLowerBound(lowerLimit);
                int ub = SolverChoco.toUpperBound(upperLimit);
                if (ub < lb) {
                    // Bounds became infeasible after rounding to int - keep them crossed so Choco itself
                    // discovers the infeasibility rather than silently "fixing" the model.
                    ub = lb;
                }

                delegateVars[i] = delegate.intVar(name, lb, ub);
            }

            model.constraints().forEach(mConstr -> SolverChoco.addConstraint(delegate, mConstr, delegateVars, ADJUSTED_CONSTRAINTS));

            Expression modObj = model.objective();
            Set<IntIndex> objKeys = modObj.getLinearKeySet();
            int nbObjTerms = objKeys.size();

            IntVar[] objVars = new IntVar[nbObjTerms];
            int[] objCoeffs = new int[nbObjTerms];

            long objMin = 0L;
            long objMax = 0L;

            int t = 0;
            for (IntIndex key : objKeys) {

                IntVar termVar = delegateVars[key.index];
                int coefficient = SolverChoco.toInt(modObj.doubleValue(key, ADJUSTED_OBJECTIVE));

                objVars[t] = termVar;
                objCoeffs[t] = coefficient;

                long lo = (long) coefficient * termVar.getLB();
                long hi = (long) coefficient * termVar.getUB();
                objMin += Math.min(lo, hi);
                objMax += Math.max(lo, hi);

                t++;
            }

            int objLB = (int) Math.max(IntVar.MIN_INT_BOUND, objMin);
            int objUB = (int) Math.min(IntVar.MAX_INT_BOUND, objMax);
            if (objUB < objLB) {
                objUB = objLB;
            }

            IntVar objVar = delegate.intVar("objective", objLB, objUB);
            if (nbObjTerms > 0) {
                delegate.scalar(objVars, objCoeffs, "=", objVar).post();
            } else {
                delegate.arithm(objVar, "=", 0).post();
            }

            boolean maximise = model.getOptimisationSense() == Optimisation.Sense.MAX;

            return new SolverChoco(delegate, delegateVars, objVar, maximise, model.options);
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

    /**
     * Translates one {@link Expression} (constraint) of the ojAlgo model into 1-2 Choco scalar constraints
     * (equality constraints become 1, ranged/one-sided constraints become as many as there are active
     * bounds).
     */
    static void addConstraint(final org.chocosolver.solver.Model delegate, final Expression mConstr, final IntVar[] delegateVars, final boolean adjusted) {

        Set<IntIndex> keys = mConstr.getLinearKeySet();
        int nbTerms = keys.size();

        IntVar[] termVars = new IntVar[nbTerms];
        int[] termCoeffs = new int[nbTerms];

        int t = 0;
        for (IntIndex key : keys) {
            termVars[t] = delegateVars[key.index];
            termCoeffs[t] = SolverChoco.toInt(mConstr.doubleValue(key, adjusted));
            t++;
        }

        if (nbTerms <= 0) {
            // No variables in this expression - nothing meaningful to constrain via a scalar constraint.
            return;
        }

        if (mConstr.isEqualityConstraint()) {
            int rhs = SolverChoco.toInt(mConstr.getLowerLimit(adjusted, ZERO));
            delegate.scalar(termVars, termCoeffs, "=", rhs).post();
        } else {
            if (mConstr.isLowerConstraint()) {
                int rhs = SolverChoco.toInt(mConstr.getLowerLimit(adjusted, NEGATIVE_INFINITY));
                delegate.scalar(termVars, termCoeffs, ">=", rhs).post();
            }
            if (mConstr.isUpperConstraint()) {
                int rhs = SolverChoco.toInt(mConstr.getUpperLimit(adjusted, POSITIVE_INFINITY));
                delegate.scalar(termVars, termCoeffs, "<=", rhs).post();
            }
        }
    }

    /**
     * Rounds a model coefficient/limit to the nearest representable {@code int}, clamping rather than
     * overflowing if the magnitude is too large. See the class javadoc for why this rounding happens at all.
     */
    static int toInt(final double value) {
        if (Double.isNaN(value)) {
            return 0;
        } else if (value >= Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        } else if (value <= -Integer.MAX_VALUE) {
            return -Integer.MAX_VALUE;
        } else {
            return (int) Math.round(value);
        }
    }

    static int toLowerBound(final double value) {
        if (value <= IntVar.MIN_INT_BOUND) {
            return IntVar.MIN_INT_BOUND;
        } else if (value >= IntVar.MAX_INT_BOUND) {
            return IntVar.MAX_INT_BOUND;
        } else {
            return (int) Math.ceil(value);
        }
    }

    static int toUpperBound(final double value) {
        if (value >= IntVar.MAX_INT_BOUND) {
            return IntVar.MAX_INT_BOUND;
        } else if (value <= IntVar.MIN_INT_BOUND) {
            return IntVar.MIN_INT_BOUND;
        } else {
            return (int) Math.floor(value);
        }
    }

    private final org.chocosolver.solver.Model myDelegateModel;
    private final IntVar[] myDelegateVariables;
    private final boolean myMaximise;
    private final IntVar myObjectiveVariable;
    private final long myTimeAbortMillis;
    private final long myTimeSufficeMillis;

    SolverChoco(final org.chocosolver.solver.Model model, final IntVar[] variables, final IntVar objective, final boolean maximise,
            final Optimisation.Options options) {

        super();

        myDelegateModel = model;
        myDelegateVariables = variables;
        myObjectiveVariable = objective;
        myMaximise = maximise;
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

            retState = delegateSolver.isObjectiveOptimal() ? Optimisation.State.OPTIMAL : Optimisation.State.FEASIBLE;

            retValue = solution.getIntVal(myObjectiveVariable);

            for (int i = 0; i < retSolution.length; i++) {
                retSolution[i] = solution.getIntVal(myDelegateVariables[i]);
            }

            return Optimisation.Result.of(retValue, retState, retSolution);

        } else if (delegateSolver.getSearchState() == SearchState.STOPPED) {
            // Time (or other) limit reached before a first feasible solution was found - the search is
            // neither exhausted (so not proven infeasible) nor did it produce anything to report.
            retState = Optimisation.State.FAILED;

            if (myTimeSufficeMillis < myTimeAbortMillis) {
                long elapsed = System.currentTimeMillis() - startMillis;
                long remaining = myTimeAbortMillis - elapsed;
                if (remaining > 1000L) {
                    delegateSolver.limitTime(remaining);

                    solution = delegateSolver.findOptimalSolution(myObjectiveVariable, myMaximise);

                    if (solution != null) {
                        retState = delegateSolver.isObjectiveOptimal() ? Optimisation.State.OPTIMAL : Optimisation.State.FEASIBLE;
                        retValue = solution.getIntVal(myObjectiveVariable);
                        for (int i = 0; i < retSolution.length; i++) {
                            retSolution[i] = solution.getIntVal(myDelegateVariables[i]);
                        }
                        return Optimisation.Result.of(retValue, retState, retSolution);
                    }
                }
            }

        } else {
            // The search space was exhausted without finding a solution - proven infeasible.
            retState = Optimisation.State.INFEASIBLE;
        }

        return Optimisation.Result.of(retState);
    }

}
