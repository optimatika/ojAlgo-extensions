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
package org.ojalgo.optimisation.solver.osqp;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.GroupLayout;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemoryLayout.PathElement;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;

/**
 * FFM bindings for the OSQP (v1.0) C API.
 * <p>
 * OSQP's struct layouts are not opaque (unlike e.g. HiGHS's {@code void*} handle), so this class defines
 * {@link MemoryLayout}s that mirror {@code osqp_api_types.h} exactly. Those layouts were verified against the
 * compiled library on this machine with a small {@code offsetof}/{@code sizeof} probe (OSQP built with its
 * default configuration: {@code OSQP_USE_LONG} on - so {@code OSQPInt} is a 64-bit {@code long long} -
 * {@code OSQP_USE_FLOAT} off - so {@code OSQPFloat} is a {@code double} - and {@code OSQP_PACK_SETTINGS} off,
 * i.e. the settings struct uses ordinary, padded, C alignment).
 * <p>
 * The {@code OSQPCscMatrix} and {@code OSQPSettings} structs are allocated directly out of a caller-supplied
 * {@link Arena} and populated from Java (rather than via the {@code OSQPCscMatrix_new}/
 * {@code OSQPSettings_new} allocator functions OSQP 1.0 also offers) so their lifetime is tied to that arena
 * and no matching {@code _free} call is needed.
 *
 * @see <a href="https://osqp.org/docs/interfaces/C.html">OSQP C API</a>
 */
final class OSQPBindings {

    // osqp_status_type (osqp_api_constants.h) - values of OSQPInfo.status_val
    static final long OSQP_SOLVED = 1L;
    static final long OSQP_SOLVED_INACCURATE = 2L;
    static final long OSQP_PRIMAL_INFEASIBLE = 3L;
    static final long OSQP_PRIMAL_INFEASIBLE_INACCURATE = 4L;
    static final long OSQP_DUAL_INFEASIBLE = 5L;
    static final long OSQP_DUAL_INFEASIBLE_INACCURATE = 6L;
    static final long OSQP_MAX_ITER_REACHED = 7L;
    static final long OSQP_TIME_LIMIT_REACHED = 8L;
    static final long OSQP_NON_CVX = 9L;
    static final long OSQP_SIGINT = 10L;
    static final long OSQP_UNSOLVED = 11L;

    /**
     * OSQP's own sentinel for "unbounded" ({@code OSQP_INFTY} in osqp_api_constants.h). Every unbounded bound
     * entry (constraint or variable) must be clamped to +/- this value before being handed to OSQP - passing
     * real IEEE-754 infinities instead was confirmed (empirically, against the real library) to break the
     * internal scaling badly enough that the solver stalls at the iteration limit instead of converging.
     */
    static final double OSQP_INFTY = 1.0e30;

    // Struct: OSQPCscMatrix { OSQPInt m, n; OSQPInt* p; OSQPInt* i; OSQPFloat* x; OSQPInt nzmax, nz, owned; }
    // (osqp_api_types.h) - 8 naturally-aligned 8-byte members, so no padding is needed.
    private static final GroupLayout CSC_MATRIX_LAYOUT = MemoryLayout.structLayout(ValueLayout.JAVA_LONG.withName("m"), ValueLayout.JAVA_LONG.withName("n"),
            ValueLayout.ADDRESS.withName("p"), ValueLayout.ADDRESS.withName("i"), ValueLayout.ADDRESS.withName("x"), ValueLayout.JAVA_LONG.withName("nzmax"),
            ValueLayout.JAVA_LONG.withName("nz"), ValueLayout.JAVA_LONG.withName("owned"));

