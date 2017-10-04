/*
 * Copyright 1997-2017 Optimatika
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
package org.ojalgo.commons.math3.optim.linear;

import java.util.List;

import org.apache.commons.math3.optim.InitialGuess;
import org.apache.commons.math3.optim.OptimizationData;
import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.SimpleBounds;
import org.apache.commons.math3.optim.linear.LinearObjectiveFunction;
import org.apache.commons.math3.optim.linear.SimplexSolver;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.ojalgo.access.Access1D;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.Optimisation;
import org.ojalgo.optimisation.Variable;

final class CommonsMathSimplexSolver implements Optimisation.Solver {

    public static final ExpressionsBasedModel.Integration<CommonsMathSimplexSolver> INTEGRATION = new ExpressionsBasedModel.Integration<CommonsMathSimplexSolver>() {

        public CommonsMathSimplexSolver build(final ExpressionsBasedModel model) {

            Access1D<Double> linearFactors = model.objective().toFunction().getLinearFactors();

            LinearObjectiveFunction obj = new LinearObjectiveFunction(linearFactors.toRawCopy1D(), 0.0);

            Variable tmpVariable;
            List<Variable> variables = model.getVariables();

            double[] lowerBounds = new double[variables.size()];
            double[] upperBounds = new double[variables.size()];
            for (int v = 0; v < variables.size(); v++) {
                tmpVariable = variables.get(v);
                lowerBounds[v] = tmpVariable.getAdjustedLowerLimit();
                upperBounds[v] = variables.get(v).getAdjustedUpperLimit();
            }
            SimpleBounds simpleBounds = new SimpleBounds(lowerBounds, upperBounds);

            GoalType goalType = model.isMaximisation() ? GoalType.MAXIMIZE : GoalType.MINIMIZE;

            OptimizationData[] data = new OptimizationData[] { goalType, obj, simpleBounds };

            // TODO Auto-generated method stub
            return null;
        }

        public boolean isCapable(final ExpressionsBasedModel model) {
            return !model.isAnyVariableInteger() && !model.isAnyExpressionQuadratic();
        }

    };

    private final OptimizationData[] myModelData;
    private final Optimisation.Options myOptions;

    public Optimisation.Result solve(final Result kickStarter) {

        InitialGuess guess = new InitialGuess(kickStarter.toRawCopy1D());

        SimplexSolver solver = new SimplexSolver();

        PointValuePair solutionAndValue = solver.optimize(myModelData);

        Optimisation.State state = Optimisation.State.OPTIMAL;
        double value = solutionAndValue.getValue();
        Access1D<Double> solution = Access1D.wrap(solutionAndValue.getPoint());

        Optimisation.Result result = new Optimisation.Result(state, value, solution);

        return result;
    }

    CommonsMathSimplexSolver(OptimizationData[] modelData, Optimisation.Options options) {
        super();
        myModelData = modelData;
        myOptions = options;
    }

}
