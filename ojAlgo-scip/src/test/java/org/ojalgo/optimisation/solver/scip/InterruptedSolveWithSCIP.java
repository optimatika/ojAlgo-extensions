package org.ojalgo.optimisation.solver.scip;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.Expression;
import org.ojalgo.optimisation.Optimisation;
import org.ojalgo.concurrent.InterruptForwarder;
import org.ojalgo.optimisation.Variable;

/**
 * Interrupting the thread that started a solve has to stop the solver. Without that the only way out of a
 * runaway solve is its time limit, and an application that wants to abandon one is stuck waiting for work it
 * has already given up on.
 */
public class InterruptedSolveWithSCIP {

    /**
     * How long the solver gets to react to the interrupt. Generous - what is being tested is that it reacts
     * at all. Should it not, the test fails here rather than running until the solver's own time limit.
     */
    private static final long TIMEOUT_MILLIS = 30_000L;

    /**
     * Market split: equality constraints on binary variables with a right-hand side of half the coefficient
     * sum. Infeasible, and hard enough to prove so that branch-and-bound is still working on it hours later -
     * which is what makes it a reliable thing to interrupt.
     */
    private static ExpressionsBasedModel newHardModel() {

        Optimisation.Environment environment = Optimisation.newEnvironment();
        environment.addIntegration(SolverSCIP.INTEGRATION);

        ExpressionsBasedModel model = environment.newModel();

        Variable[] variables = new Variable[50];
        for (int j = 0; j < variables.length; j++) {
            variables[j] = model.addVariable("x" + j).binary().weight(1.0);
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

            constraint.level(sum / 2);
        }

        return model;
    }

    @Test
    public void testInterruptStopsTheSolver() throws InterruptedException {

        ExpressionsBasedModel model = InterruptedSolveWithSCIP.newHardModel();

        Optimisation.Result[] result = new Optimisation.Result[1];

        Thread solving = new Thread(() -> result[0] = model.minimise(), "solving");
        solving.start();

        Thread.sleep(500L);

        solving.interrupt();
        solving.join(TIMEOUT_MILLIS);

        assertFalse(solving.isAlive(), "The solver ignored the interrupt");

        // Abandoned, so neither answer this model has: no optimal solution, and no proof of infeasibility
        // either. Exactly which state it is depends on how far SCIP had got - UNEXPLORED if the interrupt
        // beat it to the first node, APPROXIMATE (SCIP_STATUS_USERINTERRUPT) once it is solving - and that is
        // a race with model translation, so the test does not pin it down.
        Optimisation.State state = result[0].getState();
        assertFalse(state.isOptimal(), () -> String.valueOf(result[0]));
        assertNotEquals(Optimisation.State.INFEASIBLE, state, () -> String.valueOf(result[0]));
    }

    /**
     * The watch itself, with no solver involved. A busy-wait stands in for the native call: it ignores the
     * interrupt exactly as native code does, so nothing but the watch can turn that interrupt into a stop.
     */
    @Test
    public void testWatchStopsSolverOnInterrupt() throws InterruptedException {

        AtomicBoolean stopped = new AtomicBoolean(false);

        Thread solving = new Thread(() -> {
            try (InterruptForwarder interrupter = InterruptForwarder.watch(() -> stopped.set(true))) {
                long deadline = System.currentTimeMillis() + 3_000L;
                while (System.currentTimeMillis() < deadline && !stopped.get()) {
                    // Deliberately uninterruptible.
                }
            }
        }, "solving");

        solving.start();
        Thread.sleep(200L);
        solving.interrupt();
        solving.join(TIMEOUT_MILLIS);

        assertFalse(solving.isAlive());
        assertTrue(stopped.get(), "The interrupt never reached the solver");
    }

    /**
     * An interrupt that arrives before the solver has been handed the model must not be missed – the thread
     * carries the flag until someone clears it, and nobody does that here.
     */
    @Test
    public void testWatchStopsSolverInterruptedUpFront() throws InterruptedException {

        AtomicBoolean stopped = new AtomicBoolean(false);

        Thread solving = new Thread(() -> {
            Thread.currentThread().interrupt();
            try (InterruptForwarder interrupter = InterruptForwarder.watch(() -> stopped.set(true))) {
                long deadline = System.currentTimeMillis() + 3_000L;
                while (System.currentTimeMillis() < deadline && !stopped.get()) {
                    // Deliberately uninterruptible.
                }
            }
        }, "solving");

        solving.start();
        solving.join(TIMEOUT_MILLIS);

        assertFalse(solving.isAlive());
        assertTrue(stopped.get(), "The interrupt was already set and got missed");
    }

}
