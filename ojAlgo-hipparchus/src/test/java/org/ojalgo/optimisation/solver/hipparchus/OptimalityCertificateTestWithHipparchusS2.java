package org.ojalgo.optimisation.solver.hipparchus;

import java.util.List;

import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.OptimalityCertificateTest;
import org.ojalgo.type.context.NumberContext;
import org.ojalgo.type.keyvalue.KeyValue;

/**
 * The SQPOptimizerS2 optimiser, for quadratic problems and quadratic constraints – the latter also with
 * {@link SolverHipparchus}, that uses that optimiser for them. Its results have dual values only if its
 * multipliers satisfy the optimality conditions, see {@link SQPOptimizerImplS2}, so they are not checked
 * here – {@link SQPOptimizerImplTest#testSQPOptimizerS2DualValues()} does that.
 */
public class OptimalityCertificateTestWithHipparchusS2 extends OptimalityCertificateTest {

    @Override
    protected List<KeyValue<String, ExpressionsBasedModel.Integration<?>>> getIntegerIntegrations() {
        return List.of();
    }

    @Override
    protected List<KeyValue<String, ExpressionsBasedModel.Integration<?>>> getLinearIntegrations() {
        return List.of();
    }

    /**
     * With its default convergence tolerance the optimiser satisfies quadratic constraints to about 6 digits.
     */
    @Override
    protected NumberContext getQuadraticConstraintAccuracy() {
        return NumberContext.of(6);
    }

    @Override
    protected List<KeyValue<String, ExpressionsBasedModel.Integration<?>>> getQuadraticConstraintIntegrations() {
        return List.of(KeyValue.of("S2", SQPOptimizerImplS2.INTEGRATION), KeyValue.of("Hipparchus", SolverHipparchus.INTEGRATION));
    }

    @Override
    protected List<KeyValue<String, ExpressionsBasedModel.Integration<?>>> getQuadraticIntegrations() {
        return List.of(KeyValue.of("S2", SQPOptimizerImplS2.INTEGRATION));
    }

    @Override
    protected boolean isDualsProvided() {
        return false;
    }

}
