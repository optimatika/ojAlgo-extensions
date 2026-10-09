package org.ojalgo.optimisation.solver.gurobi;

import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.ojalgo.optimisation.ExpressionsBasedModel.Integration;
import org.ojalgo.optimisation.linear.TestBasicLP;

@Disabled
public class TestBasicLPWithGurobi extends TestBasicLP {

    @Override
    protected List<Integration<?>> integrations() {
        return List.of(SolverGurobi.INTEGRATION);
    }

}
