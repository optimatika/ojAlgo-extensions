package org.ojalgo.optimisation.solver.clarabel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;
import org.ojalgo.optimisation.Expression;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.Optimisation;
import org.ojalgo.optimisation.Variable;

/**
 * Interrupting the thread that started a solve has to stop the solver. Clarabel stops itself: it asks a
 * callback once per iteration, and the callback reports the interrupt.
 * <p>
 * Tested by interrupting up front rather than mid-solve. Clarabel is an interior-point solver that converges in
 * a few dozen iterations, so there is no reliable window in which to interrupt it - but a thread that is
 * already interrupted when the solve starts makes the very first iteration the one that stops, which pins the
 * outcome down exactly. That the flag survives until someone clears it is the same property the mid-solve case
 * relies on.
 */
public class InterruptedSolveWithClarabel {

    private static final long TIMEOUT_MILLIS = 30_000L;

    /**
     * Portfolio QP: small, and solved to optimality in milliseconds when nothing interferes.
     */
    private static ExpressionsBasedModel newModel() {

        Optimisation.Environment environment = Optimisation.newEnvironment();
        environment.addIntegration(SolverClarabel.INTEGRATION);

        ExpressionsBasedModel model = environment.newModel();

        Variable w1 = model.addVariable("w1").lower(0.0).upper(1.0);
        Variable w2 = model.addVariable("w2").lower(0.0).upper(1.0);
        Variable w3 = model.addVariable("w3").lower(0.0).upper(1.0);

        Expression variance = model.addExpression("variance").weight(1.0);
        variance.set(w1, w1, 0.04);
        variance.set(w2, w2, 0.09);
        variance.set(w3, w3, 0.01);
        variance.set(w1, w2, 0.02);

        model.addExpression("budget").set(w1, 1.0).set(w2, 1.0).set(w3, 1.0).level(1.0);

        return model;
    }

    /**
     * The same model, solved without interference, to show that what the other test asserts is the interrupt
     * and not the model being unsolvable.
     */
    @Test
    public void testSolvesWhenNotInterrupted() {
        assertEquals(Optimisation.State.OPTIMAL, InterruptedSolveWithClarabel.newModel().minimise().getState());
    }

    @Test
    public void testInterruptStopsTheSolver() throws InterruptedException {

        ExpressionsBasedModel model = InterruptedSolveWithClarabel.newModel();

        Optimisation.Result[] result = new Optimisation.Result[1];

        Thread solving = new Thread(() -> {
            Thread.currentThread().interrupt();
            result[0] = model.minimise();
        }, "solving");

        solving.start();
        solving.join(TIMEOUT_MILLIS);

        assertFalse(solving.isAlive(), "The solver ignored the interrupt");

        // ClarabelSolverStatus::CallbackTerminated - stopped on request, like reaching a limit, rather than
        // having failed.
        assertEquals(Optimisation.State.APPROXIMATE, result[0].getState(), () -> String.valueOf(result[0]));
    }

}
