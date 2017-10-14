/*
 * Copyright 1997-2017 Optimatika
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
package org.ojalgo.gurobi;

import org.junit.Assert;
import org.junit.Test;
import org.ojalgo.constant.PrimitiveMath;
import org.ojalgo.optimisation.Expression;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.Optimisation;
import org.ojalgo.optimisation.Variable;
import org.ojalgo.optimisation.external.SolverGurobi;

public class ProblemGurobi {

    @Test
    public void testFixedVariables() {

        ExpressionsBasedModel.addIntegration(SolverGurobi.INTEGRATION);

        final ExpressionsBasedModel test = new ExpressionsBasedModel();
        test.addVariable(Variable.make("1").level(0.5));
        test.addVariable(Variable.make("2").lower(0).upper(5).weight(2));
        test.addVariable(Variable.make("3").lower(0).upper(1).weight(1));
        final Expression expressions = test.addExpression("1").lower(0).upper(2);
        expressions.set(1, 1).set(2, 1);

        final Optimisation.Result result = test.minimise();

        Assert.assertEquals(Optimisation.State.OPTIMAL, result.getState());
        Assert.assertEquals(0.0, result.getValue(), PrimitiveMath.MACHINE_EPSILON);

        // BasicLogger.debug(result);
    }

    @Test
    public void testGitHubIssue2() {

        final Variable[] objective = new Variable[] { new Variable("X1").weight(0.8), new Variable("X2").weight(0.2), new Variable("X3").weight(0.7),
                new Variable("X4").weight(0.3), new Variable("X5").weight(0.6), new Variable("X6").weight(0.4) };

        // ExpressionsBasedModel.addIntegration(SolverGurobi.INTEGRATION);
        final ExpressionsBasedModel model = new ExpressionsBasedModel(objective);

        model.addExpression("C1").set(0, 1).set(2, 1).set(4, 1).level(23);
        model.addExpression("C2").set(1, 1).set(3, 1).set(5, 1).level(23);
        model.addExpression("C3").set(0, 1).lower(10);
        model.addExpression("C4").set(2, 1).lower(8);
        model.addExpression("C5").set(4, 1).lower(5);

        final Optimisation.Result result = model.maximise();

        // A valid solution of 25.8 can be produced with:
        //     X1=10, X2=0, X3=8, X4=0, X5=5, X6=23
        Assert.assertEquals(25.8, result.getValue(), 0.001);
    }

}
