package org.ojalgo.commons.math3.linear;

import org.apache.commons.math3.exception.NotStrictlyPositiveException;
import org.apache.commons.math3.exception.OutOfRangeException;
import org.apache.commons.math3.linear.AbstractRealMatrix;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.ojalgo.matrix.store.RawStore;
import org.ojalgo.structure.Access2D;
import org.ojalgo.structure.Mutate2D;

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
    public Access2DWrapper createMatrix(final int rowDimension, final int columnDimension) throws NotStrictlyPositiveException {
        return new Access2DWrapper(RawStore.FACTORY.makeZero(rowDimension, columnDimension));
    }

    @Override
    public int getColumnDimension() {
        return (int) myAccess2D.countColumns();
    }

    @Override
    public double getEntry(final int row, final int column) throws OutOfRangeException {
        return myAccess2D.doubleValue(row, column);
    }

    @Override
    public int getRowDimension() {
        return (int) myAccess2D.countRows();
    }

    @Override
    public void setEntry(final int row, final int column, final double value) throws OutOfRangeException {
        if (myAccess2D instanceof Mutate2D) {
            ((Mutate2D) myAccess2D).set(row, column, value);
        } else {
            throw new UnsupportedOperationException();
        }
    }

}
