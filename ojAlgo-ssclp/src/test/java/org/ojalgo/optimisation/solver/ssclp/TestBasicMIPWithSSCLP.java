package org.ojalgo.optimisation.solver.ssclp;

import java.util.List;

import org.ojalgo.optimisation.ExpressionsBasedModel.Integration;
import org.ojalgo.optimisation.integer.TestBasicMIP;

public class TestBasicMIPWithSSCLP extends TestBasicMIP {

    @Override
    protected List<Integration<?>> integrations() {
        return List.of(SolverSSCLP.INTEGRATION);
    }

}
