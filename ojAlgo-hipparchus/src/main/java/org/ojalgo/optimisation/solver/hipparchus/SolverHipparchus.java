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

import static org.ojalgo.function.constant.PrimitiveMath.ZERO;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.hipparchus.exception.Localizable;
import org.hipparchus.exception.MathIllegalStateException;
import org.hipparchus.optim.LocalizedOptimFormats;
import org.hipparchus.optim.OptimizationData;
import org.hipparchus.optim.PointValuePair;
import org.hipparchus.optim.linear.LinearConstraint;
import org.hipparchus.optim.linear.LinearConstraintSet;
import org.hipparchus.optim.linear.LinearObjectiveFunction;
import org.hipparchus.optim.linear.NonNegativeConstraint;
import org.hipparchus.optim.linear.Relationship;
import org.hipparchus.optim.linear.SimplexSolver;
import org.hipparchus.optim.nonlinear.scalar.GoalType;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.Optimisation;
import org.ojalgo.optimisation.Variable;
import org.ojalgo.structure.Access1D;
import org.ojalgo.structure.Structure1D.IntIndex;

/**
 * Use Hipparchus' {@link SimplexSolver} from ojAlgo's {@link ExpressionsBasedModel}.
 * <P>
 * Just call {@link ExpressionsBasedModel#addIntegration(ExpressionsBasedModel.Integration)} with
 * {@link SolverHipparchus#INTEGRATION} as the argument.
 *
 * @author apete
 */
public final class SolverHipparchus implements Optimisation.Solver {

    @FunctionalInterface
    public interface Configurator {

        void configure(SimplexSolver solver, Optimisation.Options options);

    }

    public static final class Integration extends ExpressionsBasedModel.Integration<SolverHipparchus> {

        Integration() {
            super();
        }

        @Override
        public SolverHipparchus build(final ExpressionsBasedModel model) {

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

            return new SolverHipparchus(optimizationData, model.options);
        }

        @Override
        public boolean isCapable(final ExpressionsBasedModel model) {
            return !model.isAnyVariableInteger() && !model.isAnyExpressionQuadratic();
        }

    }

    public static final SolverHipparchus.Integration INTEGRATION = new Integration();

    static final Configurator DEFAULT = (solver, options) -> {
        // TODO Auto-generated method stub
    };

    private final Set<OptimizationData> myModelData;
    private final Optimisation.Options myOptions;

    SolverHipparchus(final Set<OptimizationData> modelData, final Optimisation.Options options) {
        super();
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

            SimplexSolver solver = new SimplexSolver();

            DEFAULT.configure(solver, myOptions);
            Optional<Configurator> optional = myOptions.getConfigurator(Configurator.class);
            if (optional.isPresent()) {
                optional.get().configure(solver, myOptions);
            }

            PointValuePair solutionAndValue = solver.optimize(myModelData.toArray(new OptimizationData[myModelData.size()]));

            state = Optimisation.State.OPTIMAL;
            value = solutionAndValue.getValue().doubleValue();
            solution = Access1D.wrap(solutionAndValue.getPoint());

        } catch (MathIllegalStateException cause) {

            Localizable specifier = cause.getSpecifier();

            if (LocalizedOptimFormats.NO_FEASIBLE_SOLUTION.equals(specifier)) {
                state = Optimisation.State.INFEASIBLE;
            } else if (LocalizedOptimFormats.UNBOUNDED_SOLUTION.equals(specifier)) {
                state = Optimisation.State.UNBOUNDED;
            } else {
                state = Optimisation.State.FAILED;
            }
        }

        return new Optimisation.Result(state, value, solution);
    }

}
