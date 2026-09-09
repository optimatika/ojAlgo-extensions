package org.ojalgo.optimisation.solver.osqp;

import java.util.List;

import org.ojalgo.optimisation.ExpressionsBasedModel.Integration;
import org.ojalgo.optimisation.convex.TestBasicQP;

public class TestBasicQPWithOSQP extends TestBasicQP {

    @Override
    protected List<Integration<?>> integrations() {
        return List.of(SolverOSQP.INTEGRATION);
    }

}
