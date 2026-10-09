package org.ojalgo.optimisation.solver.gurobi;

import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.ojalgo.optimisation.ExpressionsBasedModel.Integration;
import org.ojalgo.optimisation.integer.TestBasicMIP;

@Disabled
public class TestBasicMIPWithGurobi extends TestBasicMIP {

    @Override
    protected List<Integration<?>> integrations() {
        return List.of(SolverGurobi.INTEGRATION);
    }

}
