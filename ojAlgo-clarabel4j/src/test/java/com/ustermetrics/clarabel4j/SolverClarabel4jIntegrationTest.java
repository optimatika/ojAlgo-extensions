package com.ustermetrics.clarabel4j;

import static org.ojalgo.function.constant.PrimitiveMath.ONE;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ojalgo.optimisation.Expression;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.Optimisation;
import org.ojalgo.optimisation.Variable;
import org.ojalgo.optimisation.solver.clarabel4j.SolverClarabel4j;
import org.ojalgo.type.context.NumberContext;

class SolverClarabel4jIntegrationTest {

    private static final NumberContext ACCURACY = NumberContext.of(7, 10);

    @BeforeEach
    void addIntegration() {
        ExpressionsBasedModel.addIntegration(SolverClarabel4j.INTEGRATION);
    }

    @AfterEach
    void removeIntegration() {
        ExpressionsBasedModel.removeIntegration(SolverClarabel4j.INTEGRATION);
    }

    @Test
    void mixedEqualitiesAndBounds() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();

        Variable x = model.addVariable("x").lower(0).upper(2);
        Variable y = model.addVariable("y").lower(-1).upper(1);

        model.addExpression("eq1").set(x, 1).set(y, -1).level(0);
        model.addExpression("ineq1").set(x, 3).set(y, 2).upper(4);

        model.objective().set(x, 1).set(y, 2);

        Optimisation.Result result = model.minimise();

        if (!result.getState().isFeasible()) {
            throw new AssertionError("Expected feasible solution but state was " + result.getState());
        }

        double equality = result.doubleValue(0) - result.doubleValue(1);
        if (!ACCURACY.isZero(equality)) {
            throw new AssertionError("Equality constraint violated");
        }
    }

    @Test
    void addRemoveIntegrationMultipleTimes() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable x = model.addVariable("x").lower(0).upper(ONE);
        model.objective().set(x, -1);

        for (int iteration = 0; iteration < 3; iteration++) {
            ExpressionsBasedModel.addIntegration(SolverClarabel4j.INTEGRATION);
            Optimisation.Result result = model.maximise();
            if (!result.getState().isFeasible()) {
                throw new AssertionError("Feasible model became infeasible after add/remove cycles");
            }
            ExpressionsBasedModel.removeIntegration(SolverClarabel4j.INTEGRATION);
        }
    }

    @Test
    void capabilityAcceptsQuadraticObjectives() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable x = model.addVariable("x");
        Variable y = model.addVariable("y");

        model.objective().set(x, y, 1.0);

        if (!SolverClarabel4j.INTEGRATION.isCapable(model)) {
            throw new AssertionError("Clarabel4j integration should accept quadratic objectives");
        }
    }

    @Test
    void markowitzVarianceConstraintEquivalence() {

        // Two-asset Markowitz example where analytical frontier is easy to check
        // Asset 1: mu1 = 0.08, sigma1^2 = 0.04
        // Asset 2: mu2 = 0.12, sigma2^2 = 0.09
        // Covariance = 0.018
        double[] mu = { 0.08, 0.12 };
        double[][] sigma = {
                { 0.04, 0.018 },
                { 0.018, 0.09 }
        };

        // Target return strictly between the two assets
        double targetReturn = 0.10;

        // Model A: minimise variance s.t. sum w = 1, mu^T w = targetReturn (equality)
        ExpressionsBasedModel modelMinVar = new ExpressionsBasedModel();
        Variable wA0 = modelMinVar.addVariable("wA0");
        Variable wA1 = modelMinVar.addVariable("wA1");

        // Budget constraint: w0 + w1 = 1
        modelMinVar.addExpression("budget").set(wA0, 1).set(wA1, 1).level(1);
        // Return constraint: mu^T w = targetReturn
        modelMinVar.addExpression("return_level").set(wA0, mu[0]).set(wA1, mu[1]).level(targetReturn);

        // Objective: variance = w^T Sigma w
        modelMinVar.objective().set(wA0, wA0, sigma[0][0]);
        modelMinVar.objective().set(wA0, wA1, sigma[0][1]);
        modelMinVar.objective().set(wA1, wA0, sigma[1][0]);
        modelMinVar.objective().set(wA1, wA1, sigma[1][1]);

        Optimisation.Result resMinVar = modelMinVar.minimise();
        if (!resMinVar.getState().isFeasible()) {
            throw new AssertionError("Min-variance Markowitz model not feasible: " + resMinVar.getState());
        }

        double[] wMin = { resMinVar.doubleValue(0), resMinVar.doubleValue(1) };

        double varAchieved = 0.0;
        double retAchieved = 0.0;
        for (int i = 0; i < 2; i++) {
            retAchieved += mu[i] * wMin[i];
            for (int j = 0; j < 2; j++) {
                varAchieved += wMin[i] * sigma[i][j] * wMin[j];
            }
        }

        // Model B: maximise return s.t. sum w = 1, variance <= varAchieved
        ExpressionsBasedModel modelMaxRet = new ExpressionsBasedModel();
        Variable wB0 = modelMaxRet.addVariable("wB0");
        Variable wB1 = modelMaxRet.addVariable("wB1");

        modelMaxRet.addExpression("budget").set(wB0, 1).set(wB1, 1).level(1);

        Expression varExpr = modelMaxRet.addExpression("variance_cap");
        varExpr.set(wB0, wB0, sigma[0][0]);
        varExpr.set(wB0, wB1, sigma[0][1]);
        varExpr.set(wB1, wB0, sigma[1][0]);
        varExpr.set(wB1, wB1, sigma[1][1]);
        varExpr.upper(varAchieved);

        // Objective: maximise mu^T w
        modelMaxRet.objective().set(wB0, mu[0]).set(wB1, mu[1]);

        Optimisation.Result resMaxRet = modelMaxRet.maximise();
        if (!resMaxRet.getState().isFeasible()) {
            throw new AssertionError("Max-return Markowitz model not feasible: " + resMaxRet.getState());
        }

        double[] wMax = { resMaxRet.doubleValue(0), resMaxRet.doubleValue(1) };

        double varMax = 0.0;
        double retMax = 0.0;
        for (int i = 0; i < 2; i++) {
            retMax += mu[i] * wMax[i];
            for (int j = 0; j < 2; j++) {
                varMax += wMax[i] * sigma[i][j] * wMax[j];
            }
        }

        if (!ACCURACY.isZero(varAchieved - varMax)) {
            throw new AssertionError("Variance differs between formulations: " + varAchieved + " vs " + varMax);
        }

        if (!ACCURACY.isZero(retAchieved - retMax)) {
            throw new AssertionError("Return differs between formulations: " + retAchieved + " vs " + retMax);
        }
    }
}
