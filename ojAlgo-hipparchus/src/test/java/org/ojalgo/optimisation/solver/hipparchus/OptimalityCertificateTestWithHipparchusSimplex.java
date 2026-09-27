package org.ojalgo.optimisation.solver.hipparchus;

import java.util.List;

import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.OptimalityCertificateTest;
import org.ojalgo.type.keyvalue.KeyValue;

/**
 * The simplex solver provides no dual values (or reduced costs), so only the objective function value is
 * checked.
 */
public class OptimalityCertificateTestWithHipparchusSimplex extends OptimalityCertificateTest {

    @Override
    protected List<KeyValue<String, ExpressionsBasedModel.Integration<?>>> getIntegerIntegrations() {
        return List.of();
    }

    @Override
    protected List<KeyValue<String, ExpressionsBasedModel.Integration<?>>> getLinearIntegrations() {
        return List.of(KeyValue.of("Simplex", SimplexSolverImpl.INTEGRATION), KeyValue.of("SolverHipparchus", SolverHipparchus.INTEGRATION));
    }

    @Override
    protected List<KeyValue<String, ExpressionsBasedModel.Integration<?>>> getQuadraticIntegrations() {
        return List.of();
    }

    @Override
    protected boolean isDualsProvided() {
        return false;
    }

}
