package org.ojalgo.optimisation.solver.cpsat;

import java.util.List;

import org.ojalgo.optimisation.ExpressionsBasedModel.Integration;
import org.ojalgo.optimisation.integer.TimeSufficeTest;

public class TimeSufficeTestWithCPSAT extends TimeSufficeTest {

    @Override
    protected List<Integration<?>> integrations() {
        return List.of(SolverCPSAT.INTEGRATION);
    }

}
