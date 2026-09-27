package org.ojalgo.optimisation.solver.scip;

import java.util.List;

import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.OptimalityCertificateTest;
import org.ojalgo.type.context.NumberContext;
import org.ojalgo.type.keyvalue.KeyValue;

/**
 * SCIP provides no dual values (or reduced costs) through this integration, so only the objective function
 * value is checked.
 */
public class OptimalityCertificateTestWithSCIP extends OptimalityCertificateTest {

    @Override
    protected NumberContext getAccuracy() {
        return NumberContext.of(6);
    }

    @Override
    protected List<KeyValue<String, ExpressionsBasedModel.Integration<?>>> getIntegerIntegrations() {
        return List.of(KeyValue.of("SCIP", SolverSCIP.INTEGRATION));
    }

    @Override
    protected List<KeyValue<String, ExpressionsBasedModel.Integration<?>>> getLinearIntegrations() {
        return List.of(KeyValue.of("SCIP", SolverSCIP.INTEGRATION));
    }

    @Override
    protected List<KeyValue<String, ExpressionsBasedModel.Integration<?>>> getQuadraticIntegrations() {
        return List.of(KeyValue.of("SCIP", SolverSCIP.INTEGRATION));
    }

    @Override
    protected boolean isDualsProvided() {
        return false;
    }

}
