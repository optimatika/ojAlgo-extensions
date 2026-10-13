package org.ojalgo.optimisation.solver.choco;

import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.ojalgo.optimisation.ExpressionsBasedModel.Integration;
import org.ojalgo.optimisation.linear.TestBasicLP;

public class TestBasicLPWithChoco extends TestBasicLP {

    @Override
    @Test
    @Disabled("The objective is modelled as a scaled integer variable with a limited range - here it stops at -2147.48")
    public void testLargeMagnitudeSolution() {
        super.testLargeMagnitudeSolution();
    }

    @Override
    @Test
    @Disabled("The continuous variables are not solved accurately enough - the solution is only correct to about 5 digits")
    public void testRedundantEqualities() {
        super.testRedundantEqualities();
    }

    @Override
    protected List<Integration<?>> integrations() {
        return List.of(SolverChoco.INTEGRATION);
    }

}
