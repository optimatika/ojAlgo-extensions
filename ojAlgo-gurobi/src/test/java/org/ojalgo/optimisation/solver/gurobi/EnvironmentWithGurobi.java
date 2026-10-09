package org.ojalgo.optimisation.solver.gurobi;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.ojalgo.TestUtils;
import org.ojalgo.optimisation.ExpressionsBasedModel;

import com.gurobi.gurobi.GRB;
import com.gurobi.gurobi.GRBEnv;
import com.gurobi.gurobi.GRBException;

@Disabled
public class EnvironmentWithGurobi {

    /**
     * The environment is cached, and shared by all models built with the same integration. Limits set for one
     * model must not end up in it, and thereby in all later models.
     */
    @Test
    public void testModelSettingsDoNotLeak() throws GRBException {

        SolverGurobi.Integration integration = (SolverGurobi.Integration) SolverGurobi.INTEGRATION;

        ExpressionsBasedModel model = new ExpressionsBasedModel();
        model.addVariable("x").lower(0).upper(1).weight(1);
        model.options.iterations_abort = 10;
        model.options.time_abort = 10_000L;

        integration.build(model).dispose();

        GRBEnv environment = integration.getEnvironment();
        TestUtils.assertEquals(GRB.INFINITY, environment.get(GRB.DoubleParam.IterationLimit));
        TestUtils.assertEquals(GRB.INFINITY, environment.get(GRB.DoubleParam.TimeLimit));
    }

}
