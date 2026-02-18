package org.ojalgo.optimisation.solver.highs;

import java.util.List;

import org.ojalgo.optimisation.ExpressionsBasedModel.Integration;
import org.ojalgo.optimisation.integer.TestBasicMIP;

public class TestBasicMIPWithHiGHS extends TestBasicMIP {

    @Override
    protected List<Integration<?>> integrations() {
        return List.of(SolverHiGHS.INTEGRATION);
    }

}
