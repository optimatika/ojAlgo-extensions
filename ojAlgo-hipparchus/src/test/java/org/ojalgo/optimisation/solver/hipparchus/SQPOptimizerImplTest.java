package org.ojalgo.optimisation.solver.hipparchus;

import org.junit.jupiter.api.Test;
import org.ojalgo.TestUtils;
import org.ojalgo.optimisation.Expression;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.ModelEntity;
import org.ojalgo.optimisation.Optimisation;
import org.ojalgo.optimisation.Optimisation.ConstraintType;
import org.ojalgo.optimisation.Variable;
import org.ojalgo.type.context.NumberContext;
import org.ojalgo.type.keyvalue.EntryPair;
import org.ojalgo.type.keyvalue.EntryPair.KeyedPrimitive;

/**
 * The SQP optimisers only accept one-sided inequality constraints (not the two-sided ones the ADMM optimiser
 * gets), and they fail on some of the {@link org.ojalgo.optimisation.OptimalityCertificateTest} problems – so
 * only this one.
 * <p>
 * Minimising 3x + 6y + (x² + y²) / 4 subject to x + y >= 1 and x, y in [-10, 10] gives x = 3.5 and y = -2.5,
 * with dual value 4.75 for the constraint (-4.75 when maximising the negated objective). (Without the
 * constraint it would be x = -6 and y = -10.)
 * <p>
 * By default the SQP optimisers use a quasi-Newton approximation of the Hessian, and solve their QP
 * sub-problems with the ADMM optimiser (tolerance 1e-5) – the results are accurate to about 1e-3.
 */
public class SQPOptimizerImplTest {

    private static final NumberContext ACCURACY = NumberContext.of(2);

    private static void doTest(final ExpressionsBasedModel.Integration<?> integration) {

        for (Optimisation.Sense sense : Optimisation.Sense.values()) {

            double sign = sense == Optimisation.Sense.MAX ? -1.0 : 1.0;

            ExpressionsBasedModel model = new ExpressionsBasedModel();
            Variable x = model.newVariable("x").lower(-10).upper(10).weight(3 * sign);
            Variable y = model.newVariable("y").lower(-10).upper(10).weight(6 * sign);
            Expression constraint = model.newExpression("c").set(x, 1).set(y, 1).lower(1);
            Expression quadratic = model.addExpression("quadratic").weight(sign);
            quadratic.set(x, x, 0.25);
            quadratic.set(y, y, 0.25);

            Optimisation.Result result = sense == Optimisation.Sense.MAX ? model.maximise(integration) : model.minimise(integration);

            TestUtils.assertStateNotLessThanOptimal(result);
            TestUtils.assertEquals(3.5, result.doubleValue(0), ACCURACY);
            TestUtils.assertEquals(-2.5, result.doubleValue(1), ACCURACY);

            for (KeyedPrimitive<EntryPair<ModelEntity<?>, ConstraintType>> dual : result.getDualValues()) {
                if (dual.getKey().left() == constraint) {
                    TestUtils.assertEquals(ConstraintType.LOWER, dual.getKey().right());
                    TestUtils.assertEquals(sign * 4.75, dual.doubleValue(), ACCURACY);
                } else {
                    TestUtils.assertEquals(0.0, dual.doubleValue(), ACCURACY);
                }
            }
        }
    }

    @Test
    public void testSQPOptimizerGM() {
        SQPOptimizerImplTest.doTest(SQPOptimizerImplGM.INTEGRATION);
    }

    @Test
    public void testSQPOptimizerS() {
        SQPOptimizerImplTest.doTest(SQPOptimizerImplS.INTEGRATION);
    }

}
