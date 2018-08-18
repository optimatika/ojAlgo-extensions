import static org.ojalgo.constant.BigMath.*;

import org.ojalgo.OjAlgoUtils;
import org.ojalgo.netio.BasicLogger;
import org.ojalgo.optimisation.Expression;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.Optimisation.Result;
import org.ojalgo.optimisation.Optimisation.State;
import org.ojalgo.optimisation.solver.mosek.SolverMosek;
import org.ojalgo.optimisation.Variable;
import org.ojalgo.random.Uniform;

/**
 * This example creates random/simple quadratic programming (QP) problems - similar to what the MarkowitzModel
 * portfolio optimiser would create - at increasing sizes. The execution speed is measured for each problem
 * size. The problem size is doubled for every iteration. Theoretically, regardless of how long you're willing
 * to wait, the ojAlgo convex solvers have an absolute limit somewhere around 20000 variables.
 *
 * @author apete
 */
public class ConvexSolverPerformance {

    /**
     * Random number [0.0%,20.0%)
     */
    private static final Uniform UNIFORM_20 = new Uniform(0.0, 0.2);

    public static void main(final String[] args) {

        //        MatrixUtils.setAllOperationThresholds(Integer.MAX_VALUE);
        //        OjAlgoUtils.ENVIRONMENT = Hardware.makeSimple("x86", 1024L * 1024L * 1024L * 8L, 1).virtualise();

        ExpressionsBasedModel.addIntegration(SolverMosek.INTEGRATION);

        BasicLogger.debug();
        BasicLogger.debug(ConvexSolverPerformance.class.getSimpleName());
        BasicLogger.debug(OjAlgoUtils.getTitle());
        BasicLogger.debug(OjAlgoUtils.getDate());
        BasicLogger.debug();

        // Tiny bit of warmup to get rid of the worst timing problems
        ConvexSolverPerformance.buildModel(3).minimise();
        ConvexSolverPerformance.buildModel(33).minimise();
        ConvexSolverPerformance.buildModel(333).minimise();

        for (int exp = 1; exp <= 12; exp++) {
            final int tmpNumberOfVariables = (int) Math.pow(2, exp);

            final ExpressionsBasedModel tmpModel = ConvexSolverPerformance.buildModel(tmpNumberOfVariables);

            // tmpModel.options.debug(ConvexSolver.class);

            final long tmpBefore = System.nanoTime();
            final Result tmpResult = tmpModel.minimise();
            final long tmpAfter = System.nanoTime();

            final State tmpState = tmpResult.getState();
            final double tmpTime = (tmpAfter - tmpBefore) / 1_000_000_000.0;

            BasicLogger.debug(); // Print the problem size, execution time and result state
            BasicLogger.debug("{} variables =>\t{} in {}s", tmpNumberOfVariables, tmpState, tmpTime);

            // If the problem is small; also print the entire model
            if (tmpNumberOfVariables <= 10) {
                BasicLogger.debug(tmpModel);
            }
        }
    }

    private static ExpressionsBasedModel buildModel(final int numberOfVariables) {

        final Variable[] tmpVariables = new Variable[numberOfVariables];
        for (int i = 0; i < numberOfVariables; i++) {
            tmpVariables[i] = Variable.make("V" + Integer.toString(i)).lower(ZERO).weight(-UNIFORM_20.doubleValue());
        }

        final ExpressionsBasedModel retVal = new ExpressionsBasedModel(tmpVariables);

        final Expression tmp100P = retVal.addExpression("Balance");
        for (final Variable tmpVariable : tmpVariables) {
            tmp100P.set(tmpVariable, ONE);
        }
        tmp100P.level(ONE);

        final Expression tmpVar = retVal.addExpression("Variance");
        for (final Variable tmpVariable : tmpVariables) {
            tmpVar.set(tmpVariable, tmpVariable, UNIFORM_20);
        }
        tmpVar.weight(TWO);

        // ExpressionsBasedModel.INTEGRATIONS.add(SolverCPLEX.INTEGRATION);

        return retVal;
    }

}
