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
package org.ojalgo;

import java.util.List;

import org.ojalgo.access.Access1D;
import org.ojalgo.access.Mutate2D;
import org.ojalgo.access.Structure1D.IntIndex;
import org.ojalgo.access.Structure2D.IntRowColumn;
import org.ojalgo.function.multiary.MultiaryFunction.TwiceDifferentiable;
import org.ojalgo.matrix.store.MatrixStore;
import org.ojalgo.optimisation.Expression;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.Optimisation;
import org.ojalgo.optimisation.Variable;

import com.joptimizer.exception.JOptimizerException;
import com.joptimizer.functions.ConvexMultivariateRealFunction;
import com.joptimizer.functions.PSDQuadraticMultivariateRealFunction;
import com.joptimizer.optimizers.JOptimizer;
import com.joptimizer.optimizers.OptimizationRequest;

import cern.colt.matrix.tdouble.DoubleMatrix1D;
import cern.colt.matrix.tdouble.DoubleMatrix2D;
import cern.colt.matrix.tdouble.DoubleMatrix3D;
import cern.colt.matrix.tdouble.impl.DenseDoubleMatrix1D;
import cern.colt.matrix.tdouble.impl.DenseDoubleMatrix2D;

public class SolverJOptimizer implements Optimisation.Solver {

    static final class DoubleMatrixWrapper1D implements Access1D<Double> {

        private final DoubleMatrix1D myDelegate;

        DoubleMatrixWrapper1D(final DoubleMatrix1D delegate) {
            super();
            myDelegate = delegate;
        }

        public long count() {
            return myDelegate.size();
        }

        public double doubleValue(final long index) {
            return myDelegate.getQuick((int) index);
        }

        public Double get(final long index) {
            return myDelegate.getQuick((int) index);
        }

    }

    static final class MatrixStoreWrapper1D extends DoubleMatrix1D {

        private final MatrixStore<Double> myGradient;

        MatrixStoreWrapper1D(final MatrixStore<Double> gradient) {
            myGradient = gradient;
        }

        @Override
        public Object elements() {
            return myGradient;
        }

        @Override
        public double getQuick(final int index) {
            return myGradient.doubleValue(index);
        }

        @Override
        public DoubleMatrix1D like(final int size) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public DoubleMatrix2D like2D(final int rows, final int columns) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public DoubleMatrix2D reshape(final int rows, final int columns) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public DoubleMatrix3D reshape(final int slices, final int rows, final int columns) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void setQuick(final int index, final double value) {
            // TODO Auto-generated method stub

        }

        @Override
        protected DoubleMatrix1D viewSelectionLike(final int[] offsets) {
            // TODO Auto-generated method stub
            return null;
        }
    }

    static final class MatrixStoreWrapper2D extends DoubleMatrix2D {

        private final MatrixStore<Double> myDelegate;

        MatrixStoreWrapper2D(final MatrixStore<Double> delegate) {
            super();
            myDelegate = delegate;
        }

        @Override
        public Object elements() {
            return myDelegate;
        }

        @Override
        public double getQuick(final int row, final int column) {
            return myDelegate.doubleValue(row, column);
        }

        @Override
        public DoubleMatrix2D like(final int rows, final int columns) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public DoubleMatrix1D like1D(final int size) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void setQuick(final int row, final int column, final double value) {
            ((Mutate2D) myDelegate).set(row, column, value);
        }

        @Override
        public DoubleMatrix1D vectorize() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        protected DoubleMatrix1D like1D(final int size, final int zero, final int stride) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        protected DoubleMatrix2D viewSelectionLike(final int[] rowOffsets, final int[] columnOffsets) {
            // TODO Auto-generated method stub
            return null;
        }
    }

    static class TwiceDifferentiableWrapperFunction implements ConvexMultivariateRealFunction {

        private final TwiceDifferentiable<Double> myDelegate;

        TwiceDifferentiableWrapperFunction(final TwiceDifferentiable<Double> delegate) {
            super();
            myDelegate = delegate;
        }

        public int getDim() {
            return myDelegate.arity();
        }

        public DoubleMatrix1D gradient(final DoubleMatrix1D X) {

            final Access1D<Double> point = new DoubleMatrixWrapper1D(X);

            final MatrixStore<Double> gradient = myDelegate.getGradient(point);

            return new MatrixStoreWrapper1D(gradient);
        }

        public DoubleMatrix2D hessian(final DoubleMatrix1D X) {

            final Access1D<Double> point = new DoubleMatrixWrapper1D(X);

            final MatrixStore<Double> hessian = myDelegate.getHessian(point);

            return new MatrixStoreWrapper2D(hessian);
        }

        public double value(final DoubleMatrix1D X) {
            // TODO Auto-generated method stub
            return 0;
        }

    }

    public static final ExpressionsBasedModel.Integration<SolverJOptimizer> INTEGRATION = new ExpressionsBasedModel.Integration<SolverJOptimizer>() {

        public SolverJOptimizer build(final ExpressionsBasedModel model) {

            final List<Variable> variables = model.getVariables();

            final Expression objective = model.objective();
            final ConvexMultivariateRealFunction objectiveFunction = SolverJOptimizer.toFunction(objective, variables.size());

            final OptimizationRequest or = new OptimizationRequest();
            or.setF0(objectiveFunction);
            or.setInitialPoint(new double[] { 0.2, 0.2 });
            or.setFi(null);
            // TODO Auto-generated method stub
            return new SolverJOptimizer(or);
        }

        public boolean isCapable(final ExpressionsBasedModel model) {
            return !model.isAnyVariableInteger();
        }

    };

    static ConvexMultivariateRealFunction toFunction(final Expression expression, final int dim) {

        DenseDoubleMatrix2D p = null;
        DenseDoubleMatrix1D q = null;

        if (expression.isAnyQuadraticFactorNonZero()) {
            p = new DenseDoubleMatrix2D(dim, dim);

            for (final IntRowColumn key : expression.getQuadraticKeySet()) {
                p.set(key.row, key.column, expression.getAdjustedQuadraticFactor(key));
            }

        }

        if (expression.isAnyLinearFactorNonZero()) {
            q = new DenseDoubleMatrix1D(dim);

            for (final IntIndex key : expression.getLinearKeySet()) {
                q.set(key.index, expression.getAdjustedLinearFactor(key));
            }
        }

        return new PSDQuadraticMultivariateRealFunction(p, q, 0.0);
    }

    private final OptimizationRequest myOptimizationRequest;

    private final JOptimizer myOptimizer = new JOptimizer();

    SolverJOptimizer(final OptimizationRequest optimizationRequest) {
        super();
        myOptimizationRequest = optimizationRequest;
    }

    public Result solve(final Result kickStarter) {

        myOptimizer.setOptimizationRequest(myOptimizationRequest);
        try {
            myOptimizer.optimize();
        } catch (final JOptimizerException exception) {
            // TODO Auto-generated catch block
            exception.printStackTrace();
        }

        final double[] sol = myOptimizer.getOptimizationResponse().getSolution();
        // TODO Auto-generated method stub
        return null;
    }

}
