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
package org.ojalgo.optimisation.solver.acm;

import static org.ojalgo.function.constant.PrimitiveMath.ZERO;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.math3.optim.OptimizationData;
import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.linear.LinearConstraint;
import org.apache.commons.math3.optim.linear.LinearConstraintSet;
import org.apache.commons.math3.optim.linear.LinearObjectiveFunction;
import org.apache.commons.math3.optim.linear.NoFeasibleSolutionException;
import org.apache.commons.math3.optim.linear.NonNegativeConstraint;
import org.apache.commons.math3.optim.linear.Relationship;
import org.apache.commons.math3.optim.linear.SimplexSolver;
import org.apache.commons.math3.optim.linear.UnboundedSolutionException;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.Optimisation;
import org.ojalgo.optimisation.Variable;
import org.ojalgo.structure.Access1D;
import org.ojalgo.structure.Structure1D.IntIndex;

/**
 * Use Apache's {@link SimplexSolver} from ojAlgo's {@link ExpressionsBasedModel}.
 * <P>
 * Just call {@link ExpressionsBasedModel#addIntegration(ExpressionsBasedModel.Integration)} with
 * {@link SolverACM#INTEGRATION} as the argument.
 *
 * @author apete
 */
public final class SolverACM implements Optimisation.Solver {

    public static class Configurator {

        protected SimplexSolver newSimplexSolver(final Optimisation.Options options) {
            return new SimplexSolver();
        }

    }

    static final class Integration extends ExpressionsBasedModel.Integration<SolverACM> {

        Integration() {
            super();
        }

        @Override
        public SolverACM build(final ExpressionsBasedModel model) {

            Configurator configurator = model.getConfigurator(DEFAULT);

            Set<OptimizationData> optimizationData = new HashSet<>();

            List<Variable> variables = model.getVariables();
            int nbVariables = variables.size();

            double[] weights = model.objective().toFunction().getLinearFactors(false).toRawCopy1D();
            if (weights.length != nbVariables) {
                throw new IllegalStateException();
            }
            optimizationData.add(new LinearObjectiveFunction(weights, ZERO));
            optimizationData.add(model.getOptimisationSense() == Optimisation.Sense.MAX ? GoalType.MAXIMIZE : GoalType.MINIMIZE);

            List<LinearConstraint> constraints = new ArrayList<>();

            model.constraints().forEach(expr -> {

                double[] coeffs = new double[nbVariables];

                for (IntIndex key : expr.getLinearKeySet()) {
                    coeffs[key.index] = expr.doubleValue(key, true);
                }

                if (expr.isEqualityConstraint()) {
                    constraints.add(new LinearConstraint(coeffs, Relationship.EQ, expr.getUpperLimit(true, Double.POSITIVE_INFINITY)));
                } else {
                    if (expr.isLowerConstraint()) {
                        constraints.add(new LinearConstraint(coeffs, Relationship.GEQ, expr.getLowerLimit(true, Double.NEGATIVE_INFINITY)));
                    }
                    if (expr.isUpperConstraint()) {
                        constraints.add(new LinearConstraint(coeffs, Relationship.LEQ, expr.getUpperLimit(true, Double.POSITIVE_INFINITY)));
                    }
                }
            });

            boolean anyVariableNegative = false;
            Variable tmpVariable;
            for (int i = 0; i < nbVariables; i++) {
                tmpVariable = variables.get(i);

                anyVariableNegative |= tmpVariable.isNegative();

                if (tmpVariable.isConstraint()) {

                    double[] coeffs = new double[nbVariables];
                    coeffs[i] = tmpVariable.getAdjustmentFactor();

                    if (tmpVariable.isEqualityConstraint()) {
                        constraints.add(new LinearConstraint(coeffs, Relationship.EQ, tmpVariable.getUpperLimit(true, Double.POSITIVE_INFINITY)));
                    } else {
                        if (tmpVariable.isLowerConstraint()) {
                            constraints.add(new LinearConstraint(coeffs, Relationship.GEQ, tmpVariable.getLowerLimit(true, Double.NEGATIVE_INFINITY)));
                        }
                        if (tmpVariable.isUpperConstraint()) {
                            constraints.add(new LinearConstraint(coeffs, Relationship.LEQ, tmpVariable.getUpperLimit(true, Double.POSITIVE_INFINITY)));
                        }
                    }
                }
            }
            optimizationData.add(new LinearConstraintSet(constraints));

            optimizationData.add(new NonNegativeConstraint(!anyVariableNegative));

            return new SolverACM(configurator, optimizationData, model.options);
        }

        @Override
        public boolean isCapable(final ExpressionsBasedModel model) {
            return !model.isAnyVariableInteger() && !model.isAnyExpressionQuadratic();
        }

        @Override
        protected Optimisation.Sense getSolverSense() {
            return null;
        }

    }

    public static final ExpressionsBasedModel.Integration<SolverACM> INTEGRATION = new Integration();

    static final Configurator DEFAULT = new Configurator();

    private final Configurator myConfigurator;
    private final Set<OptimizationData> myModelData;
    private final Optimisation.Options myOptions;

    SolverACM(final Configurator configurator, final Set<OptimizationData> modelData, final Optimisation.Options options) {

        super();

        myConfigurator = configurator;
        myModelData = modelData;
        myOptions = options;
    }

    @Override
    public Optimisation.Result solve(final Result kickStarter) {

        //          InitialGuess guess = new InitialGuess(kickStarter.toRawCopy1D());
        //
        //        myModelData.add(guess);

        Optimisation.State state = Optimisation.State.FAILED;
        double value = Double.NaN;
        Access1D<?> solution = kickStarter;

        try {

            SimplexSolver solver = myConfigurator.newSimplexSolver(myOptions);

            PointValuePair solutionAndValue = solver.optimize(myModelData.toArray(new OptimizationData[myModelData.size()]));

            state = Optimisation.State.OPTIMAL;
            value = solutionAndValue.getValue().doubleValue();
            solution = Access1D.wrap(solutionAndValue.getPoint());

        } catch (NoFeasibleSolutionException infeasible) {

            state = Optimisation.State.INFEASIBLE;

        } catch (UnboundedSolutionException unbounded) {

            state = Optimisation.State.UNBOUNDED;
        }

        return new Optimisation.Result(state, value, solution);
    }

}
