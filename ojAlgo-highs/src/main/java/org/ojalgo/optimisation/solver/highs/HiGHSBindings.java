/*
 * Copyright 1997-2026 Optimatika
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
package org.ojalgo.optimisation.solver.highs;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.function.BooleanSupplier;

import org.ojalgo.netio.BasicLogger;

/**
 * FFM bindings for HiGHS C API.
 * <p>
 * This class provides Java bindings to the HiGHS native library using the Foreign Function & Memory API
 * (FFM). All native function calls are accessed via {@link MethodHandle} instances obtained from the native
 * linker.
 *
 * @see <a href="https://ergo-code.github.io/HiGHS/dev/interfaces/c/">HiGHS C API</a>
 */
final class HiGHSBindings {

    /**
     * HiGHS model status constants matching HighsModelStatus enum in HiGHS.
     */
    static final int MODEL_STATUS_NOTSET = 0;
    static final int MODEL_STATUS_LOAD_ERROR = 1;
    static final int MODEL_STATUS_MODEL_ERROR = 2;
    static final int MODEL_STATUS_PRESOLVE_ERROR = 3;
    static final int MODEL_STATUS_SOLVE_ERROR = 4;
    static final int MODEL_STATUS_POSTSOLVE_ERROR = 5;
    static final int MODEL_STATUS_MODEL_EMPTY = 6;
    static final int MODEL_STATUS_OPTIMAL = 7;
    static final int MODEL_STATUS_INFEASIBLE = 8;
    static final int MODEL_STATUS_UNBOUNDED_OR_INFEASIBLE = 9;
    static final int MODEL_STATUS_UNBOUNDED = 10;
    static final int MODEL_STATUS_OBJECTIVE_BOUND = 11;
    static final int MODEL_STATUS_OBJECTIVE_TARGET = 12;
    static final int MODEL_STATUS_TIME_LIMIT = 13;
    static final int MODEL_STATUS_ITERATION_LIMIT = 14;
    static final int MODEL_STATUS_UNKNOWN = 15;
    static final int MODEL_STATUS_SOLUTION_LIMIT = 16;
    static final int MODEL_STATUS_INTERRUPT = 17;
    static final int MODEL_STATUS_MEMORY_LIMIT = 18;
    /**
     * HiGHS stopping itself rather than being asked to, which it reports separately from
     * {@link #MODEL_STATUS_INTERRUPT}.
     */
    static final int MODEL_STATUS_HIGHS_INTERRUPT = 19;

    /**
     * HiGHS return status constants.
     */
    static final int STATUS_ERROR = -1;
    static final int STATUS_OK = 0;

    /** Values of the {@code primal_solution_status} info: no solution, an infeasible one, a feasible one. */
    static final int SOLUTION_STATUS_NONE = 0;
    static final int SOLUTION_STATUS_INFEASIBLE = 1;
    static final int SOLUTION_STATUS_FEASIBLE = 2;
    static final int STATUS_WARNING = 1;

    /**
     * Objective sense constants.
     */
    static final int SENSE_MINIMIZE = 1;
    static final int SENSE_MAXIMIZE = -1;

    /**
     * Matrix format constants.
     */
    static final int MATRIX_FORMAT_COLWISE = 1;
    static final int MATRIX_FORMAT_ROWWISE = 2;

    /**
     * Variable type constants.
     */
    static final int VAR_TYPE_CONTINUOUS = 0;
    static final int VAR_TYPE_INTEGER = 1;
    static final int VAR_TYPE_SEMI_CONTINUOUS = 2;
    static final int VAR_TYPE_SEMI_INTEGER = 3;
    static final int VAR_TYPE_IMPLICIT_INTEGER = 4;

    /**
     * Hessian format constants.
     */
    static final int HESSIAN_FORMAT_TRIANGULAR = 1;
    static final int HESSIAN_FORMAT_SQUARE = 2;

    /**
     * {@code HighsCallbackType} values, of which only the ones that can stop a solve are of interest here.
     * One per algorithm, because which of them runs depends on the model.
     */
    static final int CALLBACK_SIMPLEX_INTERRUPT = 1;
    static final int CALLBACK_IPM_INTERRUPT = 2;
    static final int CALLBACK_MIP_INTERRUPT = 6;

