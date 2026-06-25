package org.ojalgo.optimisation.solver.highs;

import org.junit.jupiter.api.BeforeAll;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.integer.IntegerUserFiles;

public class IntegerUserFilesWithHiGHS extends IntegerUserFiles {

    @BeforeAll
    static void setup() {
        ExpressionsBasedModel.addIntegration(SolverHiGHS.INTEGRATION);
    }

}
