package org.ojalgo.optimisation.solver.cpsat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.ExpressionsBasedModel.Integration;
import org.ojalgo.optimisation.Optimisation;
import org.ojalgo.optimisation.Optimisation.Result;
import org.ojalgo.optimisation.Optimisation.State;
import org.ojalgo.optimisation.OptimisationCase;
import org.ojalgo.optimisation.Variable;
import org.ojalgo.optimisation.integer.TestBasicMIP;

public class TestBasicMIPWithCPSAT extends TestBasicMIP {

    /**
     * CP-SAT only works with integer coefficients/bounds internally, so non-integer constraint and
     * objective coefficients must be scaled up to integers before being handed to the native solver (see
     * {@link SolverCPSAT#buildModelProtoBytes}), and the objective value scaled back down again on the way
     * out. None of the shared {@link TestBasicMIP} cases exercise that path (all their coefficients are
     * already integers), so this covers it directly.
     *
     * <pre>
     * max  0.5*x + 0.25*y
     * s.t. 1.5*x + 2.5*y <= 7.75
     *      x, y binary
     * </pre>
     *
     * Every candidate (x, y) in {0,1}^2 satisfies the constraint (the tightest case, x=y=1, gives 4.0 <=
     * 7.75), so the optimum is simply both variables at 1: objective = 0.75.
     */
    @Test
    public void testFractionalCoefficients() {

        ExpressionsBasedModel model = new ExpressionsBasedModel();

        Variable x = model.newVariable("x").binary().weight(0.5);
        Variable y = model.newVariable("y").binary().weight(0.25);

        model.newExpression("constraint").upper(7.75).set(x, 1.5).set(y, 2.5);

        Result result = Result.of(0.75, State.OPTIMAL, 1.0, 1.0);

        OptimisationCase testCase = OptimisationCase.of(model, Optimisation.Sense.MAX, result);
        for (Integration<?> integration : this.integrations()) {
            testCase.assertResult(integration);
        }
    }

    /**
     * A trivially infeasible model (x + y must be both &gt;= 1 and &lt;= 0 with x, y binary) - checks that
     * {@link CPSATBindings#STATUS_INFEASIBLE} is correctly translated to {@link State#INFEASIBLE} rather
     * than being mistaken for some other terminal state.
     */
    @Test
    public void testInfeasible() {

        ExpressionsBasedModel model = new ExpressionsBasedModel();

        Variable x = model.newVariable("x").binary();
        Variable y = model.newVariable("y").binary();

        model.newExpression("atLeastOne").lower(1).set(x, 1).set(y, 1);
        model.newExpression("atMostZero").upper(0).set(x, 1).set(y, 1);

        for (Integration<?> integration : this.integrations()) {
            Optimisation.Result actual = model.minimise(integration);
            if (actual.getState() != State.INFEASIBLE && actual.getState() != State.INVALID) {
                throw new AssertionError("Expected an infeasible/invalid state, got: " + actual.getState());
            }
        }
    }

    @Override
    protected List<Integration<?>> integrations() {
        return List.of(SolverCPSAT.INTEGRATION);
    }

}
