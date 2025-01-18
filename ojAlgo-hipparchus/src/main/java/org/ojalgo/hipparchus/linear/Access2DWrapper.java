/*
 * Copyright 1997-2025 Optimatika
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
package org.ojalgo.hipparchus.linear;

import org.hipparchus.exception.MathIllegalArgumentException;
import org.hipparchus.linear.AbstractRealMatrix;
import org.hipparchus.linear.Array2DRowRealMatrix;
import org.hipparchus.linear.RealMatrix;
import org.ojalgo.matrix.store.RawStore;
import org.ojalgo.structure.Access2D;
import org.ojalgo.structure.Mutate2D;

/**
 * Wraps essentially any ojAlgo 2D data structure and turns it into a {@link RealMatrix}.
 *
 * @author apete
 */
public class Access2DWrapper extends AbstractRealMatrix {

    public static Access2DWrapper of(final Access2D<?> delegate) {
        return new Access2DWrapper(delegate);
    }

    private final Access2D<?> myAccess2D;

    Access2DWrapper(final Access2D<?> delegate) {
        super();
        myAccess2D = delegate;
    }

    @Override
    public Array2DRowRealMatrix copy() {
        return new Array2DRowRealMatrix(myAccess2D.toRawCopy2D(), false);
    }

    @Override
    public Access2DWrapper createMatrix(final int rowDimension, final int columnDimension) throws MathIllegalArgumentException {
        return new Access2DWrapper(RawStore.FACTORY.make(rowDimension, columnDimension));
    }

    @Override
    public int getColumnDimension() {
        return myAccess2D.getColDim();
    }

    @Override
    public double getEntry(final int row, final int column) throws MathIllegalArgumentException {
        return myAccess2D.doubleValue(row, column);
    }

    @Override
    public int getRowDimension() {
        return myAccess2D.getRowDim();
    }

    @Override
    public void setEntry(final int row, final int column, final double value) throws MathIllegalArgumentException {
        if (myAccess2D instanceof Mutate2D) {
            ((Mutate2D) myAccess2D).set(row, column, value);
        } else {
            throw new UnsupportedOperationException();
        }
    }

}
