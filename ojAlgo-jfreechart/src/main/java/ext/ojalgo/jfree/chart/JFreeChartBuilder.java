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
import java.awt.Font;
import java.awt.Paint;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.Axis;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.Range;
import org.ojalgo.type.ColourData;
import org.ojalgo.type.keyvalue.StringToDouble;

import ext.ojalgo.jfree.ChartBuilder;

/**
 * @author apete
 */
public abstract class JFreeChartBuilder<B extends JFreeChartBuilder<B>> extends ChartBuilder<JFreeChartAdaptor, B> {

    public static final class AxisParameters {

        private final ArrayList<StringToDouble> myAnnotations = new ArrayList<>();
        private Double myBase = null;
        private boolean myCategory = false;
        private NumberFormat myFormat = null;
        private Range myInterval = null;
        private boolean myInverted = false;
        private String myLabel = null;
        private boolean myLogarithmic = false;
        private boolean myTime = false;
        private boolean myZeroIncluded = false;

        AxisParameters() {
            super();
        }

        public final AxisParameters annotation(final String text, final Number value) {
            myAnnotations.add(new StringToDouble(text, value.doubleValue()));
            return this;
        }

        public final AxisParameters base(final double base) {
            myBase = base;
            return this;
        }

        public final AxisParameters category(final boolean category) {
            myCategory = category;
            return this;
        }

        public final AxisParameters format(final NumberFormat format) {
            myFormat = format;
            return this;
        }

        public final AxisParameters interval(final double lower, final double upper) {
            myInterval = new Range(lower, upper);
            return this;
        }

        public final AxisParameters inverted(final boolean inverted) {
            myInverted = inverted;
            return this;
        }

        public final AxisParameters label(final String label) {
            myLabel = label;
            return this;
        }

        public final AxisParameters logarithmic(final boolean logarithmic) {
            myLogarithmic = logarithmic;
            return this;
        }

        public final AxisParameters time(final boolean time) {
            myTime = time;
            return this;
        }

        public final AxisParameters zeroIncluded(final boolean zeroIncluded) {
            myZeroIncluded = zeroIncluded;
            return this;
        }

        protected final ArrayList<StringToDouble> getAnnotations() {
            return myAnnotations;
        }

        protected final Double getBase() {
            return myBase;
        }

        protected final NumberFormat getFormat() {
            return myFormat;
        }

        protected final Range getInterval() {
            return myInterval;
        }

        protected final String getLabel() {
            return myLabel;
        }

        protected final boolean isBaseSet() {
            return myBase != null;
        }

        protected final boolean isCategory() {
            return myCategory;
        }

        protected final boolean isFormatSet() {
            return myFormat != null;
        }

        protected final boolean isIntervalSet() {
            return myInterval != null;
        }

        protected final boolean isInverted() {
            return myInverted;
        }

        protected final boolean isLabelSet() {
            return myLabel != null;
        }

        protected final boolean isLogarithmic() {
            return myLogarithmic;
        }

        protected final boolean isTime() {
            return myTime;
        }

        protected final boolean isZeroIncluded() {
            return myZeroIncluded;
        }

    }

    public static final class PlotParameters {

        private Paint myBackground = new Color(ColourData.WHITE.getRGB());
        private Orientation myOrientation = Orientation.VERTICAL;
        private Paint myOutline = new Color(ColourData.WHITE.getRGB());

        PlotParameters() {
            super();
        }

        public final PlotParameters background(final Paint background) {
            myBackground = background;
            return this;
        }

        public final PlotParameters orientation(final Orientation anOrientation) {
            myOrientation = anOrientation;
            return this;
        }

        public final PlotParameters outline(final Paint outline) {
            myOutline = outline;
            return this;
        }

        protected final Paint getBackground() {
            return myBackground;
        }

        protected final PlotOrientation getOrientation() {

            PlotOrientation retVal = PlotOrientation.VERTICAL;

            if (myOrientation == Orientation.HORISONTAL) {
                retVal = PlotOrientation.HORIZONTAL;
            }

            return retVal;
        }

        protected final Paint getOutline() {
            return myOutline;
        }

    }

