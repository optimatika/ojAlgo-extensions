package org.ojalgo.optimisation.solver.gurobi;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.linear.CuteNetlibCase;

public class CuteNetlibCaseWithGurobi extends CuteNetlibCase {

    @BeforeEach
    void configure() {
        ExpressionsBasedModel.addIntegration(SolverGurobi.INTEGRATION);
    }

    @AfterEach
    void reset() {
        ExpressionsBasedModel.clearIntegrations();
    }

}
