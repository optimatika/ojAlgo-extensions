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
package org.ojalgo.optimisation.solver.hipparchus;

import java.util.List;

import org.hipparchus.linear.RealMatrix;
import org.hipparchus.linear.RealVector;
import org.hipparchus.optim.OptimizationData;
import org.hipparchus.optim.nonlinear.scalar.ObjectiveFunction;
import org.hipparchus.optim.nonlinear.vector.constrained.BoundedConstraint;
import org.hipparchus.optim.nonlinear.vector.constrained.EqualityConstraint;
import org.hipparchus.optim.nonlinear.vector.constrained.GradientMode;
import org.hipparchus.optim.nonlinear.vector.constrained.InequalityConstraint;
import org.hipparchus.optim.nonlinear.vector.constrained.LagrangeSolution;
import org.hipparchus.optim.nonlinear.vector.constrained.SQPOptimizerS2;
import org.hipparchus.optim.nonlinear.vector.constrained.SQPOption;
import org.hipparchus.optim.nonlinear.vector.constrained.TwiceDifferentiableFunction;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.Optimisation;

/**
 * Hipparchus' {@link SQPOptimizerS2}, the replacement for its deprecated {@code SQPOptimizerS} and
 * {@code SQPOptimizerGM}. It is a general nonlinear (SQP) optimiser – for convex QP the
 * {@link QPDualActiveSolverImpl} (that this optimiser uses for its sub-problems) is faster and more accurate.
 * <p>
 * Unlike the other optimisers it also handles convex quadratic constraints (QCQP): one-sided, with a positive
 * semidefinite quadratic form if the upper limit is set, or a negative semidefinite one if the lower limit
 * is. A model with any other quadratic constraint is not handled ({@link Integration#isCapable} is false).
 * The objective may then be linear. The optimiser approximates the Hessian of the Lagrangian (BFGS), so only
 * the constraints' values and Jacobian are needed.
 * <p>
 * The objective's and constraints' own gradients are used (not finite differences), and the iterations are
 * limited to {@link Optimisation.Options#iterations_abort} if that is set.
 * <p>
 * When it converges the optimiser may return the multipliers of the previous iterate (those of a QP
 * sub-problem solved with a cruder Hessian approximation), rather than those of the solution. They are only
 * used, for the result's dual values and reduced gradient, if they satisfy the optimality conditions at the
 * solution, see {@link #getOptimalityViolation(LagrangeSolution, Translation)}. Otherwise the result has
 * none. With the (small) Maros-Meszaros QP models that was the case for most.
 * <p>
 * The optimiser has no infeasibility detection, and returns whatever it has when it stops. A solution that does
 * not satisfy the constraints, with the same tolerance as the optimiser's own convergence test, gives a
 * {@link Optimisation.State#FAILED} result.
 */
public final class SQPOptimizerImplS2 extends ConvexImpl<SQPOptimizerS2> {

    public static class Configurator extends AbstractImpl.Configurator<SQPOptimizerS2> {

        @Override
        SQPOptimizerS2 newDelegateSolver(final Options options) {
            return new SQPOptimizerS2();
        }

    }

    static final class Integration extends ConvexImpl.Integration<SQPOptimizerImplS2> {

        @Override
        boolean isBoundedConstraintSupported() {
            return false;
        }

        @Override
        boolean isQuadraticConstraintSupported() {
            return true;
        }

        @Override
        SQPOptimizerImplS2 newSolver(final ExpressionsBasedModel model, final Translation translation) {
            return new SQPOptimizerImplS2(translation, model.options, model.getConfigurator(DEFAULT));
        }

    }

    public static final Integration INTEGRATION = new Integration();

    static final Configurator DEFAULT = new Configurator();

    /**
     * The largest relative violation of the optimality conditions for the optimiser's multipliers to be used,
     * see {@link #getOptimalityViolation(LagrangeSolution, Translation)}.
     */
    static final double MULTIPLIER_TOLERANCE = 1.0E-6;

    SQPOptimizerImplS2(final Translation translation, final Options options, final AbstractImpl.Configurator<SQPOptimizerS2> configurator) {
        super(translation, options, configurator);
    }

    @Override
    void addSettings(final List<OptimizationData> data, final Optimisation.Options options) {
        SQPOption settings = new SQPOption();
        settings.setGradientMode(GradientMode.EXTERNAL);
        if (options.iterations_abort < Integer.MAX_VALUE) {
            settings.setMaxIteration(options.iterations_abort);
        }
        data.add(settings);
    }