    private static final Linker LINKER = Linker.nativeLinker();
    private static final SymbolLookup LOOKUP;
    /**
     * {@code void (*)(int callback_type, const char* message, const HighsCallbackDataOut* data_out,
     * HighsCallbackDataIn* data_in, void* user_callback_data)}
     */
    private static final FunctionDescriptor CALLBACK_DESCRIPTOR = FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS,
            ValueLayout.ADDRESS, ValueLayout.ADDRESS);
    private static final MethodHandle CALLBACK_TARGET;

    private static final MethodHandle HIGHS_CREATE;
    private static final MethodHandle HIGHS_DESTROY;
    private static final MethodHandle HIGHS_RUN;
    private static final MethodHandle HIGHS_CLEAR;
    private static final MethodHandle HIGHS_CLEAR_MODEL;
    private static final MethodHandle HIGHS_CLEAR_SOLVER;

    private static final MethodHandle HIGHS_PASS_LP;
    private static final MethodHandle HIGHS_PASS_MIP;
    private static final MethodHandle HIGHS_PASS_HESSIAN;

    private static final MethodHandle HIGHS_GET_DUAL_RAY;
    private static final MethodHandle HIGHS_GET_MODEL_STATUS;
    private static final MethodHandle HIGHS_GET_SOLUTION;
    private static final MethodHandle HIGHS_GET_NUM_COL;
    private static final MethodHandle HIGHS_GET_NUM_ROW;
    private static final MethodHandle HIGHS_GET_OBJECTIVE_VALUE;
    private static final MethodHandle HIGHS_GET_INT_INFO_VALUE;

    private static final MethodHandle HIGHS_SET_BOOL_OPTION_VALUE;
    private static final MethodHandle HIGHS_SET_INT_OPTION_VALUE;
    private static final MethodHandle HIGHS_SET_DOUBLE_OPTION_VALUE;
    private static final MethodHandle HIGHS_SET_STRING_OPTION_VALUE;

    private static final MethodHandle HIGHS_SET_CALLBACK;
    private static final MethodHandle HIGHS_START_CALLBACK;

    private static final MethodHandle HIGHS_CHANGE_COL_BOUNDS;
    private static final MethodHandle HIGHS_CHANGE_COLS_BOUNDS_BY_RANGE;
    private static final MethodHandle HIGHS_CHANGE_COLS_BOUNDS_BY_SET;
    private static final MethodHandle HIGHS_CHANGE_COLS_BOUNDS_BY_MASK;

    static {

        LOOKUP = NativeLoader.load();

        try {
            CALLBACK_TARGET = MethodHandles.lookup().findStatic(HiGHSBindings.class, "onCallback", MethodType.methodType(void.class, BooleanSupplier.class,
                    int.class, MemorySegment.class, MemorySegment.class, MemorySegment.class, MemorySegment.class));
        } catch (NoSuchMethodException | IllegalAccessException cause) {
            throw new ExceptionInInitializerError(cause);
        }

        HIGHS_CREATE = HiGHSBindings.downcallHandle("Highs_create", FunctionDescriptor.of(ValueLayout.ADDRESS));

        HIGHS_DESTROY = HiGHSBindings.downcallHandle("Highs_destroy", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

        HIGHS_RUN = HiGHSBindings.downcallHandle("Highs_run", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

        HIGHS_CLEAR = HiGHSBindings.downcallHandle("Highs_clear", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

        HIGHS_CLEAR_MODEL = HiGHSBindings.downcallHandle("Highs_clearModel", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

        HIGHS_CLEAR_SOLVER = HiGHSBindings.downcallHandle("Highs_clearSolver", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

        HIGHS_PASS_LP = HiGHSBindings.downcallHandle("Highs_passLp", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, // highs
                ValueLayout.JAVA_INT, // num_col
                ValueLayout.JAVA_INT, // num_row
                ValueLayout.JAVA_INT, // num_nz
                ValueLayout.JAVA_INT, // a_format (column-wise or row-wise)
                ValueLayout.JAVA_INT, // sense (minimize = 1, maximize = -1)
                ValueLayout.JAVA_DOUBLE, // offset
                ValueLayout.ADDRESS, // col_cost (double*)
                ValueLayout.ADDRESS, // col_lower (double*)
                ValueLayout.ADDRESS, // col_upper (double*)
                ValueLayout.ADDRESS, // row_lower (double*)
                ValueLayout.ADDRESS, // row_upper (double*)
                ValueLayout.ADDRESS, // a_start (int*)
                ValueLayout.ADDRESS, // a_index (int*)
                ValueLayout.ADDRESS)); // a_value (double*)

        HIGHS_PASS_MIP = HiGHSBindings.downcallHandle("Highs_passMip", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, // highs
                ValueLayout.JAVA_INT, // num_col
                ValueLayout.JAVA_INT, // num_row
                ValueLayout.JAVA_INT, // num_nz
                ValueLayout.JAVA_INT, // a_format
                ValueLayout.JAVA_INT, // sense
                ValueLayout.JAVA_DOUBLE, // offset
                ValueLayout.ADDRESS, // col_cost (double*)
                ValueLayout.ADDRESS, // col_lower (double*)
                ValueLayout.ADDRESS, // col_upper (double*)
                ValueLayout.ADDRESS, // row_lower (double*)
                ValueLayout.ADDRESS, // row_upper (double*)
                ValueLayout.ADDRESS, // a_start (int*)
                ValueLayout.ADDRESS, // a_index (int*)
                ValueLayout.ADDRESS, // a_value (double*)
                ValueLayout.ADDRESS)); // integrality (int*)

        HIGHS_PASS_HESSIAN = HiGHSBindings.downcallHandle("Highs_passHessian", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, // highs
                ValueLayout.JAVA_INT, // dim
                ValueLayout.JAVA_INT, // num_nz
                ValueLayout.JAVA_INT, // format (triangular or square)
                ValueLayout.ADDRESS, // q_start (int*)
                ValueLayout.ADDRESS, // q_index (int*)
                ValueLayout.ADDRESS)); // q_value (double*)

        HIGHS_GET_DUAL_RAY = HiGHSBindings.downcallHandle("Highs_getDualRay", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, // highs
                ValueLayout.ADDRESS, // has_dual_ray (HighsInt*)
                ValueLayout.ADDRESS)); // dual_ray_value (double*)

        HIGHS_GET_MODEL_STATUS = HiGHSBindings.downcallHandle("Highs_getModelStatus", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

        HIGHS_GET_SOLUTION = HiGHSBindings.downcallHandle("Highs_getSolution", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, // highs
                ValueLayout.ADDRESS, // col_value (double*)
                ValueLayout.ADDRESS, // col_dual (double*)
                ValueLayout.ADDRESS, // row_value (double*)
                ValueLayout.ADDRESS)); // row_dual (double*)

        HIGHS_GET_NUM_COL = HiGHSBindings.downcallHandle("Highs_getNumCol", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

        HIGHS_GET_NUM_ROW = HiGHSBindings.downcallHandle("Highs_getNumRow", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

        HIGHS_GET_OBJECTIVE_VALUE = HiGHSBindings.downcallHandle("Highs_getObjectiveValue",
                FunctionDescriptor.of(ValueLayout.JAVA_DOUBLE, ValueLayout.ADDRESS));

        HIGHS_GET_INT_INFO_VALUE = HiGHSBindings.downcallHandle("Highs_getIntInfoValue", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, // highs
                ValueLayout.ADDRESS, // info (const char*)
                ValueLayout.ADDRESS)); // value (HighsInt*)

        HIGHS_SET_BOOL_OPTION_VALUE = HiGHSBindings.downcallHandle("Highs_setBoolOptionValue", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, // highs
                ValueLayout.ADDRESS, // option (const char*)
                ValueLayout.JAVA_INT)); // value

        HIGHS_SET_INT_OPTION_VALUE = HiGHSBindings.downcallHandle("Highs_setIntOptionValue", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, // highs
                ValueLayout.ADDRESS, // option (const char*)
                ValueLayout.JAVA_INT)); // value

        HIGHS_SET_DOUBLE_OPTION_VALUE = HiGHSBindings.downcallHandle("Highs_setDoubleOptionValue",
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, // highs
                        ValueLayout.ADDRESS, // option (const char*)
                        ValueLayout.JAVA_DOUBLE)); // value

        HIGHS_SET_STRING_OPTION_VALUE = HiGHSBindings.downcallHandle("Highs_setStringOptionValue",
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, // highs
                        ValueLayout.ADDRESS, // option (const char*)
                        ValueLayout.ADDRESS)); // value (const char*)

        // HighsInt Highs_setCallback(void* highs, HighsCCallbackType user_callback, void* user_callback_data)
        HIGHS_SET_CALLBACK = HiGHSBindings.downcallHandleOptional("Highs_setCallback", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, // highs
                ValueLayout.ADDRESS, // user_callback
                ValueLayout.ADDRESS)); // user_callback_data

        // HighsInt Highs_startCallback(void* highs, const HighsInt callback_type)
        HIGHS_START_CALLBACK = HiGHSBindings.downcallHandleOptional("Highs_startCallback",
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));

        HIGHS_CHANGE_COL_BOUNDS = HiGHSBindings.downcallHandle("Highs_changeColBounds", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, // highs
                ValueLayout.JAVA_INT, // col
                ValueLayout.JAVA_DOUBLE, // lower
                ValueLayout.JAVA_DOUBLE)); // upper

        HIGHS_CHANGE_COLS_BOUNDS_BY_RANGE = HiGHSBindings.downcallHandle("Highs_changeColsBoundsByRange",
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, // highs
                        ValueLayout.JAVA_INT, // from_col
                        ValueLayout.JAVA_INT, // to_col
                        ValueLayout.ADDRESS, // lower (double*)
                        ValueLayout.ADDRESS)); // upper (double*)

        HIGHS_CHANGE_COLS_BOUNDS_BY_SET = HiGHSBindings.downcallHandle("Highs_changeColsBoundsBySet",
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, // highs
                        ValueLayout.JAVA_INT, // num_set_entries
                        ValueLayout.ADDRESS, // set (int*)
                        ValueLayout.ADDRESS, // lower (double*)
                        ValueLayout.ADDRESS)); // upper (double*)

        HIGHS_CHANGE_COLS_BOUNDS_BY_MASK = HiGHSBindings.downcallHandle("Highs_changeColsBoundsByMask",
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, // highs
                        ValueLayout.ADDRESS, // mask (int*, length num_col)
                        ValueLayout.ADDRESS, // lower (double*, length num_col)
                        ValueLayout.ADDRESS)); // upper (double*, length num_col)
    }

    private static MethodHandle downcallHandle(final String name, final FunctionDescriptor descriptor) {

        MemorySegment symbol = LOOKUP.find(name).orElseThrow(() -> new UnsatisfiedLinkError("Symbol not found: " + name));
        return LINKER.downcallHandle(symbol, descriptor);
    }

    /**
     * As {@link #downcallHandle(String, FunctionDescriptor)}, but null rather than an error when the symbol
     * is missing – for functions that older HiGHS builds do not have.
     */
    private static MethodHandle downcallHandleOptional(final String name, final FunctionDescriptor descriptor) {

        return LOOKUP.find(name).map(symbol -> LINKER.downcallHandle(symbol, descriptor)).orElse(null);
    }

    /**
     * What HiGHS calls, on whichever thread is doing the solving. Must not throw: an exception crossing back
     * into native code takes the process down, so anything unexpected is logged and swallowed.
     */
    private static void onCallback(final BooleanSupplier stopWanted, final int callbackType, final MemorySegment message, final MemorySegment dataOut,
            final MemorySegment dataIn, final MemorySegment userData) {

        try {
            if (!MemorySegment.NULL.equals(dataIn) && stopWanted.getAsBoolean()) {
                // user_interrupt is the first member of HighsCallbackDataIn, and HiGHS documents that as
                // fixed - other language bindings depend on it too.
                dataIn.reinterpret(Integer.BYTES).set(ValueLayout.JAVA_INT, 0L, 1);
            }
        } catch (Throwable cause) {
            BasicLogger.error("HiGHS callback failed!", cause);
        }
    }

    /**
     * True if this HiGHS build has the callback functions at all. They arrived in HiGHS 1.6; without them a
     * solve cannot be stopped.
     */
    static boolean hasCallbacks() {
        return HIGHS_SET_CALLBACK != null && HIGHS_START_CALLBACK != null;
    }

    /**
     * A native function pointer for a HiGHS callback that does one thing: ask the solve to stop, by setting
     * {@code user_interrupt}, whenever {@code stopWanted} says so.
     * <p>
     * The stub is allocated in an automatic arena, so it stays valid as long as the caller keeps the returned
     * segment reachable – and no longer. HiGHS may call it from a thread of its own, which is why the arena
     * is not a confined one.
     */
    static MemorySegment newInterruptCallback(final BooleanSupplier stopWanted) {
        return LINKER.upcallStub(CALLBACK_TARGET.bindTo(stopWanted), CALLBACK_DESCRIPTOR, Arena.ofAuto());
    }

    /**
     * Registers the callback function pointer. Nothing calls it until
     * {@link #startCallback(MemorySegment, int)} names a callback type.
     */
    static int setCallback(final MemorySegment highs, final MemorySegment callback) {

        try {
            return (int) HIGHS_SET_CALLBACK.invokeExact(highs, callback, MemorySegment.NULL);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to set the HiGHS callback", t);
        }
    }

    static int startCallback(final MemorySegment highs, final int callbackType) {

        try {
            return (int) HIGHS_START_CALLBACK.invokeExact(highs, callbackType);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to start a HiGHS callback", t);
        }
    }

    /**
     * Allocate a native double array and copy Java array contents.
     */
    static MemorySegment allocateDoubleArray(final Arena arena, final double[] values) {

        MemorySegment segment = arena.allocate(ValueLayout.JAVA_DOUBLE, values.length);
        MemorySegment.copy(values, 0, segment, ValueLayout.JAVA_DOUBLE, 0, values.length);
        return segment;
    }

    /**
     * Allocate a native int array and copy Java array contents.
     */
    static MemorySegment allocateIntArray(final Arena arena, final int[] values) {

        MemorySegment segment = arena.allocate(ValueLayout.JAVA_INT, values.length);
        MemorySegment.copy(values, 0, segment, ValueLayout.JAVA_INT, 0, values.length);
        return segment;
    }

    /**
     * Allocate a native C string (null-terminated).
     */
    static MemorySegment allocateString(final Arena arena, final String value) {
        return arena.allocateFrom(value);
    }

    /**
     * Change the bounds of a single column (variable).
     * <p>
     * This is the key method for {@link org.ojalgo.optimisation.UpdatableSolver} support.
     *
     * @param col   column index
     * @param lower new lower bound
     * @param upper new upper bound
     * @return status code
     */
    static int changeColBounds(final MemorySegment highs, final int col, final double lower, final double upper) {

        try {
            return (int) HIGHS_CHANGE_COL_BOUNDS.invokeExact(highs, col, lower, upper);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to change column bounds in HiGHS", t);
        }
    }

    /**
     * Change the bounds of a contiguous range of columns in a single native call.
     *
     * @param fromCol first column index (inclusive)
     * @param toCol   last column index (inclusive)
     * @param lower   native segment of length {@code toCol - fromCol + 1} with new lower bounds
     * @param upper   native segment of length {@code toCol - fromCol + 1} with new upper bounds
     * @return status code
     */
    static int changeColsBoundsByRange(final MemorySegment highs, final int fromCol, final int toCol, final MemorySegment lower, final MemorySegment upper) {

        try {
            return (int) HIGHS_CHANGE_COLS_BOUNDS_BY_RANGE.invokeExact(highs, fromCol, toCol, lower, upper);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to change column bounds range in HiGHS", t);
        }
    }

    /**
     * Change the bounds of a set of columns identified by an index array in a single native call. All three
     * arrays must have the same length ({@code numEntries}).
     *
     * @param numEntries number of columns to update
     * @param set        native segment of length {@code numEntries} with column indices
     * @param lower      native segment of length {@code numEntries} with new lower bounds
     * @param upper      native segment of length {@code numEntries} with new upper bounds
     * @return status code
     */
    static int changeColsBoundsBySet(final MemorySegment highs, final int numEntries, final MemorySegment set, final MemorySegment lower,
            final MemorySegment upper) {

        try {
            return (int) HIGHS_CHANGE_COLS_BOUNDS_BY_SET.invokeExact(highs, numEntries, set, lower, upper);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to change column bounds set in HiGHS", t);
        }
    }

    /**
     * Change the bounds of a subset of columns selected by a 0/1 mask in a single native call. All three
     * arrays must be of length {@code num_col}; only entries flagged by the mask are applied.
     *
     * @param mask  native int segment of length {@code num_col} (1 = apply, 0 = skip)
     * @param lower native segment of length {@code num_col} with new lower bounds
     * @param upper native segment of length {@code num_col} with new upper bounds
     * @return status code
     */
    static int changeColsBoundsByMask(final MemorySegment highs, final MemorySegment mask, final MemorySegment lower, final MemorySegment upper) {

        try {
            return (int) HIGHS_CHANGE_COLS_BOUNDS_BY_MASK.invokeExact(highs, mask, lower, upper);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to change column bounds mask in HiGHS", t);
        }
    }

    /**
     * Clear the HiGHS instance completely.
     */
    static int clear(final MemorySegment highs) {

        try {
            return (int) HIGHS_CLEAR.invokeExact(highs);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to clear HiGHS", t);
        }
    }

    /**
     * Clear only the model from the HiGHS instance.
     */
    static int clearModel(final MemorySegment highs) {

        try {
            return (int) HIGHS_CLEAR_MODEL.invokeExact(highs);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to clear HiGHS model", t);
        }
    }

    /**
     * Clear solver information (retains model).
     */
    static int clearSolver(final MemorySegment highs) {

        try {
            return (int) HIGHS_CLEAR_SOLVER.invokeExact(highs);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to clear HiGHS solver", t);
        }
    }

    /**
     * Copy native double array to Java array.
     */
    static void copyToJavaArray(final MemorySegment segment, final double[] dest) {
        MemorySegment.copy(segment, ValueLayout.JAVA_DOUBLE, 0, dest, 0, dest.length);
    }

    /**
     * Create a new HiGHS instance.
     *
     * @return pointer to the HiGHS instance
     */
    static MemorySegment create() {

        try {
            return (MemorySegment) HIGHS_CREATE.invokeExact();
        } catch (Throwable t) {
            throw new RuntimeException("Failed to create HiGHS instance", t);
        }
    }

    /**
     * Destroy a HiGHS instance and free associated memory.
     */
    static void destroy(final MemorySegment highs) {

        try {
            HIGHS_DESTROY.invokeExact(highs);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to destroy HiGHS instance", t);
        }
    }

    /**
     * The dual ray, a certificate of primal infeasibility, if HiGHS has one. HiGHS would solve an LP to get
     * one it does not have; this does not ask it to.
     *
     * @return the ray (double[numRow]), or null if there is none
     */
    static double[] getDualRay(final MemorySegment highs, final int numRow) {

        try (Arena arena = Arena.ofConfined()) {

            MemorySegment has = arena.allocate(ValueLayout.JAVA_INT);

            int status = (int) HIGHS_GET_DUAL_RAY.invokeExact(highs, has, MemorySegment.NULL);
            if (status == STATUS_ERROR || has.get(ValueLayout.JAVA_INT, 0L) == 0) {
                return null;
            }

            MemorySegment ray = arena.allocate(ValueLayout.JAVA_DOUBLE, numRow);

            status = (int) HIGHS_GET_DUAL_RAY.invokeExact(highs, has, ray);
            if (status == STATUS_ERROR || has.get(ValueLayout.JAVA_INT, 0L) == 0) {
                return null;
            }

            return ray.toArray(ValueLayout.JAVA_DOUBLE);

        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * Get the model status after solving.
     *
     * @return model status code (see MODEL_STATUS_* constants)
     */
    static int getModelStatus(final MemorySegment highs) {

        try {
            return (int) HIGHS_GET_MODEL_STATUS.invokeExact(highs);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to get model status from HiGHS", t);
        }
    }

    /**
     * Get the number of columns (variables) in the model.
     */
    static int getNumCol(final MemorySegment highs) {

        try {
            return (int) HIGHS_GET_NUM_COL.invokeExact(highs);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to get number of columns from HiGHS", t);
        }
    }

    /**
     * Get the number of rows (constraints) in the model.
     */
    static int getNumRow(final MemorySegment highs) {

        try {
            return (int) HIGHS_GET_NUM_ROW.invokeExact(highs);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to get number of rows from HiGHS", t);
        }
    }

    /**
     * HiGHS's own word on whether it holds a primal feasible solution: {@link #SOLUTION_STATUS_FEASIBLE} when
     * it does. The model status alone does not say – a solve stopped by a time, iteration or solution limit,
     * or by an interrupt, reports the limit whether or not an incumbent was found before it.
     * <p>
     * Returns {@link #SOLUTION_STATUS_NONE} if HiGHS cannot answer.
     */
    static int getPrimalSolutionStatus(final MemorySegment highs) {

        try (Arena arena = Arena.ofConfined()) {

            MemorySegment info = arena.allocateFrom("primal_solution_status");
            MemorySegment value = arena.allocate(ValueLayout.JAVA_INT);

            int status = (int) HIGHS_GET_INT_INFO_VALUE.invokeExact(highs, info, value);

            return status == STATUS_OK ? value.get(ValueLayout.JAVA_INT, 0L) : SOLUTION_STATUS_NONE;

        } catch (Throwable t) {
            return SOLUTION_STATUS_NONE;
        }
    }

    /**
     * Get the objective function value.
     */
    static double getObjectiveValue(final MemorySegment highs) {

        try {
            return (double) HIGHS_GET_OBJECTIVE_VALUE.invokeExact(highs);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to get objective value from HiGHS", t);
        }
    }

    /**
     * Get the primal and dual solution values.
     *
     * @param colValue primal variable values (double[numCol])
     * @param colDual  dual values for variable bounds (double[numCol])
     * @param rowValue constraint activity values (double[numRow])
     * @param rowDual  dual values for constraints (double[numRow])
     * @return status code
     */
    static int getSolution(final MemorySegment highs, final MemorySegment colValue, final MemorySegment colDual, final MemorySegment rowValue,
            final MemorySegment rowDual) {

        try {
            return (int) HIGHS_GET_SOLUTION.invokeExact(highs, colValue, colDual, rowValue, rowDual);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to get solution from HiGHS", t);
        }
    }

    /**
     * Pass a Hessian matrix for QP to HiGHS.
     * <p>
     * The Hessian should be in lower-triangular (half) sparse format. HiGHS solves:
     *
     * <pre>{@code
     * min  0.5 * x' * Q * x + c' * x
     * }</pre>
     *
     * @param highs  HiGHS instance pointer
     * @param dim    dimension of Hessian (number of columns)
     * @param numNz  number of non-zeros in the lower triangle
     * @param format Hessian format (HESSIAN_FORMAT_TRIANGULAR)
     * @param qStart column start indices (int[dim+1])
     * @param qIndex row indices (int[numNz])
     * @param qValue values (double[numNz])
     * @return status code
     */
    static int passHessian(final MemorySegment highs, final int dim, final int numNz, final int format, final MemorySegment qStart, final MemorySegment qIndex,
            final MemorySegment qValue) {

        try {
            return (int) HIGHS_PASS_HESSIAN.invokeExact(highs, dim, numNz, format, qStart, qIndex, qValue);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to pass Hessian to HiGHS", t);
        }
    }

    /**
     * Pass an LP model to HiGHS using column-wise sparse matrix format.
     *
     * @param highs    HiGHS instance pointer
     * @param numCol   number of columns (variables)
     * @param numRow   number of rows (constraints)
     * @param numNz    number of non-zeros in the constraint matrix
     * @param aFormat  matrix format (MATRIX_FORMAT_COLWISE or MATRIX_FORMAT_ROWWISE)
     * @param sense    objective sense (SENSE_MINIMIZE or SENSE_MAXIMIZE)
     * @param offset   constant offset to add to objective
     * @param colCost  objective coefficients (double[numCol])
     * @param colLower lower bounds for variables (double[numCol])
     * @param colUpper upper bounds for variables (double[numCol])
     * @param rowLower lower bounds for constraints (double[numRow])
     * @param rowUpper upper bounds for constraints (double[numRow])
     * @param aStart   column start indices (int[numCol+1] for col-wise)
     * @param aIndex   row indices of non-zeros (int[numNz])
     * @param aValue   values of non-zeros (double[numNz])
     * @return status code
     */
    static int passLp(final MemorySegment highs, final int numCol, final int numRow, final int numNz, final int aFormat, final int sense, final double offset,
            final MemorySegment colCost, final MemorySegment colLower, final MemorySegment colUpper, final MemorySegment rowLower, final MemorySegment rowUpper,
            final MemorySegment aStart, final MemorySegment aIndex, final MemorySegment aValue) {

        try {
            return (int) HIGHS_PASS_LP.invokeExact(highs, numCol, numRow, numNz, aFormat, sense, offset, colCost, colLower, colUpper, rowLower, rowUpper,
                    aStart, aIndex, aValue);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to pass LP to HiGHS", t);
        }
    }

    /**
     * Pass a MIP model to HiGHS (LP with integrality constraints).
     *
     * @param integrality variable types (int[numCol], see VAR_TYPE_* constants)
     * @return status code
     */
    static int passMip(final MemorySegment highs, final int numCol, final int numRow, final int numNz, final int aFormat, final int sense, final double offset,
            final MemorySegment colCost, final MemorySegment colLower, final MemorySegment colUpper, final MemorySegment rowLower, final MemorySegment rowUpper,
            final MemorySegment aStart, final MemorySegment aIndex, final MemorySegment aValue, final MemorySegment integrality) {

        try {
            return (int) HIGHS_PASS_MIP.invokeExact(highs, numCol, numRow, numNz, aFormat, sense, offset, colCost, colLower, colUpper, rowLower, rowUpper,
                    aStart, aIndex, aValue, integrality);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to pass MIP to HiGHS", t);
        }
    }

    /**
     * Run HiGHS to solve the current model.
     *
     * @return status code (STATUS_OK, STATUS_WARNING, or STATUS_ERROR)
     */
    static int run(final MemorySegment highs) {

        try {
            return (int) HIGHS_RUN.invokeExact(highs);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to run HiGHS", t);
        }
    }

    /**
     * Set a boolean option value.
     */
    static int setBoolOptionValue(final MemorySegment highs, final MemorySegment option, final int value) {

        try {
            return (int) HIGHS_SET_BOOL_OPTION_VALUE.invokeExact(highs, option, value);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to set bool option in HiGHS", t);
        }
    }

    /**
     * Set a double option value.
     */
    static int setDoubleOptionValue(final MemorySegment highs, final MemorySegment option, final double value) {

        try {
            return (int) HIGHS_SET_DOUBLE_OPTION_VALUE.invokeExact(highs, option, value);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to set double option in HiGHS", t);
        }
    }

    /**
     * Set an integer option value.
     */
    static int setIntOptionValue(final MemorySegment highs, final MemorySegment option, final int value) {

        try {
            return (int) HIGHS_SET_INT_OPTION_VALUE.invokeExact(highs, option, value);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to set int option in HiGHS", t);
        }
    }

    /**
     * Set a string option value.
     */
    static int setStringOptionValue(final MemorySegment highs, final MemorySegment option, final MemorySegment value) {

        try {
            return (int) HIGHS_SET_STRING_OPTION_VALUE.invokeExact(highs, option, value);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to set string option in HiGHS", t);
        }
    }

    private HiGHSBindings() {}

}
