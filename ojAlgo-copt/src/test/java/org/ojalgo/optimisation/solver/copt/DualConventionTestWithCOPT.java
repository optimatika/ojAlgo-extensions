package org.ojalgo.optimisation.solver.copt;

import java.util.List;

import org.ojalgo.optimisation.DualConventionTest;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.type.keyvalue.KeyValue;

/**
 * COPT's Farkas ray need not be an extreme ray: called directly, without presolve, it returned a combination of
 * the infeasible problem's three extreme rays.
 */
public class DualConventionTestWithCOPT extends DualConventionTest {

    @Override
    protected List<KeyValue<String, ExpressionsBasedModel.Integration<?>>> getCertifyingIntegrations() {
        return List.of(KeyValue.of("COPT", SolverCOPT.INTEGRATION));
    }

    @Override
    protected List<KeyValue<String, ExpressionsBasedModel.Integration<?>>> getLinearIntegrations() {
        return List.of(KeyValue.of("COPT", SolverCOPT.INTEGRATION));
    }

    @Override
    protected boolean isCertificateMinimal() {
        return false;
    }

}
