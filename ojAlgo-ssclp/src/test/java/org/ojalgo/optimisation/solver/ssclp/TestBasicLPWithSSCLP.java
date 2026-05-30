package org.ojalgo.optimisation.solver.ssclp;

import java.util.List;

import org.ojalgo.optimisation.ExpressionsBasedModel.Integration;
import org.ojalgo.optimisation.linear.TestBasicLP;

public class TestBasicLPWithSSCLP extends TestBasicLP {

    @Override
    protected List<Integration<?>> integrations() {
        return List.of(SolverSSCLP.INTEGRATION);
    }

}
