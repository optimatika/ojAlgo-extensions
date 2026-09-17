package org.ojalgo.optimisation.solver.mosek;

import java.util.List;

import org.ojalgo.optimisation.ExpressionsBasedModel.Integration;
import org.ojalgo.optimisation.integer.TestBasicMIP;

public class TestBasicMIPWithMosek extends TestBasicMIP {

    @Override
    protected List<Integration<?>> integrations() {
        return List.of(SolverMosek.INTEGRATION);
    }

}
