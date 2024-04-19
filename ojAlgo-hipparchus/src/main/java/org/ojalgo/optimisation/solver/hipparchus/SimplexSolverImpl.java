/*
 * Copyright 1997-2023 Optimatika
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

import java.util.Set;

import org.hipparchus.optim.OptimizationData;
import org.hipparchus.optim.linear.SimplexSolver;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.Optimisation;

public final class SimplexSolverImpl extends LinearImpl<SimplexSolver> {

    @FunctionalInterface
    public interface Configurator extends AbstractImpl.Configurator<SimplexSolver> {

    }

    public static final class Integration extends LinearImpl.Integration<SimplexSolverImpl> {

        Integration() {
            super();
        }

        @Override
        public boolean isCapable(final ExpressionsBasedModel model) {
            return !model.isAnyVariableInteger() && !model.isAnyExpressionQuadratic();
        }

        @Override
        SimplexSolverImpl newSolver(final Set<OptimizationData> optimisationdata, final Options options) {
            return new SimplexSolverImpl(optimisationdata, options);
        }

    }

    public static final SimplexSolverImpl.Integration INTEGRATION = new Integration();

    static final Configurator DEFAULT = (solver, options) -> {};

    SimplexSolverImpl(final Set<OptimizationData> modelData, final Optimisation.Options options) {
        super(modelData, options, DEFAULT);
    }

    @Override
    SimplexSolver newDelegateSolver() {
        return new SimplexSolver();
    }

}
