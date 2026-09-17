package org.ojalgo.optimisation.solver.mosek;

import java.util.List;

import org.ojalgo.optimisation.ExpressionsBasedModel.Integration;
import org.ojalgo.optimisation.linear.TestBasicLP;

public class TestBasicLPWithMosek extends TestBasicLP {

    @Override
    protected List<Integration<?>> integrations() {
        return List.of(SolverMosek.INTEGRATION);
    }

}
