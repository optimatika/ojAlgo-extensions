package org.ojalgo.commons.math3.optim.linear;

import java.util.List;

import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.OptimalityCertificateTest;
import org.ojalgo.optimisation.solver.acm.SolverACM;
import org.ojalgo.type.keyvalue.KeyValue;

/**
 * Apache's simplex solver provides no dual values (or reduced costs), so only the objective function value is
 * checked, and it only solves linear problems.
 */
public class OptimalityCertificateTestWithACM extends OptimalityCertificateTest {

    @Override
    protected List<KeyValue<String, ExpressionsBasedModel.Integration<?>>> getIntegerIntegrations() {
        return List.of();
    }

    @Override
    protected List<KeyValue<String, ExpressionsBasedModel.Integration<?>>> getLinearIntegrations() {
        return List.of(KeyValue.of("ACM", SolverACM.INTEGRATION));
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
