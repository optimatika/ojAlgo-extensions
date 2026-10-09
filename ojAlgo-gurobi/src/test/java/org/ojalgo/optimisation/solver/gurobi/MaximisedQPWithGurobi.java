package org.ojalgo.optimisation.solver.gurobi;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.ojalgo.TestUtils;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.Optimisation;
import org.ojalgo.optimisation.Variable;
import org.ojalgo.type.context.NumberContext;

@Disabled
public class MaximisedQPWithGurobi {

    private static final NumberContext ACCURACY = NumberContext.of(6);

    /**
     * max -x² + 4x, x in [0,10] => x = 2, objective = 4
     */
    @Test
    public void testMaximiseConcaveQuadratic() {

        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable x = model.addVariable("x").lower(0.0).upper(10.0);
        model.addExpression("obj").weight(1.0).set(x, x, -1.0).set(x, 4.0);

        Optimisation.Result result = model.maximise(SolverGurobi.INTEGRATION);

        TestUtils.assertEquals(Optimisation.State.OPTIMAL, result.getState());
        TestUtils.assertEquals(4.0, result.getValue(), ACCURACY);
        TestUtils.assertEquals(2.0, result.doubleValue(0), ACCURACY);
    }

    /**
     * A maximised quadratic with an integer variable: max -x² + 5x with x integer in [0,10] => x = 2 or 3,
     * objective = 6
     */
    @Test
    public void testMaximiseIntegerQuadratic() {

        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable x = model.addVariable("x").lower(0.0).upper(10.0).integer(true);
        model.addExpression("obj").weight(1.0).set(x, x, -1.0).set(x, 5.0);

        Optimisation.Result result = model.maximise(SolverGurobi.INTEGRATION);

        TestUtils.assertEquals(Optimisation.State.OPTIMAL, result.getState());
        TestUtils.assertEquals(6.0, result.getValue(), ACCURACY);
    }

    /**
     * Two variables, maximised: max -(x² + y²) + 4x + 6y => x = 2, y = 3, objective = 13
     */
    @Test
    public void testMaximiseTwoVariableQuadratic() {

        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable x = model.addVariable("x").lower(0.0).upper(10.0);
        Variable y = model.addVariable("y").lower(0.0).upper(10.0);
        model.addExpression("obj").weight(1.0).set(x, x, -1.0).set(y, y, -1.0).set(x, 4.0).set(y, 6.0);

        Optimisation.Result result = model.maximise(SolverGurobi.INTEGRATION);

        TestUtils.assertEquals(Optimisation.State.OPTIMAL, result.getState());
        TestUtils.assertEquals(13.0, result.getValue(), ACCURACY);
        TestUtils.assertEquals(2.0, result.doubleValue(0), ACCURACY);
        TestUtils.assertEquals(3.0, result.doubleValue(1), ACCURACY);
    }

    /**
     * The mirrored minimisation: min x² - 4x, x in [0,10] => x = 2, objective = -4
     */
    @Test
    public void testMinimiseConvexQuadratic() {

        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable x = model.addVariable("x").lower(0.0).upper(10.0);
        model.addExpression("obj").weight(1.0).set(x, x, 1.0).set(x, -4.0);

        Optimisation.Result result = model.minimise(SolverGurobi.INTEGRATION);

        TestUtils.assertEquals(Optimisation.State.OPTIMAL, result.getState());
        TestUtils.assertEquals(-4.0, result.getValue(), ACCURACY);
        TestUtils.assertEquals(2.0, result.doubleValue(0), ACCURACY);
    }

}
