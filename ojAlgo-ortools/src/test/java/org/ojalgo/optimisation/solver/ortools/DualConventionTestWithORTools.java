package org.ojalgo.optimisation.solver.ortools;

import java.util.List;

import org.ojalgo.optimisation.DualConventionTest;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.type.keyvalue.KeyValue;

/**
 * No infeasibility certificates.
 */
public class DualConventionTestWithORTools extends DualConventionTest {

    @Override
    protected List<KeyValue<String, ExpressionsBasedModel.Integration<?>>> getCertifyingIntegrations() {
        return List.of();
    }

    @Override
    protected List<KeyValue<String, ExpressionsBasedModel.Integration<?>>> getLinearIntegrations() {
        return List.of(KeyValue.of("OR-Tools", SolverORTools.INTEGRATION));
    }

}
