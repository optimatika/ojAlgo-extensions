package org.ojalgo.optimisation.solver.clarabel4j;

import static org.ojalgo.function.constant.PrimitiveMath.ZERO;

import org.ojalgo.array.SparseArray;
import org.ojalgo.array.SparseArray.NonzeroView;
import org.ojalgo.matrix.store.ColumnsSupplier;
import org.ojalgo.matrix.store.R064Store;

import com.ustermetrics.clarabel4j.Matrix;

/**
 * Builder for Clarabel CSC matrices using ojAlgo's {@link ColumnsSupplier} in the same spirit as
 * {@link org.ojalgo.matrix.store.R064CSC.Builder}. This keeps sparse assembly consistent with the core ojAlgo
 * sparse infrastructure while producing {@link Matrix} instances in CSC format.
 */
final class MatrixBuilder {

    private int myColDim = 0;
    private int myRowDim = 0;
    private final ColumnsSupplier<Double> myColumns = R064Store.FACTORY.makeColumnsSupplier(Integer.MAX_VALUE);

    MatrixBuilder() {
        super();
    }

    MatrixBuilder(final int dim) {
        this(dim, dim);
    }

    MatrixBuilder(final int nbRows, final int nbCols) {
        super();
        this.set(nbRows - 1, nbCols - 1, ZERO);
    }

    private void update(final int row, final int col) {
        myRowDim = Math.max(myRowDim, row + 1);
        myColDim = Math.max(myColDim, col + 1);
    }

    void add(final int row, final int col, final double addend) {
        if (col >= myColumns.getColDim()) {
            myColumns.addColumns(1 + col - myColumns.getColDim());
        }
        myColumns.add(row, col, addend);
        this.update(row, col);
    }

    Matrix build() {

        int nnz = myColumns.countNonzeros();

        if (nnz == 0 || myRowDim == 0 || myColDim == 0) {
            return null;
        }

        long[] colPtr = new long[myColDim + 1];
        long[] rowIdx = new long[nnz];
        double[] values = new double[nnz];

        int cursor = 0;
        for (int c = 0; c < myColDim; c++) {
            colPtr[c] = cursor;
            if (c < myColumns.getColDim()) {
                SparseArray<Double> column = myColumns.getColumn(c);
                for (NonzeroView<Double> nz : column.nonzeros()) {
                    rowIdx[cursor] = nz.index();
                    values[cursor] = nz.doubleValue();
                    cursor++;
                }
            }
        }
        colPtr[myColDim] = cursor;

        return new Matrix(myRowDim, myColDim, colPtr, rowIdx, values);
    }

    int getColDim() {
        return myColDim;
    }

    int getRowDim() {
        return myRowDim;
    }

    void set(final int row, final int col, final double value) {
        if (col >= myColumns.getColDim()) {
            myColumns.addColumns(1 + col - myColumns.getColDim());
        }
        myColumns.set(row, col, value);
        this.update(row, col);
    }

}