    public final AxisParameters domain = new AxisParameters();
    public final PlotParameters plot = new PlotParameters();
    public final AxisParameters range = new AxisParameters();

    private Paint myBackground = new Color(ColourData.WHITE.getRGB());
    private boolean myBorder = false;
    private final HashMap<Comparable<?>, Paint> myColours = new HashMap<>();

    private boolean myLegend = false;
    private String myTitle = null;
    private boolean myTooltips = false;
    private boolean myUrls = false;

    protected JFreeChartBuilder() {
        super();
    }

    @SuppressWarnings("unchecked")
    public final B background(final Paint aPaint) {
        myBackground = aPaint;
        return (B) this;
    }

    @SuppressWarnings("unchecked")
    public final B border(final boolean aFlag) {
        myBorder = aFlag;
        return (B) this;
    }

    @Override
    public final JFreeChartAdaptor build() {

        final String tmpTitle = this.getTitle();
        final Font tmpTitleFont = JFreeChart.DEFAULT_TITLE_FONT;

        final Plot tmpPlot = this.makePlot(plot);

        final boolean tmpLegend = this.isLegend();

        final JFreeChart retVal = new JFreeChart(tmpTitle, tmpTitleFont, tmpPlot, tmpLegend);
        retVal.setBackgroundPaint(this.getBackground());
        retVal.setBorderVisible(this.isBorder());

        return new JFreeChartAdaptor(retVal);
    }

    @SuppressWarnings("unchecked")
    public final B legend(final boolean aFlag) {
        myLegend = aFlag;
        return (B) this;
    }

    @SuppressWarnings("unchecked")
    public final B title(final String aTitle) {
        myTitle = aTitle;
        return (B) this;
    }

    @SuppressWarnings("unchecked")
    public final B tooltips(final boolean aFlag) {
        myTooltips = aFlag;
        return (B) this;
    }

    @SuppressWarnings("unchecked")
    public final B urls(final boolean aFlag) {
        myUrls = aFlag;
        return (B) this;
    }

    protected final Paint getBackground() {
        return myBackground;
    }

    protected final Paint getColour(final Comparable<?> key) {

        Paint retVal = myColours.get(key);

        if (retVal == null) {

            retVal = new Color(ColourData.random().getRGB());

            myColours.put(key, retVal);
        }

        return retVal;
    }

    protected final Set<Entry<Comparable<?>, Paint>> getColourSet() {
        return myColours.entrySet();
    }

    protected final String getTitle() {
        return myTitle;
    }

    protected final boolean isBorder() {
        return myBorder;
    }

    protected final boolean isLegend() {
        return myLegend;
    }

    protected final boolean isTooltips() {
        return myTooltips;
    }

    protected final boolean isUrls() {
        return myUrls;
    }

    @SuppressWarnings("unchecked")
    protected final <A extends Axis> A makeAxis(final JFreeChartBuilder.AxisParameters parameters) {
        if (parameters.isCategory()) {
            return (A) this.makeCategoryAxis(parameters);
        } else {
            return (A) this.makeValueAxis(parameters);
        }
    }

    @SuppressWarnings("unchecked")
    protected final <A extends CategoryAxis> A makeCategoryAxis(final JFreeChartBuilder.AxisParameters parameters) {
        return (A) new CategoryAxis(parameters.getLabel());
    }

    protected abstract Plot makePlot(JFreeChartBuilder.PlotParameters parameters);

    @SuppressWarnings("unchecked")
    protected final <A extends ValueAxis> A makeValueAxis(final JFreeChartBuilder.AxisParameters parameters) {

        final A retVal = (A) new NumberAxis(parameters.getLabel());

        if (parameters.isIntervalSet()) {
            retVal.setAutoRange(false);
            retVal.setRange(parameters.getInterval());
        } else {
            retVal.setAutoRange(true);
        }
        ((NumberAxis) retVal).setNumberFormatOverride(parameters.getFormat());

        return retVal;

    }

    protected final Paint putColour(final Comparable<?> key, final Paint value) {
        return myColours.put(key, value);
    }

}
