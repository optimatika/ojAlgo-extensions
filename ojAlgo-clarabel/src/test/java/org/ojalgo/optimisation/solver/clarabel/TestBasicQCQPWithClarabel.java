package org.ojalgo.optimisation.solver.clarabel;

import java.util.List;

import org.ojalgo.optimisation.ExpressionsBasedModel.Integration;
import org.ojalgo.optimisation.convex.TestBasicQCQP;

public class TestBasicQCQPWithClarabel extends TestBasicQCQP {

    @Override
    protected List<Integration<?>> integrations() {
        return List.of(SolverClarabel.INTEGRATION);
    }

}
