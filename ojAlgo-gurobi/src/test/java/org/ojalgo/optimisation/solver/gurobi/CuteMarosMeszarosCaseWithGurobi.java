package org.ojalgo.optimisation.solver.gurobi;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.convex.CuteMarosMeszarosCase;

public class CuteMarosMeszarosCaseWithGurobi extends CuteMarosMeszarosCase {

    /**
     * Gurobi does not solve VALUES in reasonable time (timed out after 5 minutes in the Maros-Meszaros
     * benchmark).
     */
    @Override
    @Test
    @Tag("slow")
    @Tag("unstable")
    public void testVALUES() {
        super.testVALUES();
    }

    @BeforeEach
    void configure() {
        ExpressionsBasedModel.addIntegration(SolverGurobi.INTEGRATION);
    }

    @AfterEach
    void reset() {
        ExpressionsBasedModel.clearIntegrations();
    }

}
