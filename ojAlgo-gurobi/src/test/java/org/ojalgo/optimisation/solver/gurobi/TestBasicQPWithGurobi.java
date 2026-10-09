package org.ojalgo.optimisation.solver.gurobi;

import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.ojalgo.optimisation.ExpressionsBasedModel.Integration;
import org.ojalgo.optimisation.convex.TestBasicQP;

@Disabled
public class TestBasicQPWithGurobi extends TestBasicQP {

    @Override
    protected List<Integration<?>> integrations() {
        return List.of(SolverGurobi.INTEGRATION);
    }

}