    // Struct: OSQPSettings (osqp_api_types.h). OSQP_PACK_SETTINGS is off in a standard build, so this is
    // the natural (padded) C layout: the two enum members (4 bytes each) are each followed by 4 bytes of
    // padding to keep the following OSQPInt/OSQPFloat (8 bytes) aligned.
    private static final GroupLayout SETTINGS_LAYOUT = MemoryLayout.structLayout(ValueLayout.JAVA_LONG.withName("device"),
            ValueLayout.JAVA_INT.withName("linsys_solver"), MemoryLayout.paddingLayout(4), ValueLayout.JAVA_LONG.withName("allocate_solution"),
            ValueLayout.JAVA_LONG.withName("verbose"), ValueLayout.JAVA_LONG.withName("profiler_level"), ValueLayout.JAVA_LONG.withName("warm_starting"),
            ValueLayout.JAVA_LONG.withName("scaling"), ValueLayout.JAVA_LONG.withName("polishing"), ValueLayout.JAVA_DOUBLE.withName("rho"),
            ValueLayout.JAVA_LONG.withName("rho_is_vec"), ValueLayout.JAVA_DOUBLE.withName("sigma"), ValueLayout.JAVA_DOUBLE.withName("alpha"),
            ValueLayout.JAVA_LONG.withName("cg_max_iter"), ValueLayout.JAVA_LONG.withName("cg_tol_reduction"),
            ValueLayout.JAVA_DOUBLE.withName("cg_tol_fraction"), ValueLayout.JAVA_INT.withName("cg_precond"), MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG.withName("adaptive_rho"), ValueLayout.JAVA_LONG.withName("adaptive_rho_interval"),
            ValueLayout.JAVA_DOUBLE.withName("adaptive_rho_fraction"), ValueLayout.JAVA_DOUBLE.withName("adaptive_rho_tolerance"),
            ValueLayout.JAVA_LONG.withName("max_iter"), ValueLayout.JAVA_DOUBLE.withName("eps_abs"), ValueLayout.JAVA_DOUBLE.withName("eps_rel"),
            ValueLayout.JAVA_DOUBLE.withName("eps_prim_inf"), ValueLayout.JAVA_DOUBLE.withName("eps_dual_inf"),
            ValueLayout.JAVA_LONG.withName("scaled_termination"), ValueLayout.JAVA_LONG.withName("check_termination"),
            ValueLayout.JAVA_LONG.withName("check_dualgap"), ValueLayout.JAVA_DOUBLE.withName("time_limit"), ValueLayout.JAVA_DOUBLE.withName("delta"),
            ValueLayout.JAVA_LONG.withName("polish_refine_iter"));

    // Struct: OSQPInfo (osqp_api_types.h)
    private static final GroupLayout INFO_LAYOUT = MemoryLayout.structLayout(MemoryLayout.sequenceLayout(32, ValueLayout.JAVA_BYTE).withName("status"),
            ValueLayout.JAVA_LONG.withName("status_val"), ValueLayout.JAVA_LONG.withName("status_polish"), ValueLayout.JAVA_DOUBLE.withName("obj_val"),
            ValueLayout.JAVA_DOUBLE.withName("dual_obj_val"), ValueLayout.JAVA_DOUBLE.withName("prim_res"), ValueLayout.JAVA_DOUBLE.withName("dual_res"),
            ValueLayout.JAVA_DOUBLE.withName("duality_gap"), ValueLayout.JAVA_LONG.withName("iter"), ValueLayout.JAVA_LONG.withName("rho_updates"),
            ValueLayout.JAVA_DOUBLE.withName("rho_estimate"), ValueLayout.JAVA_DOUBLE.withName("setup_time"), ValueLayout.JAVA_DOUBLE.withName("solve_time"),
            ValueLayout.JAVA_DOUBLE.withName("update_time"), ValueLayout.JAVA_DOUBLE.withName("polish_time"), ValueLayout.JAVA_DOUBLE.withName("run_time"),
            ValueLayout.JAVA_DOUBLE.withName("primdual_int"), ValueLayout.JAVA_DOUBLE.withName("rel_kkt_error"));

    // Struct: OSQPSolution (osqp_api_types.h)
    private static final GroupLayout SOLUTION_LAYOUT = MemoryLayout.structLayout(ValueLayout.ADDRESS.withName("x"), ValueLayout.ADDRESS.withName("y"),
            ValueLayout.ADDRESS.withName("prim_inf_cert"), ValueLayout.ADDRESS.withName("dual_inf_cert"));

