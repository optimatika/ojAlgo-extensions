package org.ojalgo.optimisation.solver.scip;

import java.util.List;

import org.ojalgo.optimisation.ExpressionsBasedModel.Integration;
import org.ojalgo.optimisation.linear.TestBasicLP;

public class TestBasicLPWithSCIP extends TestBasicLP {

    @Override
    protected List<Integration<?>> integrations() {
        return List.of(SolverSCIP.INTEGRATION);
    }

}
