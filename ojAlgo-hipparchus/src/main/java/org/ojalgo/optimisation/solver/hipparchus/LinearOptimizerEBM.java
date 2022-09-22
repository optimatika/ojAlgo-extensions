package org.ojalgo.optimisation.solver.hipparchus;

import java.math.BigDecimal;
import java.util.List;

import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.exception.MathIllegalArgumentException;
import org.hipparchus.linear.RealVector;
import org.hipparchus.optim.LocalizedOptimFormats;
import org.hipparchus.optim.PointValuePair;
import org.hipparchus.optim.linear.LinearConstraint;
import org.hipparchus.optim.linear.LinearObjectiveFunction;
import org.hipparchus.optim.linear.LinearOptimizer;
import org.hipparchus.optim.linear.Relationship;
import org.hipparchus.optim.nonlinear.scalar.GoalType;
import org.ojalgo.optimisation.Expression;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.ModelEntity;
import org.ojalgo.optimisation.Optimisation.Result;
import org.ojalgo.optimisation.Optimisation.State;
import org.ojalgo.optimisation.Variable;
import org.ojalgo.type.context.NumberContext;

/**
 * Intended to be a drop-in replacement to {@link org.apache.commons.math3.optim.linear.SimplexSolver} and/or
 * the deprecated {@link org.apache.commons.math3.optimization.linear.SimplexSolver}.
 * <p>
 * If you have any problems using those solvers, and would like to try ojAlgo instead. Just add
 * ojAlgo-commons-math3 as a dependency and use {@link LinearOptimizerEBM} instead of {@link SimplexSolver} –
 * different class name, but the same API.
 * <p>
 * Internally this implementation builds an {@link ExpressionsBasedModel} and thus makes use of its pre-solve
 * functionality and the ojAlgo suite of solvers. Code wise this is a bit of a detour. Should you find that
 * this works better for you, then consider building an {@link ExpressionsBasedModel} direcly, and skip any
 * usage of the Apache solver.
 *
 * @author apete
 */
public final class LinearOptimizerEBM extends LinearOptimizer {

    private static final double LIMIT_CUT_OFF = 1E8;
    private static final NumberContext PARAMETER_ACCURACY = NumberContext.of(12, 10);

    static BigDecimal convert(final double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException();
        }
        return PARAMETER_ACCURACY.toBigDecimal(value);
    }

    static boolean isZero(final double value) {
        return PARAMETER_ACCURACY.isZero(value);
    }

    static void limit(final ModelEntity<?> entity, final Relationship constrType, final double limit) {
        if (Math.abs(limit) < LIMIT_CUT_OFF) {
            BigDecimal convertedLimit = LinearOptimizerEBM.convert(limit);
            switch (constrType) {
            case GEQ:
                entity.lower(convertedLimit);
                break;
            case LEQ:
                entity.upper(convertedLimit);
                break;
            case EQ:
                entity.level(convertedLimit);
                break;
            default:
                break;
            }
        }
    }

    @Override
    protected PointValuePair doOptimize() {

        ExpressionsBasedModel model = new ExpressionsBasedModel();

        LinearObjectiveFunction objective = this.getFunction();
        RealVector objFuncCoeffs = objective.getCoefficients();
        int nbVars = objFuncCoeffs.getDimension();

        boolean restrictedToNonNegative = this.isRestrictedToNonNegative();

        double[] lowerBounds = this.getLowerBound();
        if (lowerBounds != null && lowerBounds.length != nbVars) {
            throw new MathIllegalArgumentException(LocalizedCoreFormats.DIMENSIONS_MISMATCH);
        }

        double[] upperBounds = this.getUpperBound();
        if (upperBounds != null && upperBounds.length != nbVars) {
            throw new MathIllegalArgumentException(LocalizedCoreFormats.DIMENSIONS_MISMATCH);
        }

        for (int i = 0; i < nbVars; i++) {

            BigDecimal weight = LinearOptimizerEBM.convert(objFuncCoeffs.getEntry(i));

            double lower = restrictedToNonNegative ? 0.0 : Double.NEGATIVE_INFINITY;
            if (lowerBounds != null) {
                lower = Math.max(lower, lowerBounds[i]);
            }

            double upper = Double.POSITIVE_INFINITY;
            if (upperBounds != null) {
                upper = Math.min(upper, upperBounds[i]);
            }

            Variable variable = model.addVariable();

            variable.weight(weight);

            LinearOptimizerEBM.limit(variable, Relationship.GEQ, lower);
            LinearOptimizerEBM.limit(variable, Relationship.LEQ, upper);
        }

        List<Variable> variables = model.getVariables();

        for (LinearConstraint constraint : this.getConstraints()) {

            RealVector coefficients = constraint.getCoefficients();
            Relationship constrType = constraint.getRelationship();
            double limit = constraint.getValue();

            if (coefficients.getDimension() != nbVars) {
                throw new MathIllegalArgumentException(LocalizedCoreFormats.DIMENSIONS_MISMATCH);
            }

            Expression expression = model.addExpression();

            for (int i = 0; i < nbVars; i++) {
                double coeff = coefficients.getEntry(i);
                if (!LinearOptimizerEBM.isZero(coeff)) {
                    BigDecimal linearFactor = LinearOptimizerEBM.convert(coeff);
                    expression.set(variables.get(i), linearFactor);
                }
            }

            LinearOptimizerEBM.limit(expression, constrType, limit);
        }

        Result result = this.getGoalType() == GoalType.MAXIMIZE ? model.maximise() : model.minimise();

        State state = result.getState();
        if (state == State.UNBOUNDED) {
            throw new MathIllegalArgumentException(LocalizedOptimFormats.UNBOUNDED_SOLUTION);
        } else if (!state.isFeasible()) {
            throw new MathIllegalArgumentException(LocalizedOptimFormats.NO_FEASIBLE_SOLUTION);
        }

        double[] point = result.toRawCopy1D();
        double value = objective.value(point);
        return new PointValuePair(point, value);
    }

}
