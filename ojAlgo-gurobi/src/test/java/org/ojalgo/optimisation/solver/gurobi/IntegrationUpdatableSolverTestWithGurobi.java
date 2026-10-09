package org.ojalgo.optimisation.solver.gurobi;

import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.ojalgo.optimisation.ExpressionsBasedModel.Integration;
import org.ojalgo.optimisation.linear.IntegrationUpdatableSolverTest;

@Disabled
public class IntegrationUpdatableSolverTestWithGurobi extends IntegrationUpdatableSolverTest {

    @Override
    protected List<Integration<?>> integrations() {
        return List.of(SolverGurobi.INTEGRATION);
    }

}
