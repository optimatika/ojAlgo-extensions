/*
 * Copyright 1997-2022 Optimatika
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
package org.ojalgo.optimatika.optimisation;

import java.math.BigDecimal;

import org.ojalgo.function.constant.BigMath;
import org.ojalgo.optimisation.Variable;

public abstract class OptVariable {

    public static final class BinaryVariable extends OptVariable {

        BinaryVariable(final Variable variable) {
            super(variable.binary());
        }

        public BinaryVariable fix() {
            boolean current = this.booleanValue();
            this.getVariable().level(OptModel.value(current));
            return this;
        }

        public BinaryVariable value(final BigDecimal value) {
            return this.value(OptModel.booleanValue(value));
        }

        public BinaryVariable value(final boolean value) {
            this.getVariable().setValue(OptModel.value(value));
            return this;
        }

        public BinaryVariable value(final long value) {
            return this.value(OptModel.booleanValue(value));
        }

    }

    public static final class IntegerVariable extends OptVariable {

        IntegerVariable(final Variable variable) {
            super(variable.integer(true).lower(BigMath.ZERO));
        }

        public IntegerVariable clear() {
            this.getVariable().level(null);
            return this;
        }

        public IntegerVariable fix() {
            long current = this.longValue();
            this.getVariable().level(OptModel.value(current));
            return this;
        }

        public IntegerVariable lower(final BigDecimal lower) {
            return this.lower(OptModel.longValue(lower));
        }

        public IntegerVariable lower(final long lower) {
            this.getVariable().lower(OptModel.value(lower));
            return this;
        }

        public IntegerVariable upper(final BigDecimal upper) {
            return this.upper(OptModel.longValue(upper));
        }

        public IntegerVariable upper(final long upper) {
            this.getVariable().upper(OptModel.value(upper));
            return this;
        }

        public IntegerVariable value(final BigDecimal value) {
            return this.value(OptModel.longValue(value));
        }

        public IntegerVariable value(final long value) {
            this.getVariable().setValue(OptModel.value(value));
            return this;
        }

    }

    public static final class RealVariable extends OptVariable {

        RealVariable(final Variable variable) {
            super(variable.lower(BigMath.ZERO));
        }

        public RealVariable clear() {
            this.getVariable().level(null);
            return this;
        }

        public RealVariable fix() {
            BigDecimal current = this.getValue();
            this.getVariable().level(current);
            return this;
        }

        public RealVariable lower(final BigDecimal lower) {
            this.getVariable().lower(lower);
            return this;
        }

        public RealVariable lower(final double lower) {
            return this.lower(OptModel.value(lower));
        }

        public RealVariable upper(final BigDecimal upper) {
            this.getVariable().upper(upper);
            return this;
        }

        public RealVariable upper(final double upper) {
            return this.upper(OptModel.value(upper));
        }

        public RealVariable value(final BigDecimal value) {
            this.getVariable().setValue(value);
            return this;
        }

        public RealVariable value(final double value) {
            return this.value(OptModel.value(value));
        }

    }

    static final class SemicontinuousVariable extends OptVariable {

        private final Variable myTrigger;

        SemicontinuousVariable(final Variable variable, final BinaryVariable trigger) {
            super(variable);
            myTrigger = trigger.getVariable();
        }

    }

    private final Variable myVariable;

    OptVariable(final Variable variable) {
        super();
        myVariable = variable;
    }

    public boolean booleanValue() {
        BigDecimal value = myVariable.getValue();
        return OptModel.booleanValue(value);
    }

    public double doubleValue() {
        BigDecimal value = myVariable.getValue();
        return OptModel.doubleValue(value);
    }

    public float floatValue() {
        BigDecimal value = myVariable.getValue();
        return OptModel.floatValue(value);
    }

    public BigDecimal getValue() {
        return myVariable.getValue();
    }

    public int intValue() {
        BigDecimal value = myVariable.getValue();
        return OptModel.intValue(value);
    }

    public long longValue() {
        BigDecimal value = myVariable.getValue();
        return OptModel.longValue(value);
    }

    public short shortValue() {
        BigDecimal value = myVariable.getValue();
        return OptModel.shortValue(value);
    }

    Variable getVariable() {
        return myVariable;
    }

}
