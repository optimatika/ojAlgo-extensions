package org.ojalgo.optimisation.solver.gurobi;

import java.util.List;

import org.ojalgo.optimisation.ExpressionsBasedModel.Integration;
import org.ojalgo.optimisation.integer.TestBasicMIQP;

public class TestBasicMIQPWithGurobi extends TestBasicMIQP {

    @Override
    protected List<Integration<?>> integrations() {
        return List.of(SolverGurobi.INTEGRATION);
    }

}
