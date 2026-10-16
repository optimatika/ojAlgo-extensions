package org.ojalgo.optimisation.solver.xpress;

import java.util.List;

import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.OptimalityCertificateTest;
import org.ojalgo.type.context.NumberContext;
import org.ojalgo.type.keyvalue.KeyValue;

public class OptimalityCertificateTestWithXpress extends OptimalityCertificateTest {

    @Override
    protected NumberContext getAccuracy() {
        return NumberContext.of(7);
    }

    /**
     * Xpress solves QPs with its barrier (interior point) method: at a degenerate optimum the multiplier and
     * the slack are both about the square root of the duality gap.
     */
    @Override
    protected NumberContext getComplementarityAccuracy() {
        return NumberContext.of(4);
    }

    @Override
    protected List<KeyValue<String, ExpressionsBasedModel.Integration<?>>> getIntegerIntegrations() {
        return List.of(KeyValue.of("Xpress", SolverXpress.INTEGRATION));
    }

    @Override
    protected List<KeyValue<String, ExpressionsBasedModel.Integration<?>>> getLinearIntegrations() {
        return List.of(KeyValue.of("Xpress", SolverXpress.INTEGRATION));
    }

    @Override
    protected List<KeyValue<String, ExpressionsBasedModel.Integration<?>>> getQuadraticIntegrations() {
        return List.of(KeyValue.of("Xpress", SolverXpress.INTEGRATION));
    }

}
