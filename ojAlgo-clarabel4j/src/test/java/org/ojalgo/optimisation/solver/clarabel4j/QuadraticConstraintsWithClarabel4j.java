package org.ojalgo.optimisation.solver.clarabel4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.Optimisation;
import org.ojalgo.optimisation.Variable;

/**
 * Quadratic constraints are modelled as second-order cones. These cases cover the parts of that translation
 * that the shared {@link org.ojalgo.optimisation.convex.TestBasicQCQP} models do not reach: linear terms
 * inside a quadratic constraint, coupled (non-diagonal) quadratic forms, rank-deficient but still convex
 * forms, and the non-convex shapes that have to be rejected rather than quietly dropped.
 */
public class QuadraticConstraintsWithClarabel4j {

    private static final double ACCURACY = 1E-6;

    /**
     * A quadratic constraint with a linear term in it: min -x subject to x² + 2x &lt;= 3, 0 &lt;= x &lt;= 10.
     * The constraint binds at x = 1. Ignoring the linear term would give x = sqrt(3).
     */
    @Test
    public void testLinearTermInQuadraticConstraint() {

        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable x = model.addVariable("x").lower(0.0).upper(10.0).weight(-1.0);
        model.addExpression("qc").set(x, x, 1.0).set(x, 2.0).upper(3.0);

        Optimisation.Result result = model.minimise(SolverClarabel4j.INTEGRATION);

        assertEquals(Optimisation.State.OPTIMAL, result.getState());
        assertEquals(1.0, result.doubleValue(0), ACCURACY);
        assertEquals(-1.0, result.getValue(), ACCURACY);
    }

    /**
     * A lower-limited quadratic constraint with a linear term, which is convex only after negation: min x
     * subject to -x² + 4x &gt;= 3, 0 &lt;= x &lt;= 10. The feasible region is [1,3], so the optimum is x = 1.
     */
    @Test
    public void testLinearTermInNegatedQuadraticConstraint() {

        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable x = model.addVariable("x").lower(0.0).upper(10.0).weight(1.0);
        model.addExpression("qc").set(x, x, -1.0).set(x, 4.0).lower(3.0);

        Optimisation.Result result = model.minimise(SolverClarabel4j.INTEGRATION);

        assertEquals(Optimisation.State.OPTIMAL, result.getState());
        assertEquals(1.0, result.doubleValue(0), ACCURACY);
    }

    /**
     * A coupled quadratic form, where the factorisation is not diagonal: min -x - y subject to x² + xy + y²
     * &lt;= 3. By symmetry x = y, so 3x² &lt;= 3 gives x = y = 1 and objective -2.
     */
    @Test
    public void testCoupledQuadraticForm() {

        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable x = model.addVariable("x").lower(0.0).upper(10.0).weight(-1.0);
        Variable y = model.addVariable("y").lower(0.0).upper(10.0).weight(-1.0);
        model.addExpression("qc").set(x, x, 1.0).set(x, y, 1.0).set(y, y, 1.0).upper(3.0);

        Optimisation.Result result = model.minimise(SolverClarabel4j.INTEGRATION);

        assertEquals(Optimisation.State.OPTIMAL, result.getState());
        assertEquals(1.0, result.doubleValue(0), ACCURACY);
        assertEquals(1.0, result.doubleValue(1), ACCURACY);
        assertEquals(-2.0, result.getValue(), ACCURACY);
    }

    /**
     * A rank-deficient but still convex quadratic form - Cholesky cannot factorise this, but it is a
     * legitimate constraint: min -x subject to (x + y)² &lt;= 4 with y fixed at 0, so x &lt;= 2.
     */
    @Test
    public void testPositiveSemiDefiniteQuadraticForm() {

        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable x = model.addVariable("x").lower(0.0).upper(10.0).weight(-1.0);
        Variable y = model.addVariable("y").level(0.0);
        model.addExpression("qc").set(x, x, 1.0).set(x, y, 2.0).set(y, y, 1.0).upper(4.0);

        Optimisation.Result result = model.minimise(SolverClarabel4j.INTEGRATION);

        assertEquals(Optimisation.State.OPTIMAL, result.getState());
        assertEquals(2.0, result.doubleValue(0), ACCURACY);
    }

    /**
     * An indefinite quadratic form is not convex. It must be rejected rather than dropped - dropping it
     * silently returned x = 10 here, reported as OPTIMAL.
     */
    @Test
    public void testIndefiniteQuadraticFormIsRejected() {

        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable x = model.addVariable("x").lower(0.0).upper(10.0).weight(-1.0);
        Variable y = model.addVariable("y").lower(0.0).upper(10.0);
        model.addExpression("qc").set(x, x, 1.0).set(y, y, -0.5).upper(1.0);

        assertThrows(IllegalArgumentException.class, () -> model.minimise(SolverClarabel4j.INTEGRATION));
    }

    /**
     * The region between two quadratic limits is not convex, so a two-sided quadratic constraint must be
     * rejected. Previously the lower limit was silently ignored.
     */
    @Test
    public void testTwoSidedQuadraticConstraintIsRejected() {

        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable x = model.addVariable("x").lower(0.0).upper(10.0).weight(-1.0);
        model.addExpression("qc").set(x, x, 1.0).lower(1.0).upper(4.0);

        assertThrows(IllegalArgumentException.class, () -> model.minimise(SolverClarabel4j.INTEGRATION));
    }

}
