package org.ojalgo.optimisation.solver.copt;

import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;
import org.ojalgo.TestUtils;
import org.ojalgo.optimisation.Expression;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.ExpressionsBasedModel.Integration;
import org.ojalgo.optimisation.Optimisation;
import org.ojalgo.optimisation.Variable;
import org.ojalgo.optimisation.integer.TimeSufficeTest;

import copt.CoptException;
import copt.DblParam;
import copt.IntParam;

public class TimeSufficeTestWithCOPT extends TimeSufficeTest {

    /**
     * Market split: equality constraints on binary variables, with a right-hand side of half the coefficient
     * sum. Infeasible, and hard enough to prove so that branch-and-bound is still working on it long after the
     * test is done.
     * <p>
     * With slack, the total deviation from the equalities is minimised instead. All zeros is then feasible, so
     * there is an incumbent right away, but proving optimality is just as hard.
     */
    private static ExpressionsBasedModel newMarketSplit(final boolean slack) {

        ExpressionsBasedModel model = new ExpressionsBasedModel();

        Variable[] variables = new Variable[50];
        for (int j = 0; j < variables.length; j++) {
            variables[j] = model.addVariable("x" + j).binary().weight(slack ? 0.0 : 1.0);
        }

        Random random = new Random(1234L);

        for (int i = 0; i < 6; i++) {

            Expression constraint = model.addExpression("split" + i);
            int sum = 0;

            for (Variable variable : variables) {
                int coefficient = random.nextInt(100);
                constraint.set(variable, coefficient);
                sum += coefficient;
            }

            if (slack) {
                constraint.set(model.addVariable("surplus" + i).lower(0).weight(1.0), -1);
                constraint.set(model.addVariable("shortage" + i).lower(0).weight(1.0), 1);
            }

            constraint.level(sum / 2);
        }

        return model;
    }

    /**
     * When time_suffice is reached with an incumbent, that incumbent is the result - COPT reports the time
     * limit, not a feasible state, so this must not be mistaken for having no solution (and then continuing
     * until time_abort).
     */
    @Test
    public void testIncumbentAtTimeSuffice() {

        ExpressionsBasedModel model = TimeSufficeTestWithCOPT.newMarketSplit(true);

        model.options.time_suffice = 1_000L;
        model.options.time_abort = 30_000L;

        long before = System.currentTimeMillis();
        Optimisation.Result result = model.minimise(SolverCOPT.INTEGRATION);
        long elapsed = System.currentTimeMillis() - before;

        TestUtils.assertEquals(Optimisation.State.FEASIBLE, result.getState());
        TestUtils.assertTrue(model.validate(result));
        TestUtils.assertTrue("Solve took " + elapsed + "ms, should stop soon after time_suffice", elapsed < model.options.time_abort / 2L);
    }

    /**
     * Without an incumbent at time_suffice, the solver continues until time_abort - with limits that must not
     * remain for the next solve.
     */
    @Test
    public void testLimitsRestoredAfterContinuing() throws CoptException {

        ExpressionsBasedModel model = TimeSufficeTestWithCOPT.newMarketSplit(false);

        model.options.time_suffice = 1_000L;
        model.options.time_abort = 3_000L;
        model.options.iterations_suffice = 1_000_000;
        model.options.iterations_abort = 2_000_000;

        SolverCOPT solver = SolverCOPT.INTEGRATION.build(model);
        try {

            copt.Model delegate = solver.getCOPTModel();

            long before = System.currentTimeMillis();
            Optimisation.Result result = solver.solve(null);
            long elapsed = System.currentTimeMillis() - before;

            TestUtils.assertEquals(Optimisation.State.APPROXIMATE, result.getState());
            TestUtils.assertTrue("Solve took " + elapsed + "ms, should have continued after time_suffice", elapsed > 2_000L);

            TestUtils.assertEquals(1.0, delegate.getDblParam(DblParam.TimeLimit));
            TestUtils.assertEquals(1_000_000, delegate.getIntParam(IntParam.BarIterLimit));

        } finally {
            solver.dispose();
        }
    }

    @Override
    protected List<Integration<?>> integrations() {
        return List.of(SolverCOPT.INTEGRATION);
    }

}
