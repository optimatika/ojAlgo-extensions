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

import java.awt.Color;
import java.util.Map.Entry;

import org.jfree.data.time.FixedMillisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.time.TimeSeriesDataItem;
import org.jfree.data.xy.DefaultTableXYDataset;
import org.jfree.data.xy.IntervalXYDataset;
import org.jfree.data.xy.TableXYDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.ojalgo.function.constant.PrimitiveMath;
import org.ojalgo.random.ContinuousDistribution;
import org.ojalgo.random.Distribution;
import org.ojalgo.scalar.Scalar;
import org.ojalgo.series.CalendarDateSeries;
import org.ojalgo.series.CoordinationSet;
import org.ojalgo.type.CalendarDate;
import org.ojalgo.type.ColourData;

/**
 * TimeSeriesData
 *
 * @author apete
 */
public abstract class CalendarDateSeriesCollection extends AbstractSeriesData<CalendarDate, Number, CalendarDateSeriesCollection> {

    protected final TimeSeriesCollection myCollection = new TimeSeriesCollection() {

        @Override
        public Number getEndY(final int someSeries, final int someItem) {

            final Number tmpVal = super.getY(someSeries, someItem);

            if (tmpVal instanceof NumberWithRange) {
                return ((NumberWithRange) tmpVal).high;
            } else

            if (tmpVal instanceof ContinuousDistributionWrapper) {
                return ((ContinuousDistributionWrapper) tmpVal).getUpperConfidenceQuantile(CalendarDateSeriesCollection.this.getConfidence());
            } else if (tmpVal instanceof ContinuousDistribution) {
                final double tmpProbability = PrimitiveMath.ONE - ((PrimitiveMath.ONE - CalendarDateSeriesCollection.this.getConfidence()) / PrimitiveMath.TWO);
                return ((ContinuousDistribution) tmpVal).getQuantile(tmpProbability);
            } else {
                return tmpVal;
            }
        }

        @Override
        public Number getStartY(final int someSeries, final int someItem) {

            final Number tmpVal = super.getY(someSeries, someItem);
            if (tmpVal instanceof NumberWithRange) {
                return ((NumberWithRange) tmpVal).low;
            } else

            if (tmpVal instanceof ContinuousDistributionWrapper) {
                return ((ContinuousDistributionWrapper) tmpVal).getLowerConfidenceQuantile(CalendarDateSeriesCollection.this.getConfidence());
            } else if (tmpVal instanceof ContinuousDistribution) {
                final double tmpProbability = (PrimitiveMath.ONE - CalendarDateSeriesCollection.this.getConfidence()) / PrimitiveMath.TWO;
                return ((ContinuousDistribution) tmpVal).getQuantile(tmpProbability);
            } else {
                return tmpVal;
            }
        }

        @Override
        public Number getY(final int someSeries, final int someItem) {

            final Number tmpVal = super.getY(someSeries, someItem);
            if (tmpVal instanceof NumberWithRange) {
                return ((NumberWithRange) tmpVal).value;
            } else

            if (tmpVal instanceof ContinuousDistributionWrapper) {
                //return ((LogNormal) tmpVal).getGeometricMean();
                return Double.NaN;
            } else if (tmpVal instanceof Distribution) {
                return ((Distribution) tmpVal).getExpected();
                //return Double.NaN;
            } else {
                return tmpVal;
            }
        }

    };

    private double myConfidence = 0.68;
    private boolean myDeviationRenderer = false;
    private float myAlpha = 0.25F;

    protected CalendarDateSeriesCollection() {
        super();

        domain.time(true);
    }

    public void add(final CalendarDateSeries<?> aSeries) {

        final String tmpName = aSeries.getName();

        final TimeSeries tmpSeries = new TimeSeries(tmpName);
        for (final Entry<CalendarDate, ?> tmpEntry : aSeries.entrySet()) {
            final CalendarDate tmpKey = tmpEntry.getKey();
            final FixedMillisecond tmpPeriod = new FixedMillisecond(tmpKey.millis);
            final Comparable<?> tmpValue = (Comparable<?>) tmpEntry.getValue();
            if (tmpValue instanceof Number) {
                final TimeSeriesDataItem tmpItem = new TimeSeriesDataItem(tmpPeriod, (Number) tmpValue);
                tmpSeries.add(tmpItem);
            } else if (tmpValue instanceof ContinuousDistribution) {
                final TimeSeriesDataItem tmpItem = new TimeSeriesDataItem(tmpPeriod, new ContinuousDistributionWrapper((ContinuousDistribution) tmpValue));
                tmpSeries.add(tmpItem);
            } else {
                final TimeSeriesDataItem tmpItem = new TimeSeriesDataItem(tmpPeriod, Scalar.doubleValue(tmpValue));
                tmpSeries.add(tmpItem);
            }
        }
        myCollection.addSeries(tmpSeries);

        ColourData tmpColour2 = aSeries.getColour();
        if (tmpColour2 == null) {
            tmpColour2 = ColourData.random();
            aSeries.colour(tmpColour2);
        }
        final int tmpRgb = tmpColour2.getRGB();
        final Color tmpColour = new Color(tmpRgb);

        this.putColour(tmpName, tmpColour);
    }

    public void add(final CoordinationSet<?> aSet) {
        for (final CalendarDateSeries<?> tmpSeries : aSet.values()) {
            this.add(tmpSeries);
        }
    }

    public CalendarDateSeriesCollection deviationRenderer(final boolean flag) {
        myDeviationRenderer = flag;
        return this;
    }

    public CalendarDateSeriesCollection deviationRendererAlpha(final float value) {
        myAlpha = value;
        return this;
    }

    protected double getConfidence() {
        return myConfidence;
    }

    protected float getDeviationRendererAlpha() {
        return myAlpha;
    }

    @Override
    protected IntervalXYDataset getIntervalXYData() {
        return myCollection;
    }

    @Override
    protected TableXYDataset getTableXYData() {

        final DefaultTableXYDataset retVal = new DefaultTableXYDataset();

        for (final Object tmpSeries : myCollection.getSeries()) {
            retVal.addSeries((XYSeries) tmpSeries);
        }

        return retVal;
    }

    @Override
    protected XYDataset getXYData() {
        return myCollection;
    }

    protected boolean isDeviationRenderer() {
        return myDeviationRenderer;
    }

    protected void setConfidence(final double newConfidence) {
        myConfidence = newConfidence;
    }

}
