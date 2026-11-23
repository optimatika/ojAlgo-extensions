package org.ojalgo.optimisation.solver.hipparchus;

import java.util.List;

import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.OptimalityCertificateTest;
import org.ojalgo.type.keyvalue.KeyValue;

/**
 * The dual active-set QP solver, also what {@link SolverHipparchus} uses for quadratic problems – at the
 * default accuracy.
 */
public class OptimalityCertificateTestWithHipparchusDualActive extends OptimalityCertificateTest {

    @Override
    protected List<KeyValue<String, ExpressionsBasedModel.Integration<?>>> getIntegerIntegrations() {
        return List.of();
    }

    @Override
    protected List<KeyValue<String, ExpressionsBasedModel.Integration<?>>> getLinearIntegrations() {
        return List.of();
    }

    @Override
    protected List<KeyValue<String, ExpressionsBasedModel.Integration<?>>> getQuadraticIntegrations() {
        return List.of(KeyValue.of("DualActive", QPDualActiveSolverImpl.INTEGRATION), KeyValue.of("SolverHipparchus", SolverHipparchus.INTEGRATION));
    }

}
