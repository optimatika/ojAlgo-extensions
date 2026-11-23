package org.ojalgo.optimisation.solver.hipparchus;

import java.util.List;

import org.ojalgo.optimisation.ExpressionsBasedModel.Integration;
import org.ojalgo.optimisation.convex.TestBasicQCQP;

/**
 * Quadratic constraints are handled by {@link SQPOptimizerImplS2}, also when solving with
 * {@link SolverHipparchus}.
 */
public class TestBasicQCQPWithHipparchus extends TestBasicQCQP {

    @Override
    protected List<Integration<?>> integrations() {
        return List.of(SQPOptimizerImplS2.INTEGRATION, SolverHipparchus.INTEGRATION);
    }

}
