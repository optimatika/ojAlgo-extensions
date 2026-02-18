package org.ojalgo.optimisation.solver.cplex;

import java.util.List;

import org.ojalgo.optimisation.ExpressionsBasedModel.Integration;
import org.ojalgo.optimisation.convex.TestBasicQP;

public class TestBasicQPWithCPLEX extends TestBasicQP {

    @Override
    protected List<Integration<?>> integrations() {
        return List.of(SolverCPLEX.INTEGRATION);
    }

}
