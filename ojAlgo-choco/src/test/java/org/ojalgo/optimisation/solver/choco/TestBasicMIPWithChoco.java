package org.ojalgo.optimisation.solver.choco;

import java.util.List;

import org.ojalgo.optimisation.ExpressionsBasedModel.Integration;
import org.ojalgo.optimisation.integer.TestBasicMIP;

public class TestBasicMIPWithChoco extends TestBasicMIP {

    @Override
    protected List<Integration<?>> integrations() {
        return List.of(SolverChoco.INTEGRATION);
    }

}
