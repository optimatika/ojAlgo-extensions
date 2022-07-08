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

import org.ojalgo.optimisation.Expression;

public final class OptConstraint {

    private final Expression myExpression;

    OptConstraint(final Expression expression) {
        super();
        myExpression = expression;
    }

    public OptConstraint coefficient(final OptVariable index, final BigDecimal factor) {
        myExpression.set(index.getVariable(), factor);
        return this;
    }

    public OptConstraint coefficient(final OptVariable index, final boolean factor) {
        return this.coefficient(index, OptModel.value(factor));
    }

    public OptConstraint coefficient(final OptVariable index, final double factor) {
        return this.coefficient(index, OptModel.value(factor));
    }

    public OptConstraint coefficient(final OptVariable index, final long factor) {
        return this.coefficient(index, OptModel.value(factor));
    }

    public OptConstraint level(final BigDecimal level) {
        myExpression.level(level);
        return this;
    }

    public OptConstraint level(final boolean level) {
        return this.level(OptModel.value(level));
    }

    public OptConstraint level(final double level) {
        return this.level(OptModel.value(level));
    }

    public OptConstraint level(final long level) {
        return this.level(OptModel.value(level));
    }

    public OptConstraint lower(final BigDecimal lower) {
        myExpression.lower(lower);
        return this;
    }

    public OptConstraint lower(final boolean lower) {
        return this.lower(OptModel.value(lower));
    }

    public OptConstraint lower(final double lower) {
        return this.lower(OptModel.value(lower));
    }

    public OptConstraint lower(final long lower) {
        return this.lower(OptModel.value(lower));
    }

    public OptConstraint upper(final BigDecimal upper) {
        myExpression.upper(upper);
        return this;
    }

    public OptConstraint upper(final boolean upper) {
        return this.upper(OptModel.value(upper));
    }

    public OptConstraint upper(final double upper) {
        return this.upper(OptModel.value(upper));
    }

    public OptConstraint upper(final long upper) {
        return this.upper(OptModel.value(upper));
    }

    Expression getExpression() {
        return myExpression;
    }

}
