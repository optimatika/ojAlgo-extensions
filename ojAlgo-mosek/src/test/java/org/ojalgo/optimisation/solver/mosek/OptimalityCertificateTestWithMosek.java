package org.ojalgo.optimisation.solver.mosek;

import java.util.List;

import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.OptimalityCertificateTest;
import org.ojalgo.type.context.NumberContext;
import org.ojalgo.type.keyvalue.KeyValue;

public class OptimalityCertificateTestWithMosek extends OptimalityCertificateTest {

    /**
     * MOSEK solves QPs with its interior point optimiser. At a degenerate optimum that gets within about 1e-4 of
     * the solution, and the dual values are then accurate to about 1e-6.
     */
    @Override
    protected NumberContext getAccuracy() {
        return NumberContext.of(5);
    }

    @Override
    protected List<KeyValue<String, ExpressionsBasedModel.Integration<?>>> getIntegerIntegrations() {
        return List.of(KeyValue.of("MOSEK", SolverMosek.INTEGRATION));
    }

    @Override
    protected List<KeyValue<String, ExpressionsBasedModel.Integration<?>>> getLinearIntegrations() {
        return List.of(KeyValue.of("MOSEK", SolverMosek.INTEGRATION));
    }

    @Override
    protected List<KeyValue<String, ExpressionsBasedModel.Integration<?>>> getQuadraticConstraintIntegrations() {
        return List.of(KeyValue.of("MOSEK", SolverMosek.INTEGRATION));
    }

    @Override
    protected List<KeyValue<String, ExpressionsBasedModel.Integration<?>>> getQuadraticIntegrations() {
        return List.of(KeyValue.of("MOSEK", SolverMosek.INTEGRATION));
    }

}
