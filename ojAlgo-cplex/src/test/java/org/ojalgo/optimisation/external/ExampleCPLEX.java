/*
 * Copyright 1997-2015 Optimatika
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
package org.ojalgo.optimisation.external;

import java.io.File;

import org.ojalgo.netio.BasicLogger;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.MathProgSysModel;
import org.ojalgo.optimisation.Optimisation.Options;

import ilog.cplex.IloCplex;

public class ExampleCPLEX {

    static final String PATH_TO_FILE = "./src/test/java/org/ojalgo/optimisation/external/share2b.mps";

    public static void main(final String[] args) {

        final File tmpFile = new File(PATH_TO_FILE);

        final MathProgSysModel tmpMPS = MathProgSysModel.make(tmpFile);

        final ExpressionsBasedModel tmpModel = tmpMPS.getExpressionsBasedModel();

        tmpModel.options.setConfigurator(new SolverCPLEX.Configurator() {

            public void configure(final IloCplex cplex, final Options options) {
                BasicLogger.debug();
                BasicLogger.debug("I'm the CPLEX configurator!");
                BasicLogger.debug();
            }
        });

        // Essentially this is what you need to do to "integrate" CPLEX with ojAlgo
        ExpressionsBasedModel.addIntegration(SolverCPLEX.INTEGRATION);
        // Just add the CPLEX integration to the set of integrations...

        BasicLogger.debug();
        // In order for this to work the jvm property 'java.library.path' must
        // contain the path to the CPLEX installation
        BasicLogger.debug(System.getProperty("java.library.path"));
        BasicLogger.debug();
        BasicLogger.debug(tmpModel.minimise());
        BasicLogger.debug();
        BasicLogger.debug(tmpModel);
    }

    ExampleCPLEX() {
        super();
    }

}
