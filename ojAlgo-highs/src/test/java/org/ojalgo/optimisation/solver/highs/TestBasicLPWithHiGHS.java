package org.ojalgo.optimisation.solver.highs;

import java.util.List;

import org.ojalgo.optimisation.ExpressionsBasedModel.Integration;
import org.ojalgo.optimisation.linear.TestBasicLP;

public class TestBasicLPWithHiGHS extends TestBasicLP {

    @Override
    protected List<Integration<?>> integrations() {
        return List.of(SolverHiGHS.INTEGRATION);
    }

}
