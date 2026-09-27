package org.ojalgo.optimisation.solver.gurobi;

import org.junit.jupiter.api.Test;
import org.ojalgo.TestUtils;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.Optimisation;
import org.ojalgo.optimisation.Variable;
import org.ojalgo.type.context.NumberContext;

/**
 * Asked for dual values with quadratic constraints (QCPDual), which the integration does for continuous models,
 * Gurobi refuses to solve a non-convex model. Without them it solves it to global optimality.
 */
public class NonConvexQCQPWithGurobi {

    private static final NumberContext ACCURACY = NumberContext.of(6);

    /**
     * Minimise x + y outside a ball, x² + y² >= 1, with 0 <= x, y <= 2. The optimum is 1, at (1, 0) or (0, 1).
     */
    @Test
    public void testOutsideBall() {

        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable x = model.addVariable("x").lower(0.0).upper(2.0).weight(1.0);
        Variable y = model.addVariable("y").lower(0.0).upper(2.0).weight(1.0);
        model.addExpression("outside").set(x, x, 1.0).set(y, y, 1.0).lower(1.0);

        Optimisation.Result result = model.minimise(SolverGurobi.INTEGRATION);

        TestUtils.assertEquals(Optimisation.State.OPTIMAL, result.getState());
        TestUtils.assertEquals(1.0, result.getValue(), ACCURACY);
        TestUtils.assertTrue(model.validate(result, ACCURACY));
    }

}
