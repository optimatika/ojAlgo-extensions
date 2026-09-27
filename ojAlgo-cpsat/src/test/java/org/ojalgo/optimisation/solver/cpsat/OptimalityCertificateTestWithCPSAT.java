package org.ojalgo.optimisation.solver.cpsat;

import java.util.List;

import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.OptimalityCertificateTest;
import org.ojalgo.type.keyvalue.KeyValue;

/**
 * CP-SAT only solves integer problems.
 */
public class OptimalityCertificateTestWithCPSAT extends OptimalityCertificateTest {

    @Override
    protected List<KeyValue<String, ExpressionsBasedModel.Integration<?>>> getIntegerIntegrations() {
        return List.of(KeyValue.of("CP-SAT", SolverCPSAT.INTEGRATION));
    }

    @Override
    protected List<KeyValue<String, ExpressionsBasedModel.Integration<?>>> getLinearIntegrations() {
        return List.of();
    }

    @Override
    protected List<KeyValue<String, ExpressionsBasedModel.Integration<?>>> getQuadraticIntegrations() {
        return List.of();
    }

}
