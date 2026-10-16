package org.ojalgo.optimisation.solver.highs;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.Random;

import org.junit.jupiter.api.Test;
import org.ojalgo.optimisation.Expression;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.Optimisation;
import org.ojalgo.optimisation.Variable;

/**
 * Interrupting the thread that started a solve has to stop the solver. Without that the only way out of a
 * runaway solve is its time limit, and an application that wants to abandon one is stuck waiting for work it
 * has already given up on.
 * <p>
 * HiGHS stops itself: it polls a callback from inside its own algorithms, and the callback reports the
 * interrupt.
 */
public class InterruptedSolveWithHiGHS {

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
        environment.addIntegration(SolverHiGHS.INTEGRATION);

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

        ExpressionsBasedModel model = InterruptedSolveWithHiGHS.newHardModel();

        Optimisation.Result[] result = new Optimisation.Result[1];

        Thread solving = new Thread(() -> result[0] = model.minimise(), "solving");
        solving.start();

        Thread.sleep(500L);

        solving.interrupt();
        solving.join(TIMEOUT_MILLIS);

        assertFalse(solving.isAlive(), "The solver ignored the interrupt");

        // Abandoned, so neither answer this model has: no optimal solution, and no proof of infeasibility
        // either. Which state exactly depends on how far HiGHS had got when the callback reported the
        // interrupt, and that is a race with model translation, so the test does not pin it down.
        Optimisation.State state = result[0].getState();
        assertFalse(state.isOptimal(), () -> String.valueOf(result[0]));
        assertNotEquals(Optimisation.State.INFEASIBLE, state, () -> String.valueOf(result[0]));
    }

}
