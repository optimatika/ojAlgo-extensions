package org.ojalgo.optimisation.solver.cplex;

import org.junit.jupiter.api.BeforeAll;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.linear.MeszarosCase;


public class MeszarosCaseWithCPLEX extends MeszarosCase {

    @BeforeAll
    public static void configure() {
        ExpressionsBasedModel.addIntegration(SolverCPLEX.INTEGRATION);
    }

}
