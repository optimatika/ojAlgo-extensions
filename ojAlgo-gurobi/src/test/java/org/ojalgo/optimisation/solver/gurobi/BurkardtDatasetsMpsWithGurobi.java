package org.ojalgo.optimisation.solver.gurobi;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.linear.BurkardtDatasetsMps;

@Disabled
public class BurkardtDatasetsMpsWithGurobi extends BurkardtDatasetsMps {

    @BeforeEach
    void configure() {
        ExpressionsBasedModel.addIntegration(SolverGurobi.INTEGRATION);
    }

    @AfterEach
    void reset() {
        ExpressionsBasedModel.clearIntegrations();
    }

}
