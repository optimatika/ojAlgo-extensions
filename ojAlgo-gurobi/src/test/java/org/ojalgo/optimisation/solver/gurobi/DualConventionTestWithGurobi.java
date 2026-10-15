package org.ojalgo.optimisation.solver.gurobi;

import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.ojalgo.optimisation.DualConventionTest;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.type.keyvalue.KeyValue;

/**
 * No infeasibility certificates (Gurobi's FarkasDual is not used).
 */
@Disabled
public class DualConventionTestWithGurobi extends DualConventionTest {

    @Override
    protected List<KeyValue<String, ExpressionsBasedModel.Integration<?>>> getCertifyingIntegrations() {
        return List.of();
    }

    @Override
    protected List<KeyValue<String, ExpressionsBasedModel.Integration<?>>> getLinearIntegrations() {
        return List.of(KeyValue.of("Gurobi", SolverGurobi.INTEGRATION));
    }

}
