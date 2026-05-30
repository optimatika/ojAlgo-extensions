package org.ojalgo.optimisation.solver.ssclp;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.integer.MIPLIBTheEasySet;

@Disabled
public class MIPLIBTheEasySetWithSSCLP extends MIPLIBTheEasySet {

    @BeforeEach
    void configure() {
        ExpressionsBasedModel.addIntegration(SolverSSCLP.INTEGRATION);
    }

    @AfterEach
    void reset() {
        ExpressionsBasedModel.clearIntegrations();
    }

}
