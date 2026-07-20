/*
 * Copyright 1997-2025 Optimatika
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
package org.ojalgo.optimisation.solver.clarabel;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;

/**
 * FFM bindings for the Clarabel C API.
 */
final class ClarabelBindings {

    // ClarabelSolverStatus enum values
    static final int STATUS_UNSOLVED = 0;
    static final int STATUS_SOLVED = 1;
    static final int STATUS_PRIMAL_INFEASIBLE = 2;
    static final int STATUS_DUAL_INFEASIBLE = 3;
    static final int STATUS_ALMOST_SOLVED = 4;
    static final int STATUS_ALMOST_PRIMAL_INFEASIBLE = 5;
    static final int STATUS_ALMOST_DUAL_INFEASIBLE = 6;
    static final int STATUS_MAX_ITERATIONS = 7;
    static final int STATUS_MAX_TIME = 8;
    static final int STATUS_NUMERICAL_ERROR = 9;
    static final int STATUS_INSUFFICIENT_PROGRESS = 10;
    static final int STATUS_CALLBACK_TERMINATED = 11;

    // ClarabelSupportedConeT_Tag enum values
    static final int CONE_ZERO = 0;
    static final int CONE_NONNEGATIVE = 1;
    static final int CONE_SECOND_ORDER = 2;
    static final int CONE_EXPONENTIAL = 3;
    static final int CONE_POWER = 4;
    static final int CONE_GEN_POWER = 5;

    // Struct: ClarabelCscMatrix_f64 { uintptr_t m, n; uintptr_t* colptr; uintptr_t* rowval; double* nzval }
    static final GroupLayout CSC_MATRIX_LAYOUT = MemoryLayout.structLayout(
            ValueLayout.JAVA_LONG.withName("m"),
            ValueLayout.JAVA_LONG.withName("n"),
            ValueLayout.ADDRESS.withName("colptr"),
            ValueLayout.ADDRESS.withName("rowval"),
            ValueLayout.ADDRESS.withName("nzval"));

