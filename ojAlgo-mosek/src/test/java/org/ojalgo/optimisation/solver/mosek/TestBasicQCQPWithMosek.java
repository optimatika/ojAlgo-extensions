package org.ojalgo.optimisation.solver.mosek;

import java.util.List;

import org.ojalgo.optimisation.ExpressionsBasedModel.Integration;
import org.ojalgo.optimisation.convex.TestBasicQCQP;

public class TestBasicQCQPWithMosek extends TestBasicQCQP {

    @Override
    protected List<Integration<?>> integrations() {
        return List.of(SolverMosek.INTEGRATION);
    }

}
