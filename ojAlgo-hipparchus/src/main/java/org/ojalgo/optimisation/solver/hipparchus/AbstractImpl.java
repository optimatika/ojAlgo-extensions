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

import static org.ojalgo.function.constant.PrimitiveMath.ONE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hipparchus.exception.Localizable;
import org.hipparchus.exception.MathIllegalStateException;
import org.hipparchus.optim.BaseMultivariateOptimizer;
import org.hipparchus.optim.MaxIter;
import org.hipparchus.optim.OptimizationData;
import org.ojalgo.optimisation.ConstraintsMetaData;
import org.ojalgo.optimisation.Expression;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.ModelEntity;
import org.ojalgo.optimisation.Optimisation;
import org.ojalgo.optimisation.Variable;
import org.ojalgo.structure.Access1D;
import org.ojalgo.structure.Structure1D.IntIndex;
import org.ojalgo.structure.Structure2D.IntRowColumn;

abstract class AbstractImpl<S extends BaseMultivariateOptimizer<R>, R> implements Optimisation.Solver {

    public static abstract class Configurator<S extends BaseMultivariateOptimizer<?>> {

        abstract S newDelegateSolver(Optimisation.Options options);

    }

    static final class ConstraintTemplate {

        final double[] body;
        /**
         * For a variable's bound: its coefficient in the body, else 0.
         */
        final double boundCoefficient;
        /**
         * Whether the body and limits are the entity's adjusted (scaled) parameters.
         */
        final boolean adjusted;
        /**
         * The model entity - an expression, or a variable for its bounds.
         */
        final ModelEntity<?> entity;
        double lower = Double.NEGATIVE_INFINITY;
        double upper = Double.POSITIVE_INFINITY;
        /**
         * For a variable's bound: its index, else -1.
         */
        final int variable;

        ConstraintTemplate(final int dim, final ModelEntity<?> entity, final boolean adjusted, final int variable, final double boundCoefficient) {
            super();
            body = new double[dim];
            this.entity = entity;
            this.adjusted = adjusted;
            this.variable = variable;
            this.boundCoefficient = boundCoefficient;
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

        /**
         * Variable bounds are constraints, with coefficient 1 (or the variable's adjustment factor if
         * adjusted).
         */
        static final boolean ADJUSTED_BOUNDS = false;
        static final boolean ADJUSTED_CONSTRAINTS = true;
        static final boolean ADJUSTED_OBJECTIVE = false;

        static double[][] getHessian(final Expression objective, final int dim) {
            double[][] retVal = new double[dim][dim];
            for (IntRowColumn key : objective.getQuadraticKeySet()) {
                double value = objective.doubleValue(key, ADJUSTED_OBJECTIVE);
                retVal[key.row][key.column] += value;
                retVal[key.column][key.row] += value;
            }
            return retVal;
        }

        static double[] getLinearFactors(final Expression objective, final int dim) {
            double[] retVal = new double[dim];
            for (IntIndex key : objective.getLinearKeySet()) {
                retVal[key.index] = objective.doubleValue(key, ADJUSTED_OBJECTIVE);
            }
            return retVal;
        }

        @Override
        public final S build(final ExpressionsBasedModel model) {

            List<ConstraintTemplate> equalities = new ArrayList<>();
            List<ConstraintTemplate> ranges = new ArrayList<>();

            List<Variable> variables = model.getVariables();
            int nbVariables = variables.size();

            model.constraints().forEach(expr -> {

                ConstraintTemplate template = new ConstraintTemplate(nbVariables, expr, ADJUSTED_CONSTRAINTS, -1, 0.0);
                double[] coeffs = template.body;

                for (IntIndex key : expr.getLinearKeySet()) {
                    coeffs[key.index] = expr.doubleValue(key, ADJUSTED_CONSTRAINTS);
                }

                template.lower = expr.getLowerLimit(ADJUSTED_CONSTRAINTS, Double.NEGATIVE_INFINITY);
                template.upper = expr.getUpperLimit(ADJUSTED_CONSTRAINTS, Double.POSITIVE_INFINITY);

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

                    double coefficient = ADJUSTED_BOUNDS ? tmpVariable.getAdjustmentFactor() : ONE;

                    ConstraintTemplate template = new ConstraintTemplate(nbVariables, tmpVariable, ADJUSTED_BOUNDS, i, coefficient);
                    template.body[i] = coefficient;

                    template.lower = tmpVariable.getLowerLimit(ADJUSTED_BOUNDS, Double.NEGATIVE_INFINITY);
                    template.upper = tmpVariable.getUpperLimit(ADJUSTED_BOUNDS, Double.POSITIVE_INFINITY);

                    if (tmpVariable.isEqualityConstraint()) {
                        equalities.add(template);
                    } else {
                        ranges.add(template);
                    }
                }
            }

            Expression objective = model.objective();

            Translation translation = new Translation(ADJUSTED_OBJECTIVE ? objective.getAdjustmentFactor() : ONE,
                    ExpressionsBasedModel.Integration.getObjectiveConstant(model));

            this.translate(translation, objective, nbVariables, model.getOptimisationSense() == Optimisation.Sense.MAX, equalities, ranges,
                    anyVariableNegative);

            return this.newSolver(model, translation);
        }

        /**
         * The solvers minimise (a maximisation's objective is negated), and their multipliers are converted
         * to the signed rows convention, see {@link Translation#dualSigns}. Those are then converted to the
         * model's dual values with
         * {@link ExpressionsBasedModel.Integration#toModelDuals(Optimisation.Result, ExpressionsBasedModel, Optimisation.Sense)}.
         */
        @Override
        public Result toModelState(final Result solverState, final ExpressionsBasedModel model) {
            return ExpressionsBasedModel.Integration.toModelDuals(solverState, model, Optimisation.Sense.MIN);
        }

        @Override
        protected final Optimisation.Sense getSolverSense() {
            return null;
        }

        abstract S newSolver(ExpressionsBasedModel model, Translation translation);

        abstract void translate(Translation translation, Expression objective, int dim, boolean maximise, List<ConstraintTemplate> equalities,
                List<ConstraintTemplate> ranges, boolean negative);

    }