    /**
     * How far the multipliers are from satisfying the optimality (KKT) conditions at the solution – the
     * largest of these, each relative to the magnitude of its terms:
     * <ol>
     * <li>stationarity, ∇f(x) - J'λ = 0, per variable (relative to at least the tolerance times the largest
     * terms of any variable – rounding errors are relative to those);
     * <li>the sign of an inequality multiplier, λ >= 0, relative to the largest multiplier;
     * <li>complementary slackness – the slack, c(x) - l, of an inequality with a (relatively) nonzero
     * multiplier, relative to the constraint's terms.
     * </ol>
     *
     * @return The largest relative violation, or infinity if the multipliers do not match the constraints.
     */
    static double getOptimalityViolation(final LagrangeSolution solution, final Translation translation) {

        TwiceDifferentiableFunction objective = null;
        EqualityConstraint equalities = null;
        InequalityConstraint inequalities = null;
        for (OptimizationData data : translation.data) {
            if (data instanceof ObjectiveFunction function) {
                objective = (TwiceDifferentiableFunction) function.getObjectiveFunction();
            } else if (data instanceof EqualityConstraint constraint) {
                equalities = constraint;
            } else if (data instanceof InequalityConstraint constraint) {
                inequalities = constraint;
            }
        }

        RealVector x = solution.getX();
        double[] lambda = solution.getLambda().toArray();
        int nbEqualities = equalities != null ? equalities.dimY() : 0;
        int nbInequalities = inequalities != null ? inequalities.dimY() : 0;
        if (objective == null || lambda.length != nbEqualities + nbInequalities) {
            return Double.POSITIVE_INFINITY;
        }

        double[] residual = objective.gradient(x).toArray();
        double[] magnitude = new double[residual.length];
        for (int j = 0; j < residual.length; j++) {
            magnitude[j] = Math.abs(residual[j]);
        }

        double largest = 0.0;
        for (double multiplier : lambda) {
            largest = Math.max(largest, Math.abs(multiplier));
        }

        double retVal = 0.0;

        if (equalities != null) {
            SQPOptimizerImplS2.subtract(equalities.jacobian(x), lambda, 0, residual, magnitude);
        }

        if (inequalities != null) {

            RealMatrix jacobian = inequalities.jacobian(x);
            SQPOptimizerImplS2.subtract(jacobian, lambda, nbEqualities, residual, magnitude);

            RealVector values = inequalities.value(x);
            RealVector limits = inequalities.getLowerBound();
            double[] point = x.toArray();

            for (int i = 0; i < nbInequalities; i++) {

                double multiplier = lambda[nbEqualities + i];
                if (largest > 0.0) {
                    retVal = Math.max(retVal, -multiplier / largest);
                }

                double terms = Math.abs(limits.getEntry(i));
                for (int j = 0; j < point.length; j++) {
                    terms += Math.abs(jacobian.getEntry(i, j) * point[j]);
                }
                double slack = values.getEntry(i) - limits.getEntry(i);
                if (largest > 0.0 && terms > 0.0) {
                    retVal = Math.max(retVal, Math.min(Math.abs(multiplier) / largest, Math.abs(slack) / terms));
                }
            }
        }

        // Rounding errors are relative to the largest terms, not to those of a variable with tiny ones
        double floor = 0.0;
        for (int j = 0; j < residual.length; j++) {
            floor = Math.max(floor, MULTIPLIER_TOLERANCE * magnitude[j]);
        }
        for (int j = 0; j < residual.length; j++) {
            double scale = Math.max(magnitude[j], floor);
            if (scale > 0.0) {
                retVal = Math.max(retVal, Math.abs(residual[j]) / scale);
            }
        }

        return retVal;
    }

    /**
     * Subtracts J'λ, of the rows starting at offset in λ, from the residual, and adds the magnitudes of the
     * terms.
     */
    private static void subtract(final RealMatrix jacobian, final double[] lambda, final int offset, final double[] residual, final double[] magnitude) {
        for (int i = 0; i < jacobian.getRowDimension(); i++) {
            double multiplier = lambda[offset + i];
            for (int j = 0; j < residual.length; j++) {
                double term = multiplier * jacobian.getEntry(i, j);
                residual[j] -= term;
                magnitude[j] += Math.abs(term);
            }
        }
    }

    /**
     * The optimiser's multipliers are used only if they satisfy the optimality (KKT) conditions at the
     * solution, see {@link #getOptimalityViolation(LagrangeSolution, Translation)}. Otherwise there are no
     * dual values, see the class documentation.
     */
    @Override
    Optimisation.Result complete(final Optimisation.Result result, final LagrangeSolution solutionAndValue, final Translation translation) {
        if (SQPOptimizerImplS2.getOptimalityViolation(solutionAndValue, translation) <= MULTIPLIER_TOLERANCE) {
            return super.complete(result, solutionAndValue, translation);
        } else {
            return result;
        }
    }

    /**
     * The optimiser's convergence test requires the constraint violation (L¹-norm) to be at most the square
     * root of {@link SQPOption#getEps()}.
     */
    @Override
    Optimisation.State extractState(final LagrangeSolution solutionAndValue, final Translation translation) {

        RealVector x = solutionAndValue.getX();

        double violation = 0.0;
        for (OptimizationData data : translation.data) {
            if (data instanceof BoundedConstraint constraint) {
                violation += constraint.overshoot(constraint.value(x));
            }
        }

        return violation <= Math.sqrt(SQPOption.DEFAULT_EPSILON) ? Optimisation.State.OPTIMAL : Optimisation.State.FAILED;
    }

}
