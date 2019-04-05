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

import org.apache.spark.Dependency;
import org.apache.spark.Partition;
import org.apache.spark.SparkContext;
import org.apache.spark.TaskContext;
import org.apache.spark.rdd.RDD;
import org.ojalgo.ProgrammingError;
import org.ojalgo.matrix.store.MatrixStore;

import scala.collection.Iterator;
import scala.collection.Seq;
import scala.reflect.ClassManifestFactory$;
import scala.reflect.ClassTag;

public abstract class OtherBlockMatrixRDD<N extends Number> extends RDD<MatrixStore<N>> {

    @SuppressWarnings({ "unused", "rawtypes" })
    private static final ClassTag<MatrixStore> CLASS_TAG = ClassManifestFactory$.MODULE$.fromClass(MatrixStore.class);

    private final Partition2D[] myBlocks = null;

    private final int myBlockStructure = 2;

    public OtherBlockMatrixRDD(final RDD<?> oneParent, final ClassTag<MatrixStore<N>> evidence$2) {
        super(oneParent, evidence$2);
    }

    public OtherBlockMatrixRDD(final SparkContext _sc, final Seq<Dependency<?>> deps, final ClassTag<MatrixStore<N>> evidence$1) {
        super(_sc, deps, evidence$1);
    }

    @Override
    public Iterator<MatrixStore<N>> compute(final Partition partition, final TaskContext context) {
        ProgrammingError.throwIfNull(partition, context);
        if (partition instanceof Partition2D) {
            return this.compute((Partition2D) partition, context);
        } else {
            throw new IllegalArgumentException();
        }
    }

    @Override
    public Partition2D[] getPartitions() {
        return myBlocks;
    }

    protected abstract Iterator<MatrixStore<N>> compute(Partition2D partition, TaskContext context);

}
