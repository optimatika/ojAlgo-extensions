package org.ojalgo.commons.math3.linear;

import org.apache.commons.math3.exception.NotStrictlyPositiveException;
import org.apache.commons.math3.exception.OutOfRangeException;
import org.apache.commons.math3.linear.AbstractRealMatrix;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.ojalgo.access.Access2D;
import org.ojalgo.access.Mutate2D;
import org.ojalgo.matrix.store.PrimitiveDenseStore;

public class Access2DWrapper extends AbstractRealMatrix {

    public static Access2DWrapper of(Access2D<?> delegate) {
        return new Access2DWrapper(delegate);
    }

    private final Access2D<?> myAccess2D;

    Access2DWrapper(Access2D<?> delegate) {
        super();
        myAccess2D = delegate;
    }

    @Override
    public Array2DRowRealMatrix copy() {
        return new Array2DRowRealMatrix(myAccess2D.toRawCopy2D(), false);
    }

    @Override
    public RealMatrix createMatrix(int rowDimension, int columnDimension) throws NotStrictlyPositiveException {
        return new Access2DWrapper(PrimitiveDenseStore.FACTORY.makeZero(rowDimension, columnDimension));
    }

    @Override
    public int getColumnDimension() {
        return (int) myAccess2D.countColumns();
    }

    @Override
    public double getEntry(int row, int column) throws OutOfRangeException {
        return myAccess2D.doubleValue(row, column);
    }

    @Override
    public int getRowDimension() {
        return (int) myAccess2D.countRows();
    }

    @Override
    public void setEntry(int row, int column, double value) throws OutOfRangeException {
        if (myAccess2D instanceof Mutate2D) {
            ((Mutate2D) myAccess2D).set(row, column, value);
        } else {
            throw new UnsupportedOperationException();
        }
    }

}
