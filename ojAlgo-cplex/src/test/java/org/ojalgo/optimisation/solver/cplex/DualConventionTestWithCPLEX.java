package org.ojalgo.optimisation.solver.cplex;

import java.util.List;

import org.ojalgo.optimisation.DualConventionTest;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.Optimisation;
import org.ojalgo.type.keyvalue.KeyValue;

import ilog.concert.IloException;
import ilog.cplex.IloCplex;

/**
 * CPLEX only has a Farkas proof if a simplex optimiser proved the infeasibility, so presolve is turned off.
 */
public class DualConventionTestWithCPLEX extends DualConventionTest {

    private static final SolverCPLEX.Configurator WITHOUT_PRESOLVE = new SolverCPLEX.Configurator() {

        @Override
        protected IloCplex newIloCplex(final Optimisation.Options options) throws IloException {
            IloCplex retVal = super.newIloCplex(options);
            retVal.setParam(IloCplex.Param.Preprocessing.Presolve, false);
            retVal.setParam(IloCplex.Param.RootAlgorithm, IloCplex.Algorithm.Dual);
            return retVal;
        }

    };

    @Override
    protected void configure(final Optimisation.Environment environment) {
        environment.setConfigurator(WITHOUT_PRESOLVE);
    }

    @Override
    protected List<KeyValue<String, ExpressionsBasedModel.Integration<?>>> getCertifyingIntegrations() {
        return List.of(KeyValue.of("CPLEX", SolverCPLEX.INTEGRATION));
    }

    @Override
    protected List<KeyValue<String, ExpressionsBasedModel.Integration<?>>> getLinearIntegrations() {
        return List.of(KeyValue.of("CPLEX", SolverCPLEX.INTEGRATION));
    }

}
