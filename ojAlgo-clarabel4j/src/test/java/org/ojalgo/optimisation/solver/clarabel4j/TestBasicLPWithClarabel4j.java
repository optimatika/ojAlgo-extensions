package org.ojalgo.optimisation.solver.clarabel4j;

import java.util.List;

import org.ojalgo.optimisation.ExpressionsBasedModel.Integration;
import org.ojalgo.optimisation.linear.TestBasicLP;

public class TestBasicLPWithClarabel4j extends TestBasicLP {

    @Override
    protected List<Integration<?>> integrations() {
        return List.of(SolverClarabel4j.INTEGRATION);
    }

}
