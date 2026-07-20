package org.ojalgo.optimisation.solver.scip;

import java.util.List;

import org.ojalgo.optimisation.ExpressionsBasedModel.Integration;
import org.ojalgo.optimisation.integer.TestBasicMIP;

public class TestBasicMIPWithSCIP extends TestBasicMIP {

    @Override
    protected List<Integration<?>> integrations() {
        return List.of(SolverSCIP.INTEGRATION);
    }

}
