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
package org.ojalgo.commons.math3.distribution;

import org.apache.commons.math3.distribution.RealDistribution;
import org.ojalgo.random.ContinuousDistribution;
import org.ojalgo.random.RandomNumber;

public final class RealDistributionWrapper<D extends RealDistribution> extends RandomNumber implements ContinuousDistribution {

    public static <D extends RealDistribution> RealDistributionWrapper<D> of(final D delegateDistribution) {
        return new RealDistributionWrapper<>(delegateDistribution);
    }

    private final D myDelegate;

    private RealDistributionWrapper(final D delegateDistribution) {
        super();
        myDelegate = delegateDistribution;
    }

    public D getDelegate() {
        return myDelegate;
    }

    public double getDensity(final double value) {
        return myDelegate.density(value);
    }

    public double getDistribution(final double value) {
        return myDelegate.cumulativeProbability(value);
    }

    public double getExpected() {
        return myDelegate.getNumericalMean();
    }

    public double getQuantile(final double probability) {
        return myDelegate.inverseCumulativeProbability(probability);
    }

    @Override
    public double getStandardDeviation() {
        return Math.sqrt(this.getVariance());
    }

    @Override
    public double getVariance() {
        return myDelegate.getNumericalVariance();
    }

    @Override
    protected double generate() {
        return myDelegate.sample();
    }

}
