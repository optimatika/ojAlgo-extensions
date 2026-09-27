package org.ojalgo.optimisation.solver.joptimizer;

import java.util.List;

import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.OptimalityCertificateTest;
import org.ojalgo.type.context.NumberContext;
import org.ojalgo.type.keyvalue.KeyValue;

/**
 * JOptimizer provides no dual values (or reduced costs), so only the solution's feasibility and the objective
 * function value are checked.
 */
public class OptimalityCertificateTestWithJOptimizer extends OptimalityCertificateTest {

    @Override
    protected NumberContext getAccuracy() {
        return NumberContext.of(6);
    }

    @Override
    protected List<KeyValue<String, ExpressionsBasedModel.Integration<?>>> getIntegerIntegrations() {
        return List.of();
    }

    @Override
    protected List<KeyValue<String, ExpressionsBasedModel.Integration<?>>> getLinearIntegrations() {
        return List.of(KeyValue.of("JOptimizer", SolverJOptimizer.INTEGRATION));
    }

    @Override
    protected List<KeyValue<String, ExpressionsBasedModel.Integration<?>>> getQuadraticIntegrations() {
        return List.of(KeyValue.of("JOptimizer", SolverJOptimizer.INTEGRATION));
    }

    @Override
    protected boolean isDualsProvided() {
        return false;
    }

}
