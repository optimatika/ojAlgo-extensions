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
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.ojalgo.ProgrammingError;

import ext.ojalgo.jfree.ChartBuilder;

public final class JFreeChartAdaptor implements ChartBuilder.ChartResource<JFreeChart> {

    private static final String IMAGE_PNG = "image/png";

    private final JFreeChart myDelegate;

    private int myHeight;
    private int myWidth;

    public JFreeChartAdaptor(final JFreeChart aDelegate) {

        super();

        myDelegate = aDelegate;
    }

    @SuppressWarnings("unused")
    private JFreeChartAdaptor() {

        super();

        myDelegate = null;

        ProgrammingError.throwForIllegalInvocation();
    }

    public Paint getBackground() {
        return myDelegate.getBackgroundPaint();
    }

    public JFreeChart getDelegate() {
        return myDelegate;
    }

    public int getHeight() {
        return myHeight;
    }

    public String getMimeType() {
        return IMAGE_PNG;
    }

    public int getWidth() {
        return myWidth;
    }

    public void setBackground(final Paint aPaint) {
        myDelegate.setBackgroundPaint(aPaint);
    }

    public void setHeight(final int aHeight) {
        myHeight = aHeight;
    }

    public void setWidth(final int aWidth) {
        myWidth = aWidth;
    }

    public byte[] toByteArray() {

        final ByteArrayOutputStream tmpStream = new ByteArrayOutputStream();

        try {
            ChartUtils.writeChartAsPNG(tmpStream, myDelegate, myWidth, myHeight);
        } catch (final IOException anException) {
            // TODO Something!!
        }

        return tmpStream.toByteArray();
    }

}
