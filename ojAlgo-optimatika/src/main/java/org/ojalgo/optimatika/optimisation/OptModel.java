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

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

import org.ojalgo.function.constant.BigMath;
import org.ojalgo.netio.ASCII;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.Optimisation.Result;
import org.ojalgo.optimisation.solver.cplex.SolverCPLEX;
import org.ojalgo.optimisation.solver.gurobi.SolverGurobi;
import org.ojalgo.optimisation.solver.mosek.SolverMosek;
import org.ojalgo.type.context.NumberContext;

public final class OptModel {

    @FunctionalInterface
    public interface BinaryReceiver extends Consumer<Boolean> {

        void accept(boolean value);

        @Override
        default void accept(final Boolean value) {
            this.accept(value.booleanValue());
        }

    }

    @FunctionalInterface
    public interface IntegerReceiver extends IntConsumer {
        // For interface name consistency
    }

    @FunctionalInterface
    public interface RealReceiver extends Consumer<BigDecimal> {
        // For interface name consistency
    }

    private static final NumberContext REAL = NumberContext.of(12, 8);

    static {

        String javaLibraryPath = System.getProperty("java.library.path");
        if (javaLibraryPath != null && javaLibraryPath.length() > 0) {
            String lowerCasePath = javaLibraryPath.toLowerCase();
            if (lowerCasePath.contains("cplex")) {
                ExpressionsBasedModel.addIntegration(SolverCPLEX.INTEGRATION);
            } else if (lowerCasePath.contains("gurobi")) {
                ExpressionsBasedModel.addIntegration(SolverGurobi.INTEGRATION);
            } else if (lowerCasePath.contains("mosek")) {
                ExpressionsBasedModel.addIntegration(SolverMosek.INTEGRATION);
            }
        }
    }

    public static OptModel parse(final File file) {

        ExpressionsBasedModel ebm = ExpressionsBasedModel.parse(file);

        return new OptModel(ebm);

    }

    private static boolean handle(final Result result, final List<Consumer<BigDecimal>> consumers) {

        boolean retVal = result.getState().isFeasible();

        if (retVal && result.size() == consumers.size()) {
            Consumer<BigDecimal> consumer = null;
            for (int i = 0, limit = result.size(); i < limit; i++) {
                consumer = consumers.get(i);
                if (consumer != null) {
                    consumer.accept(result.get(i));
                }
            }
        }

        return retVal;
    }

    private static String name() {
        return ASCII.generateRandom(8, ASCII::isAlphanumeric);
    }

    static boolean booleanValue(final BigDecimal value) {
        return value != null && value.compareTo(BigMath.HALF) > 0;
    }

    static boolean booleanValue(final long value) {
        return value >= 1L;
    }

    static double doubleValue(final BigDecimal value) {
        if (value != null) {
            return value.doubleValue();
        }
        return 0.0;
    }

    static float floatValue(final BigDecimal value) {
        if (value != null) {
            return value.floatValue();
        }
        return 0.0F;
    }

    static int intValue(final BigDecimal value) {
        if (value != null) {
            return value.setScale(0, RoundingMode.HALF_EVEN).intValue();
        }
        return 0;
    }

    static long longValue(final BigDecimal value) {
        if (value != null) {
            return value.setScale(0, RoundingMode.HALF_EVEN).longValue();
        }
        return 0L;
    }

    static short shortValue(final BigDecimal value) {
        if (value != null) {
            return value.setScale(0, RoundingMode.HALF_EVEN).shortValue();
        }
        return 0;
    }

    static BigDecimal value(final boolean value) {
        return value ? BigMath.ONE : BigMath.ZERO;
    }

    static BigDecimal value(final double value) {
        return REAL.toBigDecimal(value);
    }

    static BigDecimal value(final long value) {
        return BigDecimal.valueOf(value);
    }

    private final List<Consumer<BigDecimal>> myConsumers = new ArrayList<>();

    private final ExpressionsBasedModel myDelegate;

    private final OptObjective myObjective;

    public OptModel() {
        this(new ExpressionsBasedModel());
    }

    OptModel(final ExpressionsBasedModel delegate) {
        super();
        myDelegate = delegate;
        myObjective = new OptObjective(delegate);
    }

    public boolean maximise() {
        return OptModel.handle(myDelegate.maximise(), myConsumers);
    }

    public boolean minimise() {
        return OptModel.handle(myDelegate.minimise(), myConsumers);
    }

    public OptVariable.BinaryVariable newBinaryVariable(final BinaryReceiver valueReceiver) {
        return this.newBinaryVariable(OptModel.name(), valueReceiver);
    }

    public OptVariable.BinaryVariable newBinaryVariable(final String name, final BinaryReceiver valueReceiver) {
        myConsumers.add(value -> valueReceiver.accept(OptModel.booleanValue(value)));
        return new OptVariable.BinaryVariable(myDelegate.addVariable(name));
    }

    public OptConstraint newConstraint() {
        return this.newConstraint(OptModel.name());
    }

    public OptConstraint newConstraint(final String name) {
        return new OptConstraint(myDelegate.addExpression(name));
    }

    public OptVariable.IntegerVariable newIntegerVariable(final IntegerReceiver valueReceiver) {
        return this.newIntegerVariable(OptModel.name(), valueReceiver);
    }

    public OptVariable.IntegerVariable newIntegerVariable(final String name, final IntegerReceiver valueReceiver) {
        myConsumers.add(value -> valueReceiver.accept(OptModel.intValue(value)));
        return new OptVariable.IntegerVariable(myDelegate.addVariable(name));
    }

    public OptVariable.RealVariable newRealVariable(final RealReceiver valueReceiver) {
        return this.newRealVariable(OptModel.name(), valueReceiver);
    }

    public OptVariable.RealVariable newRealVariable(final String name, final RealReceiver valueReceiver) {
        myConsumers.add(value -> valueReceiver.accept(value));
        return new OptVariable.RealVariable(myDelegate.addVariable(name));
    }

    public OptObjective objective() {
        return myObjective;
    }

    @Override
    public String toString() {
        return myDelegate.toString();
    }

}
