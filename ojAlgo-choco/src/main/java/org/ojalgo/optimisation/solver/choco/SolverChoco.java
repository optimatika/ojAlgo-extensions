/*
 * Copyright 1997-2019 Optimatika
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
package org.ojalgo.optimisation.solver.choco;

import java.math.BigDecimal;
import java.util.List;

import org.chocosolver.solver.variables.IntVar;
import org.ojalgo.netio.CharacterRing;
import org.ojalgo.netio.CharacterRing.PrinterBuffer;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.Optimisation;
import org.ojalgo.optimisation.Variable;

public final class SolverChoco implements Optimisation.Solver {

    @FunctionalInterface
    public static interface Configurator {

        void configure(final org.chocosolver.solver.Model model, org.chocosolver.solver.Solver solver, final Optimisation.Options options);

    }

    static final class Integration extends ExpressionsBasedModel.Integration<SolverChoco> {

        private final PrinterBuffer myLog = new CharacterRing().asPrinter();

        Integration() {
            super();
        }

        public SolverChoco build(final ExpressionsBasedModel model) {

            final SolverChoco retVal = new SolverChoco(model.options);

            retVal.addVariables(model.getVariables());

            return retVal;
        }

        public boolean isCapable(final ExpressionsBasedModel model) {
            return true;
        }

        @Override
        protected boolean isSolutionMapped() {
            return true;
        }

    }

    public static final SolverChoco.Integration INTEGRATION = new Integration();

    static final Configurator DEFAULT = new Configurator() {

        public void configure(final org.chocosolver.solver.Model model, final org.chocosolver.solver.Solver solver, final Options options) {
            // TODO Auto-generated method stub

        }

    };

    private final Optimisation.Options myOptions;

    private final org.chocosolver.solver.Model myDelegateModel = new org.chocosolver.solver.Model();

    SolverChoco(final Optimisation.Options options) {

        super();

        myOptions = options;

    }

    void addVariables(final List<Variable> variables) {

        for (final Variable var : variables) {

            final String name = var.getName();
            final BigDecimal lower = var.getLowerLimit();
            final BigDecimal upper = var.getUpperLimit();

            if (var.isInteger()) {
                myDelegateModel.intVar(name, lower != null ? lower.intValue() : IntVar.MIN_INT_BOUND, upper != null ? upper.intValue() : IntVar.MAX_INT_BOUND);
            } else {
                myDelegateModel.realVar(name, lower != null ? lower.doubleValue() : Double.NEGATIVE_INFINITY,
                        upper != null ? upper.doubleValue() : Double.POSITIVE_INFINITY, myOptions.solution.epsilon());
            }
        }
    }

    public void dispose() {

        Solver.super.dispose();

    }

    public Result solve(final Result kickStarter) {
        return kickStarter;
    }

    @Override
    protected void finalize() throws Throwable {

        this.dispose();

        super.finalize();
    }

}
