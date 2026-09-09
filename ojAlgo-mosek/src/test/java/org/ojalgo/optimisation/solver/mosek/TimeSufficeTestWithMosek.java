package org.ojalgo.optimisation.solver.mosek;

import java.util.List;

import org.ojalgo.optimisation.ExpressionsBasedModel.Integration;
import org.ojalgo.optimisation.integer.TimeSufficeTest;

public class TimeSufficeTestWithMosek extends TimeSufficeTest {

    @Override
    protected List<Integration<?>> integrations() {
        return List.of(SolverMosek.INTEGRATION);
    }

}
