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
import org.ojalgo.matrix.store.PhysicalStore;

public class RealMatrixWrapper implements Access2D<Double>, Access2D.Collectable<Double, PhysicalStore<Double>> {

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

    public void supplyTo(final PhysicalStore<Double> receiver) {
        final int tmpLimRows = (int) Math.min(myRealMatrix.getRowDimension(), receiver.countRows());
        final int tmpLimCols = (int) Math.min(myRealMatrix.getColumnDimension(), receiver.countColumns());
        for (int i = 0; i < tmpLimRows; i++) {
            for (int j = 0; j < tmpLimCols; j++) {
                receiver.set(i, j, myRealMatrix.getEntry(i, j));
            }
        }
    }

}
