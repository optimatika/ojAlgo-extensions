/*
 * Copyright 1997-2025 Optimatika
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

import static org.ojalgo.function.constant.PrimitiveMath.ZERO;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hipparchus.exception.Localizable;
import org.hipparchus.optim.LocalizedOptimFormats;
import org.hipparchus.optim.OptimizationData;
import org.hipparchus.optim.nonlinear.scalar.GoalType;
import org.hipparchus.optim.nonlinear.scalar.ObjectiveFunction;
import org.hipparchus.optim.nonlinear.vector.constrained.ConstraintOptimizer;
import org.hipparchus.optim.nonlinear.vector.constrained.LagrangeSolution;
import org.hipparchus.optim.nonlinear.vector.constrained.LinearBoundedConstraint;
import org.hipparchus.optim.nonlinear.vector.constrained.LinearEqualityConstraint;
import org.hipparchus.optim.nonlinear.vector.constrained.QuadraticFunction;
import org.ojalgo.function.multiary.MultiaryFunction.TwiceDifferentiable;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.Optimisation;
import org.ojalgo.structure.Access1D;
import org.ojalgo.structure.Primitive1D;

abstract class ConvexImpl<S extends ConstraintOptimizer> extends AbstractImpl<S, LagrangeSolution> {

    abstract static class Integration<S extends ConvexImpl<?>> extends AbstractImpl.Integration<S> {

        @Override
        public final boolean isCapable(final ExpressionsBasedModel model) {
            return !model.isAnyVariableInteger() && model.isAnyObjectiveQuadratic() && !model.isAnyConstraintQuadratic();
        }

        @Override
        Set<OptimizationData> toOptimizationData(final TwiceDifferentiable<Double> objective, final boolean maximize, final List<ConstraintTemplate> equalities,
                final List<ConstraintTemplate> ranges, final boolean negative) {

            Set<OptimizationData> retVal = new HashSet<>();

            retVal.add(new ObjectiveFunction(
                    new QuadraticFunction(objective.getHessian(null).toRawCopy2D(), objective.getLinearFactors(false).toRawCopy1D(), ZERO)));
            retVal.add(maximize ? GoalType.MAXIMIZE : GoalType.MINIMIZE);

            int nbEql = equalities.size();
            if (nbEql > 0) {
                double[][] a = new double[nbEql][];
                double[] b = new double[nbEql];
                for (int i = 0; i < nbEql; i++) {
                    ConstraintTemplate constraintTemplate = equalities.get(i);
                    a[i] = constraintTemplate.body;
                    b[i] = constraintTemplate.lower;
                }
                retVal.add(new LinearEqualityConstraint(a, b));
            }

            int nbRng = ranges.size();
            if (nbRng > 0) {
                double[][] a = new double[nbRng][];
                double[] l = new double[nbRng];
                double[] u = new double[nbRng];
                for (int i = 0; i < nbRng; i++) {
                    ConstraintTemplate constraintTemplate = ranges.get(i);
                    a[i] = constraintTemplate.body;
                    l[i] = constraintTemplate.lower;
                    u[i] = constraintTemplate.upper;
                }
                retVal.add(new LinearBoundedConstraint(a, l, u));
            }

            return retVal;
        }

    }

    ConvexImpl(final Set<OptimizationData> modelData, final Options options, final Configurator<S> configurator) {
        super(modelData, options, configurator);
    }

    @Override
    Access1D<?> extractSolution(final LagrangeSolution solutionAndValue) {
        return Primitive1D.of(solutionAndValue.getX().toArray());
    }

    @Override
    State extractState(final Localizable exceptionSpecifier) {

        if (LocalizedOptimFormats.NO_FEASIBLE_SOLUTION.equals(exceptionSpecifier)) {
            return Optimisation.State.INFEASIBLE;
        } else if (LocalizedOptimFormats.UNBOUNDED_SOLUTION.equals(exceptionSpecifier)) {
            return Optimisation.State.UNBOUNDED;
        } else {
            return Optimisation.State.FAILED;
        }
    }

    @Override
    double extractValue(final LagrangeSolution solutionAndValue) {
        return solutionAndValue.getValue();
    }
}
