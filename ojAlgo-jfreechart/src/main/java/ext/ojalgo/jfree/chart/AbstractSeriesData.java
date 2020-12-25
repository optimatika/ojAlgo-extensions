/*
 * Copyright 1997-2020 Optimatika
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
package ext.ojalgo.jfree.chart;

import static org.ojalgo.function.constant.PrimitiveMath.*;

import java.awt.Shape;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;

import org.jfree.data.xy.IntervalXYDataset;
import org.jfree.data.xy.TableXYDataset;
import org.jfree.data.xy.XYDataset;
import org.ojalgo.random.ContinuousDistribution;
import org.ojalgo.random.LogNormal;
import org.ojalgo.type.ComparableNumber;

public abstract class AbstractSeriesData<K, V, B extends AbstractSeriesData<K, V, B>> extends JFreeChartBuilder<B> {

    static final class ContinuousDistributionWrapper extends Number implements ComparableNumber<ContinuousDistributionWrapper> {

        private final ContinuousDistribution myDistribution;

        ContinuousDistributionWrapper(ContinuousDistribution distribution) {
            super();
            myDistribution = distribution;
        }

        public int compareTo(ContinuousDistributionWrapper other) {
            return Double.compare(this.doubleValue(), other.doubleValue());
        }

        @Override
        public double doubleValue() {
            return myDistribution.getExpected();
        }

        @Override
        public float floatValue() {
            return (float) myDistribution.getExpected();
        }

        @Override
        public int intValue() {
            return (int) myDistribution.getExpected();
        }

        @Override
        public long longValue() {
            return (long) myDistribution.getExpected();
        }

        public double getLowerConfidenceQuantile(final double confidence) {
            return myDistribution.getQuantile((ONE - confidence) / TWO);
        }

        public double getUpperConfidenceQuantile(final double confidence) {
            return myDistribution.getQuantile(ONE - ((ONE - confidence) / TWO));
        }

    }

    static final class NumberWithRange extends Number implements ComparableNumber<NumberWithRange> {

        public final double high;
        public final double low;
        public final double value;

        public NumberWithRange(final double aValue) {

            super();

            low = aValue;
            value = aValue;
            high = aValue;
        }

        public NumberWithRange(final double aLow, final double aValue, final double aHigh) {

            super();

            low = aLow;
            value = aValue;
            high = aHigh;
        }

        public NumberWithRange(final LogNormal aValue, final double aConfidence) {

            super();

            low = aValue.getLowerConfidenceQuantile(aConfidence);
            value = aValue.getGeometricMean();
            high = aValue.getUpperConfidenceQuantile(aConfidence);
        }

        @SuppressWarnings("unused")
        private NumberWithRange() {

            super();

            low = ZERO;
            value = ZERO;
            high = ZERO;
        }

        public int compareTo(NumberWithRange o) {
            return Double.compare(value, o.value);
        }

        @Override
        public double doubleValue() {
            return value;
        }

        @Override
        public float floatValue() {
            return (float) value;
        }

        @Override
        public int intValue() {
            return (int) value;
        }

        @Override
        public long longValue() {
            return (long) value;
        }

        @Override
        public String toString() {
            // TODO Auto-generated method stub
            return low + " <= " + value + " <= " + high;
        }

    }

    private final HashMap<Comparable<?>, Shape> myShapes = new HashMap<>();

    protected AbstractSeriesData() {
        super();
    }

    protected abstract IntervalXYDataset getIntervalXYData();

    protected final Shape getShape(final Object key) {
        return myShapes.get(key);
    }

    protected final Set<Entry<Comparable<?>, Shape>> getShapeSet() {
        return myShapes.entrySet();
    }

    protected abstract TableXYDataset getTableXYData();

    protected abstract XYDataset getXYData();

    protected final Shape putShape(final Comparable<?> key, final Shape value) {
        return myShapes.put(key, value);
    }

}
