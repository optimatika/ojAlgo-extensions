package org.ojalgo.optimisation.solver.xpress;

import java.util.List;

import org.ojalgo.optimisation.DualConventionTest;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.Optimisation;
import org.ojalgo.type.keyvalue.KeyValue;

import com.dashoptimization.XPRSconstants;
import com.dashoptimization.XPRSprob;

/**
 * Xpress only has a dual ray if the dual simplex proved the infeasibility, so presolve is turned off.
 */
public class DualConventionTestWithXpress extends DualConventionTest {

    private static final SolverXpress.Configurator WITHOUT_PRESOLVE = new SolverXpress.Configurator() {

        @Override
        protected void configure(final XPRSprob problem, final Optimisation.Options options) {
            super.configure(problem, options);
            problem.setIntControl(XPRSconstants.PRESOLVE, 0);
            problem.setIntControl(XPRSconstants.DEFAULTALG, XPRSconstants.ALG_DUAL);
        }

    };

    @Override
    protected void configure(final Optimisation.Environment environment) {
        environment.setConfigurator(WITHOUT_PRESOLVE);
    }

    @Override
    protected List<KeyValue<String, ExpressionsBasedModel.Integration<?>>> getCertifyingIntegrations() {
        return List.of(KeyValue.of("Xpress", SolverXpress.INTEGRATION));
    }

    @Override
    protected List<KeyValue<String, ExpressionsBasedModel.Integration<?>>> getLinearIntegrations() {
        return List.of(KeyValue.of("Xpress", SolverXpress.INTEGRATION));
    }

}
