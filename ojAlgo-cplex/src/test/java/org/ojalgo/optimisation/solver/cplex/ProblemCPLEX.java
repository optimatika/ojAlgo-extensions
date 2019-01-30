/*
 * Copyright 1997-2018 Optimatika
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
package org.ojalgo.optimisation.solver.cplex;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.ojalgo.TestUtils;
import org.ojalgo.constant.PrimitiveMath;
import org.ojalgo.optimisation.Expression;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.Optimisation;
import org.ojalgo.optimisation.Variable;
import org.ojalgo.optimisation.solver.cplex.SolverCPLEX;

public class ProblemCPLEX {

    /**
     * https://github.com/optimatika/ojAlgo-extensions/issues/3 <br>
     * "compensating" didn't work because of an incorrectly used stream - did peek(...) instead of map(...).
     */
    @Test
    public void testCompensate() {

        ExpressionsBasedModel.addIntegration(SolverCPLEX.INTEGRATION);

        final ExpressionsBasedModel test = new ExpressionsBasedModel();
        test.addVariable(Variable.make("X1").lower(0).upper(5).weight(1));
        test.addVariable(Variable.make("X2").lower(0).upper(5).weight(1));
        test.addVariable(Variable.make("X3").level(4).weight(1));
        final Expression expressions = test.addExpression("1").upper(5);
        expressions.set(0, 1).set(1, 1).set(2, 1);
        final Optimisation.Result result = test.maximise();

        TestUtils.assertTrue(test.validate(result));

        TestUtils.assertTrue(result.getState().isOptimal());

        TestUtils.assertEquals(5.0, result.getValue(), PrimitiveMath.MACHINE_EPSILON);

        TestUtils.assertEquals(0.0, result.doubleValue(0), PrimitiveMath.MACHINE_EPSILON);
        TestUtils.assertEquals(1.0, result.doubleValue(1), PrimitiveMath.MACHINE_EPSILON);
        TestUtils.assertEquals(4.0, result.doubleValue(2), PrimitiveMath.MACHINE_EPSILON);
    }

    /**
     * https://github.com/optimatika/ojAlgo-extensions/issues/1
     */
    @Test
    public void testFixedVariables() {

        ExpressionsBasedModel.addIntegration(SolverCPLEX.INTEGRATION);

        final ExpressionsBasedModel test = new ExpressionsBasedModel();
        test.addVariable(Variable.make("V1").level(0.5));
        test.addVariable(Variable.make("V2").lower(0).upper(5).weight(2));
        test.addVariable(Variable.make("V3").lower(0).upper(1).weight(1));
        final Expression expressions = test.addExpression("E1").lower(0).upper(2);
        expressions.set(1, 1).set(2, 1);

        final Optimisation.Result minResult = test.minimise();
        TestUtils.assertTrue(test.validate(minResult));
        TestUtils.assertEquals(Optimisation.State.OPTIMAL, minResult.getState());
        TestUtils.assertEquals(0.0, minResult.getValue(), PrimitiveMath.MACHINE_EPSILON);
        TestUtils.assertEquals(0.5, minResult.doubleValue(0), PrimitiveMath.MACHINE_EPSILON);
        TestUtils.assertEquals(0.0, minResult.doubleValue(1), PrimitiveMath.MACHINE_EPSILON);
        TestUtils.assertEquals(0.0, minResult.doubleValue(2), PrimitiveMath.MACHINE_EPSILON);

        final Optimisation.Result maxResult = test.maximise();
        TestUtils.assertTrue(test.validate(maxResult));
        TestUtils.assertEquals(Optimisation.State.OPTIMAL, maxResult.getState());
        TestUtils.assertEquals(4.0, maxResult.getValue(), PrimitiveMath.MACHINE_EPSILON);
        TestUtils.assertEquals(0.5, maxResult.doubleValue(0), PrimitiveMath.MACHINE_EPSILON);
        TestUtils.assertEquals(2.0, maxResult.doubleValue(1), PrimitiveMath.MACHINE_EPSILON);
        TestUtils.assertEquals(0.0, maxResult.doubleValue(2), PrimitiveMath.MACHINE_EPSILON);
    }

    /**
     * https://github.com/optimatika/ojAlgo-extensions/issues/2 <br>
     * Reported as a problem with the Gurobi integration. The problem is unbounded. Many solvers do not return
     * a feasible solution in such case - even if they could.
     */
    @Test
    @Tag("unstable")
    public void testGitHubIssue2() {

        final Variable[] objective = new Variable[] { new Variable("X1").weight(0.8), new Variable("X2").weight(0.2), new Variable("X3").weight(0.7),
                new Variable("X4").weight(0.3), new Variable("X5").weight(0.6), new Variable("X6").weight(0.4) };

        // ExpressionsBasedModel.addIntegration(SolverCPLEX.INTEGRATION);
        final ExpressionsBasedModel model = new ExpressionsBasedModel(objective);

        model.addExpression("C1").set(0, 1).set(2, 1).set(4, 1).level(23);
        model.addExpression("C2").set(1, 1).set(3, 1).set(5, 1).level(23);
        model.addExpression("C3").set(0, 1).lower(10);
        model.addExpression("C4").set(2, 1).lower(8);
        model.addExpression("C5").set(4, 1).lower(5);

        final Optimisation.Result result = model.maximise();

        // A valid solution of 25.8 can be produced with:
        //     X1=10, X2=0, X3=8, X4=0, X5=5, X6=23
        TestUtils.assertEquals(25.8, result.getValue(), 0.001);
    }

}