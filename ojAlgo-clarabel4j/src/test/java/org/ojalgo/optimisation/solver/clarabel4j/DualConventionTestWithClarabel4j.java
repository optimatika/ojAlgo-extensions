package org.ojalgo.optimisation.solver.clarabel4j;

import java.util.List;

import org.ojalgo.optimisation.DualConventionTest;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.type.context.NumberContext;
import org.ojalgo.type.keyvalue.KeyValue;

/**
 * An interior point method: the solution and its certificates are approximate, and a certificate need not be
 * the minimal one.
 */
public class DualConventionTestWithClarabel4j extends DualConventionTest {

    @Override
    protected NumberContext getAccuracy() {
        return NumberContext.of(5);
    }

    @Override
    protected List<KeyValue<String, ExpressionsBasedModel.Integration<?>>> getCertifyingIntegrations() {
        return List.of(KeyValue.of("Clarabel4j", SolverClarabel4j.INTEGRATION));
    }

    @Override
    protected List<KeyValue<String, ExpressionsBasedModel.Integration<?>>> getLinearIntegrations() {
        return List.of(KeyValue.of("Clarabel4j", SolverClarabel4j.INTEGRATION));
    }

    @Override
    protected boolean isCertificateMinimal() {
        return false;
    }

}
