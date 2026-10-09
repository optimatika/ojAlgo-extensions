/*
 * Copyright 1997-2026 Optimatika
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.ojalgo.optimisation.solver.gurobi;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.ExpressionsBasedModel.Integration;
import org.ojalgo.optimisation.linear.LinearSolver;
import org.ojalgo.optimisation.linear.ReducedCostsTest;
import org.ojalgo.type.keyvalue.KeyValue;

/**
 * Only the relaxed checks – where the optimal dual solution is not unique (at a presolve-derived bound, say)
 * Gurobi may report another one than the strict checks expect.
 */
@Disabled
public class ReducedCostsTestWithGurobi extends ReducedCostsTest {

    /**
     * Gurobi's integration is typed Integration<SolverGurobi>, not Integration<LinearSolver>. It is still a
     * valid ExpressionsBasedModel.Integration usable with ExpressionsBasedModel#minimise/maximise, so we
     * widen via raw type.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static KeyValue<String, Integration<LinearSolver>> getAdditionalEntry() {
        return (KeyValue) KeyValue.of("Gurobi", SolverGurobi.INTEGRATION);
    }

    @Override
    protected List<KeyValue<String, ExpressionsBasedModel.Integration<LinearSolver>>> getAllIntegrations() {

        List<KeyValue<String, ExpressionsBasedModel.Integration<LinearSolver>>> retVal = new ArrayList<>(super.getAllIntegrations());

        retVal.add(ReducedCostsTestWithGurobi.getAdditionalEntry());

        return retVal;
    }

}
