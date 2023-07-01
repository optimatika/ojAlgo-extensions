package org.ojalgo.optimisation.solver.ortools;

import org.junit.jupiter.api.BeforeAll;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.integer.GeneratedCutTest;

public class GeneratedCutTestWithOrTools extends GeneratedCutTest {

    @BeforeAll
    public static void configure() {
        ExpressionsBasedModel.addIntegration(SolverORTools.INTEGRATION);
    }
}
