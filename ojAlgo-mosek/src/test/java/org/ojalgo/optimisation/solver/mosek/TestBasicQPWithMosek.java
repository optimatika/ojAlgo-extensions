package org.ojalgo.optimisation.solver.mosek;

import java.util.List;

import org.ojalgo.optimisation.ExpressionsBasedModel.Integration;
import org.ojalgo.optimisation.convex.TestBasicQP;

public class TestBasicQPWithMosek extends TestBasicQP {

    @Override
    protected List<Integration<?>> integrations() {
        return List.of(SolverMosek.INTEGRATION);
    }

}
