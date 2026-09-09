package org.ojalgo.optimisation.solver.cplex;

import java.util.List;

import org.ojalgo.optimisation.ExpressionsBasedModel.Integration;
import org.ojalgo.optimisation.linear.IntegrationUpdatableSolverTest;

public class IntegrationUpdatableSolverTestWithCPLEX extends IntegrationUpdatableSolverTest {

    @Override
    protected List<Integration<?>> integrations() {
        return List.of(SolverCPLEX.INTEGRATION);
    }

}
