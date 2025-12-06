package com.ustermetrics.clarabel4j;

import org.junit.jupiter.api.BeforeAll;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.linear.LinearDesignTestCases;
import org.ojalgo.optimisation.solver.clarabel4j.SolverClarabel4j;

public class LinearDesignTestCasesWithClarabel4j extends LinearDesignTestCases {

    @BeforeAll
    public static void configure() {
        ExpressionsBasedModel.addIntegration(SolverClarabel4j.INTEGRATION);
    }

}