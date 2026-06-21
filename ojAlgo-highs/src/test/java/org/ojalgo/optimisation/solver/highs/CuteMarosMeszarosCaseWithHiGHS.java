package org.ojalgo.optimisation.solver.highs;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.convex.CuteMarosMeszarosCase;

public class CuteMarosMeszarosCaseWithHiGHS extends CuteMarosMeszarosCase {

    @BeforeEach
    void configure() {
        ExpressionsBasedModel.addIntegration(SolverHiGHS.INTEGRATION);
    }

    @Override
    @Test
    @Tag("unstable")
    public void testKSIP() {
        super.testKSIP();
    }

    @Override
    @Test
    @Tag("unstable")
    public void testQPCSTAIR() {
        super.testQPCSTAIR();
    }

    @AfterEach
    void reset() {
        ExpressionsBasedModel.clearIntegrations();
    }

}
