package org.ojalgo.optimisation.solver.mosek;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.ojalgo.TestUtils;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.Optimisation;
import org.ojalgo.type.context.NumberContext;

/**
 * The objective function value includes the constant and the contribution of the fixed variables. The models
 * are solved directly with the solver, as {@link ExpressionsBasedModel#minimise()} re-evaluates the objective
 * function anyway.
 */
public class ObjectiveValueWithMosek {

    private static final NumberContext ACCURACY = NumberContext.of(8);

    private static void assertValue(final Optimisation.Sense sense, final double expected) {

        ExpressionsBasedModel model = ObjectiveValueWithMosek.newModel(sense);

        SolverMosek solver = SolverMosek.INTEGRATION.build(model);
        try {
            Optimisation.Result result = SolverMosek.INTEGRATION.toModelState(solver.solve(null), model);
            TestUtils.assertEquals(Optimisation.State.OPTIMAL, result.getState());
            TestUtils.assertEquals(expected, result.getValue(), ACCURACY);
            TestUtils.assertEquals(expected, model.objective().evaluate(result).doubleValue(), ACCURACY);
        } finally {
            solver.dispose();
        }
    }

    /**
     * x + 4y + 2z + 7.5 subject to x + y = 5 and z >= 1, with y fixed at 2 (so x = 3) and z <= 10. The constant
     * is the negated right-hand side of the objective row.
     */
    private static ExpressionsBasedModel newModel(final Optimisation.Sense sense) {

        String mps = String.join("\n", //
                "NAME          OBJCONST", //
                "OBJSENSE", //
                "    " + sense.name(), //
                "ROWS", //
                " N  COST", //
                " E  LINK", //
                " G  ZLO", //
                "COLUMNS", //
                "    X         COST         1.0   LINK         1.0", //
                "    Y         COST         4.0   LINK         1.0", //
                "    Z         COST         2.0   ZLO          1.0", //
                "RHS", //
                "    RHS       COST        -7.5   LINK         5.0", //
                "    RHS       ZLO          1.0", //
                "BOUNDS", //
                " UP BND       X          100.0", //
                " FX BND       Y            2.0", //
                " UP BND       Z           10.0", //
                "ENDATA", //
                "");

        return ExpressionsBasedModel.parse(new ByteArrayInputStream(mps.getBytes(StandardCharsets.US_ASCII)), ExpressionsBasedModel.FileFormat.MPS);
    }

    /**
     * z = 10: 3 + 8 + 20 + 7.5
     */
    @Test
    public void testMaximise() {
        ObjectiveValueWithMosek.assertValue(Optimisation.Sense.MAX, 38.5);
    }

    /**
     * z = 1: 3 + 8 + 2 + 7.5
     */
    @Test
    public void testMinimise() {
        ObjectiveValueWithMosek.assertValue(Optimisation.Sense.MIN, 20.5);
    }

}
