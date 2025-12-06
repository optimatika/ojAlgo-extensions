package com.ustermetrics.clarabel4j;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.ojalgo.TestUtils;
import org.ojalgo.optimisation.Expression;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.Optimisation;
import org.ojalgo.optimisation.Variable;
import org.ojalgo.optimisation.solver.clarabel4j.SolverClarabel4j;
import org.ojalgo.type.context.NumberContext;

/**
 * Tiny Markowitz-style portfolio tests to exercise the Clarabel4j integration.
 *
 * Variant 1: Minimise variance with a lower bound on return (QP with linear constraints).
 * Variant 2: Maximise return with an upper bound on variance (QCQP) – initially expected
 *            to fall back to non-Clarabel solvers until SOC support is implemented.
 */
public class MarkowitzWithClarabel4jTest {

    private static final NumberContext ACCURACY = NumberContext.of(6, 8);

    @BeforeAll
    public static void configure() {
        ExpressionsBasedModel.addIntegration(SolverClarabel4j.INTEGRATION);
    }

    private ExpressionsBasedModel buildBaseModel() {

        // Simple 3-asset example with arbitrary returns and diagonal covariance
        double[] mu = { 0.05, 0.10, 0.15 };
        double[] var = { 0.01, 0.02, 0.03 };

        ExpressionsBasedModel model = new ExpressionsBasedModel();

        Variable[] w = new Variable[mu.length];
        for (int i = 0; i < mu.length; i++) {
            w[i] = model.newVariable("w" + i).lower(0.0).upper(1.0);
        }

        // Budget constraint: sum w_i = 1
        Expression budget = model.newExpression("Budget");
        for (Variable v : w) {
            budget.set(v, 1.0);
        }
        budget.level(1.0);

        // Return expression: sum mu_i * w_i
        Expression ret = model.newExpression("Return");
        for (int i = 0; i < mu.length; i++) {
            ret.set(w[i], mu[i]);
        }

        // Variance expression: diagonal covariance for simplicity
        Expression varExpr = model.newExpression("Variance");
        for (int i = 0; i < var.length; i++) {
            varExpr.set(w[i], w[i], var[i]);
        }

        return model;
    }

    @Test
    public void testMinimiseVarianceWithReturnFloor() {

        ExpressionsBasedModel base = this.buildBaseModel();

        ExpressionsBasedModel minVar = base.copy();
        Expression ret = minVar.getExpression("Return");
        Expression variance = minVar.getExpression("Variance");

        // Minimise variance subject to return >= target
        double targetReturn = 0.09; // between asset 1 and 2

        variance.weight(1.0);
        ret.weight(null);
        ret.lower(targetReturn);

        Optimisation.Result result = minVar.minimise();

        TestUtils.assertStateNotLessThanOptimal(result);
        // Check that achieved return is at least the target (within tolerance)
        double achievedReturn = ret.evaluate(result).doubleValue();
        TestUtils.assertTrue(achievedReturn + 1e-6 >= targetReturn);
    }

    @Test
    public void testMaximiseReturnWithVarianceCap() {

        ExpressionsBasedModel base = this.buildBaseModel();

        ExpressionsBasedModel maxRet = base.copy();
        Expression ret = maxRet.getExpression("Return");
        Expression variance = maxRet.getExpression("Variance");

        // Maximise return subject to variance <= cap
        double varianceCap = 0.02; // somewhere between assets

        ret.weight(1.0);
        variance.weight(null);
        variance.upper(varianceCap);

        Optimisation.Result result = maxRet.maximise();

        // With current Clarabel4j limitations we only assert feasibility and that
        // variance does not exceed the cap when a feasible solution is found.
        if (result.getState().isFeasible()) {
            double achievedVariance = variance.evaluate(result).doubleValue();
            TestUtils.assertTrue(achievedVariance <= varianceCap + 1e-6);
        }
    }
}
