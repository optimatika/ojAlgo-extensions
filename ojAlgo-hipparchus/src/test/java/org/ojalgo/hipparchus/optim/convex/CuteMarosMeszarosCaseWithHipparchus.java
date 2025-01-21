package org.ojalgo.hipparchus.optim.convex;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.convex.CuteMarosMeszarosCase;
import org.ojalgo.optimisation.solver.hipparchus.SolverHipparchus;

@Tag("slow")
public class CuteMarosMeszarosCaseWithHipparchus extends CuteMarosMeszarosCase {

    @BeforeEach
    void configure() {
        ExpressionsBasedModel.addIntegration(SolverHipparchus.INTEGRATION);
        // ExpressionsBasedModel.addIntegration(SimplexSolverImpl.INTEGRATION);
        // ExpressionsBasedModel.addIntegration(ADMMQPOptimizerImpl.INTEGRATION);
    }

    @AfterEach
    void reset() {
        ExpressionsBasedModel.clearIntegrations();
    }

}
