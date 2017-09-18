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

import org.ojalgo.access.Access1D;
import org.ojalgo.matrix.store.ElementsConsumer;
import org.ojalgo.matrix.store.MatrixStore;
import org.ojalgo.matrix.store.PhysicalStore;
import org.ojalgo.type.context.NumberContext;

public class TestMatrix implements MatrixStore<Double> {

    public TestMatrix() {
    }

    public org.ojalgo.matrix.store.PhysicalStore.Factory<Double, ?> physical() {
        // TODO Auto-generated method stub
        return null;
    }

    public void supplyTo(final ElementsConsumer<Double> receiver) {
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

    public double doubleValue(final long row, final long col) {
        // TODO Auto-generated method stub
        return 0;
    }

    public Double get(final long row, final long col) {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean isAbsolute(final long row, final long col) {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isSmall(final long row, final long col, final double comparedTo) {
        // TODO Auto-generated method stub
        return false;
    }

    public PhysicalStore<Double> copy() {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean equals(final MatrixStore<Double> other, final NumberContext context) {
        // TODO Auto-generated method stub
        return false;
    }

    public Double multiplyBoth(final Access1D<Double> leftAndRight) {
        // TODO Auto-generated method stub
        return null;
    }

}
