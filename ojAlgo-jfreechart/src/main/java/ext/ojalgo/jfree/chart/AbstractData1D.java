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

import org.jfree.data.general.DefaultKeyedValuesDataset;
import org.jfree.data.general.KeyedValuesDataset;

/**
 * @see KeyedValuesDataset
 * @author apete
 */
public abstract class AbstractData1D extends JFreeChartBuilder<AbstractData1D> {

    private final DefaultKeyedValuesDataset myDataset = new DefaultKeyedValuesDataset();

    protected AbstractData1D() {
        super();
    }

    public AbstractData1D value(final Comparable<?> aKey, final Number aValue) {
        myDataset.setValue(aKey, aValue);
        return this;
    }

    public AbstractData1D value(final Comparable<?> aKey, final Number aValue, final Paint aPaint) {
        myDataset.setValue(aKey, aValue);
        this.putColour(aKey, aPaint);
        return this;
    }

    protected KeyedValuesDataset getDataset() {
        return myDataset;
    }

}
