package com.ustermetrics.clarabel4j;

import org.junit.jupiter.api.BeforeAll;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.convex.CuteMarosMeszarosCase;
import org.ojalgo.optimisation.solver.clarabel4j.SolverClarabel4j;

public class CuteMarosMeszarosCaseWithClarabel4j extends CuteMarosMeszarosCase {

    @BeforeAll
    public static void configure() {
        ExpressionsBasedModel.addIntegration(SolverClarabel4j.INTEGRATION);
    }

}
