package org.ojalgo.optimisation.solver.highs;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.convex.CuteMarosMeszarosCase;

@Disabled
public class CuteMarosMeszarosCaseWithHiGHS extends CuteMarosMeszarosCase {

    @Override
    @Test
    @Tag("unstable")
    public void testHUES_MOD() {
        super.testHUES_MOD();
    }

    @Override
    @Test
    @Tag("unstable")
    public void testHUESTIS() {
        super.testHUESTIS();
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
    public void testLASER() {
        super.testLASER();
    }

    @Override
    @Test
    @Tag("unstable")
    public void testMOSARQP1() {
        super.testMOSARQP1();
    }

    @Override
    @Test
    @Tag("unstable")
    public void testMOSARQP2() {
        super.testMOSARQP2();
    }

    @Override
    @Test
    @Tag("unstable")
    public void testQPCSTAIR() {
        super.testQPCSTAIR();
    }

    @BeforeEach
    void configure() {
        ExpressionsBasedModel.addIntegration(SolverHiGHS.INTEGRATION);
    }

    @AfterEach
    void reset() {
        ExpressionsBasedModel.clearIntegrations();
    }

}
