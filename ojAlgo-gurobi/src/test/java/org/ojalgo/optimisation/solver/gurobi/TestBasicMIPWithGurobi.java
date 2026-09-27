package org.ojalgo.optimisation.solver.gurobi;

import java.util.List;

import org.ojalgo.optimisation.ExpressionsBasedModel.Integration;
import org.ojalgo.optimisation.integer.TestBasicMIP;

public class TestBasicMIPWithGurobi extends TestBasicMIP {

    @Override
    protected List<Integration<?>> integrations() {
        return List.of(SolverGurobi.INTEGRATION);
    }

}
