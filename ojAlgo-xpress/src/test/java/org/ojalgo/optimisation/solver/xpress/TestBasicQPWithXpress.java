package org.ojalgo.optimisation.solver.xpress;

import java.util.List;

import org.ojalgo.optimisation.ExpressionsBasedModel.Integration;
import org.ojalgo.optimisation.convex.TestBasicQP;

public class TestBasicQPWithXpress extends TestBasicQP {

    @Override
    protected List<Integration<?>> integrations() {
        return List.of(SolverXpress.INTEGRATION);
    }

}
