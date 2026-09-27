package org.ojalgo.optimisation.solver.copt;

import org.junit.jupiter.api.Test;
import org.ojalgo.TestUtils;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.Optimisation;
import org.ojalgo.optimisation.Variable;
import org.ojalgo.type.context.NumberContext;

/**
 * The solver may only conclude that a model is infeasible or unbounded, and its unbounded status does not come
 * with a solution. The integration should find out which, and for an unbounded model return a feasible
 * solution. The models are solved directly with the solver, as ojAlgo's own presolve would
 * otherwise catch some of them first.
 */
public class InfeasibleOrUnboundedWithCOPT {

    /**
     * Infeasible: x + y >= 10 and x + y <= 5. There is also a variable, z, that could improve the objective
     * without limit.
     */
    private static ExpressionsBasedModel newInfeasibleWithUnboundedRay(final boolean integer) {

        ExpressionsBasedModel model = new ExpressionsBasedModel();

        Variable x = model.addVariable("x").lower(0.0).weight(1.0).integer(integer);
        Variable y = model.addVariable("y").lower(0.0).integer(integer);
        model.addVariable("z").lower(0.0).weight(-1.0).integer(integer);

        model.addExpression("c1").set(x, 1.0).set(y, 1.0).lower(10.0);
        model.addExpression("c2").set(x, 1.0).set(y, 1.0).upper(5.0);

        return model;
    }

    /**
     * Unbounded: minimise -x subject to x - y <= 3
     */
    private static ExpressionsBasedModel newUnbounded(final boolean integer) {

        ExpressionsBasedModel model = new ExpressionsBasedModel();

        Variable x = model.addVariable("x").lower(0.0).weight(-1.0).integer(integer);
        Variable y = model.addVariable("y").lower(0.0).integer(integer);

        model.addExpression("c").set(x, 1.0).set(y, -1.0).upper(3.0);

        return model;
    }

    /**
     * Unbounded, and with nothing but a lower bound: minimise -x subject to x >= 0
     */
    private static ExpressionsBasedModel newUnboundedVariable(final boolean integer) {

        ExpressionsBasedModel model = new ExpressionsBasedModel();

        model.addVariable("x").lower(0.0).weight(-1.0).integer(integer);

        return model;
    }

    private static Optimisation.Result solve(final ExpressionsBasedModel model) {
        SolverCOPT solver = SolverCOPT.INTEGRATION.build(model);
        try {
            return SolverCOPT.INTEGRATION.toModelState(solver.solve(null), model);
        } finally {
            solver.dispose();
        }
    }

    /**
     * ojAlgo's unbounded state comes with a feasible solution.
     */
    private static void assertUnbounded(final ExpressionsBasedModel model) {
        Optimisation.Result result = InfeasibleOrUnboundedWithCOPT.solve(model);
        TestUtils.assertEquals(Optimisation.State.UNBOUNDED, result.getState());
        TestUtils.assertTrue(result.toString(), model.validate(result));
        TestUtils.assertEquals(model.objective().evaluate(result).doubleValue(), result.getValue(), NumberContext.of(8));
    }

    @Test
    public void testInfeasible() {
        for (boolean integer : new boolean[] { false, true }) {
            ExpressionsBasedModel model = InfeasibleOrUnboundedWithCOPT.newInfeasibleWithUnboundedRay(integer);
            TestUtils.assertEquals(Optimisation.State.INFEASIBLE, InfeasibleOrUnboundedWithCOPT.solve(model).getState());
        }
    }

    @Test
    public void testUnbounded() {
        for (boolean integer : new boolean[] { false, true }) {
            InfeasibleOrUnboundedWithCOPT.assertUnbounded(InfeasibleOrUnboundedWithCOPT.newUnbounded(integer));
        }
    }

    @Test
    public void testUnboundedVariable() {
        for (boolean integer : new boolean[] { false, true }) {
            InfeasibleOrUnboundedWithCOPT.assertUnbounded(InfeasibleOrUnboundedWithCOPT.newUnboundedVariable(integer));
        }
    }

}
