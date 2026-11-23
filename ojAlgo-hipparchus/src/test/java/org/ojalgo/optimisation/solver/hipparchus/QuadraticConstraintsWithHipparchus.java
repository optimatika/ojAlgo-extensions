package org.ojalgo.optimisation.solver.hipparchus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.ojalgo.optimisation.Expression;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.Optimisation;
import org.ojalgo.optimisation.Variable;

/**
 * Only {@link SQPOptimizerImplS2} handles quadratic constraints, and only convex ones. These cases cover what
 * is specific to this module, and so not in the shared {@link org.ojalgo.optimisation.convex.TestBasicQCQP}:
 * which integrations are capable, which quadratic constraints are rejected, and what an infeasible model
 * gives.
 */
public class QuadraticConstraintsWithHipparchus {

    private static void assertRejected(final ExpressionsBasedModel model) {
        assertFalse(SQPOptimizerImplS2.INTEGRATION.isCapable(model));
        assertFalse(SolverHipparchus.INTEGRATION.isCapable(model));
        assertThrows(IllegalArgumentException.class, () -> model.minimise(SQPOptimizerImplS2.INTEGRATION));
    }

    /**
     * min -x - y subject to x² + y² <= 1, 0 <= x, y <= 10
     */
    private static ExpressionsBasedModel makeModel() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        model.addVariable("x").lower(0.0).upper(10.0).weight(-1.0);
        model.addVariable("y").lower(0.0).upper(10.0).weight(-1.0);
        return model;
    }

    private static Expression makeQuadratic(final ExpressionsBasedModel model, final double xx, final double yy) {
        Variable x = model.getVariable(0);
        Variable y = model.getVariable(1);
        return model.addExpression("qc").set(x, x, xx).set(y, y, yy);
    }

    /**
     * Of the Hipparchus integrations only S2, and SolverHipparchus (that uses S2 for this), handle a
     * quadratic constraint.
     */
    @Test
    public void testCapable() {

        ExpressionsBasedModel model = QuadraticConstraintsWithHipparchus.makeModel();
        QuadraticConstraintsWithHipparchus.makeQuadratic(model, 1.0, 1.0).upper(1.0);

        assertTrue(SQPOptimizerImplS2.INTEGRATION.isCapable(model));
        assertTrue(SolverHipparchus.INTEGRATION.isCapable(model));

        assertFalse(QPDualActiveSolverImpl.INTEGRATION.isCapable(model));
        assertFalse(ADMMQPOptimizerImpl.INTEGRATION.isCapable(model));
        assertFalse(SimplexSolverImpl.INTEGRATION.isCapable(model));
    }

    /**
     * x² + y² >= 1 is not convex (the region outside a circle).
     */
    @Test
    public void testConvexFormWithLowerLimitIsRejected() {
        ExpressionsBasedModel model = QuadraticConstraintsWithHipparchus.makeModel();
        QuadraticConstraintsWithHipparchus.makeQuadratic(model, 1.0, 1.0).lower(1.0);
        QuadraticConstraintsWithHipparchus.assertRejected(model);
    }

    /**
     * x² - y² <= 1 is not convex.
     */
    @Test
    public void testIndefiniteQuadraticFormIsRejected() {
        ExpressionsBasedModel model = QuadraticConstraintsWithHipparchus.makeModel();
        QuadraticConstraintsWithHipparchus.makeQuadratic(model, 1.0, -1.0).upper(1.0);
        QuadraticConstraintsWithHipparchus.assertRejected(model);
    }

    /**
     * Inside the circle, x² + y² <= 1, and x + y >= 2 – no solution. The optimiser has no infeasibility
     * detection, but the point it stops at does not satisfy the constraints.
     */
    @Test
    public void testInfeasible() {

        ExpressionsBasedModel model = QuadraticConstraintsWithHipparchus.makeModel();
        QuadraticConstraintsWithHipparchus.makeQuadratic(model, 1.0, 1.0).upper(1.0);
        model.addExpression("sum").set(0, 1.0).set(1, 1.0).lower(2.0);

        Optimisation.Result result = model.minimise(SolverHipparchus.INTEGRATION);

        assertFalse(result.getState().isFeasible());
    }

    /**
     * -x² - y² >= -1 is the same circle as x² + y² <= 1, and convex.
     */
    @Test
    public void testNegatedConvexFormWithLowerLimit() {

        ExpressionsBasedModel model = QuadraticConstraintsWithHipparchus.makeModel();
        QuadraticConstraintsWithHipparchus.makeQuadratic(model, -1.0, -1.0).lower(-1.0);

        assertTrue(SolverHipparchus.INTEGRATION.isCapable(model));

        Optimisation.Result result = model.minimise(SolverHipparchus.INTEGRATION);

        double expected = Math.sqrt(0.5);
        assertEquals(Optimisation.State.OPTIMAL, result.getState());
        assertEquals(expected, result.doubleValue(0), 1E-6);
        assertEquals(expected, result.doubleValue(1), 1E-6);
    }

    /**
     * x² + y² = 1 is not convex.
     */
    @Test
    public void testQuadraticEqualityIsRejected() {
        ExpressionsBasedModel model = QuadraticConstraintsWithHipparchus.makeModel();
        QuadraticConstraintsWithHipparchus.makeQuadratic(model, 1.0, 1.0).level(1.0);
        QuadraticConstraintsWithHipparchus.assertRejected(model);
    }

    /**
     * 1 <= x² + y² <= 4 is not convex – the region between two circles.
     */
    @Test
    public void testTwoSidedQuadraticConstraintIsRejected() {
        ExpressionsBasedModel model = QuadraticConstraintsWithHipparchus.makeModel();
        QuadraticConstraintsWithHipparchus.makeQuadratic(model, 1.0, 1.0).lower(1.0).upper(4.0);
        QuadraticConstraintsWithHipparchus.assertRejected(model);
    }

}
