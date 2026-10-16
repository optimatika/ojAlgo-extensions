package org.ojalgo.optimisation.solver.scip;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.Optimisation;
import org.ojalgo.optimisation.Variable;

/**
 * SCIP has no direct entry point for a quadratic objective, so it is passed via an auxiliary variable pinned
 * to x'Qx by a quadratic constraint. Getting the sign of that reformulation wrong is only visible when
 * maximising - the shared {@link org.ojalgo.optimisation.convex.TestBasicQP} cases all minimise - so the
 * maximisation cases are covered here.
 * <p>
 * Note the two accuracy levels. The objective value is accurate, but the solution vector is not: at a
 * quadratic optimum the objective is flat, so SCIP's default feasibility tolerance (numerics/feastol, 1E-6)
 * admits roughly 1E-3 of error in x. That is inherent to the reformulation, not a tolerance that could simply
 * be tightened, and it is why the shared TestBasicQP suite is not run against SCIP.
 */
public class MaximisedQPWithSCIP {

    private static final double ACCURACY_OBJECTIVE = 1E-5;
    private static final double ACCURACY_SOLUTION = 5E-3;

    /**
     * max -x² + 4x, x in [0,10] => x = 2, objective = 4
     */
    @Test
    public void testMaximiseConcaveQuadratic() {

        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable x = model.addVariable("x").lower(0.0).upper(10.0);
        model.addExpression("obj").weight(1.0).set(x, x, -1.0).set(x, 4.0);

        Optimisation.Result result = model.maximise(SolverSCIP.INTEGRATION);

        assertEquals(Optimisation.State.OPTIMAL, result.getState());
        assertEquals(4.0, result.getValue(), ACCURACY_OBJECTIVE);
        assertEquals(2.0, result.doubleValue(0), ACCURACY_SOLUTION);
    }

    /**
     * The mirrored minimisation: min x² - 4x, x in [0,10] => x = 2, objective = -4
     */
    @Test
    public void testMinimiseConvexQuadratic() {

        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable x = model.addVariable("x").lower(0.0).upper(10.0);
        model.addExpression("obj").weight(1.0).set(x, x, 1.0).set(x, -4.0);

        Optimisation.Result result = model.minimise(SolverSCIP.INTEGRATION);

        assertEquals(Optimisation.State.OPTIMAL, result.getState());
        assertEquals(-4.0, result.getValue(), ACCURACY_OBJECTIVE);
        assertEquals(2.0, result.doubleValue(0), ACCURACY_SOLUTION);
    }

    /**
     * Two variables, maximised: max -(x² + y²) + 4x + 6y => x = 2, y = 3, objective = 13
     */
    @Test
    public void testMaximiseTwoVariableQuadratic() {

        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable x = model.addVariable("x").lower(0.0).upper(10.0);
        Variable y = model.addVariable("y").lower(0.0).upper(10.0);
        model.addExpression("obj").weight(1.0).set(x, x, -1.0).set(y, y, -1.0).set(x, 4.0).set(y, 6.0);

        Optimisation.Result result = model.maximise(SolverSCIP.INTEGRATION);

        assertEquals(Optimisation.State.OPTIMAL, result.getState());
        assertEquals(13.0, result.getValue(), ACCURACY_OBJECTIVE);
        assertEquals(2.0, result.doubleValue(0), ACCURACY_SOLUTION);
        assertEquals(3.0, result.doubleValue(1), ACCURACY_SOLUTION);
    }

    /**
     * A maximised quadratic with an integer variable - the sign error was reachable through MIQP too. max -x²
     * + 5x with x integer in [0,10] => x = 2 or 3, objective = 6
     */
    @Test
    public void testMaximiseIntegerQuadratic() {

        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable x = model.addVariable("x").lower(0.0).upper(10.0).integer(true);
        model.addExpression("obj").weight(1.0).set(x, x, -1.0).set(x, 5.0);

        Optimisation.Result result = model.maximise(SolverSCIP.INTEGRATION);

        assertEquals(Optimisation.State.OPTIMAL, result.getState());
        assertEquals(6.0, result.getValue(), ACCURACY_OBJECTIVE);
    }

}
