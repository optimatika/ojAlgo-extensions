/*
 * Copyright 1997-2016 Optimatika
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
package org.ojalgo.array;

import java.util.Arrays;

import org.ojalgo.TestUtils;
import org.ojalgo.function.constant.PrimitiveMath;

public class SetGetTest extends BasicArrayTest {

    public SetGetTest() {
        super();
    }

    @Override
    void doTest(final BasicArray<Double> array) {

        for (int i = 0; i < INDICES.length; i++) {
            array.set(INDICES[i], 1.0);
        }

        for (long i = 0L; i < COUNT; i++) {

            final int tmpIndex = Arrays.binarySearch(INDICES, i);

            if (tmpIndex >= 0) {

                TestUtils.assertEquals(i, INDICES[tmpIndex]);

                TestUtils.assertEquals(1.0, array.doubleValue(i), PrimitiveMath.MACHINE_EPSILON);

            } else {

                TestUtils.assertEquals(0.0, array.doubleValue(i), PrimitiveMath.MACHINE_EPSILON);
            }
        }

        for (int i = 0; i < INDICES.length; i++) {
            TestUtils.assertEquals(1.0, array.doubleValue(INDICES[i]), PrimitiveMath.MACHINE_EPSILON);
        }

    }

}
