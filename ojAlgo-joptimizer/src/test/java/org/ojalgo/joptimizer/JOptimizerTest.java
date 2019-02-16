/*
 * Copyright 1997-2019 Optimatika
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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.ojalgo.TestUtils;
import org.ojalgo.optimisation.Expression;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.Optimisation;
import org.ojalgo.optimisation.Variable;
import org.ojalgo.structure.Access1D;
import org.ojalgo.type.context.NumberContext;

import com.joptimizer.exception.JOptimizerException;
import com.joptimizer.functions.ConvexMultivariateRealFunction;
import com.joptimizer.functions.FunctionsUtils;
import com.joptimizer.functions.PDQuadraticMultivariateRealFunction;
import com.joptimizer.optimizers.JOptimizer;
import com.joptimizer.optimizers.OptimizationRequest;

public class JOptimizerTest {

    @BeforeAll
    public static void configure() {
        ExpressionsBasedModel.addPreferredSolver(SolverJOptimizer.INTEGRATION);
    }

    /**
     * Re-implementation of http://www.joptimizer.com/qcQuadraticProgramming.html
     *
     * @throws JOptimizerException
     */
    @Test
    public void testQCQP() throws JOptimizerException {

        // Objective function
        double[][] P = new double[][] { { 1., 0.4 }, { 0.4, 1. } };
        PDQuadraticMultivariateRealFunction objectiveFunction = new PDQuadraticMultivariateRealFunction(P, null, 0);

        //inequalities
        ConvexMultivariateRealFunction[] inequalities = new ConvexMultivariateRealFunction[1];
        inequalities[0] = FunctionsUtils.createCircle(2, 1.75, new double[] { -2, -2 });

        //optimization problem
        OptimizationRequest or = new OptimizationRequest();
        or.setF0(objectiveFunction);
        or.setInitialPoint(new double[] { -2., -2. });
        or.setFi(inequalities);
        or.setCheckKKTSolutionAccuracy(true);

        //optimization
        JOptimizer opt = new JOptimizer();
        opt.setOptimizationRequest(or);
        opt.optimize();

        double[] sol = opt.getOptimizationResponse().getSolution();

        double expected = -2 + (1.75 / Math.sqrt(2));
        NumberContext precision = NumberContext.getGeneral(6, 8);
        Optimisation.Result expResult = new Optimisation.Result(Optimisation.State.OPTIMAL, Access1D.wrap(new double[] { expected, expected }));

        TestUtils.assertEquals(expected, sol[0], precision);
        TestUtils.assertEquals(expected, sol[1], precision);

        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable x0 = model.addVariable("x0");
        Variable x1 = model.addVariable("x1");
        x0.setValue(-2);
        x1.setValue(-2);

        Expression obj = model.addExpression("Obj").weight(0.5);
        obj.set(x0, x0, 1.0);
        obj.set(x1, x1, 1.0);
        obj.set(x0, x1, 0.4);
        obj.set(x1, x0, 0.4);

        Expression constr = model.addExpression("Constr").upper(-4.9375);
        constr.set(x0, x0, 1.0);
        constr.set(x1, x1, 1.0);
        constr.set(x0, 4.0);
        constr.set(x1, 4.0);

        TestUtils.assertTrue(model.validate(expResult));

        Optimisation.Result actResult = model.minimise();

        //        BasicLogger.debug(actResult);
        //        BasicLogger.debug(model);
        TestUtils.assertStateNotLessThanOptimal(actResult);
        TestUtils.assertTrue(model.validate(actResult));
        TestUtils.assertEquals(expected, actResult.doubleValue(0), precision);
        TestUtils.assertEquals(expected, actResult.doubleValue(1), precision);
        TestUtils.assertStateAndSolution(expResult, actResult, precision);
    }

}
