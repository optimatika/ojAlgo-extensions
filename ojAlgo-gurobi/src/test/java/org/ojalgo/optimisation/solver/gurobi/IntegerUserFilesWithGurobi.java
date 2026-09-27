package org.ojalgo.optimisation.solver.gurobi;

import org.junit.jupiter.api.BeforeAll;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.integer.IntegerUserFiles;

public class IntegerUserFilesWithGurobi extends IntegerUserFiles {

    @BeforeAll
    static void setup() {
        ExpressionsBasedModel.addIntegration(SolverGurobi.INTEGRATION);
    }

}
