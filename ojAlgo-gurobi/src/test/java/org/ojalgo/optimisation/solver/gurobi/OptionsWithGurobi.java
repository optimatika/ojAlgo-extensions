package org.ojalgo.optimisation.solver.gurobi;

import org.junit.jupiter.api.Test;
import org.ojalgo.TestUtils;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.type.context.NumberContext;

import com.gurobi.gurobi.GRB;
import com.gurobi.gurobi.GRBException;
import com.gurobi.gurobi.GRBModel;

/**
 * ojAlgo's options are passed on to Gurobi only when configured to be something other than the defaults.
 * Otherwise Gurobi keeps its own defaults.
 */
public class OptionsWithGurobi {

    private static ExpressionsBasedModel newModel() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        model.addVariable("x").lower(0).upper(1).weight(1);
        return model;
    }

    @Test
    public void testConfiguredFeasibility() throws GRBException {

        ExpressionsBasedModel model = OptionsWithGurobi.newModel();
        model.options.feasibility = NumberContext.of(8, 7);

        double expected = model.options.feasibility.getAbsoluteError();

        SolverGurobi solver = SolverGurobi.INTEGRATION.build(model);
        try {
            GRBModel delegate = solver.getGRBModel();
            TestUtils.assertEquals(expected, delegate.get(GRB.DoubleParam.FeasibilityTol));
            TestUtils.assertEquals(expected, delegate.get(GRB.DoubleParam.IntFeasTol));
        } finally {
            solver.dispose();
        }
    }

    @Test
    public void testDefaultFeasibility() throws GRBException {

        ExpressionsBasedModel model = OptionsWithGurobi.newModel();

        SolverGurobi solver = SolverGurobi.INTEGRATION.build(model);
        try {
            GRBModel delegate = solver.getGRBModel();
            TestUtils.assertEquals(1E-6, delegate.get(GRB.DoubleParam.FeasibilityTol));
            TestUtils.assertEquals(1E-5, delegate.get(GRB.DoubleParam.IntFeasTol));
        } finally {
            solver.dispose();
        }
    }

}
