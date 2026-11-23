package org.ojalgo.optimisation.solver.hipparchus;

import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.ojalgo.TestUtils;
import org.ojalgo.optimisation.Expression;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.ModelEntity;
import org.ojalgo.optimisation.Optimisation;
import org.ojalgo.optimisation.Optimisation.ConstraintType;
import org.ojalgo.optimisation.Variable;
import org.ojalgo.optimisation.convex.CuteMarosMeszarosCase;
import org.ojalgo.structure.Access1D;
import org.ojalgo.type.context.NumberContext;
import org.ojalgo.type.keyvalue.EntryPair;
import org.ojalgo.type.keyvalue.EntryPair.KeyedPrimitive;

/**
 * The SQP optimisers only accept one-sided inequality constraints (not the two-sided ones the ADMM optimiser
 * gets), and the deprecated GM and S optimisers fail on some of the
 * {@link org.ojalgo.optimisation.OptimalityCertificateTest} problems – so only this one. (S2 is also tested
 * with those, see {@link OptimalityCertificateTestWithHipparchusS2}.)
 * <p>
 * Minimising 3x + 6y + (x² + y²) / 4 subject to x + y >= 1 and x, y in [-10, 10] gives x = 3.5 and y = -2.5,
 * with dual value 4.75 for the constraint (-4.75 when maximising the negated objective). (Without the
 * constraint it would be x = -6 and y = -10.)
 * <p>
 * By default the SQP optimisers use a quasi-Newton approximation of the Hessian. The deprecated GM and S
 * optimisers solve their QP sub-problems with the ADMM optimiser (tolerance 1e-5) – the results are accurate
 * to about 1e-3. The S2 optimiser solves them with the dual active-set solver, and provides dual values only
 * when its multipliers satisfy the optimality conditions – see {@link #testSQPOptimizerS2DualValues()}.
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
    @SuppressWarnings("deprecation")
    public void testSQPOptimizerGM() {
        SQPOptimizerImplTest.doTest(SQPOptimizerImplGM.INTEGRATION);
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testSQPOptimizerS() {
        SQPOptimizerImplTest.doTest(SQPOptimizerImplS.INTEGRATION);
    }

    @Test
    public void testSQPOptimizerS2() {
        SQPOptimizerImplTest.doTest(SQPOptimizerImplS2.INTEGRATION);
    }

    /**
     * The S2 optimiser's multipliers are only used if they satisfy the optimality conditions – then the dual
     * values and the reduced gradient are those of the dual active-set solver. Of these models, S2 returned
     * valid multipliers for HS53, ZECEVIC2 and QPTEST, but not for HS21 or HS118.
     */
    @Test
    public void testSQPOptimizerS2DualValues() {

        NumberContext accuracy = NumberContext.of(6);
        int withDuals = 0;

        for (String name : new String[] { "HS21.SIF", "HS53.SIF", "HS118.SIF", "QPTEST.SIF", "ZECEVIC2.SIF" }) {

            ExpressionsBasedModel model = CuteMarosMeszarosCase.makeModel(name);
            Optimisation.Result actual = model.minimise(SQPOptimizerImplS2.INTEGRATION);
            TestUtils.assertStateNotLessThanOptimal(actual);

            if (actual.getReducedGradient().isPresent()) {
                withDuals++;

                Optimisation.Result expected = model.minimise(QPDualActiveSolverImpl.INTEGRATION);

                Access1D<?> expectedGradient = expected.getReducedGradient().map(Supplier::get).get();
                Access1D<?> actualGradient = actual.getReducedGradient().map(Supplier::get).get();
                TestUtils.assertEquals(name, expectedGradient, actualGradient, accuracy);

                List<KeyedPrimitive<EntryPair<ModelEntity<?>, ConstraintType>>> expectedDuals = expected.getDualValues();
                List<KeyedPrimitive<EntryPair<ModelEntity<?>, ConstraintType>>> actualDuals = actual.getDualValues();
                TestUtils.assertEquals(name, expectedDuals.size(), actualDuals.size());
                for (int i = 0; i < expectedDuals.size(); i++) {
                    TestUtils.assertEquals(name, expectedDuals.get(i).getKey(), actualDuals.get(i).getKey());
                    TestUtils.assertEquals(name, expectedDuals.get(i).doubleValue(), actualDuals.get(i).doubleValue(), accuracy);
                }

            } else {
                TestUtils.assertTrue(name, actual.getDualValues().isEmpty());
            }
        }

        TestUtils.assertTrue(withDuals > 0);
    }

}
