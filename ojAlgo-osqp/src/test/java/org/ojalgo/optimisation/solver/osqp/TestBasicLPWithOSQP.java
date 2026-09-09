package org.ojalgo.optimisation.solver.osqp;

import java.util.List;

import org.ojalgo.optimisation.ExpressionsBasedModel.Integration;
import org.ojalgo.optimisation.linear.TestBasicLP;

public class TestBasicLPWithOSQP extends TestBasicLP {

    @Override
    protected List<Integration<?>> integrations() {
        return List.of(SolverOSQP.INTEGRATION);
    }

}
