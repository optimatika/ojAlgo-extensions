package org.ojalgo.optimisation.solver.clarabel4j;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.convex.CuteMarosMeszarosCase;

public class CuteMarosMeszarosCaseWithClarabel4j extends CuteMarosMeszarosCase {

    @BeforeEach
    void configure() {
        ExpressionsBasedModel.addIntegration(SolverClarabel4j.INTEGRATION);
    }

    @AfterEach
    void reset() {
        ExpressionsBasedModel.clearIntegrations();
    }

}
