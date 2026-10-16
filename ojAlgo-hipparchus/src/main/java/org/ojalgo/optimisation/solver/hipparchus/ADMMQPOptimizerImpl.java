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

import org.hipparchus.optim.nonlinear.vector.constrained.ADMMQPOptimizer;
import org.ojalgo.optimisation.ExpressionsBasedModel;

public final class ADMMQPOptimizerImpl extends ConvexImpl<ADMMQPOptimizer> {

    public static class Configurator extends AbstractImpl.Configurator<ADMMQPOptimizer> {

        @Override
        ADMMQPOptimizer newDelegateSolver(final Options options) {
            return new ADMMQPOptimizer();
        }

    }

    static final class Integration extends ConvexImpl.Integration<ADMMQPOptimizerImpl> {

        @Override
        boolean isBoundedConstraintSupported() {
            return true;
        }

        @Override
        ADMMQPOptimizerImpl newSolver(final ExpressionsBasedModel model, final Translation translation) {
            return new ADMMQPOptimizerImpl(translation, model.options, model.getConfigurator(DEFAULT));
        }

    }

    public static final Integration INTEGRATION = new Integration();

    static final Configurator DEFAULT = new Configurator();

    /**
     * The iteration limit used when {@link Options#iterations_abort} is not set.
     */
    static final int DEFAULT_ITERATION_LIMIT = 10_000;

    ADMMQPOptimizerImpl(final Translation translation, final Options options,
            final org.ojalgo.optimisation.solver.hipparchus.AbstractImpl.Configurator<ADMMQPOptimizer> configurator) {
        super(translation, options, configurator);
    }

    /**
     * Hipparchus' ADMM iterates until it converges, and has no infeasibility detection – with an infeasible
     * problem it would never stop. The iterations are limited to {@link Options#iterations_abort}, or to
     * {@value #DEFAULT_ITERATION_LIMIT} if that is not set. Reaching the limit gives a FAILED result.
     */
    @Override
    int getIterationLimit(final Options options) {
        return options.iterations_abort < Integer.MAX_VALUE ? options.iterations_abort : DEFAULT_ITERATION_LIMIT;
    }

}
