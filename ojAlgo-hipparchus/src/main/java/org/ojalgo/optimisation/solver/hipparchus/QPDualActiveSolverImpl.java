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

import org.hipparchus.optim.OptimizationData;
import org.hipparchus.optim.nonlinear.vector.constrained.LagrangeSolution;
import org.hipparchus.optim.nonlinear.vector.constrained.QPDualActiveSolver;
import org.hipparchus.optim.nonlinear.vector.constrained.QPDualActiveSolverOption;
import org.hipparchus.optim.nonlinear.vector.constrained.RegularizedCholeskyDecomposition;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.Optimisation;

/**
 * Hipparchus' {@link QPDualActiveSolver}, the dual active-set method of Goldfarb and Idnani, for convex QP.
 * Being an active-set method it solves accurately, with dual values, and it detects infeasibility.
 * <p>
 * The Hessian is factorised with a {@link RegularizedCholeskyDecomposition}, that shifts the diagonal until
 * the factorisation succeeds. A positive semidefinite Hessian is fine (the shift is tiny), but the model must
 * be convex – a non-convex model is not detected.
 * <p>
 * The solver returns, rather than throws, when it fails. Its error codes are translated to
 * {@link Optimisation.State#INFEASIBLE} or {@link Optimisation.State#FAILED}.
 *
 * @see "https://doi.org/10.1007/BF02591962"
 */
public final class QPDualActiveSolverImpl extends ConvexImpl<QPDualActiveSolver> {

    public static class Configurator extends AbstractImpl.Configurator<QPDualActiveSolver> {

        @Override
        QPDualActiveSolver newDelegateSolver(final Options options) {
            return new QPDualActiveSolver();
        }

    }

    static final class Integration extends ConvexImpl.Integration<QPDualActiveSolverImpl> {

        /**
         * The bounded constraints' multipliers are laid out differently than {@link ConvexImpl} assumes – all
         * inequalities are passed as one-sided constraints.
         */
        @Override
        boolean isBoundedConstraintSupported() {
            return false;
        }

        @Override
        QPDualActiveSolverImpl newSolver(final ExpressionsBasedModel model, final Translation translation) {
            return new QPDualActiveSolverImpl(translation, model.options, model.getConfigurator(DEFAULT));
        }

    }

    public static final Integration INTEGRATION = new Integration();

    static final Configurator DEFAULT = new Configurator();

    /**
     * The tolerance for (normalised) constraint violation, and for the Cholesky decomposition. Hipparchus'
     * default, machine epsilon, is too strict – with it the solver wrongly reports some ill-conditioned
     * models (DUALC2 in the Maros-Meszaros test set) as infeasible.
     */
    static final double EPSILON = 1.0E-12;

    QPDualActiveSolverImpl(final Translation translation, final Options options, final AbstractImpl.Configurator<QPDualActiveSolver> configurator) {
        super(translation, options, configurator);
    }

    @Override
    void addSettings(final List<OptimizationData> data, final Optimisation.Options options) {
        QPDualActiveSolverOption settings = new QPDualActiveSolverOption();
        settings.setEps(EPSILON);
        data.add(settings);
    }

    /**
     * A failure is returned as an empty solution, with an error code as the only multiplier.
     */
    @Override
    Optimisation.State extractState(final LagrangeSolution solutionAndValue, final Translation translation) {

        if (solutionAndValue.getX().getDimension() > 0) {
            return Optimisation.State.OPTIMAL;
        }

        if (solutionAndValue.getLambda().getDimension() == 1 && solutionAndValue.getLambda().getEntry(0) == QPDualActiveSolver.ERROR_INFEASIBLE) {
            return Optimisation.State.INFEASIBLE;
        } else {
            return Optimisation.State.FAILED;
        }
    }

}
