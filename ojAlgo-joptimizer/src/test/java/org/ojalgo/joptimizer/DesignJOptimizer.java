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
package org.ojalgo.joptimizer;

import org.junit.Assert;
import org.junit.Test;
import org.ojalgo.constant.PrimitiveMath;
import org.ojalgo.optimisation.Expression;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.Optimisation;
import org.ojalgo.optimisation.Variable;

public class DesignJOptimizer {

    @Test
    public void testSimplyLowerAndUpperBounds() {

        ExpressionsBasedModel.addIntegration(SolverJOptimizer.INTEGRATION);

        final ExpressionsBasedModel model = new ExpressionsBasedModel();
        model.addVariable(Variable.make("A").level(2).weight(3));
        model.addVariable(Variable.make("B").lower(1).upper(3).weight(2));
        model.addVariable(Variable.make("C").lower(0).upper(4).weight(1));
        model.addExpression("SUM").set(0, 1).set(1, 1).set(2, 1).level(6);

        final Optimisation.Result minResult = model.minimise();
        Assert.assertTrue(model.validate(minResult));
        Assert.assertTrue(minResult.getState().isOptimal());
        Assert.assertEquals(11, minResult.getValue(), PrimitiveMath.MACHINE_EPSILON);
        Assert.assertEquals(2, minResult.doubleValue(0), PrimitiveMath.MACHINE_EPSILON);
        Assert.assertEquals(1, minResult.doubleValue(1), PrimitiveMath.MACHINE_EPSILON);
        Assert.assertEquals(3, minResult.doubleValue(2), PrimitiveMath.MACHINE_EPSILON);

        final Optimisation.Result maxResult = model.maximise();
        Assert.assertTrue(model.validate(maxResult));
        Assert.assertTrue(maxResult.getState().isOptimal());
        Assert.assertEquals(13, maxResult.getValue(), PrimitiveMath.MACHINE_EPSILON);
        Assert.assertEquals(2, maxResult.doubleValue(0), PrimitiveMath.MACHINE_EPSILON);
        Assert.assertEquals(3, maxResult.doubleValue(1), PrimitiveMath.MACHINE_EPSILON);
        Assert.assertEquals(1, maxResult.doubleValue(2), PrimitiveMath.MACHINE_EPSILON);
    }

    /**
     * Copied from ProblemCPLEX <br>
     * https://github.com/optimatika/ojAlgo-extensions/issues/3
     */
    @Test
    public void testCompensate() {

        ExpressionsBasedModel.addIntegration(SolverJOptimizer.INTEGRATION);

        final ExpressionsBasedModel test = new ExpressionsBasedModel();
        test.addVariable(Variable.make("X1").lower(0).upper(5).weight(1));
        test.addVariable(Variable.make("X2").lower(0).upper(5).weight(1));
        test.addVariable(Variable.make("X3").level(4).weight(1));
        final Expression expressions = test.addExpression("1").upper(5);
        expressions.set(0, 1).set(1, 1).set(2, 1);
        final Optimisation.Result result = test.maximise();

        Assert.assertTrue(test.validate(result));

        Assert.assertTrue(result.getState().isOptimal());

        Assert.assertEquals(5.0, result.getValue(), PrimitiveMath.MACHINE_EPSILON);

        Assert.assertEquals(0.0, result.doubleValue(0), PrimitiveMath.MACHINE_EPSILON);
        Assert.assertEquals(1.0, result.doubleValue(1), PrimitiveMath.MACHINE_EPSILON);
        Assert.assertEquals(4.0, result.doubleValue(2), PrimitiveMath.MACHINE_EPSILON);
    }

    /**
     * Copied from ProblemCPLEX <br>
     * https://github.com/optimatika/ojAlgo-extensions/issues/1
     */
    @Test
    public void testFixedVariables() {

        ExpressionsBasedModel.addIntegration(SolverJOptimizer.INTEGRATION);

        final ExpressionsBasedModel test = new ExpressionsBasedModel();
        test.addVariable(Variable.make("1").level(0.5));
        test.addVariable(Variable.make("2").lower(0).upper(5).weight(2));
        test.addVariable(Variable.make("3").lower(0).upper(1).weight(1));
        final Expression expressions = test.addExpression("1").lower(0).upper(2);
        expressions.set(1, 1).set(2, 1);

        final Optimisation.Result minResult = test.minimise();
        Assert.assertTrue(test.validate(minResult));
        Assert.assertEquals(Optimisation.State.OPTIMAL, minResult.getState());
        Assert.assertEquals(0.0, minResult.getValue(), PrimitiveMath.MACHINE_EPSILON);
        Assert.assertEquals(0.5, minResult.doubleValue(0), PrimitiveMath.MACHINE_EPSILON);
        Assert.assertEquals(0.0, minResult.doubleValue(1), PrimitiveMath.MACHINE_EPSILON);
        Assert.assertEquals(0.0, minResult.doubleValue(2), PrimitiveMath.MACHINE_EPSILON);

        final Optimisation.Result maxResult = test.maximise();
        Assert.assertTrue(test.validate(maxResult));
        Assert.assertEquals(Optimisation.State.OPTIMAL, maxResult.getState());
        Assert.assertEquals(4.0, maxResult.getValue(), PrimitiveMath.MACHINE_EPSILON);
        Assert.assertEquals(0.5, maxResult.doubleValue(0), PrimitiveMath.MACHINE_EPSILON);
        Assert.assertEquals(2.0, maxResult.doubleValue(1), PrimitiveMath.MACHINE_EPSILON);
        Assert.assertEquals(0.0, maxResult.doubleValue(2), PrimitiveMath.MACHINE_EPSILON);
    }

}
