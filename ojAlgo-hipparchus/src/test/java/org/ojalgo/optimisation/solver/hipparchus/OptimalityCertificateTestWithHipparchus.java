package org.ojalgo.optimisation.solver.hipparchus;

import java.util.List;

import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.OptimalityCertificateTest;
import org.ojalgo.type.context.NumberContext;
import org.ojalgo.type.keyvalue.KeyValue;

/**
 * The ADMM optimiser, for quadratic problems. See also {@link OptimalityCertificateTestWithHipparchusSimplex}
 * and {@link SQPOptimizerImplTest}.
 */
public class OptimalityCertificateTestWithHipparchus extends OptimalityCertificateTest {

    /**
     * The ADMM optimiser's default tolerance is 1e-5, without polishing.
     */
    @Override
    protected NumberContext getAccuracy() {
        return NumberContext.of(4);
    }

    /**
     * The slacks are accurate to about 1e-5, and the multipliers are of order 1-10.
     */
    @Override
    protected NumberContext getComplementarityAccuracy() {
        return NumberContext.of(3);
    }

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
        return List.of(KeyValue.of("ADMM", ADMMQPOptimizerImpl.INTEGRATION), KeyValue.of("SolverHipparchus", SolverHipparchus.INTEGRATION));
    }

}
