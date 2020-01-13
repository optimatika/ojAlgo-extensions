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

import java.awt.Paint;

import org.jfree.chart.renderer.AbstractRenderer;
import org.jfree.data.KeyedValues2D;
import org.jfree.data.general.DefaultKeyedValues2DDataset;

/**
 * @author apete
 */
public abstract class AbstractData2D extends JFreeChartBuilder<AbstractData2D> {

    private final DefaultKeyedValues2DDataset myDataset = new DefaultKeyedValues2DDataset();

    protected AbstractData2D() {
        super();
    }

    public AbstractData2D colour(final Comparable<?> aKey, final Paint aPaint) {
        this.putColour(aKey, aPaint);
        return this;
    }

    public AbstractData2D value(final Comparable<?> aRowKey, final Comparable<?> aColumnKey, final Number aValue) {
        myDataset.setValue(aValue, aRowKey, aColumnKey);
        return this;
    }

    /**
     * It seems JFreeChart assumes row=series and column=category, and the paint is associated with a series
     */
    public AbstractData2D value(final Comparable<?> aRowKey, final Comparable<?> aColumnKey, final Number aValue, final Paint aPaint) {
        myDataset.setValue(aValue, aRowKey, aColumnKey);
        this.putColour(aRowKey, aPaint);
        return this;
    }

    protected DefaultKeyedValues2DDataset getDataset() {
        return myDataset;
    }

    protected void setColours(final AbstractRenderer renderer, final KeyedValues2D dataset) {
        final int tmpRowDim = dataset.getRowCount();
        for (int i = 0; i < tmpRowDim; i++) {
            final Comparable<?> tmpKey = dataset.getRowKey(i);
            final Paint tmpPaint = this.getColour(tmpKey);
            renderer.setSeriesPaint(i, tmpPaint);
        }
    }

}
