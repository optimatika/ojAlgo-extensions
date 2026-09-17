package org.ojalgo.optimisation.solver.choco;

import java.util.List;

import org.ojalgo.optimisation.ExpressionsBasedModel.Integration;
import org.ojalgo.optimisation.linear.TestBasicLP;

public class TestBasicLPWithChoco extends TestBasicLP {

    @Override
    protected List<Integration<?>> integrations() {
        return List.of(SolverChoco.INTEGRATION);
    }

}
