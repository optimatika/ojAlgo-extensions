package org.ojalgo.commons.math3.linear;
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

import static org.ojalgo.constant.PrimitiveMath.*;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.DiagonalMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.ojalgo.access.Access2D;
import org.ojalgo.matrix.store.PhysicalStore;

public abstract class RealMatrixWrapper implements Access2D<Double>, Access2D.Collectable<Double, PhysicalStore<Double>> {

    static final class Array2DRowWrapper extends RealMatrixWrapper {

        private final Array2DRowRealMatrix myArray2DRow;

        Array2DRowWrapper(final Array2DRowRealMatrix delegate) {
            super(delegate);
            myArray2DRow = delegate;
        }

        @Override
        public void supplyTo(final PhysicalStore<Double> receiver) {

            final int tmpLimRows = (int) Math.min(myArray2DRow.getRowDimension(), receiver.countRows());
            final int tmpLimCols = (int) Math.min(myArray2DRow.getColumnDimension(), receiver.countColumns());

            for (int i = 0; i < tmpLimRows; i++) {
                for (int j = 0; j < tmpLimCols; j++) {
                    receiver.set(i, j, myArray2DRow.getEntry(i, j));
                }
            }
        }

    }

    static final class DefaultWrapper extends RealMatrixWrapper {

        DefaultWrapper(final RealMatrix delegate) {
            super(delegate);
        }

        public void supplyTo(final PhysicalStore<Double> receiver) {

            final long tmpLimRows = Math.min(this.countRows(), receiver.countRows());
            final long tmpLimCols = Math.min(this.countColumns(), receiver.countColumns());

            for (long j = 0L; j < tmpLimCols; j++) {
                for (long i = 0L; i < tmpLimRows; i++) {
                    receiver.set(i, j, this.doubleValue(i, j));
                }
            }
        }

    }

    static final class DiagonalWrapper extends RealMatrixWrapper {

        private final DiagonalMatrix myDiagonal;

        DiagonalWrapper(final DiagonalMatrix delegate) {
            super(delegate);
            myDiagonal = delegate;
        }

        @Override
        public void supplyTo(final PhysicalStore<Double> receiver) {

            receiver.fillAll(ZERO);

            final double[] diagonal = myDiagonal.getDataRef();
            for (int ij = 0; ij < diagonal.length; ij++) {
                receiver.set(ij, ij, diagonal[ij]);
            }
        }

    }

    public static RealMatrixWrapper of(final RealMatrix delegate) {
        if (delegate instanceof Array2DRowRealMatrix) {
            return new Array2DRowWrapper((Array2DRowRealMatrix) delegate);
        } else if (delegate instanceof DiagonalMatrix) {
            return new DiagonalWrapper((DiagonalMatrix) delegate);
        } else {
            return new DefaultWrapper(delegate);
        }
    }

    private final RealMatrix myRealMatrix;

    RealMatrixWrapper(final RealMatrix delegate) {
        super();
        myRealMatrix = delegate;
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

}