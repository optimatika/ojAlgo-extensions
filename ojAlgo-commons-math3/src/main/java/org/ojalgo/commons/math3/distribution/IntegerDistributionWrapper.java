/*
 * Copyright 1997-2018 Optimatika
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

import org.apache.commons.math3.distribution.IntegerDistribution;
import org.ojalgo.random.DiscreteDistribution;
import org.ojalgo.random.RandomNumber;

public final class IntegerDistributionWrapper<D extends IntegerDistribution> extends RandomNumber implements DiscreteDistribution {

    public static <D extends IntegerDistribution> IntegerDistributionWrapper<D> of(D delegateDistribution) {
        return new IntegerDistributionWrapper<D>(delegateDistribution);
    }

    private final D myDelegateDistribution;

    private IntegerDistributionWrapper(D delegateDistribution) {
        super();
        myDelegateDistribution = delegateDistribution;
    }

    public D getDelegateDistribution() {
        return myDelegateDistribution;
    }

    public double getExpected() {
        return myDelegateDistribution.getNumericalMean();
    }

    public double getProbability(int value) {
        return myDelegateDistribution.probability(value);
    }

    @Override
    public double getStandardDeviation() {
        return Math.sqrt(this.getVariance());
    }

    @Override
    public double getVariance() {
        return myDelegateDistribution.getNumericalVariance();
    }

    @Override
    protected double generate() {
        return myDelegateDistribution.sample();
    }

}
