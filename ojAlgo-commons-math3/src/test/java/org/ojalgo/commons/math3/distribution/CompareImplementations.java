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
import org.apache.commons.math3.special.Erf;
import org.junit.jupiter.api.Test;
import org.ojalgo.TestUtils;
import org.ojalgo.function.special.ErrorFunction;
import org.ojalgo.random.Cauchy;
import org.ojalgo.random.ContinuousDistribution;
import org.ojalgo.random.Normal;
import org.ojalgo.random.RandomNumberTest;
import org.ojalgo.random.TDistribution;

public class CompareImplementations {

    static void compareDensity(final RealDistribution acm, final ContinuousDistribution oj) {

        ContinuousDistribution expected = RealDistributionWrapper.of(acm);
        ContinuousDistribution actual = oj;

        RandomNumberTest.compareDensity(expected, actual);
    }

    static void compareDistribution(final RealDistribution acm, final ContinuousDistribution oj) {

        ContinuousDistribution expected = RealDistributionWrapper.of(acm);
        ContinuousDistribution actual = oj;

        RandomNumberTest.compareDistribution(expected, actual);
    }

    static void compareQuantile(final RealDistribution acm, final ContinuousDistribution oj) {

        ContinuousDistribution expected = RealDistributionWrapper.of(acm);
        ContinuousDistribution actual = oj;

        RandomNumberTest.compareQuantile(expected, actual);
    }

    @Test
    public void testCauchy() {

        org.apache.commons.math3.distribution.CauchyDistribution acm = new org.apache.commons.math3.distribution.CauchyDistribution();
        Cauchy oj = new Cauchy();

        CompareImplementations.compareDensity(acm, oj);
        CompareImplementations.compareDistribution(acm, oj);
        CompareImplementations.compareQuantile(acm, oj);
    }

    @Test
    public void testERF() {
        for (int d = -10; d < 11; d++) {
            TestUtils.assertEquals(Erf.erf(d), ErrorFunction.erf(d), 0.0000000001);
        }
    }

    @Test
    public void testERFC() {
        for (int d = -10; d < 11; d++) {
            TestUtils.assertEquals(Erf.erfc(d), ErrorFunction.erfc(d), 0.0000000001);
        }
    }

    @Test
    public void testERFI() {
        for (int d = -9; d < 10; d++) {
            double value = d / 10.0;
            TestUtils.assertEquals(Erf.erfInv(value), ErrorFunction.erfi(value), 0.0000000001);
        }
    }

    @Test
    public void testNormal() {

        org.apache.commons.math3.distribution.NormalDistribution acm = new org.apache.commons.math3.distribution.NormalDistribution();
        Normal oj = new Normal();

        CompareImplementations.compareDensity(acm, oj);
        CompareImplementations.compareDistribution(acm, oj);
        CompareImplementations.compareQuantile(acm, oj);
    }

    @Test
    public void testTDistribution() {

        int[] densityDegrees = { 1, 2, 3, 4, Integer.MAX_VALUE };
        for (int d = 0; d < densityDegrees.length; d++) {
            int degreesOfFreedom = densityDegrees[d];
            org.apache.commons.math3.distribution.TDistribution acm = new org.apache.commons.math3.distribution.TDistribution(degreesOfFreedom);
            TDistribution oj = TDistribution.make(degreesOfFreedom);
            CompareImplementations.compareDensity(acm, oj);
        }

        int[] distributionDegrees = { 1, 2, Integer.MAX_VALUE };
        for (int d = 0; d < distributionDegrees.length; d++) {
            int degreesOfFreedom = distributionDegrees[d];
            org.apache.commons.math3.distribution.TDistribution acm = new org.apache.commons.math3.distribution.TDistribution(degreesOfFreedom);
            TDistribution oj = TDistribution.make(degreesOfFreedom);
            CompareImplementations.compareDistribution(acm, oj);
        }

        int[] quantileDegrees = { 1, 2, 4, Integer.MAX_VALUE };
        for (int d = 0; d < quantileDegrees.length; d++) {
            int degreesOfFreedom = quantileDegrees[d];
            org.apache.commons.math3.distribution.TDistribution acm = new org.apache.commons.math3.distribution.TDistribution(degreesOfFreedom);
            TDistribution oj = TDistribution.make(degreesOfFreedom);
            CompareImplementations.compareQuantile(acm, oj);
        }

    }

}
