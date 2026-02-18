package org.ojalgo.optimisation.solver.clarabel4j;

import java.util.List;

import org.ojalgo.optimisation.ExpressionsBasedModel.Integration;
import org.ojalgo.optimisation.convex.TestBasicQP;

public class TestBasicQPWithClarabel4j extends TestBasicQP {

    @Override
    protected List<Integration<?>> integrations() {
        return List.of(SolverClarabel4j.INTEGRATION);
    }

}
