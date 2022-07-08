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
import org.ojalgo.optimisation.Expression;
import org.ojalgo.optimisation.ExpressionsBasedModel;

public final class OptObjective {

    private final ExpressionsBasedModel myModel;
    private Expression myQuadraticPart = null;

    OptObjective(final ExpressionsBasedModel model) {
        super();
        myModel = model;
    }

    public BigDecimal getValue() {
        return myModel.objective().evaluate(myModel.getVariableValues());
    }

    public OptObjective weight(final OptVariable index, final BigDecimal weight) {
        index.getVariable().weight(weight);
        return this;
    }

    public OptObjective weight(final OptVariable index, final boolean weight) {
        return this.weight(index, OptModel.value(weight));
    }

    public OptObjective weight(final OptVariable index, final double weight) {
        return this.weight(index, OptModel.value(weight));
    }

    public OptObjective weight(final OptVariable index, final long weight) {
        return this.weight(index, OptModel.value(weight));
    }

    public OptObjective weight(final OptVariable row, final OptVariable col, final BigDecimal weight) {
        if (myQuadraticPart == null) {
            myQuadraticPart = myModel.addExpression("Objective^2").weight(BigMath.ONE);
        }
        myQuadraticPart.set(row.getVariable(), col.getVariable(), weight);
        return this;
    }

    public OptObjective weight(final OptVariable row, final OptVariable col, final boolean weight) {
        return this.weight(row, col, OptModel.value(weight));
    }

    public OptObjective weight(final OptVariable row, final OptVariable col, final double weight) {
        return this.weight(row, col, OptModel.value(weight));
    }

    public OptObjective weight(final OptVariable row, final OptVariable col, final long weight) {
        return this.weight(row, col, OptModel.value(weight));
    }

}
