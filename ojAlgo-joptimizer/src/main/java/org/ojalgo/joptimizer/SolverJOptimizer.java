/*
 * Copyright 1997-2017 Optimatika
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
package org.ojalgo.joptimizer;

import java.util.List;
import java.util.stream.Collectors;

import org.ojalgo.access.Access1D;
import org.ojalgo.access.Structure1D.IntIndex;
import org.ojalgo.access.Structure2D.IntRowColumn;
import org.ojalgo.optimisation.Expression;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.Optimisation;
import org.ojalgo.optimisation.Variable;

import com.joptimizer.exception.JOptimizerException;
import com.joptimizer.functions.ConvexMultivariateRealFunction;
import com.joptimizer.functions.LinearMultivariateRealFunction;
import com.joptimizer.functions.PSDQuadraticMultivariateRealFunction;
import com.joptimizer.optimizers.JOptimizer;
import com.joptimizer.optimizers.OptimizationRequest;
import com.joptimizer.optimizers.OptimizationResponse;

import cern.colt.matrix.tdouble.impl.DenseDoubleMatrix1D;
import cern.colt.matrix.tdouble.impl.DenseDoubleMatrix2D;

public class SolverJOptimizer implements Optimisation.Solver {

    public static final ExpressionsBasedModel.Integration<SolverJOptimizer> INTEGRATION = new ExpressionsBasedModel.Integration<SolverJOptimizer>() {

        public SolverJOptimizer build(final ExpressionsBasedModel model) {

            final List<Variable> variables = model.getVariables();
            final int numbVars = variables.size();

            final Expression objective = model.objective();
            final ConvexMultivariateRealFunction objFunc = SolverJOptimizer.toFunction(objective, numbVars, false);

            final OptimizationRequest request = new OptimizationRequest();
            request.setF0(objFunc);

            final List<Expression> equalities = model.constraints().filter(c -> c.isEqualityConstraint() && !c.isAnyQuadraticFactorNonZero())
                    .collect(Collectors.toList());
            final int numbEquals = equalities.size();

            final DenseDoubleMatrix2D a = new DenseDoubleMatrix2D(numbEquals, numbVars);
            final DenseDoubleMatrix1D b = new DenseDoubleMatrix1D(numbEquals);

            for (int i = 0; i < numbEquals; i++) {
                final Expression constr = equalities.get(i);

                final boolean negate = false;

                for (final IntIndex key : constr.getLinearKeySet()) {
                    final double adjusted = constr.getAdjustedLinearFactor(key);
                    a.set(i, key.index, negate ? -adjusted : adjusted);
                }

                b.set(i, constr.getAdjustedLowerLimit());
            }

            request.setA(a);
            request.setB(b);

            return new SolverJOptimizer(request, model.options);
        }

        public boolean isCapable(final ExpressionsBasedModel model) {
            return !model.isAnyVariableInteger();
        }

    };

    static ConvexMultivariateRealFunction toFunction(final Expression expression, final int dim, final boolean negate) {

        DenseDoubleMatrix2D p = null;
        DenseDoubleMatrix1D q = null;
        final double r = 0.0;

        if (expression.isAnyQuadraticFactorNonZero()) {
            p = new DenseDoubleMatrix2D(dim, dim);

            for (final IntRowColumn key : expression.getQuadraticKeySet()) {
                final double adjusted = expression.getAdjustedQuadraticFactor(key);
                p.set(key.row, key.column, negate ? -adjusted : adjusted);
            }

        }

        if (expression.isAnyLinearFactorNonZero()) {
            q = new DenseDoubleMatrix1D(dim);

            for (final IntIndex key : expression.getLinearKeySet()) {
                final double adjusted = expression.getAdjustedLinearFactor(key);
                q.set(key.index, negate ? -adjusted : adjusted);
            }
        }

        if (p != null) {
            return new PSDQuadraticMultivariateRealFunction(p, q, r);
        } else {
            return new LinearMultivariateRealFunction(q, r);
        }

    }

    private final OptimizationRequest myOptimizationRequest;

    private final JOptimizer myOptimizer = new JOptimizer();

    private final Optimisation.Options myOptions;

    protected SolverJOptimizer(final OptimizationRequest optimizationRequest, final Optimisation.Options options) {
        super();
        myOptimizationRequest = optimizationRequest;
        myOptions = options;
    }

    public Result solve(final Result kickStarter) {

        //  myOptimizationRequest.setInitialPoint(new double[] { 0.2, 0.2 });

        this.configure(myOptimizer, myOptimizationRequest, myOptions);

        myOptimizer.setOptimizationRequest(myOptimizationRequest);
        try {

            myOptimizer.optimize();
        } catch (final JOptimizerException exception) {

            exception.printStackTrace();
        }

        final OptimizationResponse response = myOptimizer.getOptimizationResponse();

        final State retState = Optimisation.State.OPTIMAL;
        final double retValue = response.getValue();
        final Access1D<Double> retSolution = Access1D.wrap(response.getSolution());

        return new Optimisation.Result(retState, retValue, retSolution);
    }

    /**
     * Create a subclass and override this method to configure
     */
    protected void configure(final JOptimizer optimizer, final OptimizationRequest request, final Optimisation.Options options) {

    }

}
