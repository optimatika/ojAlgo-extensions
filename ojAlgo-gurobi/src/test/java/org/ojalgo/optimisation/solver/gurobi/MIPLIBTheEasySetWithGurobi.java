package org.ojalgo.optimisation.solver.gurobi;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.integer.MIPLIBTheEasySet;

@Disabled
public class MIPLIBTheEasySetWithGurobi extends MIPLIBTheEasySet {

    @BeforeEach
    void configure() {
        ExpressionsBasedModel.addIntegration(SolverGurobi.INTEGRATION);
    }

    @AfterEach
    void reset() {
        ExpressionsBasedModel.clearIntegrations();
    }

}
