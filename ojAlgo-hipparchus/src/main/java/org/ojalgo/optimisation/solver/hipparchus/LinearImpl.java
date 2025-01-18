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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hipparchus.exception.Localizable;
import org.hipparchus.optim.LocalizedOptimFormats;
import org.hipparchus.optim.OptimizationData;
import org.hipparchus.optim.PointValuePair;
import org.hipparchus.optim.linear.LinearConstraint;
import org.hipparchus.optim.linear.LinearConstraintSet;
import org.hipparchus.optim.linear.LinearObjectiveFunction;
import org.hipparchus.optim.linear.NonNegativeConstraint;
import org.hipparchus.optim.linear.Relationship;
import org.hipparchus.optim.nonlinear.scalar.GoalType;
import org.hipparchus.optim.nonlinear.scalar.MultivariateOptimizer;
import org.ojalgo.function.multiary.MultiaryFunction.TwiceDifferentiable;
import org.ojalgo.optimisation.Optimisation;
import org.ojalgo.structure.Access1D;
import org.ojalgo.structure.Primitive1D;

abstract class LinearImpl<S extends MultivariateOptimizer> extends AbstractImpl<S, PointValuePair> {

    abstract static class Integration<S extends LinearImpl<?>> extends AbstractImpl.Integration<S> {

        @Override
        Set<OptimizationData> toOptimizationData(final TwiceDifferentiable<Double> objective, final boolean maximize, final List<ConstraintTemplate> equalities,
                final List<ConstraintTemplate> ranges, final boolean negative) {

            Set<OptimizationData> retVal = new HashSet<>();

            retVal.add(new LinearObjectiveFunction(objective.getLinearFactors(false).toRawCopy1D(), ZERO));
            retVal.add(maximize ? GoalType.MAXIMIZE : GoalType.MINIMIZE);

            List<LinearConstraint> constraints = new ArrayList<>();

            for (ConstraintTemplate equality : equalities) {
                constraints.add(new LinearConstraint(equality.body, Relationship.EQ, equality.upper));
            }

            for (ConstraintTemplate range : ranges) {
                if (range.isLower()) {
                    constraints.add(new LinearConstraint(range.body, Relationship.GEQ, range.lower));
                }
                if (range.isUpper()) {
                    constraints.add(new LinearConstraint(range.body, Relationship.LEQ, range.upper));
                }
            }

            retVal.add(new LinearConstraintSet(constraints));

            retVal.add(new NonNegativeConstraint(!negative));

            return retVal;
        }

    }

    LinearImpl(final Set<OptimizationData> modelData, final Options options, final Configurator<S> configurator) {
        super(modelData, options, configurator);
    }

    @Override
    Access1D<?> extractSolution(final PointValuePair solutionAndValue) {
        return Primitive1D.of(solutionAndValue.getPoint());
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
    double extractValue(final PointValuePair solutionAndValue) {
        return solutionAndValue.getValue();
    }

}
