/*
 * Copyright 1997-2026 Optimatika
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.ojalgo.optimisation.solver.joptimizer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.Optimisation;
import org.ojalgo.optimisation.Variable;

/**
 * JOptimizer takes quadratic constraints as convex functions, fi(x) <= 0. These cases cover what is
 * specific to this integration, and so not in the shared {@link org.ojalgo.optimisation.convex.TestBasicQCQP}:
 * quadratic constraints that are linear once some variables are fixed, and the non-convex shapes that have to
 * be rejected rather than quietly dropped or (mis)solved.
 */
public class QuadraticConstraintsWithJOptimizer {

    private static final double ACCURACY = 1E-6;

    /**
     * A quadratic equality constraint is not convex. It must be rejected rather than dropped - without it the
     * solution would be x = y = 10.
     */
    @Test
    public void testQuadraticEqualityIsRejected() {

        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable x = model.addVariable("x").lower(0.0).upper(10.0).weight(-1.0);
        Variable y = model.addVariable("y").lower(0.0).upper(10.0).weight(-1.0);
        model.addExpression("circle").set(x, x, 1.0).set(y, y, 1.0).level(1.0);

        assertThrows(IllegalArgumentException.class, () -> model.minimise(SolverJOptimizer.INTEGRATION));
    }

    /**
     * A bilinear equality with one of its variables fixed is linear: min (x - 3)² subject to xy = 2 with y
     * fixed at 1, so x = 2.
     */
    @Test
    public void testQuadraticEqualityLinearWhenFixed() {

        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable x = model.addVariable("x").lower(0.0).upper(10.0);
        Variable y = model.addVariable("y").level(1.0);
        model.addExpression("obj").set(x, x, 1.0).set(x, -6.0).weight(1.0);
        model.addExpression("bilinear").set(x, y, 1.0).level(2.0);

        Optimisation.Result result = model.minimise(SolverJOptimizer.INTEGRATION);

        assertEquals(Optimisation.State.OPTIMAL, result.getState());
        assertEquals(2.0, result.doubleValue(0), ACCURACY);
        assertEquals(1.0, result.doubleValue(1), ACCURACY);
    }

    /**
     * An indefinite quadratic form is not convex, and must be rejected.
     */
    @Test
    public void testIndefiniteQuadraticFormIsRejected() {

        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable x = model.addVariable("x").lower(0.0).upper(10.0).weight(-1.0);
        Variable y = model.addVariable("y").lower(0.0).upper(10.0);
        model.addExpression("qc").set(x, x, 1.0).set(y, y, -0.5).upper(1.0);

        assertThrows(IllegalArgumentException.class, () -> model.minimise(SolverJOptimizer.INTEGRATION));
    }

    /**
     * The region between two quadratic limits is not convex, so a two-sided quadratic constraint must be
     * rejected.
     */
    @Test
    public void testTwoSidedQuadraticConstraintIsRejected() {

        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable x = model.addVariable("x").lower(0.0).upper(10.0).weight(-1.0);
        model.addExpression("qc").set(x, x, 1.0).lower(1.0).upper(4.0);

        assertThrows(IllegalArgumentException.class, () -> model.minimise(SolverJOptimizer.INTEGRATION));
    }

    /**
     * Minimising a concave quadratic is not convex: min -x² - y², 0 <= x, y <= 1.
     */
    @Test
    public void testConcaveObjectiveIsRejected() {

        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable x = model.addVariable("x").lower(0.0).upper(1.0);
        Variable y = model.addVariable("y").lower(0.0).upper(1.0);
        model.addExpression("obj").set(x, x, -1.0).set(y, y, -1.0).weight(1.0);

        assertThrows(IllegalArgumentException.class, () -> model.minimise(SolverJOptimizer.INTEGRATION));
    }

    /**
     * Nor is maximising a convex quadratic: max x² + y², 0 <= x, y <= 1.
     */
    @Test
    public void testMaximisedConvexObjectiveIsRejected() {

        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable x = model.addVariable("x").lower(0.0).upper(1.0);
        Variable y = model.addVariable("y").lower(0.0).upper(1.0);
        model.addExpression("obj").set(x, x, 1.0).set(y, y, 1.0).weight(1.0);

        assertThrows(IllegalArgumentException.class, () -> model.maximise(SolverJOptimizer.INTEGRATION));
    }

}
