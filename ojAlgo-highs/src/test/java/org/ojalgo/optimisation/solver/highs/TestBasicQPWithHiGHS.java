package org.ojalgo.optimisation.solver.highs;

import java.util.List;

import org.ojalgo.optimisation.ExpressionsBasedModel.Integration;
import org.ojalgo.optimisation.convex.TestBasicQP;

public class TestBasicQPWithHiGHS extends TestBasicQP {

    @Override
    protected List<Integration<?>> integrations() {
        return List.of(SolverHiGHS.INTEGRATION);
    }

}
