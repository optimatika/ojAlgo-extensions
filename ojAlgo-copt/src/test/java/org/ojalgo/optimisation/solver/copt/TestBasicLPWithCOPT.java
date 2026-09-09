package org.ojalgo.optimisation.solver.copt;

import java.util.List;

import org.ojalgo.optimisation.ExpressionsBasedModel.Integration;
import org.ojalgo.optimisation.linear.TestBasicLP;

public class TestBasicLPWithCOPT extends TestBasicLP {

    @Override
    protected List<Integration<?>> integrations() {
        return List.of(SolverCOPT.INTEGRATION);
    }

}
