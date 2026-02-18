package org.ojalgo.optimisation.solver.ortools;

import java.util.List;

import org.ojalgo.optimisation.ExpressionsBasedModel.Integration;
import org.ojalgo.optimisation.linear.TestBasicLP;

public class TestBasicLPWithORTools extends TestBasicLP {

    @Override
    protected List<Integration<?>> integrations() {
        return List.of(SolverORTools.INTEGRATION);
    }

}
