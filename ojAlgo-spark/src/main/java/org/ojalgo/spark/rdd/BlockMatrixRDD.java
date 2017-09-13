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
package org.ojalgo.spark.rdd;

import org.apache.spark.Partition;
import org.apache.spark.SparkContext;
import org.apache.spark.TaskContext;
import org.apache.spark.rdd.RDD;

import scala.collection.Iterator;
import scala.collection.mutable.ArrayBuffer;
import scala.reflect.ClassManifestFactory$;
import scala.reflect.ClassTag;

public final class BlockMatrixRDD extends RDD<Double> {

    private static final ClassTag<Double> CLASS_TAG = ClassManifestFactory$.MODULE$.fromClass(double.class);

    public BlockMatrixRDD(final BlockMatrixRDD oneParent) {
        super(oneParent, CLASS_TAG);
    }

    public BlockMatrixRDD(final SparkContext context) {
        super(context, new ArrayBuffer<>(), CLASS_TAG);
    }

    private final int myBlockStructure = 2;
    private final MatrixBlock[] myBlocks = null;

    @Override
    public Iterator<Double> compute(final Partition partition, final TaskContext context) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public MatrixBlock[] getPartitions() {
        return myBlocks;
    }
}
