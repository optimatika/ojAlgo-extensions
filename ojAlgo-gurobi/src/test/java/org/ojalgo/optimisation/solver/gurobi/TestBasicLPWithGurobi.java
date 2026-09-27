package org.ojalgo.optimisation.solver.gurobi;

import java.util.List;

import org.ojalgo.optimisation.ExpressionsBasedModel.Integration;
import org.ojalgo.optimisation.linear.TestBasicLP;

public class TestBasicLPWithGurobi extends TestBasicLP {

    @Override
    protected List<Integration<?>> integrations() {
        return List.of(SolverGurobi.INTEGRATION);
    }

}
