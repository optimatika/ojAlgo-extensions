package org.ojalgo.optimisation.solver.clarabel4j;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.Variable;

/**
 * Quadratic constraints are modelled as second-order cones. The convex shapes are covered by the shared
 * {@link org.ojalgo.optimisation.convex.TestBasicQCQP}. These cases cover the non-convex shapes that have to be
 * rejected rather than quietly dropped.
 */
public class QuadraticConstraintsWithClarabel4j {

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