    /**
     * The data handed to the Hipparchus solver, and what is needed to translate its solution back to model
     * terms.
     */
    static final class Translation {

        /**
         * For each of the solver's multipliers: the index of the variable whose bound it is (else -1), and
         * that variable's coefficient.
         */
        double[] boundCoefficients = null;
        int[] boundVariables = null;
        /**
         * Only set for solvers that provide multipliers – matches them, converted by {@link #dualSigns}, to
         * the model entities.
         */
        ConstraintsMetaData constraints = null;
        final Set<OptimizationData> data = new HashSet<>();
        /**
         * For each of the solver's multipliers, the factor (1 or -1) that converts it to the signed
         * convention of {@link ConstraintsMetaData#setSignedRows(boolean)}.
         */
        double[] dualSigns = null;
        /**
         * The objective was negated, to minimise a maximisation problem.
         */
        boolean negated = false;
        /**
         * The objective constant, in model units and the model's sense.
         */
        final double objectiveConstant;
        /**
         * The objective's adjustment factor – the objective the solver sees is scaled by this.
         */
        final double objectiveScale;

        Translation(final double objectiveScale, final double objectiveConstant) {
            super();
            this.objectiveScale = objectiveScale;
            this.objectiveConstant = objectiveConstant;
        }

    }

    private final Configurator<S> myConfigurator;
    private final Optimisation.Options myOptions;
    private final Translation myTranslation;

    AbstractImpl(final Translation translation, final Optimisation.Options options, final Configurator<S> configurator) {

        super();

        myTranslation = translation;
        myOptions = options;
        myConfigurator = configurator;
    }

    @Override
    public final Optimisation.Result solve(final Result kickStarter) {

        try {

            S solver = myConfigurator.newDelegateSolver(myOptions);

            List<OptimizationData> data = new ArrayList<>(myTranslation.data);
            int iterationLimit = this.getIterationLimit(myOptions);
            if (iterationLimit < Integer.MAX_VALUE) {
                data.add(new MaxIter(iterationLimit));
            }
            R solutionAndValue = solver.optimize(data.toArray(new OptimizationData[data.size()]));

            double value = this.extractValue(solutionAndValue) / myTranslation.objectiveScale;
            if (myTranslation.negated) {
                value = -value;
            }
            value += myTranslation.objectiveConstant;

            Optimisation.Result result = new Optimisation.Result(Optimisation.State.OPTIMAL, value, this.extractSolution(solutionAndValue));

            return this.complete(result, solutionAndValue, myTranslation);

        } catch (MathIllegalStateException cause) {

            return new Optimisation.Result(this.extractState(cause.getSpecifier()), Double.NaN, kickStarter);

        } catch (RuntimeException cause) {

            // Hipparchus' optimisers can fail in other ways too (the SQP optimisers throw NullPointerException
            // or MathIllegalArgumentException on some problems)
            return new Optimisation.Result(Optimisation.State.FAILED, Double.NaN, kickStarter);
        }
    }

    /**
     * Add whatever else (dual values...) the solver provides to the result.
     */
    abstract Optimisation.Result complete(Optimisation.Result result, R solutionAndValue, Translation translation);

    abstract Access1D<?> extractSolution(R solutionAndValue);

    /**
     * The iteration limit passed to the Hipparchus optimiser, or {@link Integer#MAX_VALUE} to pass none and
     * leave it to the optimiser.
     */
    int getIterationLimit(final Optimisation.Options options) {
        return Integer.MAX_VALUE;
    }

    abstract Optimisation.State extractState(Localizable exceptionSpecifier);

    abstract double extractValue(R solutionAndValue);

}
