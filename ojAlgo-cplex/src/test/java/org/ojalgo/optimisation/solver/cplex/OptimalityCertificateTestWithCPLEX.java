package org.ojalgo.optimisation.solver.cplex;

import java.util.List;

import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.OptimalityCertificateTest;
import org.ojalgo.type.context.NumberContext;
import org.ojalgo.type.keyvalue.KeyValue;

public class OptimalityCertificateTestWithCPLEX extends OptimalityCertificateTest {

    @Override
    protected NumberContext getAccuracy() {
        return NumberContext.of(7);
    }

    @Override
    protected List<KeyValue<String, ExpressionsBasedModel.Integration<?>>> getIntegerIntegrations() {
        return List.of(KeyValue.of("CPLEX", SolverCPLEX.INTEGRATION));
    }

    @Override
    protected List<KeyValue<String, ExpressionsBasedModel.Integration<?>>> getLinearIntegrations() {
        return List.of(KeyValue.of("CPLEX", SolverCPLEX.INTEGRATION));
    }

    @Override
    protected List<KeyValue<String, ExpressionsBasedModel.Integration<?>>> getQuadraticIntegrations() {
        return List.of(KeyValue.of("CPLEX", SolverCPLEX.INTEGRATION));
    }

}
