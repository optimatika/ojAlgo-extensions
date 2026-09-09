package org.ojalgo.optimisation.solver.copt;

import java.util.List;

import org.ojalgo.optimisation.ExpressionsBasedModel.Integration;
import org.ojalgo.optimisation.linear.IntegrationUpdatableSolverTest;

public class IntegrationUpdatableSolverTestWithCOPT extends IntegrationUpdatableSolverTest {

    @Override
    protected List<Integration<?>> integrations() {
        return List.of(SolverCOPT.INTEGRATION);
    }

}
