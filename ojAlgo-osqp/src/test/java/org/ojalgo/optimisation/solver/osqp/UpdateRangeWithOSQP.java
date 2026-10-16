package org.ojalgo.optimisation.solver.osqp;

import org.junit.jupiter.api.Test;
import org.ojalgo.TestUtils;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.ModelEntity;
import org.ojalgo.optimisation.Optimisation.ConstraintType;
import org.ojalgo.optimisation.Optimisation.Result;
import org.ojalgo.optimisation.Variable;
import org.ojalgo.type.context.NumberContext;
import org.ojalgo.type.keyvalue.EntryPair;
import org.ojalgo.type.keyvalue.EntryPair.KeyedPrimitive;

public class UpdateRangeWithOSQP {

    private static KeyedPrimitive<EntryPair<ModelEntity<?>, ConstraintType>> getDualValue(final Result result, final ModelEntity<?> entity) {
        for (KeyedPrimitive<EntryPair<ModelEntity<?>, ConstraintType>> dual : result.getDualValues()) {
            if (dual.getKey().left() == entity) {
                return dual;
            }
        }
        throw new IllegalArgumentException();
    }

    /**
     * A variable's bound row is registered with the type of its bounds, and the sign of its multiplier is
     * interpreted accordingly. When {@link SolverOSQP#updateRange(int, double, double)} changes that type the
     * reported dual values must follow – without changing those of a result already returned. A variable
     * without bounds has no bound row, and then the update is refused.
     * <p>
     * Minimising (x^2 + y^2 + z^2) / 2 - 800x - 800y subject to x + y <= 1500, x, y >= 0 and z free gives x =
     * y = 750. Adding the upper bound x <= 200 gives x = 200 with bound multiplier 600, and y = 800.
     */
    @Test
    public void testUpdateRangeChangesBoundType() {

        NumberContext accuracy = NumberContext.of(6);

        ExpressionsBasedModel model = new ExpressionsBasedModel();

        Variable x = model.addVariable("x").lower(0);
        Variable y = model.addVariable("y").lower(0);
        Variable z = model.addVariable("z");

        model.addExpression("objective").weight(1).set(x, x, 0.5).set(x, -800).set(y, y, 0.5).set(y, -800).set(z, z, 0.5);
        model.addExpression("constraint").set(x, 1).set(y, 1).upper(1500);

        SolverOSQP solver = SolverOSQP.INTEGRATION.build(model);

        Result initial = solver.solve();
        TestUtils.assertStateNotLessThanOptimal(initial);
        TestUtils.assertEquals(750.0, initial.doubleValue(0), accuracy);

        TestUtils.assertFalse(solver.updateRange(2, -1.0, 1.0));

        TestUtils.assertTrue(solver.updateRange(0, 0.0, 200.0));

        Result updated = solver.solve();
        TestUtils.assertStateNotLessThanOptimal(updated);
        TestUtils.assertEquals(200.0, updated.doubleValue(0), accuracy);
        TestUtils.assertEquals(800.0, updated.doubleValue(1), accuracy);

        KeyedPrimitive<EntryPair<ModelEntity<?>, ConstraintType>> bound = UpdateRangeWithOSQP.getDualValue(updated, x);
        TestUtils.assertEquals(ConstraintType.UPPER, bound.getKey().right());
        TestUtils.assertEquals(600.0, bound.doubleValue(), accuracy);

        TestUtils.assertEquals(ConstraintType.LOWER, UpdateRangeWithOSQP.getDualValue(initial, x).getKey().right());

        solver.dispose();
    }

}
