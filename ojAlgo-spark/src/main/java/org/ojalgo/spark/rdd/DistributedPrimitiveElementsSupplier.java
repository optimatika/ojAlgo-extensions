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

import java.util.ArrayList;
import java.util.List;

import org.apache.spark.SparkContext;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;
import org.ojalgo.function.BinaryFunction;
import org.ojalgo.function.UnaryFunction;
import org.ojalgo.matrix.store.TransformableRegion;
import org.ojalgo.matrix.store.ElementsSupplier;
import org.ojalgo.matrix.store.MatrixStore;
import org.ojalgo.matrix.store.PhysicalStore.Factory;
import org.ojalgo.matrix.store.Primitive64Store;
import org.ojalgo.structure.Access2D;

import scala.reflect.ClassManifestFactory$;
import scala.reflect.ClassTag;

public class DistributedPrimitiveElementsSupplier implements ElementsSupplier<Double> {

    private static final ClassTag<double[]> CLASS_TAG = ClassManifestFactory$.MODULE$.fromClass(double[].class);

    static long BLOCK_SIZE = 4L;

    private final long myColumnsCount;

    private final JavaRDD<double[]> myDelegate;

    private final long myRowsCount;

    public DistributedPrimitiveElementsSupplier(final SparkContext context, final Access2D<?> elements) {

        super();

        long numbRowBlocks = elements.countRows() / BLOCK_SIZE;
        if ((elements.countRows() % BLOCK_SIZE) != 0L) {
            numbRowBlocks++;
        }

        long numbColBlocks = elements.countColumns() / BLOCK_SIZE;
        if ((elements.countColumns() % BLOCK_SIZE) != 0L) {
            numbColBlocks++;
        }

        final List<double[]> blocks = new ArrayList<>((int) (numbRowBlocks * numbColBlocks));

        for (long bj = 0L; bj < numbColBlocks; bj++) {
            final long firstCol = bj * BLOCK_SIZE;
            final long limitCol = Math.min((bj + 1l) * BLOCK_SIZE, elements.countColumns());
            final int countCols = (int) (limitCol - firstCol);

            for (long bi = 0L; bi < numbRowBlocks; bi++) {
                final long firstRow = bi * BLOCK_SIZE;
                final long limitRow = Math.min((bi + 1l) * BLOCK_SIZE, elements.countRows());
                final int countRows = (int) (limitRow - firstRow);

                final double[] block = new double[countRows * countCols];

                for (int j = 0; j < countCols; j++) {
                    for (int i = 0; i < countRows; i++) {
                        block[i + (j * countRows)] = elements.doubleValue(firstRow + i, firstCol + j);
                    }
                }

                blocks.add(block);
            }
        }

        myDelegate = JavaSparkContext.fromSparkContext(context).parallelize(blocks);

        myRowsCount = elements.countRows();
        myColumnsCount = elements.countColumns();
    }

    DistributedPrimitiveElementsSupplier(final JavaRDD<double[]> delegate, final long rowsCount, final long columnsCount) {

        super();

        myDelegate = delegate;

        myRowsCount = rowsCount;
        myColumnsCount = columnsCount;
    }

    public long countColumns() {
        return myColumnsCount;
    }

    public long countRows() {
        return myRowsCount;
    }

    public ElementsSupplier<Double> operateOnAll(final UnaryFunction<Double> operator) {

        final Function<double[], double[]> mapper = input -> {
            final double[] retVal = new double[input.length];
            for (int i = 0; i < retVal.length; i++) {
                retVal[i] = operator.applyAsDouble(input[i]);
            }
            return retVal;
        };

        return new DistributedPrimitiveElementsSupplier(myDelegate.map(mapper), myRowsCount, myColumnsCount);
    }

    public ElementsSupplier<Double> operateOnMatching(final BinaryFunction<Double> operator, final MatrixStore<Double> right) {
        // TODO Auto-generated method stub
        return ElementsSupplier.super.operateOnMatching(operator, right);
    }

    public ElementsSupplier<Double> operateOnMatching(final MatrixStore<Double> left, final BinaryFunction<Double> operator) {
        // TODO Auto-generated method stub
        return ElementsSupplier.super.operateOnMatching(left, operator);
    }

    public Factory<Double, Primitive64Store> physical() {
        return Primitive64Store.FACTORY;
    }

    public void supplyTo(final TransformableRegion<Double> receiver) {
        // TODO Auto-generated method stub
    }

}
