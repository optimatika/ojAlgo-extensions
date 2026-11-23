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

import org.hipparchus.linear.RealVector;
import org.hipparchus.optim.OptimizationData;
import org.hipparchus.optim.nonlinear.vector.constrained.BoundedConstraint;
import org.hipparchus.optim.nonlinear.vector.constrained.GradientMode;
import org.hipparchus.optim.nonlinear.vector.constrained.LagrangeSolution;
import org.hipparchus.optim.nonlinear.vector.constrained.SQPOptimizerS2;
import org.hipparchus.optim.nonlinear.vector.constrained.SQPOption;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.Optimisation;

/**
 * Hipparchus' {@link SQPOptimizerS2}, the replacement for its deprecated {@code SQPOptimizerS} and
 * {@code SQPOptimizerGM}. It is a general nonlinear (SQP) optimiser – for convex QP the
 * {@link QPDualActiveSolverImpl} (that this optimiser uses for its sub-problems) is faster and more accurate.
 * The objective's and constraints' own gradients are used (not finite differences), and the iterations are
 * limited to {@link Optimisation.Options#iterations_abort} if that is set. The result has no dual values or
 * reduced gradient. When it converges the optimiser may return the multipliers of the previous iterate (those
 * of a QP sub-problem solved with a cruder Hessian approximation), rather than those of the solution. The
 * optimiser has no infeasibility detection, and returns whatever it has when it stops. A solution that does
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
        SQPOptimizerImplS2 newSolver(final ExpressionsBasedModel model, final Translation translation) {
            return new SQPOptimizerImplS2(translation, model.options, model.getConfigurator(DEFAULT));
        }

    }

    public static final Integration INTEGRATION = new Integration();

    static final Configurator DEFAULT = new Configurator();

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
     * No dual values, see the class documentation.
     */
    @Override
    Optimisation.Result complete(final Optimisation.Result result, final LagrangeSolution solutionAndValue, final Translation translation) {
        return result;
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
            if (data instanceof BoundedConstraint) {
                BoundedConstraint constraint = (BoundedConstraint) data;
                violation += constraint.overshoot(constraint.value(x));
            }
        }

        return violation <= Math.sqrt(SQPOption.DEFAULT_EPSILON) ? Optimisation.State.OPTIMAL : Optimisation.State.FAILED;
    }

}
