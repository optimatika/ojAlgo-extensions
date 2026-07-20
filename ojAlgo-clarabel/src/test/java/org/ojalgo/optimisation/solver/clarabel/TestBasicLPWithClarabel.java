package org.ojalgo.optimisation.solver.clarabel;

import java.util.List;

import org.ojalgo.optimisation.ExpressionsBasedModel.Integration;
import org.ojalgo.optimisation.linear.TestBasicLP;

public class TestBasicLPWithClarabel extends TestBasicLP {

    @Override
    protected List<Integration<?>> integrations() {
        return List.of(SolverClarabel.INTEGRATION);
    }

}
