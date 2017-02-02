package org.ojalgo.commons;
/*
 * Copyright 1997-2017 Optimatika (www.optimatika.se)
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

import org.apache.commons.math3.linear.RealMatrix;
import org.ojalgo.access.Access2D;
import org.ojalgo.access.Stream2D;
import org.ojalgo.function.BinaryFunction;
import org.ojalgo.function.UnaryFunction;
import org.ojalgo.matrix.store.PhysicalStore;

public class RealMatrixWrapper implements Access2D<Double>, Stream2D<Double, Access2D<Double>, PhysicalStore<Double>, RealMatrixWrapper> {

    private final RealMatrix myRealMatrix;

    public RealMatrixWrapper(final RealMatrix realMatrix) {
        super();
        myRealMatrix = realMatrix;
    }

    public long countColumns() {
        return myRealMatrix.getColumnDimension();
    }

    public long countRows() {
        return myRealMatrix.getRowDimension();
    }

    public double doubleValue(final long row, final long col) {
        return myRealMatrix.getEntry((int) row, (int) col);
    }

    public Double get(final long row, final long col) {
        return myRealMatrix.getEntry((int) row, (int) col);
    }

    public RealMatrixWrapper operateOnAll(final UnaryFunction<Double> operator) {
        this.loopAll((r, c) -> myRealMatrix.setEntry((int) r, (int) c, operator.invoke(myRealMatrix.getEntry((int) r, (int) c))));
        return this;
    }

    public RealMatrixWrapper operateOnMatching(final Access2D<Double> left, final BinaryFunction<Double> operator) {
        // TODO Auto-generated method stub
        return null;
    }

    public RealMatrixWrapper operateOnMatching(final BinaryFunction<Double> operator, final Access2D<Double> right) {
        // TODO Auto-generated method stub
        return null;
    }

    public void supplyTo(final PhysicalStore<Double> consumer) {
        // TODO Auto-generated method stub

    }

}
