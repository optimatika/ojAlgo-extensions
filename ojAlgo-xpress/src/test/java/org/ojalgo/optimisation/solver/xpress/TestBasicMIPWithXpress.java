package org.ojalgo.optimisation.solver.xpress;

import java.util.List;

import org.ojalgo.optimisation.ExpressionsBasedModel.Integration;
import org.ojalgo.optimisation.integer.TestBasicMIP;

public class TestBasicMIPWithXpress extends TestBasicMIP {

    @Override
    protected List<Integration<?>> integrations() {
        return List.of(SolverXpress.INTEGRATION);
    }

}
