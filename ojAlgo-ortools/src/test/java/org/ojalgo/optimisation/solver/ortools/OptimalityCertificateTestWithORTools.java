package org.ojalgo.optimisation.solver.ortools;

import java.util.List;

import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.OptimalityCertificateTest;
import org.ojalgo.type.context.NumberContext;
import org.ojalgo.type.keyvalue.KeyValue;

/**
 * The linear problems, solved by the default LP solver (GLOP). The ModelBuilder API has no quadratic
 * objectives.
 */
public class OptimalityCertificateTestWithORTools extends OptimalityCertificateTest {

    @Override
    protected NumberContext getAccuracy() {
        return NumberContext.of(7);
    }

    @Override
    protected List<KeyValue<String, ExpressionsBasedModel.Integration<?>>> getIntegerIntegrations() {
        return List.of(KeyValue.of("OR-Tools", SolverORTools.INTEGRATION));
    }

    @Override
    protected List<KeyValue<String, ExpressionsBasedModel.Integration<?>>> getLinearIntegrations() {
        return List.of(KeyValue.of("OR-Tools", SolverORTools.INTEGRATION));
    }

    @Override
    protected List<KeyValue<String, ExpressionsBasedModel.Integration<?>>> getQuadraticIntegrations() {
        return List.of();
    }

}
