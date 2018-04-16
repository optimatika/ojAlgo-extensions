package org.ojalgo.optimisation.external;

import java.math.BigDecimal;

import org.ojalgo.array.BigArray;
import org.ojalgo.array.DenseArray;
import org.ojalgo.array.Primitive64Array;
import org.ojalgo.netio.BasicLogger;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.Optimisation.Result;

public class OJAlgoTest3 {

    /**
     * @param args the command line arguments Objective: allocate the maximum qty, and try to keep
     *        proportionality between customer.
     */
    public static void main(final String[] args) {

        final ExpressionsBasedModel tmpModel1 = P20150720.buildModel1();
        final ExpressionsBasedModel tmpModel2 = P20150720.buildModel2();
        final ExpressionsBasedModel tmpModel3 = P20150720.buildModel3();

        tmpModel1.relax(true);
        tmpModel2.relax(true);
        tmpModel3.relax(true);

        // Solutions from (pure) ojAlgo
        // ojAlgo 1 OPTIMAL 49999.669171627706 @ [23731.5685592, 14658.0750029, 8725.18286313, 0.0, 0.0, 0.0, 0.0, 2885.17357483, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0185765703008, 0.0116103564374, 0.00696621386358, 0.00232207128747, -0.574712643678]
        // ojAlgo 2 OPTIMAL 8.174791914387633E8 @ [22988.5057471, 14367.816092, 8620.68965517, 0.0, 0.0, 0.0, 0.0, 2873.56321839, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0]
        // ojAlgo 3 OPTIMAL 560300.4702803734 @ [80150.9434796, 48981.1320493, 13358.4905589, 40075.4716767, 0.0, 40075.4716767, 13358.4905589, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 24000.0, 0.0, 12000.0, 0.0, 0.0, 300.0, 24000.0, 0.0, 20000.0, 0.0, 0.0, 0.0, 12000.0, 12000.0, 0.0, 0.0, 0.0, 72000.0, 0.0, 0.0, 0.0, 28000.0, 0.0, 0.0, 0.0, 24000.0, 0.0, 0.0, 0.0, 16000.0, 0.0, 0.0, 0.0, 24000.0, 0.0, 0.0, 0.0, 56000.0]

        // Solutions from CPLEX (with ojAlgo presolve turned off)
        // OPTIMAL 848.3941025136476 @ [399.999999654, 249.999999654, 149.999999654, 0.0, 0.0, 0.0, 0.0, 49.9999996543, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -0.564712643687, -0.564712643692, -0.564712643701, -0.564712643747, -0.574712643678]
        // Solution 1 OK
        // OPTIMAL 8.174791914387629E8 @ [22988.5054745, 14367.8158165, 8620.68937468, 0.0, 0.0, 0.0, 0.0, 2873.56291279, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0]
        // Solution 2 OK
        // OPTIMAL 560300.4700496289 @ [74542.7288388, 48961.3064461, 13408.93322, 40061.705994, 0.0, 40061.705994, 13408.93322, 0.0, 7.04812228046, 2773.81908243, 2773.81908243, 0.0, 0.0, 0.0, 11604.5308488, 12395.4691449, 4339.53410099, 7660.46589271, 78.8113907425, 221.188602961, 11604.5308488, 12395.4691449, 10813.3798123, 0.0, 9186.62018135, 0.0, 11999.9999937, 0.0, 0.0, 4339.53410099, 7660.46589271, 0.0, 0.0, 47953.0468934, 24046.9531003, 0.0, 0.0, 14244.4816129, 13755.5183808, 12395.4691449, 0.0, 11604.5308488, 0.0, 0.0, 0.0, 6679.71873106, 9320.28126264, 12395.4691449, 0.0, 11604.5308488, 0.0, 55999.9999937]
        // Solution 3 OK

        final DenseArray<BigDecimal> tmpOjSol1 = BigArray.FACTORY.copy(new double[] { 23731.5685592, 14658.0750029, 8725.18286313, 0.0, 0.0, 0.0, 0.0,
                2885.17357483, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0185765703008, 0.0116103564374, 0.00696621386358, 0.00232207128747, -0.574712643678 });
        final DenseArray<BigDecimal> tmpOjSol2 = BigArray.FACTORY
                .copy(new double[] { 22988.5057471, 14367.816092, 8620.68965517, 0.0, 0.0, 0.0, 0.0, 2873.56321839, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 });
        final DenseArray<BigDecimal> tmpOjSol3 = BigArray.FACTORY
                .copy(new double[] { 80150.9434796, 48981.1320493, 13358.4905589, 40075.4716767, 0.0, 40075.4716767, 13358.4905589, 0.0, 0.0, 0.0, 0.0, 0.0,
                        0.0, 0.0, 24000.0, 0.0, 12000.0, 0.0, 0.0, 300.0, 24000.0, 0.0, 20000.0, 0.0, 0.0, 0.0, 12000.0, 12000.0, 0.0, 0.0, 0.0, 72000.0, 0.0,
                        0.0, 0.0, 28000.0, 0.0, 0.0, 0.0, 24000.0, 0.0, 0.0, 0.0, 16000.0, 0.0, 0.0, 0.0, 24000.0, 0.0, 0.0, 0.0, 56000.0 });

        if (!tmpModel1.validate(tmpOjSol1)) {
            BasicLogger.debug("ojAlgo solution 1 problem!");
        } else {
            BasicLogger.debug("ojAlgo solution 1 OK");
        }

        if (!tmpModel2.validate(tmpOjSol2)) {
            BasicLogger.debug("ojAlgo solution 2 problem!");
        } else {
            BasicLogger.debug("ojAlgo solution 2 OK");
        }

        if (!tmpModel3.validate(tmpOjSol3)) {
            BasicLogger.debug("ojAlgo solution 3 problem!");
        } else {
            BasicLogger.debug("ojAlgo solution 3 OK");
        }

        tmpModel1.options.validate = false; // A must have full (row) rank!
        tmpModel2.options.validate = false; // Q must be positive semidefinite!
        tmpModel3.options.validate = false; // Q must be positive semidefinite!

        ExpressionsBasedModel.addIntegration(SolverCPLEX.INTEGRATION);

        final Result tmpResult1 = tmpModel1.maximise();
        final Result tmpResult2 = tmpModel2.maximise();
        final Result tmpResult3 = tmpModel3.maximise();

        BasicLogger.debug(tmpResult1);
        if (!tmpModel1.validate(tmpResult1)) {
        } else {
            BasicLogger.debug("Solution 1 OK");
        }

        BasicLogger.debug(tmpResult2);
        if (!tmpModel2.validate(tmpResult2)) {
        } else {
            BasicLogger.debug("Solution 2 OK");
        }

        BasicLogger.debug(tmpResult3);
        if (!tmpModel3.validate(tmpResult3)) {
        } else {
            BasicLogger.debug("Solution 3 OK");
        }

        //  BasicLogger.debug(tmpModel3);

        // GOOD  OPTIMAL 560300.4702803734
        // WRONG OPTIMAL 560300.4015031556
        BasicLogger.debug("");
        BasicLogger.debug("");
        //  variables.forEach(v -> BasicLogger.debug(v.getName() + " = " + v.getValue().doubleValue()));

        /*
         * WRONG ALLOCATION CUSTOMER_A_1|PRODUCT_2 = 236000.0 CUSTOMER_A_2|PRODUCT_2 = 0.0
         * CUSTOMER_A_3|PRODUCT_2 = 0.0 CUSTOMER_A_4|PRODUCT_2 = 0.0 CUSTOMER_A_5|PRODUCT_2 = 0.0
         * CUSTOMER_A_6|PRODUCT_2 = 0.0 CUSTOMER_A_7|PRODUCT_2 = 0.0 CUSTOMER_A_8|PRODUCT_2 = 0.0
         * CUSTOMER_A_9|PRODUCT_2 = 0.0 CUSTOMER_A_10|PRODUCT_2 = 0.0 CUSTOMER_A_11|PRODUCT_2 = 0.0
         * CUSTOMER_A_12|PRODUCT_2 = 0.0 CUSTOMER_A_13|PRODUCT_2 = 0.0 CUSTOMER_A_14|PRODUCT_2 = 0.0
         * CUSTOMER_B_1|PRODUCT_2 = 24000.0 CUSTOMER_B_1|PRODUCT_3 = 0.0 CUSTOMER_B_2|PRODUCT_2 = 12000.0
         * CUSTOMER_B_2|PRODUCT_3 = 0.0 CUSTOMER_B_3|PRODUCT_2 = 0.0 CUSTOMER_B_3|PRODUCT_3 = 300.0
         * CUSTOMER_C_1|PRODUCT_2 = 24000.0 CUSTOMER_C_1|PRODUCT_3 = 0.0 CUSTOMER_D_1|PRODUCT_4 = 20000.0
         * CUSTOMER_D_1|PRODUCT_5 = 0.0 CUSTOMER_D_1|PRODUCT_2 = 0.0 CUSTOMER_D_1|PRODUCT_3 = 0.0
         * CUSTOMER_E_1|PRODUCT_1 = 12000.0 CUSTOMER_F_1|PRODUCT_4 = 12000.0 CUSTOMER_F_1|PRODUCT_5 = 0.0
         * CUSTOMER_F_1|PRODUCT_2 = 0.0 CUSTOMER_F_1|PRODUCT_3 = 0.0 CUSTOMER_G_1|PRODUCT_4 = 72000.0
         * CUSTOMER_G_1|PRODUCT_5 = 0.0 CUSTOMER_G_1|PRODUCT_2 = 0.0 CUSTOMER_G_1|PRODUCT_3 = 0.0
         * CUSTOMER_H_1|PRODUCT_4 = 28000.0 CUSTOMER_H_1|PRODUCT_5 = 0.0 CUSTOMER_H_1|PRODUCT_2 = 0.0
         * CUSTOMER_H_1|PRODUCT_3 = 0.0 CUSTOMER_I_1|PRODUCT_4 = 24000.0 CUSTOMER_I_1|PRODUCT_5 = 0.0
         * CUSTOMER_I_1|PRODUCT_2 = 0.0 CUSTOMER_I_1|PRODUCT_3 = 0.0 CUSTOMER_J_1|PRODUCT_4 = 16000.0
         * CUSTOMER_J_1|PRODUCT_5 = 0.0 CUSTOMER_J_1|PRODUCT_2 = 0.0 CUSTOMER_J_1|PRODUCT_3 = 0.0
         * CUSTOMER_K_1|PRODUCT_4 = 24000.0 CUSTOMER_K_1|PRODUCT_5 = 0.0 CUSTOMER_K_1|PRODUCT_2 = 0.0
         * CUSTOMER_K_1|PRODUCT_3 = 0.0 CUSTOMER_L_1|PRODUCT_1 = 56000.0
         */
        /*
         * GOOD ALLOCATION CUSTOMER_A_1|PRODUCT_2 = 80150.9434796 CUSTOMER_A_2|PRODUCT_2 = 48981.1320493
         * CUSTOMER_A_3|PRODUCT_2 = 13358.4905589 CUSTOMER_A_4|PRODUCT_2 = 40075.4716767
         * CUSTOMER_A_5|PRODUCT_2 = 0.0 CUSTOMER_A_6|PRODUCT_2 = 40075.4716767 CUSTOMER_A_7|PRODUCT_2 =
         * 13358.4905589 CUSTOMER_A_8|PRODUCT_2 = 0.0 CUSTOMER_A_9|PRODUCT_2 = 0.0 CUSTOMER_A_10|PRODUCT_2 =
         * 0.0 CUSTOMER_A_11|PRODUCT_2 = 0.0 CUSTOMER_A_12|PRODUCT_2 = 0.0 CUSTOMER_A_13|PRODUCT_2 = 0.0
         * CUSTOMER_A_14|PRODUCT_2 = 0.0 CUSTOMER_B_1|PRODUCT_2 = 24000.0 CUSTOMER_B_1|PRODUCT_3 = 0.0
         * CUSTOMER_B_2|PRODUCT_2 = 12000.0 CUSTOMER_B_2|PRODUCT_3 = 0.0 CUSTOMER_B_3|PRODUCT_2 = 0.0
         * CUSTOMER_B_3|PRODUCT_3 = 300.0 CUSTOMER_C_1|PRODUCT_2 = 24000.0 CUSTOMER_C_1|PRODUCT_3 = 0.0
         * CUSTOMER_D_1|PRODUCT_4 = 20000.0 CUSTOMER_D_1|PRODUCT_5 = 0.0 CUSTOMER_D_1|PRODUCT_2 = 0.0
         * CUSTOMER_D_1|PRODUCT_3 = 0.0 CUSTOMER_E_1|PRODUCT_1 = 12000.0 CUSTOMER_F_1|PRODUCT_4 = 12000.0
         * CUSTOMER_F_1|PRODUCT_5 = 0.0 CUSTOMER_F_1|PRODUCT_2 = 0.0 CUSTOMER_F_1|PRODUCT_3 = 0.0
         * CUSTOMER_G_1|PRODUCT_4 = 72000.0 CUSTOMER_G_1|PRODUCT_5 = 0.0 CUSTOMER_G_1|PRODUCT_2 = 0.0
         * CUSTOMER_G_1|PRODUCT_3 = 0.0 CUSTOMER_H_1|PRODUCT_4 = 28000.0 CUSTOMER_H_1|PRODUCT_5 = 0.0
         * CUSTOMER_H_1|PRODUCT_2 = 0.0 CUSTOMER_H_1|PRODUCT_3 = 0.0 CUSTOMER_I_1|PRODUCT_4 = 24000.0
         * CUSTOMER_I_1|PRODUCT_5 = 0.0 CUSTOMER_I_1|PRODUCT_2 = 0.0 CUSTOMER_I_1|PRODUCT_3 = 0.0
         * CUSTOMER_J_1|PRODUCT_4 = 16000.0 CUSTOMER_J_1|PRODUCT_5 = 0.0 CUSTOMER_J_1|PRODUCT_2 = 0.0
         * CUSTOMER_J_1|PRODUCT_3 = 0.0 CUSTOMER_K_1|PRODUCT_4 = 24000.0 CUSTOMER_K_1|PRODUCT_5 = 0.0
         * CUSTOMER_K_1|PRODUCT_2 = 0.0 CUSTOMER_K_1|PRODUCT_3 = 0.0 CUSTOMER_L_1|PRODUCT_1 = 56000.0
         */

        ExpressionsBasedModel.addIntegration(SolverCPLEX.INTEGRATION);

        // Solution obtained from CPLEX with ojAlgo presolve turned off
        // Total time on 4 threads = 0.02 sec. (0.24 ticks)
        // OPTIMAL 560300.4700496289 @ [74542.7288388, 48961.3064461, 13408.93322, 40061.705994, 0.0, 40061.705994, 13408.93322, 0.0, 7.04812228046, 2773.81908243, 2773.81908243, 0.0, 0.0, 0.0, 11604.5308488, 12395.4691449, 4339.53410099, 7660.46589271, 78.8113907425, 221.188602961, 11604.5308488, 12395.4691449, 10813.3798123, 0.0, 9186.62018135, 0.0, 11999.9999937, 0.0, 0.0, 4339.53410099, 7660.46589271, 0.0, 0.0, 47953.0468934, 24046.9531003, 0.0, 0.0, 14244.4816129, 13755.5183808, 12395.4691449, 0.0, 11604.5308488, 0.0, 0.0, 0.0, 6679.71873106, 9320.28126264, 12395.4691449, 0.0, 11604.5308488, 0.0, 55999.9999937]

        final double[] tmpCplexSolution = new double[] { 74542.7288388, 48961.3064461, 13408.93322, 40061.705994, 0.0, 40061.705994, 13408.93322, 0.0,
                7.04812228046, 2773.81908243, 2773.81908243, 0.0, 0.0, 0.0, 11604.5308488, 12395.4691449, 4339.53410099, 7660.46589271, 78.8113907425,
                221.188602961, 11604.5308488, 12395.4691449, 10813.3798123, 0.0, 9186.62018135, 0.0, 11999.9999937, 0.0, 0.0, 4339.53410099, 7660.46589271, 0.0,
                0.0, 47953.0468934, 24046.9531003, 0.0, 0.0, 14244.4816129, 13755.5183808, 12395.4691449, 0.0, 11604.5308488, 0.0, 0.0, 0.0, 6679.71873106,
                9320.28126264, 12395.4691449, 0.0, 11604.5308488, 0.0, 55999.9999937 };
        final Primitive64Array tmpSolution1 = Primitive64Array.wrap(tmpCplexSolution);
        final DenseArray<BigDecimal> tmpSolution2 = BigArray.FACTORY.copy(tmpSolution1);

        if (tmpModel3.validate(tmpSolution2)) {
            BasicLogger.debug("OK");
        } else {
            BasicLogger.debug("WRONG");
        }

    }
}
