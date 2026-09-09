package org.ojalgo.optimisation.solver.copt;

import java.util.List;

import org.ojalgo.optimisation.ExpressionsBasedModel.Integration;
import org.ojalgo.optimisation.integer.TestBasicMIP;

public class TestBasicMIPWithCOPT extends TestBasicMIP {

    @Override
    protected List<Integration<?>> integrations() {
        return List.of(SolverCOPT.INTEGRATION);
    }

}
