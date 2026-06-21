package org.ojalgo.optimisation.solver.ortools;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.ojalgo.optimisation.ExpressionsBasedModel.Integration;
import org.ojalgo.optimisation.linear.TestBasicLP;

public class TestBasicLPWithORTools extends TestBasicLP {

    /**
     * GLOP's presolve reports INFEASIBLE_OR_UNBOUNDED for unbounded problems, and the ModelBuilder API maps
     * that to INFEASIBLE (there is no INFEASIBLE_OR_UNBOUNDED in the SolveStatus enum).
     */
    @Override
    @Test
    public void testUnboundedLP() {
        // GLOP cannot distinguish unbounded from infeasible
    }

    @Override
    protected List<Integration<?>> integrations() {
        return List.of(SolverORTools.INTEGRATION);
    }

}
