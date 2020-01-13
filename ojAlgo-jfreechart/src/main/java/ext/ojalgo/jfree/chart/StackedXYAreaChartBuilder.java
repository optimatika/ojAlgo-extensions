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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Paint;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.annotations.XYLineAnnotation;
import org.jfree.chart.annotations.XYPointerAnnotation;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.LogarithmicAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StackedXYAreaRenderer2;
import org.jfree.chart.urls.StandardXYURLGenerator;
import org.jfree.chart.urls.XYURLGenerator;
import org.jfree.data.xy.TableXYDataset;
import org.ojalgo.random.Uniform;
import org.ojalgo.type.keyvalue.StringToDouble;

/**
 * @see ChartFactory#createStackedXYAreaChart(String, String, String, TableXYDataset, PlotOrientation,
 *      boolean, boolean, boolean)
 * @author apete
 */
public class StackedXYAreaChartBuilder extends NumberSeriesCollection {

    public StackedXYAreaChartBuilder() {
        super();
    }

    @Override
    protected Plot makePlot(final JFreeChartBuilder.PlotParameters parameters) {

        final TableXYDataset tmpDataset = this.getTableXYData();

        final NumberAxis tmpDomainAxis = domain.isLogarithmic() ? new LogarithmicAxis(domain.getLabel()) : new NumberAxis(domain.getLabel());
        tmpDomainAxis.setNumberFormatOverride(domain.getFormat());
        if (domain.isIntervalSet()) {
            tmpDomainAxis.setAutoRange(false);
            tmpDomainAxis.setRange(domain.getInterval());
        } else {
            tmpDomainAxis.setAutoRange(true);
            tmpDomainAxis.setAutoRangeIncludesZero(false);
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
        }

        XYToolTipGenerator toolTipGenerator = null;
        if (this.isTooltips()) {
            toolTipGenerator = new StandardXYToolTipGenerator();
        }

        XYURLGenerator urlGenerator = null;
        if (this.isUrls()) {
            urlGenerator = new StandardXYURLGenerator();
        }

        final StackedXYAreaRenderer2 tmpRenderer = new StackedXYAreaRenderer2(toolTipGenerator, urlGenerator);
        //tmpRenderer.setOutline(true);

        String tmpKey;
        Paint tmpPaint;
        for (int i = 0; i < tmpDataset.getSeriesCount(); i++) {
            tmpKey = (String) tmpDataset.getSeriesKey(i);
            tmpPaint = this.getColour(tmpKey);
            tmpRenderer.setSeriesPaint(i, tmpPaint);
        }

        final XYPlot retVal = new XYPlot(tmpDataset, tmpDomainAxis, tmpRangeAxis, tmpRenderer);
        retVal.setOrientation(parameters.getOrientation());

        final double tmpLowerRangeBound = tmpRangeAxis.getRange().getLowerBound();
        final double tmpUpperRangeBound = tmpRangeAxis.getRange().getUpperBound();
        retVal.setRangeAxisLocation(0, AxisLocation.TOP_OR_LEFT);

        ValueAxis tmpRangeAxis2;
        try {
            tmpRangeAxis2 = (ValueAxis) retVal.getRangeAxis(0).clone();
            retVal.setRangeAxis(1, tmpRangeAxis2);
            retVal.setRangeAxisLocation(1, AxisLocation.BOTTOM_OR_RIGHT);
        } catch (final CloneNotSupportedException exception) {
            // TODO Auto-generated catch block
            exception.printStackTrace();
        }

        final BasicStroke tmpAnnotationStroke = new BasicStroke(1F);
        final Paint tmpAnnotationPaint = new Color(Color.BLACK.getRed(), Color.BLACK.getGreen(), Color.BLACK.getBlue(), 0.5F);
        for (final StringToDouble tmpAnnotationData : domain.getAnnotations()) {

            double tmpDomainValue = tmpAnnotationData.value;
            tmpDomainValue = Math.max(tmpDomainValue, tmpDomainAxis.getRange().getLowerBound());
            tmpDomainValue = Math.min(tmpDomainValue, tmpDomainAxis.getRange().getUpperBound());

            final XYLineAnnotation tmpLineAnnotation = new XYLineAnnotation(tmpDomainValue, tmpLowerRangeBound, tmpDomainValue, tmpUpperRangeBound,
                    tmpAnnotationStroke, tmpAnnotationPaint);

            final Uniform tmpUniform = new Uniform(0.0, 0.3);
            final double tmpY = tmpUniform.doubleValue() + tmpUniform.doubleValue() + tmpUniform.doubleValue();

            final XYPointerAnnotation tmpPointerAnnotation = new XYPointerAnnotation(tmpAnnotationData.key, tmpDomainValue, tmpY, -Math.PI / 4.0);
            tmpPointerAnnotation.setLabelOffset(10F);

            //final XYShapeAnnotation tmpShapeAnnotation = new XYShapeAnnotation(new Rectangle(50, 50), null, Color.YELLOW, Color.YELLOW);

            tmpPointerAnnotation.setBackgroundPaint(Color.LIGHT_GRAY);
            tmpPointerAnnotation.setOutlineVisible(false);

            retVal.addAnnotation(tmpLineAnnotation);
            //  tmpPlot.addAnnotation(tmpShapeAnnotation);
            retVal.addAnnotation(tmpPointerAnnotation);
        }

        retVal.setBackgroundPaint(parameters.getBackground());
        retVal.setOutlinePaint(parameters.getOutline());

        return retVal;
    }
}