    // Struct: OSQPSolver (osqp_api_types.h) - the four public pointer members; "work" is opaque and unused.
    private static final GroupLayout SOLVER_LAYOUT = MemoryLayout.structLayout(ValueLayout.ADDRESS.withName("settings"),
            ValueLayout.ADDRESS.withName("solution"), ValueLayout.ADDRESS.withName("info"), ValueLayout.ADDRESS.withName("work"));

    private static final VarHandle CSC_I = CSC_MATRIX_LAYOUT.varHandle(PathElement.groupElement("i"));
    private static final VarHandle CSC_M = CSC_MATRIX_LAYOUT.varHandle(PathElement.groupElement("m"));
    private static final VarHandle CSC_N = CSC_MATRIX_LAYOUT.varHandle(PathElement.groupElement("n"));
    private static final VarHandle CSC_NZ = CSC_MATRIX_LAYOUT.varHandle(PathElement.groupElement("nz"));
    private static final VarHandle CSC_NZMAX = CSC_MATRIX_LAYOUT.varHandle(PathElement.groupElement("nzmax"));
    private static final VarHandle CSC_OWNED = CSC_MATRIX_LAYOUT.varHandle(PathElement.groupElement("owned"));
    private static final VarHandle CSC_P = CSC_MATRIX_LAYOUT.varHandle(PathElement.groupElement("p"));
    private static final VarHandle CSC_X = CSC_MATRIX_LAYOUT.varHandle(PathElement.groupElement("x"));

    private static final VarHandle INFO_OBJ_VAL = INFO_LAYOUT.varHandle(PathElement.groupElement("obj_val"));
    private static final VarHandle INFO_STATUS_VAL = INFO_LAYOUT.varHandle(PathElement.groupElement("status_val"));

    private static final Linker LINKER = Linker.nativeLinker();
    private static final SymbolLookup LOOKUP;

    private static final MethodHandle OSQP_CLEANUP;
    private static final MethodHandle OSQP_ERROR_MESSAGE;
    private static final MethodHandle OSQP_SET_DEFAULT_SETTINGS;
    private static final MethodHandle OSQP_SETUP;
    private static final MethodHandle OSQP_SOLVE;
    private static final MethodHandle OSQP_UPDATE_DATA_VEC;
    private static final MethodHandle OSQP_UPDATE_SETTINGS;
    private static final MethodHandle OSQP_WARM_START;

    private static final VarHandle SETTINGS_EPS_ABS = SETTINGS_LAYOUT.varHandle(PathElement.groupElement("eps_abs"));
    private static final VarHandle SETTINGS_EPS_DUAL_INF = SETTINGS_LAYOUT.varHandle(PathElement.groupElement("eps_dual_inf"));
    private static final VarHandle SETTINGS_EPS_PRIM_INF = SETTINGS_LAYOUT.varHandle(PathElement.groupElement("eps_prim_inf"));
    private static final VarHandle SETTINGS_EPS_REL = SETTINGS_LAYOUT.varHandle(PathElement.groupElement("eps_rel"));
    private static final VarHandle SETTINGS_MAX_ITER = SETTINGS_LAYOUT.varHandle(PathElement.groupElement("max_iter"));
    private static final VarHandle SETTINGS_POLISHING = SETTINGS_LAYOUT.varHandle(PathElement.groupElement("polishing"));
    private static final VarHandle SETTINGS_SCALING = SETTINGS_LAYOUT.varHandle(PathElement.groupElement("scaling"));
    private static final VarHandle SETTINGS_TIME_LIMIT = SETTINGS_LAYOUT.varHandle(PathElement.groupElement("time_limit"));
    private static final VarHandle SETTINGS_VERBOSE = SETTINGS_LAYOUT.varHandle(PathElement.groupElement("verbose"));

