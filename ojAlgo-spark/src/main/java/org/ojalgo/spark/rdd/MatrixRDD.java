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

import org.apache.spark.Dependency;
import org.apache.spark.Partition;
import org.apache.spark.SparkContext;
import org.apache.spark.TaskContext;
import org.apache.spark.rdd.RDD;
import org.ojalgo.access.Access2D;
import org.ojalgo.access.Mutate2D;
import org.ojalgo.access.Mutate2D.Receiver;
import org.ojalgo.access.Stream2D;
import org.ojalgo.function.BinaryFunction;
import org.ojalgo.function.UnaryFunction;

import scala.collection.Iterator;
import scala.collection.Seq;
import scala.reflect.ClassTag;

public final class MatrixRDD<N extends Number> extends RDD<N> implements Stream2D<N, Access2D<N>, Mutate2D.Receiver<N>, MatrixRDD<N>> {

    public static void main() {

        final MatrixRDD rdd = null;

    }

    public MatrixRDD(final RDD<?> oneParent, final ClassTag<N> evidence$2) {
        super(oneParent, evidence$2);
    }

    public MatrixRDD(final SparkContext _sc, final Seq<Dependency<?>> deps, final ClassTag<N> evidence$1) {
        super(_sc, deps, evidence$1);
    }

    public void supplyTo(final Receiver<N> receiver) {
        // TODO Auto-generated method stub

    }

    public long countColumns() {
        // TODO Auto-generated method stub
        return 0;
    }

    public long countRows() {
        // TODO Auto-generated method stub
        return 0;
    }

    public MatrixRDD<N> operateOnAll(final UnaryFunction<N> operator) {
        // TODO Auto-generated method stub
        return null;
    }

    public MatrixRDD<N> operateOnMatching(final Access2D<N> left, final BinaryFunction<N> operator) {
        // TODO Auto-generated method stub
        return null;
    }

    public MatrixRDD<N> operateOnMatching(final BinaryFunction<N> operator, final Access2D<N> right) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Iterator<N> compute(final Partition arg0, final TaskContext arg1) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Partition[] getPartitions() {
        // TODO Auto-generated method stub
        return null;
    }

    public MatrixRDD<N> transpose() {
        // TODO Auto-generated method stub
        return null;
    }

}
