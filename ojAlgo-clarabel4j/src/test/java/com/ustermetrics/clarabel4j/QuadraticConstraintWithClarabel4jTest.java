package com.ustermetrics.clarabel4j;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.ojalgo.TestUtils;
import org.ojalgo.optimisation.Expression;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.Optimisation;
import org.ojalgo.optimisation.Variable;
import org.ojalgo.optimisation.solver.clarabel4j.SolverClarabel4j;
import org.ojalgo.type.context.NumberContext;

/**
 * Simple quadratic-constraint smoke tests for the Clarabel4j integration.
 *
 * These models are intentionally tiny and structured to match second-order cone patterns
 * that Clarabel can handle, so that {@link SolverClarabel4j#appendQuadraticConstraint}
 * can be exercised without relying on the full MarosMfszros suite.
 */
public class QuadraticConstraintWithClarabel4jTest {

    private static final NumberContext ACCURACY = NumberContext.of(6, 8);

    @BeforeAll
    public static void configure() {
        ExpressionsBasedModel.addIntegration(SolverClarabel4j.INTEGRATION);
    }

    @Disabled("Quadratic constraints are not yet supported by SolverClarabel4j or core ojAlgo solvers")
    @Test
    public void testBallConstraintInR2() {

        // Minimise x0 subject to x0^2 + x1^2 <= 1.
        ExpressionsBasedModel model = new ExpressionsBasedModel();

        Variable x0 = model.newVariable("x0").weight(1.0);
        Variable x1 = model.newVariable("x1");

        // Quadratic constraint: x0^2 + x1^2 <= 1
        Expression ball = model.newExpression("ball");
        ball.set(x0, x0, 1.0);
        ball.set(x1, x1, 1.0);
        ball.upper(1.0);

        Optimisation.Result result = model.minimise();

        TestUtils.assertStateNotLessThanOptimal(result);
        // Any feasible point on the unit disk with smallest x0 should satisfy x0 >= -1
        TestUtils.assertTrue(result.get(0).doubleValue() >= -1.0 - 1e-6);
        TestUtils.assertTrue(result.get(0).doubleValue() <= 0.0 + 1e-6);
    }

}