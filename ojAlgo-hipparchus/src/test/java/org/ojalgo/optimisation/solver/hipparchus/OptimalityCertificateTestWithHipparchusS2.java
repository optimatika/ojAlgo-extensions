package org.ojalgo.optimisation.solver.hipparchus;

import java.util.List;

import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.OptimalityCertificateTest;
import org.ojalgo.type.keyvalue.KeyValue;

/**
 * The SQPOptimizerS2 optimiser, for quadratic problems. Its results have no dual values, see
 * {@link SQPOptimizerImplS2}.
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

    @Override
    protected List<KeyValue<String, ExpressionsBasedModel.Integration<?>>> getQuadraticIntegrations() {
        return List.of(KeyValue.of("S2", SQPOptimizerImplS2.INTEGRATION));
    }

    @Override
    protected boolean isDualsProvided() {
        return false;
    }

}
