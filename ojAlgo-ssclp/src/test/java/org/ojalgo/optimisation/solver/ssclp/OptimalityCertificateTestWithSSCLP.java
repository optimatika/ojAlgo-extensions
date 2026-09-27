package org.ojalgo.optimisation.solver.ssclp;

import java.util.List;

import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.OptimalityCertificateTest;
import org.ojalgo.type.keyvalue.KeyValue;

/**
 * SSC-LP provides no dual values (or reduced costs), so only the solution's feasibility and the objective
 * function value are checked, and it only solves linear problems.
 */
public class OptimalityCertificateTestWithSSCLP extends OptimalityCertificateTest {

    @Override
    protected List<KeyValue<String, ExpressionsBasedModel.Integration<?>>> getIntegerIntegrations() {
        return List.of(KeyValue.of("SSC-LP", SolverSSCLP.INTEGRATION));
    }

    @Override
    protected List<KeyValue<String, ExpressionsBasedModel.Integration<?>>> getLinearIntegrations() {
        return List.of(KeyValue.of("SSC-LP", SolverSSCLP.INTEGRATION));
    }

    @Override
    protected List<KeyValue<String, ExpressionsBasedModel.Integration<?>>> getQuadraticIntegrations() {
        return List.of();
    }

    @Override
    protected boolean isDualsProvided() {
        return false;
    }

}
