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
package org.ojalgo.optimisation.external;

import org.junit.Assert;
import org.junit.Test;
import org.ojalgo.constant.PrimitiveMath;
import org.ojalgo.optimisation.Expression;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.Optimisation;
import org.ojalgo.optimisation.Variable;

public class ProblemCPLEX {

    @Test
    public void testFixedVariables() {

        ExpressionsBasedModel.addIntegration(SolverCPLEX.INTEGRATION);

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

}
