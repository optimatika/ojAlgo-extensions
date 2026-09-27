package org.ojalgo.optimisation.solver.gurobi;

import org.junit.jupiter.api.Test;
import org.ojalgo.TestUtils;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.IntermediateSolver;
import org.ojalgo.optimisation.Optimisation;
import org.ojalgo.type.context.NumberContext;

import com.gurobi.gurobi.GRB;
import com.gurobi.gurobi.GRBException;
import com.gurobi.gurobi.GRBModel;

/**
 * A feasible solution supplied to start from is Gurobi's MIP start. With a solution limit of 1 Gurobi stops at
 * the first solution it has, and that is the MIP start when there is one.
 */
public class MIPStartWithGurobi {

    private static final NumberContext ACCURACY = NumberContext.of(8);

    private static final SolverGurobi.Configurator FIRST_SOLUTION = new SolverGurobi.Configurator() {

        @Override
        protected void configure(final GRBModel model, final Optimisation.Options options) {
            super.configure(model, options);
            try {
                model.set(GRB.IntParam.SolutionLimit, 1);
            } catch (GRBException cause) {
                throw new RuntimeException(cause);
            }
        }

    };

    /**
     * The market split model, with slack, in an environment where Gurobi stops at its first solution.
     */
    private static ExpressionsBasedModel newFirstSolutionModel() {
        Optimisation.Environment environment = Optimisation.newEnvironment();
        environment.setConfigurator(FIRST_SOLUTION);
        return TimeSufficeTestWithGurobi.newMarketSplit(environment, true);
    }

    private static Optimisation.Result solve(final ExpressionsBasedModel model, final Optimisation.Result start) {

        IntermediateSolver solver = model.prepare(Optimisation.Sense.MIN, prepared -> new IntermediateSolver(prepared) {

            @Override
            protected ExpressionsBasedModel.Integration<?> getIntegration() {
                return SolverGurobi.INTEGRATION;
            }

        });

        return solver.solve(start);
    }

    @Test
    public void testStartIsUsed() {

        ExpressionsBasedModel reference = TimeSufficeTestWithGurobi.newMarketSplit(true);
        reference.options.time_suffice = 2_000L;
        reference.options.time_abort = 60_000L;
        Optimisation.Result start = reference.minimise(SolverGurobi.INTEGRATION);

        TestUtils.assertStateNotLessThanFeasible(start);

        ExpressionsBasedModel withStart = MIPStartWithGurobi.newFirstSolutionModel();
        Optimisation.Result result = MIPStartWithGurobi.solve(withStart, start);

        TestUtils.assertEquals(Optimisation.State.FEASIBLE, result.getState());
        TestUtils.assertTrue(withStart.validate(result));
        TestUtils.assertEquals(start.getValue(), result.getValue(), ACCURACY);

        ExpressionsBasedModel withoutStart = MIPStartWithGurobi.newFirstSolutionModel();
        Optimisation.Result own = MIPStartWithGurobi.solve(withoutStart, null);

        TestUtils.assertEquals(Optimisation.State.FEASIBLE, own.getState());
        TestUtils.assertTrue("Gurobi's own first solution should be worse than the start: " + own.getValue() + " vs " + start.getValue(),
                own.getValue() > start.getValue() + ACCURACY.epsilon());
    }

}
