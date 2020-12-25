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

import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.LogAxis;
import org.jfree.chart.axis.LogarithmicAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.DeviationRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataset;

/**
 * @author apete
 */
public class HistoryAndForecastBuilder extends CalendarDateSeriesCollection {

    public HistoryAndForecastBuilder() {

        super();
        final CalendarDateSeriesCollection r = this.legend(true).deviationRenderer(true);
        r.range.logarithmic(true);

    }

    @Override
    protected Plot makePlot(final JFreeChartBuilder.PlotParameters parameters) {

        final XYDataset tmpDataset = this.getXYData();

        final ValueAxis tmpTimeAxis = new DateAxis(domain.getLabel());
        if (domain.isIntervalSet()) {
            tmpTimeAxis.setAutoRange(false);
            tmpTimeAxis.setRange(domain.getInterval());
        } else {
            tmpTimeAxis.setAutoRange(true);
        }
        tmpTimeAxis.setLowerMargin(0.02); // reduce the default margins
        tmpTimeAxis.setUpperMargin(0.02);

        ValueAxis tmpValueAxis = null;
        if (range.isLogarithmic()) {
            if (range.isBaseSet()) {
                tmpValueAxis = new LogAxis(range.getLabel());
                ((LogAxis) tmpValueAxis).setBase(range.getBase());
                ((LogAxis) tmpValueAxis).setNumberFormatOverride(range.getFormat());
            } else {
                tmpValueAxis = new LogarithmicAxis(range.getLabel());
                ((NumberAxis) tmpValueAxis).setNumberFormatOverride(range.getFormat());
            }
        } else {
            tmpValueAxis = new NumberAxis(range.getLabel());
            ((NumberAxis) tmpValueAxis).setNumberFormatOverride(range.getFormat());
        }

        if (range.isIntervalSet()) {
            tmpValueAxis.setAutoRange(false);
            tmpValueAxis.setRange(range.getInterval());
        } else {
            tmpValueAxis.setAutoRange(true);
        }

        final XYPlot retVal = new XYPlot(tmpDataset, tmpTimeAxis, tmpValueAxis, null);

        XYLineAndShapeRenderer tmpRenderer = null;
        if (this.isDeviationRenderer()) {
            tmpRenderer = new DeviationRenderer(true, false);
            ((DeviationRenderer) tmpRenderer).setAlpha(this.getDeviationRendererAlpha());
        } else {
            tmpRenderer = new XYLineAndShapeRenderer(true, false);
        }
        tmpRenderer.setDefaultToolTipGenerator(null);
        tmpRenderer.setURLGenerator(null);

        String tmpKey;
        Paint tmpPaint;
        for (int i = 0; i < tmpDataset.getSeriesCount(); i++) {
            tmpKey = (String) tmpDataset.getSeriesKey(i);
            tmpPaint = this.getColour(tmpKey);
            if (tmpPaint != null) {
                tmpRenderer.setSeriesPaint(i, tmpPaint);
                tmpRenderer.setSeriesFillPaint(i, tmpPaint);
            }
        }

        retVal.setRenderer(tmpRenderer);

        retVal.setBackgroundPaint(parameters.getBackground());
        retVal.setOutlinePaint(parameters.getOutline());

        return retVal;
    }

}