    // Struct: ClarabelSupportedConeT_f64 { int tag; padding(4); union { long dim; double power; struct{ptr,long,long} } }
    static final GroupLayout CONE_LAYOUT = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT.withName("tag"),
            MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG.withName("dim_or_data_0"),
            ValueLayout.JAVA_LONG.withName("data_1"),
            ValueLayout.JAVA_LONG.withName("data_2"));

    // Struct: ClarabelDefaultSolution_f64
    static final GroupLayout SOLUTION_LAYOUT = MemoryLayout.structLayout(
            ValueLayout.ADDRESS.withName("x"),
            ValueLayout.JAVA_LONG.withName("x_length"),
            ValueLayout.ADDRESS.withName("z"),
            ValueLayout.JAVA_LONG.withName("z_length"),
            ValueLayout.ADDRESS.withName("s"),
            ValueLayout.JAVA_LONG.withName("s_length"),
            ValueLayout.JAVA_INT.withName("status"),
            MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_DOUBLE.withName("obj_val"),
            ValueLayout.JAVA_DOUBLE.withName("obj_val_dual"),
            ValueLayout.JAVA_DOUBLE.withName("solve_time"),
            ValueLayout.JAVA_INT.withName("iterations"),
            MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_DOUBLE.withName("r_prim"),
            ValueLayout.JAVA_DOUBLE.withName("r_dual"));

    private static final VarHandle SOL_X = SOLUTION_LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("x"));
    private static final VarHandle SOL_X_LEN = SOLUTION_LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("x_length"));
    private static final VarHandle SOL_STATUS = SOLUTION_LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("status"));
    private static final VarHandle SOL_OBJ_VAL = SOLUTION_LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("obj_val"));

    // Settings struct layout - we only need it to pass to the default constructor and tweak a few fields
    // The full layout matches ClarabelDefaultSettings_f64 from the C header
    static final GroupLayout SETTINGS_LAYOUT = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT.withName("max_iter"),
            MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_DOUBLE.withName("time_limit"),
            ValueLayout.JAVA_BOOLEAN.withName("verbose"),
            MemoryLayout.paddingLayout(7),
            ValueLayout.JAVA_DOUBLE.withName("max_step_fraction"),
            ValueLayout.JAVA_DOUBLE.withName("tol_gap_abs"),
            ValueLayout.JAVA_DOUBLE.withName("tol_gap_rel"),
            ValueLayout.JAVA_DOUBLE.withName("tol_feas"),
            ValueLayout.JAVA_DOUBLE.withName("tol_infeas_abs"),
            ValueLayout.JAVA_DOUBLE.withName("tol_infeas_rel"),
            ValueLayout.JAVA_DOUBLE.withName("tol_ktratio"),
            ValueLayout.JAVA_DOUBLE.withName("reduced_tol_gap_abs"),
            ValueLayout.JAVA_DOUBLE.withName("reduced_tol_gap_rel"),
            ValueLayout.JAVA_DOUBLE.withName("reduced_tol_feas"),
            ValueLayout.JAVA_DOUBLE.withName("reduced_tol_infeas_abs"),
            ValueLayout.JAVA_DOUBLE.withName("reduced_tol_infeas_rel"),
            ValueLayout.JAVA_DOUBLE.withName("reduced_tol_ktratio"),
            ValueLayout.JAVA_BOOLEAN.withName("equilibrate_enable"),
            MemoryLayout.paddingLayout(3),
            ValueLayout.JAVA_INT.withName("equilibrate_max_iter"),
            ValueLayout.JAVA_DOUBLE.withName("equilibrate_min_scaling"),
            ValueLayout.JAVA_DOUBLE.withName("equilibrate_max_scaling"),
            ValueLayout.JAVA_DOUBLE.withName("linesearch_backtrack_step"),
            ValueLayout.JAVA_DOUBLE.withName("min_switch_step_length"),
            ValueLayout.JAVA_DOUBLE.withName("min_terminate_step_length"),
            ValueLayout.JAVA_INT.withName("max_threads"),
            ValueLayout.JAVA_BOOLEAN.withName("direct_kkt_solver"),
            MemoryLayout.paddingLayout(3),
            ValueLayout.JAVA_INT.withName("direct_solve_method"),
            ValueLayout.JAVA_BOOLEAN.withName("static_regularization_enable"),
            MemoryLayout.paddingLayout(3),
            ValueLayout.JAVA_DOUBLE.withName("static_regularization_constant"),
            ValueLayout.JAVA_DOUBLE.withName("static_regularization_proportional"),
            ValueLayout.JAVA_BOOLEAN.withName("dynamic_regularization_enable"),
            MemoryLayout.paddingLayout(7),
            ValueLayout.JAVA_DOUBLE.withName("dynamic_regularization_eps"),
            ValueLayout.JAVA_DOUBLE.withName("dynamic_regularization_delta"),
            ValueLayout.JAVA_BOOLEAN.withName("iterative_refinement_enable"),
            MemoryLayout.paddingLayout(7),
            ValueLayout.JAVA_DOUBLE.withName("iterative_refinement_reltol"),
            ValueLayout.JAVA_DOUBLE.withName("iterative_refinement_abstol"),
            ValueLayout.JAVA_INT.withName("iterative_refinement_max_iter"),
            MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_DOUBLE.withName("iterative_refinement_stop_ratio"),
            ValueLayout.JAVA_BOOLEAN.withName("presolve_enable"),
            MemoryLayout.paddingLayout(3),
            MemoryLayout.sequenceLayout(64, ValueLayout.JAVA_INT).withName("pardiso_iparm"),
            ValueLayout.JAVA_BOOLEAN.withName("pardiso_verbose"),
            MemoryLayout.paddingLayout(3));

    private static final VarHandle SETTINGS_MAX_ITER = SETTINGS_LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("max_iter"));
    private static final VarHandle SETTINGS_TIME_LIMIT = SETTINGS_LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("time_limit"));
    private static final VarHandle SETTINGS_VERBOSE = SETTINGS_LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("verbose"));
    private static final VarHandle SETTINGS_TOL_GAP_ABS = SETTINGS_LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("tol_gap_abs"));
    private static final VarHandle SETTINGS_TOL_GAP_REL = SETTINGS_LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("tol_gap_rel"));
    private static final VarHandle SETTINGS_TOL_FEAS = SETTINGS_LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("tol_feas"));
    private static final VarHandle SETTINGS_TOL_INFEAS_ABS = SETTINGS_LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("tol_infeas_abs"));
    private static final VarHandle SETTINGS_TOL_INFEAS_REL = SETTINGS_LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("tol_infeas_rel"));

    private static final Linker LINKER = Linker.nativeLinker();
    private static final SymbolLookup LOOKUP;

    private static final MethodHandle CSC_MATRIX_INIT;
    private static final MethodHandle DEFAULT_SETTINGS;
    private static final MethodHandle SOLVER_NEW;
    private static final MethodHandle SOLVER_SOLVE;
    private static final MethodHandle SOLVER_SOLUTION;
    private static final MethodHandle SOLVER_FREE;

    static {

        LOOKUP = NativeLoader.load();

        CSC_MATRIX_INIT = ClarabelBindings.downcallHandle("clarabel_CscMatrix_f64_init", FunctionDescriptor.ofVoid(
                ValueLayout.ADDRESS, ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG,
                ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

        DEFAULT_SETTINGS = ClarabelBindings.downcallHandle("clarabel_DefaultSettings_f64_default",
                FunctionDescriptor.of(SETTINGS_LAYOUT));

        SOLVER_NEW = ClarabelBindings.downcallHandle("clarabel_DefaultSolver_f64_new", FunctionDescriptor.of(ValueLayout.ADDRESS,
                ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS,
                ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

        SOLVER_SOLVE = ClarabelBindings.downcallHandle("clarabel_DefaultSolver_f64_solve",
                FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

        SOLVER_SOLUTION = ClarabelBindings.downcallHandle("clarabel_DefaultSolver_f64_solution",
                FunctionDescriptor.of(SOLUTION_LAYOUT, ValueLayout.ADDRESS));

        SOLVER_FREE = ClarabelBindings.downcallHandle("clarabel_DefaultSolver_f64_free",
                FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));
    }

    private static MethodHandle downcallHandle(final String name, final FunctionDescriptor descriptor) {
        MemorySegment symbol = LOOKUP.find(name).orElseThrow(() -> new UnsatisfiedLinkError("Symbol not found: " + name));
        return LINKER.downcallHandle(symbol, descriptor);
    }

    static void cscMatrixInit(final MemorySegment ptr, final long m, final long n,
            final MemorySegment colptr, final MemorySegment rowval, final MemorySegment nzval) {
        try {
            CSC_MATRIX_INIT.invokeExact(ptr, m, n, colptr, rowval, nzval);
        } catch (Throwable t) {
            throw new RuntimeException("clarabel_CscMatrix_f64_init failed", t);
        }
    }

    static MemorySegment defaultSettings(final SegmentAllocator allocator) {
        try {
            return (MemorySegment) DEFAULT_SETTINGS.invokeExact(allocator);
        } catch (Throwable t) {
            throw new RuntimeException("clarabel_DefaultSettings_f64_default failed", t);
        }
    }

    static MemorySegment solverNew(final MemorySegment P, final MemorySegment q,
            final MemorySegment A, final MemorySegment b,
            final long nCones, final MemorySegment cones, final MemorySegment settings) {
        try {
            return (MemorySegment) SOLVER_NEW.invokeExact(P, q, A, b, nCones, cones, settings);
        } catch (Throwable t) {
            throw new RuntimeException("clarabel_DefaultSolver_f64_new failed", t);
        }
    }

    static void solverSolve(final MemorySegment solver) {
        try {
            SOLVER_SOLVE.invokeExact(solver);
        } catch (Throwable t) {
            throw new RuntimeException("clarabel_DefaultSolver_f64_solve failed", t);
        }
    }

    static MemorySegment solverSolution(final SegmentAllocator allocator, final MemorySegment solver) {
        try {
            return (MemorySegment) SOLVER_SOLUTION.invokeExact(allocator, solver);
        } catch (Throwable t) {
            throw new RuntimeException("clarabel_DefaultSolver_f64_solution failed", t);
        }
    }

    static void solverFree(final MemorySegment solver) {
        try {
            SOLVER_FREE.invokeExact(solver);
        } catch (Throwable t) {
            throw new RuntimeException("clarabel_DefaultSolver_f64_free failed", t);
        }
    }

    static int solutionStatus(final MemorySegment solution) {
        return (int) SOL_STATUS.get(solution, 0L);
    }

    static double solutionObjVal(final MemorySegment solution) {
        return (double) SOL_OBJ_VAL.get(solution, 0L);
    }

    static MemorySegment solutionX(final MemorySegment solution) {
        return (MemorySegment) SOL_X.get(solution, 0L);
    }

    static long solutionXLength(final MemorySegment solution) {
        return (long) SOL_X_LEN.get(solution, 0L);
    }

    static void setMaxIter(final MemorySegment settings, final int value) {
        SETTINGS_MAX_ITER.set(settings, 0L, value);
    }

    static void setTimeLimit(final MemorySegment settings, final double value) {
        SETTINGS_TIME_LIMIT.set(settings, 0L, value);
    }

    static void setVerbose(final MemorySegment settings, final boolean value) {
        SETTINGS_VERBOSE.set(settings, 0L, value);
    }

    static void setTolGapAbs(final MemorySegment settings, final double value) {
        SETTINGS_TOL_GAP_ABS.set(settings, 0L, value);
    }

    static void setTolGapRel(final MemorySegment settings, final double value) {
        SETTINGS_TOL_GAP_REL.set(settings, 0L, value);
    }

    static void setTolFeas(final MemorySegment settings, final double value) {
        SETTINGS_TOL_FEAS.set(settings, 0L, value);
    }

    static void setTolInfeasAbs(final MemorySegment settings, final double value) {
        SETTINGS_TOL_INFEAS_ABS.set(settings, 0L, value);
    }

    static void setTolInfeasRel(final MemorySegment settings, final double value) {
        SETTINGS_TOL_INFEAS_REL.set(settings, 0L, value);
    }

    static MemorySegment allocateCscMatrix(final Arena arena) {
        return arena.allocate(CSC_MATRIX_LAYOUT);
    }

    static MemorySegment allocateCone(final Arena arena, final int tag, final long dim) {
        MemorySegment cone = arena.allocate(CONE_LAYOUT);
        cone.set(ValueLayout.JAVA_INT, 0, tag);
        cone.set(ValueLayout.JAVA_LONG, 8, dim);
        return cone;
    }

    static MemorySegment allocateLongArray(final Arena arena, final long[] values) {
        MemorySegment segment = arena.allocate(ValueLayout.JAVA_LONG, values.length);
        MemorySegment.copy(values, 0, segment, ValueLayout.JAVA_LONG, 0, values.length);
        return segment;
    }

    static MemorySegment allocateDoubleArray(final Arena arena, final double[] values) {
        MemorySegment segment = arena.allocate(ValueLayout.JAVA_DOUBLE, values.length);
        MemorySegment.copy(values, 0, segment, ValueLayout.JAVA_DOUBLE, 0, values.length);
        return segment;
    }

    private ClarabelBindings() {}

}
