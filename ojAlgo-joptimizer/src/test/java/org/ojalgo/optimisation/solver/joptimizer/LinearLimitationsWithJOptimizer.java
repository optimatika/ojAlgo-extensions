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

import org.junit.jupiter.api.Test;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.ModelFileTest;
import org.ojalgo.optimisation.Optimisation;
import org.ojalgo.optimisation.Variable;

/**
 * JOptimizer's LP solver confines all variables to [-99999, 99999]. Where that box is an artificial limit
 * (the variable has no bound, or a bound beyond it, on that side), a solution at the limit is not proven
 * optimal, and infeasibility within the box is not proven infeasibility.
 */
public class LinearLimitationsWithJOptimizer {

    private static final double ACCURACY = 1E-6;

    /**
     * Unbounded: min -x, x &gt;= 0. JOptimizer stops at the box limit.
     */
    @Test
    public void testUnboundedIsNotOptimal() {

        ExpressionsBasedModel model = new ExpressionsBasedModel();
        model.addVariable("x").lower(0.0).weight(-1.0);

        Optimisation.Result result = model.minimise(SolverJOptimizer.INTEGRATION);

        assertEquals(Optimisation.State.FEASIBLE, result.getState());
    }

    /**
     * A finite optimum beyond the box: min y + z, y = z, z &gt;= -200000. The optimum is y = z = -200000, but
     * JOptimizer stops at -99999.
     */
    @Test
    public void testOptimumBeyondBoxIsNotOptimal() {

        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable y = model.addVariable("y").weight(1.0);
        Variable z = model.addVariable("z").lower(-200000.0).weight(1.0);
        model.addExpression("c").set(y, 1.0).set(z, -1.0).level(0.0);

        Optimisation.Result result = model.minimise(SolverJOptimizer.INTEGRATION);

        assertEquals(Optimisation.State.FEASIBLE, result.getState());
    }

    /**
     * Feasible, but only beyond the box: min x, x &gt;= 200000 (as a constraint). Within the box it's
     * infeasible - that must not be reported as INFEASIBLE.
     */
    @Test
    public void testInfeasibleWithinBoxIsNotInfeasible() {

        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable x = model.addVariable("x").weight(1.0);
        model.addExpression("c").set(x, 1.0).lower(200000.0);

        Optimisation.Result result = model.minimise(SolverJOptimizer.INTEGRATION);

        assertEquals(Optimisation.State.FAILED, result.getState());
    }

    /**
     * RECIPELP (Netlib) fails with the integration's tight tolerances, and is then solved with JOptimizer's
     * default tolerances.
     */
    @Test
    public void testFallbackToDefaultTolerances() {

        ExpressionsBasedModel model = ModelFileTest.makeModel("netlib", "RECIPELP.SIF", false);

        Optimisation.Result result = model.minimise(SolverJOptimizer.INTEGRATION);

        assertEquals(Optimisation.State.OPTIMAL, result.getState());
        assertEquals(-266.616, result.getValue(), 266.616 * ACCURACY);
    }

    /**
     * Without any artificial box limits, infeasible is still reported as such: x + y &gt;= 10 and x + y &lt;=
     * 5 with 0 &lt;= x, y &lt;= 100.
     */
    @Test
    public void testInfeasible() {

        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable x = model.addVariable("x").lower(0.0).upper(100.0).weight(1.0);
        Variable y = model.addVariable("y").lower(0.0).upper(100.0);
        model.addExpression("c1").set(x, 1.0).set(y, 1.0).lower(10.0);
        model.addExpression("c2").set(x, 1.0).set(y, 1.0).upper(5.0);

        Optimisation.Result result = model.minimise(SolverJOptimizer.INTEGRATION);

        assertEquals(Optimisation.State.INFEASIBLE, result.getState());
    }

    /**
     * Redundant (linearly dependent) equality constraints. JOptimizer throws an unchecked exception for that
     * ("Equalities matrix A must be pxn with rank(A) = p &lt; n") - it must be reported as FAILED, not passed
     * on.
     */
    @Test
    public void testRankDeficientEqualitiesFail() {

        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable x = model.addVariable("x").lower(0.0).upper(10.0).weight(1.0);
        Variable y = model.addVariable("y").lower(0.0).upper(10.0).weight(2.0);
        Variable z = model.addVariable("z").lower(0.0).upper(10.0).weight(3.0);
        model.addExpression("e1").set(x, 1.0).set(y, 1.0).level(4.0);
        model.addExpression("e2").set(y, 1.0).set(z, 1.0).level(6.0);
        model.addExpression("e3").set(x, 1.0).set(y, 2.0).set(z, 1.0).level(10.0);

        Optimisation.Result result = model.minimise(SolverJOptimizer.INTEGRATION);

        if (result.getState().isOptimal()) {
            // x = 4 - y, z = 6 - y, cost = 4 + 18 - 2y minimised at y = 4
            assertEquals(14.0, result.getValue(), ACCURACY);
        } else {
            assertEquals(Optimisation.State.FAILED, result.getState());
        }
    }

}
