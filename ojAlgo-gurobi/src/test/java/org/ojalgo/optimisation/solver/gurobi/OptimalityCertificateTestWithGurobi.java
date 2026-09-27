package org.ojalgo.optimisation.solver.gurobi;

import java.util.List;

import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.OptimalityCertificateTest;
import org.ojalgo.type.context.NumberContext;
import org.ojalgo.type.keyvalue.KeyValue;

public class OptimalityCertificateTestWithGurobi extends OptimalityCertificateTest {

    @Override
    protected NumberContext getAccuracy() {
        return NumberContext.of(7);
    }

    /**
     * With its default convergence tolerance (BarQCPConvTol=1e-6) Gurobi's barrier algorithm gets the solution to
     * a QCP model only to about 1e-3 or 1e-4 - an interior optimum of a quadratic objective function is the least
     * accurate. Getting the dual values wrong (sign, scale or which constraint) would show as errors of order 1.
     */
    @Override
    protected NumberContext getQuadraticConstraintAccuracy() {
        return NumberContext.of(2);
    }

    @Override
    protected List<KeyValue<String, ExpressionsBasedModel.Integration<?>>> getIntegerIntegrations() {
        return List.of(KeyValue.of("Gurobi", SolverGurobi.INTEGRATION));
    }

    @Override
    protected List<KeyValue<String, ExpressionsBasedModel.Integration<?>>> getLinearIntegrations() {
        return List.of(KeyValue.of("Gurobi", SolverGurobi.INTEGRATION));
    }

    @Override
    protected List<KeyValue<String, ExpressionsBasedModel.Integration<?>>> getQuadraticConstraintIntegrations() {
        return List.of(KeyValue.of("Gurobi", SolverGurobi.INTEGRATION));
    }

    @Override
    protected List<KeyValue<String, ExpressionsBasedModel.Integration<?>>> getQuadraticIntegrations() {
        return List.of(KeyValue.of("Gurobi", SolverGurobi.INTEGRATION));
    }

}
