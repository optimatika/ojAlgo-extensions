package org.ojalgo.optimisation.solver.xpress;

import java.util.List;

import org.ojalgo.optimisation.ExpressionsBasedModel.Integration;
import org.ojalgo.optimisation.linear.IntegrationUpdatableSolverTest;

public class IntegrationUpdatableSolverTestWithXpress extends IntegrationUpdatableSolverTest {

    @Override
    protected List<Integration<?>> integrations() {
        return List.of(SolverXpress.INTEGRATION);
    }

}
