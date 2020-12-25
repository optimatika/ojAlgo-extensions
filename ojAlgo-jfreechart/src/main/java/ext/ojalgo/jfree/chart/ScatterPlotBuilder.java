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
import java.awt.Shape;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.axis.LogarithmicAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.urls.StandardXYURLGenerator;
import org.jfree.chart.urls.XYURLGenerator;
import org.jfree.data.xy.XYDataset;

/**
 * XYSeriesData
 * {@linkplain ChartFactory#createScatterPlot(String, String, String, org.jfree.data.xy.XYDataset, org.jfree.chart.plot.PlotOrientation, boolean, boolean, boolean)}
 *
 * @author apete
 */
public class ScatterPlotBuilder extends NumberSeriesCollection {

    public ScatterPlotBuilder() {
        super();
    }

    @Override
    protected Plot makePlot(final JFreeChartBuilder.PlotParameters parameters) {

        final PlotOrientation orientation = parameters.getOrientation();

        final NumberAxis tmpDomainAxis = domain.isLogarithmic() ? new LogarithmicAxis(domain.getLabel()) : new NumberAxis(domain.getLabel());
        tmpDomainAxis.setNumberFormatOverride(domain.getFormat());
        if (domain.isIntervalSet()) {
            tmpDomainAxis.setAutoRange(false);
            tmpDomainAxis.setRange(domain.getInterval());
        } else {
            tmpDomainAxis.setAutoRange(true);
            tmpDomainAxis.setAutoRangeIncludesZero(range.isZeroIncluded());
        }
        tmpDomainAxis.setInverted(domain.isInverted());
        tmpDomainAxis.setLowerMargin(0.0);
        tmpDomainAxis.setUpperMargin(0.0);

        final NumberAxis tmpRangeAxis = range.isLogarithmic() ? new LogarithmicAxis(range.getLabel()) : new NumberAxis(range.getLabel());
        tmpRangeAxis.setNumberFormatOverride(range.getFormat());
        if (range.isIntervalSet()) {
            tmpRangeAxis.setAutoRange(false);
            tmpRangeAxis.setRange(range.getInterval());
        } else {
            tmpRangeAxis.setAutoRange(true);
            tmpRangeAxis.setAutoRangeIncludesZero(range.isZeroIncluded());
        }

        final XYDataset tmpDataset = this.getXYData();
        final XYPlot retVal = new XYPlot(tmpDataset, tmpDomainAxis, tmpRangeAxis, null);

        XYToolTipGenerator toolTipGenerator = null;
        if (this.isTooltips()) {
            toolTipGenerator = new StandardXYToolTipGenerator();
        }

        XYURLGenerator urlGenerator = null;
        if (this.isUrls()) {
            urlGenerator = new StandardXYURLGenerator();
        }
        final XYItemRenderer tmpRenderer = new XYLineAndShapeRenderer(false, true);
        tmpRenderer.setDefaultToolTipGenerator(toolTipGenerator);
        tmpRenderer.setURLGenerator(urlGenerator);
        retVal.setRenderer(tmpRenderer);
        retVal.setOrientation(orientation);

        retVal.setBackgroundPaint(parameters.getBackground());
        retVal.setOutlinePaint(parameters.getOutline());

        String tmpKey;
        Paint tmpPaint;
        Shape tmpShape;
        for (int i = 0; i < tmpDataset.getSeriesCount(); i++) {
            tmpKey = (String) tmpDataset.getSeriesKey(i);
            tmpPaint = this.getColour(tmpKey);
            if (tmpPaint != null) {
                tmpRenderer.setSeriesPaint(i, tmpPaint);
            }
            tmpShape = this.getShape(tmpKey);
            if (tmpShape != null) {
                tmpRenderer.setSeriesShape(i, tmpShape);
            }
        }

        return retVal;
    }
}
