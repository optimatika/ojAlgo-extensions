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
import java.util.Map.Entry;

import org.jfree.chart.labels.StandardPieSectionLabelGenerator;
import org.jfree.chart.labels.StandardPieToolTipGenerator;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.urls.StandardPieURLGenerator;
import org.jfree.data.general.KeyedValuesDataset;

/**
 * @see KeyedValuesDataset
 * @author apete
 */
public class PieChartBuilder extends AbstractData1D {

    public PieChartBuilder() {
        super();
    }

    @Override
    protected Plot makePlot(final JFreeChartBuilder.PlotParameters parameters) {

        final KeyedValuesDataset tmpDataset = this.getDataset();

        final PiePlot retVal = new PiePlot(tmpDataset);

        retVal.setShadowXOffset(0);
        retVal.setShadowYOffset(0);

        retVal.setBackgroundPaint(parameters.getBackground());
        retVal.setOutlinePaint(parameters.getOutline());

        retVal.setLabelGenerator(new StandardPieSectionLabelGenerator());

        if (this.isTooltips()) {
            retVal.setToolTipGenerator(new StandardPieToolTipGenerator());
        }
        if (this.isUrls()) {
            retVal.setURLGenerator(new StandardPieURLGenerator());
        }

        for (final Entry<Comparable<?>, Paint> tmpEntry : this.getColourSet()) {
            retVal.setSectionPaint(tmpEntry.getKey(), tmpEntry.getValue());
        }

        return retVal;
    }

}
