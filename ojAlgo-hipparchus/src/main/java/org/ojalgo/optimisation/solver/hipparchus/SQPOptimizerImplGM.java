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

import org.hipparchus.optim.nonlinear.vector.constrained.SQPOptimizerGM;
import org.ojalgo.optimisation.ExpressionsBasedModel;

public final class SQPOptimizerImplGM extends ConvexImpl<SQPOptimizerGM> {

    public static class Configurator extends AbstractImpl.Configurator<SQPOptimizerGM> {

        @Override
        SQPOptimizerGM newDelegateSolver(final Options options) {
            return new SQPOptimizerGM();
        }

    }

    static final class Integration extends ConvexImpl.Integration<SQPOptimizerImplGM> {

        @Override
        boolean isBoundedConstraintSupported() {
            return false;
        }

        @Override
        SQPOptimizerImplGM newSolver(final ExpressionsBasedModel model, final Translation translation) {
            return new SQPOptimizerImplGM(translation, model.options, model.getConfigurator(DEFAULT));
        }

    }

    public static final Integration INTEGRATION = new Integration();

    static final Configurator DEFAULT = new Configurator();

    SQPOptimizerImplGM(final Translation translation, final Options options,
            final org.ojalgo.optimisation.solver.hipparchus.AbstractImpl.Configurator<SQPOptimizerGM> configurator) {
        super(translation, options, configurator);
    }

}
