package org.ojalgo.optimisation.solver.highs;

import java.util.List;

import org.ojalgo.optimisation.ExpressionsBasedModel.Integration;
import org.ojalgo.optimisation.linear.IntegrationUpdatableSolverTest;

public class IntegrationUpdatableSolverTestWithHiGHS extends IntegrationUpdatableSolverTest {

    @Override
    protected List<Integration<?>> integrations() {
        return List.of(SolverHiGHS.INTEGRATION);
    }

}
