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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.hipparchus.exception.Localizable;
import org.hipparchus.exception.MathIllegalStateException;
import org.hipparchus.optim.BaseMultivariateOptimizer;
import org.hipparchus.optim.OptimizationData;
import org.ojalgo.function.multiary.MultiaryFunction.TwiceDifferentiable;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.Optimisation;
import org.ojalgo.optimisation.Variable;
import org.ojalgo.structure.Access1D;
import org.ojalgo.structure.Structure1D.IntIndex;

abstract class AbstractImpl<S extends BaseMultivariateOptimizer<R>, R> implements Optimisation.Solver {

    public static abstract class Configurator<S extends BaseMultivariateOptimizer<?>> {

        abstract S newDelegateSolver(Optimisation.Options options);

    }

    static final class ConstraintTemplate {

        final double[] body;
        double lower = Double.NEGATIVE_INFINITY;
        double upper = Double.POSITIVE_INFINITY;

        ConstraintTemplate(final int dim) {
            super();
            body = new double[dim];
        }

        @Override
        public String toString() {
            return lower + " < " + Arrays.toString(body) + " < " + upper;
        }

        boolean isLower() {
            return Double.isFinite(lower);
        }

        boolean isUpper() {
            return Double.isFinite(upper);
        }

    }

    static abstract class Integration<S extends Optimisation.Solver> extends ExpressionsBasedModel.Integration<S> {

        static final boolean ADJUSTED = true;

        @Override
        public final S build(final ExpressionsBasedModel model) {

            List<ConstraintTemplate> equalities = new ArrayList<>();
            List<ConstraintTemplate> ranges = new ArrayList<>();

            List<Variable> variables = model.getVariables();
            int nbVariables = variables.size();

            TwiceDifferentiable<Double> objective = model.objective().toFunction();

            model.constraints().forEach(expr -> {

                ConstraintTemplate template = new ConstraintTemplate(nbVariables);
                double[] coeffs = template.body;

                for (IntIndex key : expr.getLinearKeySet()) {
                    coeffs[key.index] = expr.doubleValue(key, ADJUSTED);
                }

                template.lower = expr.getLowerLimit(ADJUSTED, Double.NEGATIVE_INFINITY);
                template.upper = expr.getUpperLimit(ADJUSTED, Double.POSITIVE_INFINITY);

                if (expr.isEqualityConstraint()) {
                    equalities.add(template);
                } else {
                    ranges.add(template);
                }
            });

            boolean anyVariableNegative = false;
            Variable tmpVariable;
            for (int i = 0; i < nbVariables; i++) {
                tmpVariable = variables.get(i);

                anyVariableNegative |= tmpVariable.isNegative();

                if (tmpVariable.isConstraint()) {

                    ConstraintTemplate template = new ConstraintTemplate(nbVariables);
                    double[] coeffs = template.body;

                    coeffs[i] = ADJUSTED ? tmpVariable.getAdjustmentFactor() : 1.0;

                    template.lower = tmpVariable.getLowerLimit(ADJUSTED, Double.NEGATIVE_INFINITY);
                    template.upper = tmpVariable.getUpperLimit(ADJUSTED, Double.POSITIVE_INFINITY);

                    if (tmpVariable.isEqualityConstraint()) {
                        equalities.add(template);
                    } else {
                        ranges.add(template);
                    }
                }
            }

            Set<OptimizationData> optimisationdata = this.toOptimizationData(objective, model.getOptimisationSense() == Optimisation.Sense.MAX, equalities,
                    ranges, anyVariableNegative);

            return this.newSolver(optimisationdata, model.options);
        }

        abstract S newSolver(Set<OptimizationData> optimisationdata, Optimisation.Options options);

        abstract Set<OptimizationData> toOptimizationData(TwiceDifferentiable<Double> objective, boolean maximize, List<ConstraintTemplate> equalities,
                List<ConstraintTemplate> ranges, boolean negative);

    }

    private final Configurator<S> myConfigurator;
    private final Set<OptimizationData> myModelData;
    private final Optimisation.Options myOptions;

    AbstractImpl(final Set<OptimizationData> modelData, final Optimisation.Options options, final Configurator<S> configurator) {
        super();
        myModelData = modelData;
        myOptions = options;
        myConfigurator = configurator;
    }

    @Override
    public final Optimisation.Result solve(final Result kickStarter) {

        Optimisation.State state = Optimisation.State.UNEXPLORED;
        double value = Double.NaN;
        Access1D<?> solution = kickStarter;

        try {

            S solver = myConfigurator.newDelegateSolver(myOptions);

            R solutionAndValue = solver.optimize(myModelData.toArray(new OptimizationData[myModelData.size()]));

            state = Optimisation.State.OPTIMAL;
            value = this.extractValue(solutionAndValue);
            solution = this.extractSolution(solutionAndValue);

        } catch (MathIllegalStateException cause) {

            state = this.extractState(cause.getSpecifier());
        }

        return new Optimisation.Result(state, value, solution);
    }

    abstract Access1D<?> extractSolution(R solutionAndValue);

    abstract Optimisation.State extractState(Localizable exceptionSpecifier);

    abstract double extractValue(R solutionAndValue);

}
