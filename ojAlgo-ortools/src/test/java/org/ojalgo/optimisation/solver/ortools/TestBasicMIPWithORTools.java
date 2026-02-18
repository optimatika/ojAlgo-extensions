package org.ojalgo.optimisation.solver.ortools;

import java.util.List;

import org.ojalgo.optimisation.ExpressionsBasedModel.Integration;
import org.ojalgo.optimisation.integer.TestBasicMIP;

public class TestBasicMIPWithORTools extends TestBasicMIP {

    @Override
    protected List<Integration<?>> integrations() {
        return List.of(SolverORTools.INTEGRATION);
    }

}
