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
import java.awt.Shape;
import java.util.Map;

import org.jfree.data.xy.DefaultTableXYDataset;
import org.jfree.data.xy.IntervalXYDataset;
import org.jfree.data.xy.TableXYDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.ojalgo.scalar.Scalar;
import org.ojalgo.series.NumberSeries;

/**
 * XYSeriesData
 *
 * @author apete
 */
public abstract class NumberSeriesCollection extends AbstractSeriesData<Number, Number, NumberSeriesCollection> {

    private final XYSeriesCollection myCollection = new XYSeriesCollection();

    protected NumberSeriesCollection() {

        super();

        domain.time(false);
    }

    /**
     * @deprecated Use {@link #addSeries(NumberSeries, Shape)} instead
     */
    @Deprecated
    public void addSeries(final NumberSeries<?> aSeries) {
        this.addSeries(aSeries, null);
    }

    public void addSeries(final NumberSeries<?> aSeries, final Shape shape) {

        final XYSeries tmpSeries = new XYSeries(aSeries.getName(), true, false);

        Comparable<?> tmpKey;
        Comparable<?> tmpValue;
        for (final Map.Entry<? extends Comparable<?>, ? extends Comparable<?>> tmpEntry : aSeries.entrySet()) {
            tmpKey = tmpEntry.getKey();
            tmpValue = tmpEntry.getValue();
            tmpSeries.add(Scalar.doubleValue(tmpKey), Scalar.doubleValue(tmpValue));
        }

        myCollection.addSeries(tmpSeries);

        this.putColour(aSeries.getName(), new Color(aSeries.getColour().getRGB()));
        this.putShape(aSeries.getName(), shape);
    }

    @Override
    protected IntervalXYDataset getIntervalXYData() {
        return myCollection;
    }

    @Override
    protected TableXYDataset getTableXYData() {

        final DefaultTableXYDataset retVal = new DefaultTableXYDataset();

        for (final Object tmpXYSeries : myCollection.getSeries()) {
            retVal.addSeries((XYSeries) tmpXYSeries);
        }

        return retVal;
    }

    @Override
    protected XYDataset getXYData() {
        return myCollection;
    }
}
