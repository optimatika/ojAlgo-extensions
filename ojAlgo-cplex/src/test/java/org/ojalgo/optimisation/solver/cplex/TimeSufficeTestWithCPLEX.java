package org.ojalgo.optimisation.solver.cplex;

import java.util.List;

import org.ojalgo.optimisation.ExpressionsBasedModel.Integration;
import org.ojalgo.optimisation.integer.TimeSufficeTest;

public class TimeSufficeTestWithCPLEX extends TimeSufficeTest {

    @Override
    protected List<Integration<?>> integrations() {
        return List.of(SolverCPLEX.INTEGRATION);
    }

}
