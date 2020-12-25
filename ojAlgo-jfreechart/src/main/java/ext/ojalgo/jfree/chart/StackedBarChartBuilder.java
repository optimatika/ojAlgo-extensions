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

import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.labels.StandardCategoryToolTipGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.renderer.category.StackedBarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.chart.urls.StandardCategoryURLGenerator;
import org.jfree.data.general.DefaultKeyedValues2DDataset;

public class StackedBarChartBuilder extends AbstractData2D {

    public StackedBarChartBuilder() {
        super();
    }

    @Override
    protected Plot makePlot(final JFreeChartBuilder.PlotParameters parameters) {

        final DefaultKeyedValues2DDataset tmpDataset = this.getDataset();

        final CategoryAxis tmpCategoryAxis = this.makeCategoryAxis(domain);

        final ValueAxis tmpValueAxis = this.makeValueAxis(range);

        final StackedBarRenderer tmpRenderer = new StackedBarRenderer();
        tmpRenderer.setBarPainter(new StandardBarPainter());
        tmpRenderer.setShadowVisible(false);

        if (this.isTooltips()) {
            tmpRenderer.setDefaultToolTipGenerator(new StandardCategoryToolTipGenerator());
        }
        if (this.isUrls()) {
            tmpRenderer.setDefaultItemURLGenerator(new StandardCategoryURLGenerator());
        }

        this.setColours(tmpRenderer, tmpDataset);

        final CategoryPlot retVal = new CategoryPlot(tmpDataset, tmpCategoryAxis, tmpValueAxis, tmpRenderer);
        retVal.setOrientation(parameters.getOrientation());
        retVal.setBackgroundPaint(parameters.getBackground());
        retVal.setOutlinePaint(parameters.getOutline());

        return retVal;
    }

}