    private static final VarHandle SOLUTION_PRIM_INF_CERT = SOLUTION_LAYOUT.varHandle(PathElement.groupElement("prim_inf_cert"));
    private static final VarHandle SOLUTION_X = SOLUTION_LAYOUT.varHandle(PathElement.groupElement("x"));
    private static final VarHandle SOLUTION_Y = SOLUTION_LAYOUT.varHandle(PathElement.groupElement("y"));

    private static final VarHandle SOLVER_INFO = SOLVER_LAYOUT.varHandle(PathElement.groupElement("info"));
    private static final VarHandle SOLVER_SOLUTION = SOLVER_LAYOUT.varHandle(PathElement.groupElement("solution"));

    static {

        LOOKUP = NativeLoader.load();

        OSQP_SET_DEFAULT_SETTINGS = OSQPBindings.downcallHandle("osqp_set_default_settings", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

        // OSQPInt osqp_setup(OSQPSolver** solverp, const OSQPCscMatrix* P, const OSQPFloat* q,
        //                    const OSQPCscMatrix* A, const OSQPFloat* l, const OSQPFloat* u,
        //                    OSQPInt m, OSQPInt n, const OSQPSettings* settings)
        OSQP_SETUP = OSQPBindings.downcallHandle("osqp_setup", FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, // solverp
                ValueLayout.ADDRESS, // P
                ValueLayout.ADDRESS, // q
                ValueLayout.ADDRESS, // A
                ValueLayout.ADDRESS, // l
                ValueLayout.ADDRESS, // u
                ValueLayout.JAVA_LONG, // m
                ValueLayout.JAVA_LONG, // n
                ValueLayout.ADDRESS)); // settings

        OSQP_SOLVE = OSQPBindings.downcallHandle("osqp_solve", FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));

