package org.ojalgo.optimisation.solver.scip;

import java.util.List;

import org.ojalgo.optimisation.ExpressionsBasedModel.Integration;
import org.ojalgo.optimisation.convex.TestBasicQCQP;

public class TestBasicQCQPWithSCIP extends TestBasicQCQP {

    @Override
    protected List<Integration<?>> integrations() {
        return List.of(SolverSCIP.INTEGRATION);
    }

}
