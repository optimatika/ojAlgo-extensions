package org.ojalgo.optimisation.solver.ortools;

import org.junit.jupiter.api.BeforeAll;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.linear.MeszarosCase;


public class MeszarosCaseWithORTools extends MeszarosCase {

    @BeforeAll
    public static void configure() {
        ExpressionsBasedModel.addIntegration(SolverORTools.INTEGRATION);
    }

}
