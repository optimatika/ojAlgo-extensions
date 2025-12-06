package com.ustermetrics.clarabel4j;

import org.junit.jupiter.api.Test;
import org.ojalgo.TestUtils;
import org.ojalgo.array.ArrayR064;
import org.ojalgo.optimisation.Expression;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.Optimisation;
import org.ojalgo.optimisation.Variable;
import org.ojalgo.optimisation.convex.ConvexSolver;
import org.ojalgo.optimisation.solver.clarabel4j.SolverClarabel4j;

public class SmallQPComparisonTest {

    private static ExpressionsBasedModel buildSimpleQP() {

        // min x^2 + 2 y^2 + x + 2y
        // s.t. x + y >= 1, x >= 0, y >= 0

        ExpressionsBasedModel model = new ExpressionsBasedModel();

        Variable x = model.addVariable("x");
        x.lower(0.0);

        Variable y = model.addVariable("y");
        y.lower(0.0);

        Expression obj = model.addExpression("obj").weight(1.0);
        obj.set(0, 1.0);
        obj.set(1, 2.0);
        obj.set(0, 0, 1.0);
        obj.set(1, 1, 2.0);

        model.addExpression("c1").set(0, 1.0).set(1, 1.0).lower(1.0);

        return model;
    }

    @Test
    public void testSimpleQPConvexVsClarabel() {

        ExpressionsBasedModel model = buildSimpleQP();

        ConvexSolver convexSolver = ConvexSolver.INTEGRATION.build(model);
        Optimisation.Result convexResult = convexSolver.solve();

        SolverClarabel4j clarabelSolver = SolverClarabel4j.INTEGRATION.build(model);
        Optimisation.Result clarabelResult = clarabelSolver.solve();

        TestUtils.assertEquals(convexResult.getValue(), clarabelResult.getValue(), 1e-6);

        int size = (int) convexResult.count();
        ArrayR064 convexX = ArrayR064.make(size);
        ArrayR064 clarabelX = ArrayR064.make(size);
        for (int i = 0; i < size; i++) {
            convexX.set(i, convexResult.doubleValue(i));
            clarabelX.set(i, clarabelResult.doubleValue(i));
        }

        TestUtils.assertEquals(convexX.doubleValue(0), clarabelX.doubleValue(0), 1e-6);
        TestUtils.assertEquals(convexX.doubleValue(1), clarabelX.doubleValue(1), 1e-6);
    }
}