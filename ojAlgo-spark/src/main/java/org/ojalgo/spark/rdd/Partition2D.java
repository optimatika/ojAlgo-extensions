/*
 * Copyright 1997-2019 Optimatika
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
package org.ojalgo.spark.rdd;

import org.apache.spark.Partition;
import org.ojalgo.matrix.store.PhysicalStore;
import org.ojalgo.matrix.store.TransformableRegion;
import org.ojalgo.structure.Structure2D;

public class Partition2D implements Partition, Structure2D {

    static final int BLOCK_SIZE = 1 << 8;

    private final int myColumnsCount;
    private final int myIndex;
    private final int myRowsCount;
    private final int myStructure;

    public Partition2D(final int blockIndex, final int blockStructure) {
        this(blockIndex, blockStructure, BLOCK_SIZE, BLOCK_SIZE);
    }

    public Partition2D(final int blockIndex, final int blockStructure, final int rowsCount, final int columnsCount) {
        super();
        myIndex = blockIndex;
        myStructure = blockStructure;
        myRowsCount = rowsCount;
        myColumnsCount = columnsCount;
    }

    public int column() {
        return Structure2D.column(myIndex, myStructure);
    }

    public long countColumns() {
        return myColumnsCount;
    }

    public long countRows() {
        return myRowsCount;
    }

    public int index() {
        return myIndex;
    }

    public int row() {
        return Structure2D.row(myIndex, myStructure);
    }

    <N extends Number> TransformableRegion<N> makeConsumerRegion(final TransformableRegion<N> wholeRegion) {
        TransformableRegion<N> retVal = wholeRegion;
        if (this.index() != 0) {
            retVal = retVal.regionByOffsets(this.row() * BLOCK_SIZE, this.column() * BLOCK_SIZE);
        }
        if ((myRowsCount != BLOCK_SIZE) || ((myColumnsCount != BLOCK_SIZE))) {
            retVal = retVal.regionByLimits(myRowsCount, myColumnsCount);
        }
        return retVal;
    }

    <N extends Number> TransformableRegion<N> makeConsumerRegion(final PhysicalStore.Factory<N, PhysicalStore<N>> factory) {
        return factory.make(myRowsCount, myColumnsCount);
    }

}
