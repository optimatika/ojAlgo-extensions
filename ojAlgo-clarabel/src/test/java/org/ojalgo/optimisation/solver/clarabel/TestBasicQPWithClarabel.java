package org.ojalgo.optimisation.solver.clarabel;

import java.util.List;

import org.ojalgo.optimisation.ExpressionsBasedModel.Integration;
import org.ojalgo.optimisation.convex.TestBasicQP;

public class TestBasicQPWithClarabel extends TestBasicQP {

    @Override
    protected List<Integration<?>> integrations() {
        return List.of(SolverClarabel.INTEGRATION);
    }

}
