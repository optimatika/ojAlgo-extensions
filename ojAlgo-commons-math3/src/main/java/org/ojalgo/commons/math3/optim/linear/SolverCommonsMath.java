/*
 * Copyright 1997-2020 Optimatika
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

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.math3.optim.OptimizationData;
import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.SimpleBounds;
import org.apache.commons.math3.optim.linear.LinearConstraint;
import org.apache.commons.math3.optim.linear.LinearConstraintSet;
import org.apache.commons.math3.optim.linear.LinearObjectiveFunction;
import org.apache.commons.math3.optim.linear.NoFeasibleSolutionException;
import org.apache.commons.math3.optim.linear.Relationship;
import org.apache.commons.math3.optim.linear.SimplexSolver;
import org.apache.commons.math3.optim.linear.UnboundedSolutionException;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.Optimisation;
import org.ojalgo.optimisation.Variable;
import org.ojalgo.structure.Access1D;
import org.ojalgo.structure.Structure1D.IntIndex;

public final class SolverCommonsMath implements Optimisation.Solver {

    @FunctionalInterface
    public interface Configurator {

        void configure(SimplexSolver solver, Optimisation.Options options);

    }

    static final class Integration extends ExpressionsBasedModel.Integration<SolverCommonsMath> {

        Integration() {
            super();
        }

        public SolverCommonsMath build(final ExpressionsBasedModel model) {

            final Set<OptimizationData> data = new HashSet<>();

            final Access1D<Double> linearFactors = model.objective().toFunction().getLinearFactors();

            final LinearObjectiveFunction obj = new LinearObjectiveFunction(linearFactors.toRawCopy1D(), 0.0);

            Variable tmpVariable;
            final List<Variable> variables = model.getVariables();
            model.getFixedVariables();

            final double[] lowerBounds = new double[variables.size()];
            final double[] upperBounds = new double[variables.size()];
            for (int v = 0; v < variables.size(); v++) {
                tmpVariable = variables.get(v);
                lowerBounds[v] = tmpVariable.getUnadjustedLowerLimit();
                upperBounds[v] = tmpVariable.getUnadjustedUpperLimit();
            }
            final SimpleBounds simpleBounds = new SimpleBounds(lowerBounds, upperBounds);

            final GoalType goalType = model.isMaximisation() ? GoalType.MAXIMIZE : GoalType.MINIMIZE;

            data.add(goalType);
            data.add(obj);
            data.add(new ObjectiveFunction(obj));
            data.add(simpleBounds);

            final Set<LinearConstraint> constraints = new HashSet<>();
            model.constraints().forEach(expr -> {

                final double[] coeffs = new double[variables.size()];

                final Set<IntIndex> keySet = expr.getLinearKeySet();
                for (final IntIndex tmpIntIndex : keySet) {
                    coeffs[tmpIntIndex.index] = expr.getAdjustedLinearFactor(tmpIntIndex);
                }

                if (expr.isEqualityConstraint()) {
                    constraints.add(new LinearConstraint(coeffs, Relationship.EQ, expr.getAdjustedUpperLimit()));
                } else {
                    if (expr.isLowerConstraint()) {
                        constraints.add(new LinearConstraint(coeffs, Relationship.GEQ, expr.getAdjustedLowerLimit()));
                    } else if (expr.isUpperConstraint()) {
                        constraints.add(new LinearConstraint(coeffs, Relationship.LEQ, expr.getAdjustedUpperLimit()));
                    }
                }
            });
            for (int i = 0; i < upperBounds.length; i++) {
                if (Double.isFinite(lowerBounds[i])) {
                    final double[] coeffs = new double[variables.size()];
                    coeffs[i] = 1.0;
                    constraints.add(new LinearConstraint(coeffs, Relationship.GEQ, lowerBounds[i]));
                }
                if (Double.isFinite(upperBounds[i])) {
                    final double[] coeffs = new double[variables.size()];
                    coeffs[i] = 1.0;
                    constraints.add(new LinearConstraint(coeffs, Relationship.LEQ, upperBounds[i]));
                }
            }

            data.add(new LinearConstraintSet(constraints));

            return new SolverCommonsMath(data, model.options);
        }

        public boolean isCapable(final ExpressionsBasedModel model) {
            return !model.isAnyVariableInteger() && !model.isAnyExpressionQuadratic();
        }

        @Override
        protected boolean isSolutionMapped() {
            return false;
        }

    }

    public static final SolverCommonsMath.Integration INTEGRATION = new Integration();

    static final Configurator DEFAULT = new Configurator() {

        public void configure(final SimplexSolver solver, final Options options) {
            // TODO Auto-generated method stub
        }

    };

    private final Set<OptimizationData> myModelData;

    private final Optimisation.Options myOptions;

    SolverCommonsMath(final Set<OptimizationData> modelData, final Optimisation.Options options) {
        super();
        myModelData = modelData;
        myOptions = options;
    }

    public Optimisation.Result solve(final Result kickStarter) {

        //        final InitialGuess guess = new InitialGuess(kickStarter.toRawCopy1D());
        //
        //        myModelData.add(guess);

        Optimisation.State state = Optimisation.State.FAILED;
        double value = Double.NaN;
        Access1D<?> solution = kickStarter;

        try {

            final SimplexSolver solver = new SimplexSolver();

            DEFAULT.configure(solver, myOptions);
            final Optional<Configurator> optional = myOptions.getConfigurator(Configurator.class);
            if (optional.isPresent()) {
                optional.get().configure(solver, myOptions);
            }

            final PointValuePair solutionAndValue = solver.optimize(myModelData.toArray(new OptimizationData[myModelData.size()]));

            state = Optimisation.State.OPTIMAL;
            value = solutionAndValue.getValue();
            solution = Access1D.wrap(solutionAndValue.getPoint());

        } catch (final NoFeasibleSolutionException infeasible) {

            state = Optimisation.State.INFEASIBLE;

        } catch (final UnboundedSolutionException unbounded) {

            state = Optimisation.State.UNBOUNDED;
        }

        final Optimisation.Result result = new Optimisation.Result(state, value, solution);

        return result;
    }

}
