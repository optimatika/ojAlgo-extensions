package org.ojalgo.commons.math3.linear;
/*
 * Copyright 1997-2020 Optimatika
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

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.DiagonalMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.ojalgo.matrix.store.MatrixStore;
import org.ojalgo.matrix.store.PhysicalStore;
import org.ojalgo.matrix.store.Primitive64Store;
import org.ojalgo.matrix.store.RawStore;
import org.ojalgo.matrix.store.TransformableRegion;
import org.ojalgo.structure.Access2D;

public abstract class RealMatrixWrapper implements MatrixStore<Double> {

    static final class Array2DRowWrapper extends RealMatrixWrapper {

        private final Array2DRowRealMatrix myArray2DRow;

        Array2DRowWrapper(final Array2DRowRealMatrix delegate) {
            super(delegate);
            myArray2DRow = delegate;
        }

        @Override
        public PhysicalStore.Factory<Double, ?> physical() {
            return RawStore.FACTORY;
        }

        public void supplyTo(final TransformableRegion<Double> receiver) {

            final int limRows = (int) Math.min(myArray2DRow.getRowDimension(), receiver.countRows());
            final int limCols = (int) Math.min(myArray2DRow.getColumnDimension(), receiver.countColumns());

            for (int i = 0; i < limRows; i++) {
                for (int j = 0; j < limCols; j++) {
                    receiver.set(i, j, myArray2DRow.getEntry(i, j));
                }
            }
        }

    }

    static final class DefaultWrapper extends RealMatrixWrapper {

        DefaultWrapper(final RealMatrix delegate) {
            super(delegate);
        }

        public void supplyTo(final TransformableRegion<Double> receiver) {

            final long limRows = Math.min(this.countRows(), receiver.countRows());
            final long limCols = Math.min(this.countColumns(), receiver.countColumns());

            for (long j = 0L; j < limCols; j++) {
                for (long i = 0L; i < limRows; i++) {
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

        public int firstInColumn(final int col) {
            return col;
        }

        public int firstInRow(final int row) {
            return row;
        }

        public int limitOfColumn(final int col) {
            return col + 1;
        }

        public int limitOfRow(final int row) {
            return row + 1;
        }

        public void supplyTo(final TransformableRegion<Double> receiver) {

            receiver.reset();

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

    public PhysicalStore.Factory<Double, ?> physical() {
        return Primitive64Store.FACTORY;
    }

    @Override
    public final String toString() {
        return Access2D.toString(this);
    }

}
