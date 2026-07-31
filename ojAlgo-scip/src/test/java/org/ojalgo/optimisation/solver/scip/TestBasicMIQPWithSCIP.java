package org.ojalgo.optimisation.solver.scip;

import java.util.List;

import org.ojalgo.optimisation.ExpressionsBasedModel.Integration;
import org.ojalgo.optimisation.integer.TestBasicMIQP;

public class TestBasicMIQPWithSCIP extends TestBasicMIQP {

    @Override
    protected List<Integration<?>> integrations() {
        return List.of(SolverSCIP.INTEGRATION);
    }

}
