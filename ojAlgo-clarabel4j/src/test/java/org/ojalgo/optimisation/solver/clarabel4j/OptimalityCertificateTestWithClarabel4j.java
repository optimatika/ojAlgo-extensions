package org.ojalgo.optimisation.solver.clarabel4j;

import java.util.List;

import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.OptimalityCertificateTest;
import org.ojalgo.type.context.NumberContext;
import org.ojalgo.type.keyvalue.KeyValue;

public class OptimalityCertificateTestWithClarabel4j extends OptimalityCertificateTest {

    @Override
    protected NumberContext getAccuracy() {
        return NumberContext.of(7);
    }

    /**
     * A quadratic constraint is a second-order cone to Clarabel, and its multiplier is recovered from the
     * cone's dual variables. That is exact only at exact complementarity, which the interior point algorithm
     * gets close to, but not all the way - errors of about 1e-6.
     */
    @Override
    protected NumberContext getQuadraticConstraintAccuracy() {
        return NumberContext.of(5);
    }

    @Override
    protected List<KeyValue<String, ExpressionsBasedModel.Integration<?>>> getIntegerIntegrations() {
        return List.of();
    }

    @Override
    protected List<KeyValue<String, ExpressionsBasedModel.Integration<?>>> getLinearIntegrations() {
        return List.of(KeyValue.of("Clarabel4j", SolverClarabel4j.INTEGRATION));
    }

    @Override
    protected List<KeyValue<String, ExpressionsBasedModel.Integration<?>>> getQuadraticConstraintIntegrations() {
        return List.of(KeyValue.of("Clarabel4j", SolverClarabel4j.INTEGRATION));
    }

    @Override
    protected List<KeyValue<String, ExpressionsBasedModel.Integration<?>>> getQuadraticIntegrations() {
        return List.of(KeyValue.of("Clarabel4j", SolverClarabel4j.INTEGRATION));
    }

}
