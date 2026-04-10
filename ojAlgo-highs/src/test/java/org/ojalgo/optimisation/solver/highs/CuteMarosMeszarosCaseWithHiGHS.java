package org.ojalgo.optimisation.solver.highs;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.convex.CuteMarosMeszarosCase;

public class CuteMarosMeszarosCaseWithHiGHS extends CuteMarosMeszarosCase {

    @BeforeEach
    void configure() {
        ExpressionsBasedModel.addIntegration(SolverHiGHS.INTEGRATION);
    }

    @AfterEach
    void reset() {
        ExpressionsBasedModel.clearIntegrations();
    }

}
