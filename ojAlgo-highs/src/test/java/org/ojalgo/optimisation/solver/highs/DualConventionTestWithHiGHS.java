package org.ojalgo.optimisation.solver.highs;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.List;

import org.ojalgo.optimisation.DualConventionTest;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.Optimisation;
import org.ojalgo.type.keyvalue.KeyValue;

/**
 * HiGHS only has a dual ray, without solving an LP to get one, if the dual simplex proved the infeasibility,
 * so presolve is turned off.
 */
public class DualConventionTestWithHiGHS extends DualConventionTest {

    private static final SolverHiGHS.Configurator WITHOUT_PRESOLVE = new SolverHiGHS.Configurator() {

        @Override
        protected void configure(final ExpressionsBasedModel model, final MemorySegment highs, final Arena arena) {
            super.configure(model, highs, arena);
            HiGHSBindings.setStringOptionValue(highs, HiGHSBindings.allocateString(arena, "presolve"), HiGHSBindings.allocateString(arena, "off"));
        }

    };

    @Override
    protected void configure(final Optimisation.Environment environment) {
        environment.setConfigurator(WITHOUT_PRESOLVE);
    }

    @Override
    protected List<KeyValue<String, ExpressionsBasedModel.Integration<?>>> getCertifyingIntegrations() {
        return List.of(KeyValue.of("HiGHS", SolverHiGHS.INTEGRATION));
    }

    @Override
    protected List<KeyValue<String, ExpressionsBasedModel.Integration<?>>> getLinearIntegrations() {
        return List.of(KeyValue.of("HiGHS", SolverHiGHS.INTEGRATION));
    }

}