        OSQP_CLEANUP = OSQPBindings.downcallHandle("osqp_cleanup", FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));

        // OSQPInt osqp_update_settings(OSQPSolver* solver, const OSQPSettings* new_settings)
        OSQP_UPDATE_SETTINGS = OSQPBindings.downcallHandle("osqp_update_settings",
                FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

        // OSQPInt osqp_update_data_vec(OSQPSolver* solver, const OSQPFloat* q_new, const OSQPFloat* l_new,
        //                              const OSQPFloat* u_new)
        OSQP_UPDATE_DATA_VEC = OSQPBindings.downcallHandle("osqp_update_data_vec", FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, // solver
                ValueLayout.ADDRESS, // q_new
                ValueLayout.ADDRESS, // l_new
                ValueLayout.ADDRESS)); // u_new

        // OSQPInt osqp_warm_start(OSQPSolver* solver, const OSQPFloat* x, const OSQPFloat* y)
        OSQP_WARM_START = OSQPBindings.downcallHandle("osqp_warm_start",
                FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

        OSQP_ERROR_MESSAGE = OSQPBindings.downcallHandle("osqp_error_message", FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));
    }

    /**
     * Clamps to OSQP's own "unbounded" sentinel - see {@link #OSQP_INFTY}.
     */
    static double clampInfinity(final double value) {
        if (value >= OSQP_INFTY) {
            return OSQP_INFTY;
        }
        if (value <= -OSQP_INFTY) {
            return -OSQP_INFTY;
        }
        return value;
    }

    static long cleanup(final MemorySegment solver) {
        try {
            return (long) OSQP_CLEANUP.invokeExact(solver);
        } catch (Throwable cause) {
            throw new RuntimeException("osqp_cleanup failed", cause);
        }
    }

    static String errorMessage(final long errorFlag) {
        try {
            MemorySegment message = (MemorySegment) OSQP_ERROR_MESSAGE.invokeExact(errorFlag);
            if (MemorySegment.NULL.equals(message)) {
                return "OSQP error " + errorFlag;
            }
            return message.reinterpret(256L).getString(0L);
        } catch (Throwable cause) {
            return "OSQP error " + errorFlag;
        }
    }

    static MemorySegment getInfo(final MemorySegment solver) {
        return ((MemorySegment) SOLVER_INFO.get(solver, 0L)).reinterpret(INFO_LAYOUT.byteSize());
    }

    static double getObjVal(final MemorySegment info) {
        return (double) INFO_OBJ_VAL.get(info, 0L);
    }

    static MemorySegment getSolution(final MemorySegment solver) {
        return ((MemorySegment) SOLVER_SOLUTION.get(solver, 0L)).reinterpret(SOLUTION_LAYOUT.byteSize());
    }

    /**
     * The certificate of primal infeasibility, signed as the dual solution y: positive where an upper limit
     * is used.
     */
    static MemorySegment getSolutionPrimInfCert(final MemorySegment solution, final long m) {
        return ((MemorySegment) SOLUTION_PRIM_INF_CERT.get(solution, 0L)).reinterpret(m * ValueLayout.JAVA_DOUBLE.byteSize());
    }

    static MemorySegment getSolutionX(final MemorySegment solution, final long n) {
        return ((MemorySegment) SOLUTION_X.get(solution, 0L)).reinterpret(n * ValueLayout.JAVA_DOUBLE.byteSize());
    }

    static MemorySegment getSolutionY(final MemorySegment solution, final long m) {
        return ((MemorySegment) SOLUTION_Y.get(solution, 0L)).reinterpret(m * ValueLayout.JAVA_DOUBLE.byteSize());
    }

    /**
     * Dereferences the {@code OSQPSolver*} written by {@link #setup} into {@code solverPtrOut}.
     */
    static MemorySegment getSolver(final MemorySegment solverPtrOut) {
        return solverPtrOut.get(ValueLayout.ADDRESS, 0L).reinterpret(SOLVER_LAYOUT.byteSize());
    }

    static long getStatusVal(final MemorySegment info) {
        return (long) INFO_STATUS_VAL.get(info, 0L);
    }

    /**
     * Wraps existing (arena-owned) arrays as an {@code OSQPCscMatrix} - no copying and no separate allocation
     * on the C side, so there is nothing to free afterwards. {@code i} and {@code x} may be
     * {@link MemorySegment#NULL} when {@code nzmax} is zero.
     *
     * @param p column pointers, {@code long[n + 1]}
     * @param i row indices, {@code long[nzmax]}
     * @param x values, {@code double[nzmax]}
     */
    static MemorySegment newCscMatrix(final Arena arena, final long m, final long n, final long nzmax, final MemorySegment p, final MemorySegment i,
            final MemorySegment x) {

        MemorySegment mat = arena.allocate(CSC_MATRIX_LAYOUT);

        CSC_M.set(mat, 0L, m);
        CSC_N.set(mat, 0L, n);
        CSC_P.set(mat, 0L, p);
        CSC_I.set(mat, 0L, i);
        CSC_X.set(mat, 0L, x);
        CSC_NZMAX.set(mat, 0L, nzmax);
        CSC_NZ.set(mat, 0L, -1L); // -1 marks this as CSC (as opposed to triplet) format
        CSC_OWNED.set(mat, 0L, 0L); // the arrays are Java-arena owned; OSQP must not try to free them

        return mat;
    }

    /**
     * Allocates an {@code OSQPSettings} struct out of {@code arena} and fills it in with OSQP's own defaults
     * (via {@code osqp_set_default_settings}).
     */
    static MemorySegment newDefaultSettings(final Arena arena) {

        MemorySegment settings = arena.allocate(SETTINGS_LAYOUT);

        try {
            OSQP_SET_DEFAULT_SETTINGS.invokeExact(settings);
        } catch (Throwable cause) {
            throw new RuntimeException("osqp_set_default_settings failed", cause);
        }

        return settings;
    }

    static void setEpsAbs(final MemorySegment settings, final double value) {
        SETTINGS_EPS_ABS.set(settings, 0L, value);
    }

    static void setEpsDualInf(final MemorySegment settings, final double value) {
        SETTINGS_EPS_DUAL_INF.set(settings, 0L, value);
    }

    static void setEpsPrimInf(final MemorySegment settings, final double value) {
        SETTINGS_EPS_PRIM_INF.set(settings, 0L, value);
    }

    static void setEpsRel(final MemorySegment settings, final double value) {
        SETTINGS_EPS_REL.set(settings, 0L, value);
    }

    static void setMaxIter(final MemorySegment settings, final long value) {
        SETTINGS_MAX_ITER.set(settings, 0L, value);
    }

    static void setPolishing(final MemorySegment settings, final boolean value) {
        SETTINGS_POLISHING.set(settings, 0L, value ? 1L : 0L);
    }

    static void setScaling(final MemorySegment settings, final long value) {
        SETTINGS_SCALING.set(settings, 0L, value);
    }

    static void setTimeLimit(final MemorySegment settings, final double value) {
        SETTINGS_TIME_LIMIT.set(settings, 0L, value);
    }

    static void setVerbose(final MemorySegment settings, final boolean value) {
        SETTINGS_VERBOSE.set(settings, 0L, value ? 1L : 0L);
    }

    static long setup(final MemorySegment solverPtrOut, final MemorySegment mtrxP, final MemorySegment q, final MemorySegment mtrxA, final MemorySegment l,
            final MemorySegment u, final long m, final long n, final MemorySegment settings) {
        try {
            return (long) OSQP_SETUP.invokeExact(solverPtrOut, mtrxP, q, mtrxA, l, u, m, n, settings);
        } catch (Throwable cause) {
            throw new RuntimeException("osqp_setup failed", cause);
        }
    }

    static long solve(final MemorySegment solver) {
        try {
            return (long) OSQP_SOLVE.invokeExact(solver);
        } catch (Throwable cause) {
            throw new RuntimeException("osqp_solve failed", cause);
        }
    }

    static long updateDataVec(final MemorySegment solver, final MemorySegment qNew, final MemorySegment lNew, final MemorySegment uNew) {
        try {
            return (long) OSQP_UPDATE_DATA_VEC.invokeExact(solver, qNew, lNew, uNew);
        } catch (Throwable cause) {
            throw new RuntimeException("osqp_update_data_vec failed", cause);
        }
    }

    static long updateSettings(final MemorySegment solver, final MemorySegment newSettings) {
        try {
            return (long) OSQP_UPDATE_SETTINGS.invokeExact(solver, newSettings);
        } catch (Throwable cause) {
            throw new RuntimeException("osqp_update_settings failed", cause);
        }
    }

    static long warmStart(final MemorySegment solver, final MemorySegment x, final MemorySegment y) {
        try {
            return (long) OSQP_WARM_START.invokeExact(solver, x, y);
        } catch (Throwable cause) {
            throw new RuntimeException("osqp_warm_start failed", cause);
        }
    }

    static MemorySegment allocateDoubleArray(final Arena arena, final double[] values) {
        MemorySegment segment = arena.allocate(ValueLayout.JAVA_DOUBLE, values.length);
        MemorySegment.copy(values, 0, segment, ValueLayout.JAVA_DOUBLE, 0, values.length);
        return segment;
    }

    /**
     * Widens ojAlgo's {@code int[]} CSC index/pointer arrays to the {@code long[]} OSQP expects
     * ({@code OSQPInt} is 64-bit in a standard OSQP build).
     */
    static MemorySegment allocateLongArray(final Arena arena, final int[] values) {
        MemorySegment segment = arena.allocate(ValueLayout.JAVA_LONG, values.length);
        for (int i = 0; i < values.length; i++) {
            segment.setAtIndex(ValueLayout.JAVA_LONG, i, values[i]);
        }
        return segment;
    }

    private static MethodHandle downcallHandle(final String name, final FunctionDescriptor descriptor) {
        MemorySegment symbol = LOOKUP.find(name).orElseThrow(() -> new UnsatisfiedLinkError("Symbol not found: " + name));
        return LINKER.downcallHandle(symbol, descriptor);
    }

    private OSQPBindings() {}

}
