package org.ojalgo.hipparchus.optim.convex;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.ojalgo.data.domain.finance.portfolio.PortfolioProblems;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.solver.hipparchus.ADMMQPOptimizerImpl;


public class PortfolioProblemsWithHipparchus extends PortfolioProblems {

    @BeforeEach
    void configure() {
        // ExpressionsBasedModel.addIntegration(SolverHipparchus.INTEGRATION);
        // ExpressionsBasedModel.addIntegration(SimplexSolverImpl.INTEGRATION);
        ExpressionsBasedModel.addIntegration(ADMMQPOptimizerImpl.INTEGRATION);
    }

    @AfterEach
    void reset() {
        ExpressionsBasedModel.clearIntegrations();
    }

}
