package org.ojalgo.optimisation.solver.scip;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

import org.junit.jupiter.api.Test;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.Optimisation;
import org.ojalgo.optimisation.Variable;

/**
 * Every native call returns a {@code SCIP_RETCODE}, and each one is verified. Without that a failed call
 * leaves a silently incomplete model - the same failure mode as a wrong answer reported as OPTIMAL.
 */
public class NativeErrorsWithSCIP {

    /**
     * Sets a parameter that does not exist, which SCIP answers with SCIP_PARAMETERUNKNOWN.
     */
    static final class UnknownParameterConfigurator extends SolverSCIP.Configurator {

        @Override
        protected void configure(final MemorySegment scip, final Arena arena, final Optimisation.Options options) {
            MemorySegment name = SCIPBindings.allocateString(arena, "this/parameter/does/not/exist");
            SCIPBindings.setIntParam(scip, name, 1);
        }

    }

    @Test
    public void testFailedNativeCallIsReported() {

        Optimisation.Environment environment = Optimisation.newEnvironment();
        environment.setConfigurator(new UnknownParameterConfigurator());
        environment.addIntegration(SolverSCIP.INTEGRATION);

        ExpressionsBasedModel model = environment.newModel();
        Variable x = model.addVariable("x").lower(0.0).upper(1.0).weight(1.0);
        model.addExpression("c").set(x, 1.0).lower(0.0);

        RuntimeException caught = assertThrows(RuntimeException.class, () -> model.minimise());

        String message = NativeErrorsWithSCIP.collectMessages(caught);
        assertTrue(message.contains("SCIP_PARAMETERUNKNOWN"), message);
        assertTrue(message.contains("SCIPsetIntParam"), message);
    }

    private static String collectMessages(final Throwable throwable) {
        StringBuilder retVal = new StringBuilder();
        for (Throwable cause = throwable; cause != null; cause = cause.getCause()) {
            retVal.append(cause.getClass().getSimpleName()).append(": ").append(cause.getMessage()).append(" | ");
        }
        return retVal.toString();
    }

}
