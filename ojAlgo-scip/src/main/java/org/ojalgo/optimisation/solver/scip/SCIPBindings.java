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
package org.ojalgo.optimisation.solver.scip;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;

/**
 * FFM bindings for the SCIP C API.
 * <p>
 * This class provides Java bindings to the SCIP native library using the Foreign Function &amp; Memory API
 * (FFM). All native function calls are accessed via {@link MethodHandle} instances obtained from the native
 * linker.
 *
 * @see <a href="https://www.scipopt.org/doc/html/">SCIP documentation</a>
 */
final class SCIPBindings {

    // SCIP_RETCODE values - SCIP_OKAY is the only non-error outcome
    static final int SCIP_OKAY = 1;
    static final int SCIP_ERROR = 0;
    static final int SCIP_NOMEMORY = -1;
    static final int SCIP_READERROR = -2;
    static final int SCIP_WRITEERROR = -3;
    static final int SCIP_NOFILE = -4;
    static final int SCIP_FILECREATEERROR = -5;
    static final int SCIP_LPERROR = -6;
    static final int SCIP_NOPROBLEM = -7;
    static final int SCIP_INVALIDCALL = -8;
    static final int SCIP_INVALIDDATA = -9;
    static final int SCIP_INVALIDRESULT = -10;
    static final int SCIP_PLUGINNOTFOUND = -11;
    static final int SCIP_PARAMETERUNKNOWN = -12;
    static final int SCIP_PARAMETERWRONGTYPE = -13;
    static final int SCIP_PARAMETERWRONGVAL = -14;
    static final int SCIP_KEYALREADYEXISTING = -15;
    static final int SCIP_MAXDEPTHLEVEL = -16;
    static final int SCIP_BRANCHERROR = -17;
    static final int SCIP_NOTIMPLEMENTED = -18;

    // SCIP_STATUS values
    static final int STATUS_UNKNOWN = 0;
    static final int STATUS_OPTIMAL = 1;
    static final int STATUS_INFEASIBLE = 2;
    static final int STATUS_UNBOUNDED = 3;
    static final int STATUS_INFORUNBD = 4;
    static final int STATUS_USERINTERRUPT = 10;
    static final int STATUS_TERMINATE = 11;
    static final int STATUS_NODELIMIT = 20;
    static final int STATUS_TOTALNODELIMIT = 21;
    static final int STATUS_STALLNODELIMIT = 22;
    static final int STATUS_TIMELIMIT = 23;
    static final int STATUS_MEMLIMIT = 24;
    static final int STATUS_GAPLIMIT = 25;
    static final int STATUS_PRIMALLIMIT = 26;
    static final int STATUS_DUALLIMIT = 27;
    static final int STATUS_SOLLIMIT = 28;
    static final int STATUS_BESTSOLLIMIT = 29;
    static final int STATUS_RESTARTLIMIT = 30;

    // SCIP_OBJSENSE values
    static final int OBJSENSE_MAXIMIZE = -1;
    static final int OBJSENSE_MINIMIZE = 1;

    // SCIP_VARTYPE values
    static final int VARTYPE_BINARY = 0;
    static final int VARTYPE_INTEGER = 1;
    static final int VARTYPE_IMPLINT = 2;
    static final int VARTYPE_CONTINUOUS = 3;

    private static final Linker LINKER = Linker.nativeLinker();
    private static final SymbolLookup LOOKUP;

    private static final MethodHandle SCIP_CREATE;
    private static final MethodHandle SCIP_FREE;
    private static final MethodHandle SCIP_INCLUDE_DEFAULT_PLUGINS;
    private static final MethodHandle SCIP_CREATE_PROB_BASIC;
    private static final MethodHandle SCIP_SET_OBJ_SENSE;

    private static final MethodHandle SCIP_CREATE_VAR_BASIC;
    private static final MethodHandle SCIP_ADD_VAR;
    private static final MethodHandle SCIP_RELEASE_VAR;
    private static final MethodHandle SCIP_CHG_VAR_LB;
    private static final MethodHandle SCIP_CHG_VAR_UB;

    private static final MethodHandle SCIP_CREATE_CONS_BASIC_LINEAR;
    private static final MethodHandle SCIP_CREATE_CONS_BASIC_QUADRATIC_NONLINEAR;
    private static final MethodHandle SCIP_ADD_CONS;
    private static final MethodHandle SCIP_RELEASE_CONS;

    private static final MethodHandle SCIP_SOLVE;
    private static final MethodHandle SCIP_SOLVE_CONCURRENT;
    private static final MethodHandle SCIP_FREE_TRANSFORM;
    private static final MethodHandle SCIP_GET_STATUS;

    private static final MethodHandle SCIP_GET_BEST_SOL;
    private static final MethodHandle SCIP_GET_SOL_VAL;
    private static final MethodHandle SCIP_GET_SOL_ORIG_OBJ;

    private static final MethodHandle SCIP_SET_INT_PARAM;
    private static final MethodHandle SCIP_SET_REAL_PARAM;
    private static final MethodHandle SCIP_SET_LONGINT_PARAM;

    static {

        LOOKUP = NativeLoader.load();

        // SCIP_RETCODE SCIPcreate(SCIP** scip)
        SCIP_CREATE = SCIPBindings.downcallHandle("SCIPcreate", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

        // SCIP_RETCODE SCIPfree(SCIP** scip)
        SCIP_FREE = SCIPBindings.downcallHandle("SCIPfree", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

        // SCIP_RETCODE SCIPincludeDefaultPlugins(SCIP* scip)
        SCIP_INCLUDE_DEFAULT_PLUGINS = SCIPBindings.downcallHandle("SCIPincludeDefaultPlugins",
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

        // SCIP_RETCODE SCIPcreateProbBasic(SCIP* scip, const char* name)
        SCIP_CREATE_PROB_BASIC = SCIPBindings.downcallHandle("SCIPcreateProbBasic",
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

        // SCIP_RETCODE SCIPsetObjsense(SCIP* scip, SCIP_OBJSENSE objsense)
        SCIP_SET_OBJ_SENSE = SCIPBindings.downcallHandle("SCIPsetObjsense",
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));

        // SCIP_RETCODE SCIPcreateVarBasic(SCIP* scip, SCIP_VAR** var, const char* name, double lb, double ub, double obj, SCIP_VARTYPE vartype)
        SCIP_CREATE_VAR_BASIC = SCIPBindings.downcallHandle("SCIPcreateVarBasic", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, // scip
                ValueLayout.ADDRESS, // var**
                ValueLayout.ADDRESS, // name
                ValueLayout.JAVA_DOUBLE, // lb
                ValueLayout.JAVA_DOUBLE, // ub
                ValueLayout.JAVA_DOUBLE, // obj
                ValueLayout.JAVA_INT)); // vartype

        // SCIP_RETCODE SCIPaddVar(SCIP* scip, SCIP_VAR* var)
        SCIP_ADD_VAR = SCIPBindings.downcallHandle("SCIPaddVar", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

        // SCIP_RETCODE SCIPreleaseVar(SCIP* scip, SCIP_VAR** var)
        SCIP_RELEASE_VAR = SCIPBindings.downcallHandle("SCIPreleaseVar", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

        // SCIP_RETCODE SCIPchgVarLb(SCIP* scip, SCIP_VAR* var, double newbound)
        SCIP_CHG_VAR_LB = SCIPBindings.downcallHandle("SCIPchgVarLb",
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_DOUBLE));

        // SCIP_RETCODE SCIPchgVarUb(SCIP* scip, SCIP_VAR* var, double newbound)
        SCIP_CHG_VAR_UB = SCIPBindings.downcallHandle("SCIPchgVarUb",
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_DOUBLE));

        // SCIP_RETCODE SCIPcreateConsBasicLinear(SCIP* scip, SCIP_CONS** cons, const char* name,
        //     int nvars, SCIP_VAR** vars, double* vals, double lhs, double rhs)
        SCIP_CREATE_CONS_BASIC_LINEAR = SCIPBindings.downcallHandle("SCIPcreateConsBasicLinear",
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, // scip
                        ValueLayout.ADDRESS, // cons**
                        ValueLayout.ADDRESS, // name
                        ValueLayout.JAVA_INT, // nvars
                        ValueLayout.ADDRESS, // vars (SCIP_VAR**)
                        ValueLayout.ADDRESS, // vals (double*)
                        ValueLayout.JAVA_DOUBLE, // lhs
                        ValueLayout.JAVA_DOUBLE)); // rhs

        // SCIP_RETCODE SCIPcreateConsBasicQuadraticNonlinear(SCIP* scip, SCIP_CONS** cons, const char* name,
        //     int nlinvars, SCIP_VAR** linvars, double* lincoefs,
        //     int nquadterms, SCIP_VAR** quadvars1, SCIP_VAR** quadvars2, double* quadcoefs,
        //     double lhs, double rhs)
        SCIP_CREATE_CONS_BASIC_QUADRATIC_NONLINEAR = SCIPBindings.downcallHandle("SCIPcreateConsBasicQuadraticNonlinear",
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, // scip
                        ValueLayout.ADDRESS, // cons**
                        ValueLayout.ADDRESS, // name
                        ValueLayout.JAVA_INT, // nlinvars
                        ValueLayout.ADDRESS, // linvars (SCIP_VAR**)
                        ValueLayout.ADDRESS, // lincoefs (double*)
                        ValueLayout.JAVA_INT, // nquadterms
                        ValueLayout.ADDRESS, // quadvars1 (SCIP_VAR**)
                        ValueLayout.ADDRESS, // quadvars2 (SCIP_VAR**)
                        ValueLayout.ADDRESS, // quadcoefs (double*)
                        ValueLayout.JAVA_DOUBLE, // lhs
                        ValueLayout.JAVA_DOUBLE)); // rhs

        // SCIP_RETCODE SCIPaddCons(SCIP* scip, SCIP_CONS* cons)
        SCIP_ADD_CONS = SCIPBindings.downcallHandle("SCIPaddCons", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

        // SCIP_RETCODE SCIPreleaseCons(SCIP* scip, SCIP_CONS** cons)
        SCIP_RELEASE_CONS = SCIPBindings.downcallHandle("SCIPreleaseCons",
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

        // SCIP_RETCODE SCIPsolve(SCIP* scip)
        SCIP_SOLVE = SCIPBindings.downcallHandle("SCIPsolve", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

        // SCIP_RETCODE SCIPsolveConcurrent(SCIP* scip) — requires TPI-enabled build (e.g. TBB)
        SCIP_SOLVE_CONCURRENT = SCIPBindings.downcallHandleOptional("SCIPsolveConcurrent", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

        // SCIP_RETCODE SCIPfreeTransform(SCIP* scip)
        SCIP_FREE_TRANSFORM = SCIPBindings.downcallHandle("SCIPfreeTransform", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

        // SCIP_STATUS SCIPgetStatus(SCIP* scip)
        SCIP_GET_STATUS = SCIPBindings.downcallHandle("SCIPgetStatus", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

        // SCIP_SOL* SCIPgetBestSol(SCIP* scip)
        SCIP_GET_BEST_SOL = SCIPBindings.downcallHandle("SCIPgetBestSol", FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS));

        // double SCIPgetSolVal(SCIP* scip, SCIP_SOL* sol, SCIP_VAR* var)
        SCIP_GET_SOL_VAL = SCIPBindings.downcallHandle("SCIPgetSolVal",
                FunctionDescriptor.of(ValueLayout.JAVA_DOUBLE, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

        // double SCIPgetSolOrigObj(SCIP* scip, SCIP_SOL* sol)
        SCIP_GET_SOL_ORIG_OBJ = SCIPBindings.downcallHandle("SCIPgetSolOrigObj",
                FunctionDescriptor.of(ValueLayout.JAVA_DOUBLE, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

        // SCIP_RETCODE SCIPsetIntParam(SCIP* scip, const char* name, int value)
        SCIP_SET_INT_PARAM = SCIPBindings.downcallHandle("SCIPsetIntParam",
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));

        // SCIP_RETCODE SCIPsetRealParam(SCIP* scip, const char* name, double value)
        SCIP_SET_REAL_PARAM = SCIPBindings.downcallHandle("SCIPsetRealParam",
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_DOUBLE));

        // SCIP_RETCODE SCIPsetLongintParam(SCIP* scip, const char* name, long value)
        SCIP_SET_LONGINT_PARAM = SCIPBindings.downcallHandle("SCIPsetLongintParam",
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));
    }

    /**
     * Verify the {@code SCIP_RETCODE} returned by a native call. Anything other than {@link #SCIP_OKAY} means
     * the call did not do what was asked, and letting that pass would leave a silently incomplete model.
     *
     * @param retcode  the value returned by the native function
     * @param function the name of the native function, for the error message
     */
    private static void check(final int retcode, final String function) {
        if (retcode != SCIP_OKAY) {
            throw new IllegalStateException(function + " failed with " + SCIPBindings.describe(retcode));
        }
    }

    private static String describe(final int retcode) {
        String name = switch (retcode) {
            case SCIP_ERROR              -> "SCIP_ERROR";
            case SCIP_NOMEMORY           -> "SCIP_NOMEMORY";
            case SCIP_READERROR          -> "SCIP_READERROR";
            case SCIP_WRITEERROR         -> "SCIP_WRITEERROR";
            case SCIP_NOFILE             -> "SCIP_NOFILE";
            case SCIP_FILECREATEERROR    -> "SCIP_FILECREATEERROR";
            case SCIP_LPERROR            -> "SCIP_LPERROR";
            case SCIP_NOPROBLEM          -> "SCIP_NOPROBLEM";
            case SCIP_INVALIDCALL        -> "SCIP_INVALIDCALL";
            case SCIP_INVALIDDATA        -> "SCIP_INVALIDDATA";
            case SCIP_INVALIDRESULT      -> "SCIP_INVALIDRESULT";
            case SCIP_PLUGINNOTFOUND     -> "SCIP_PLUGINNOTFOUND";
            case SCIP_PARAMETERUNKNOWN   -> "SCIP_PARAMETERUNKNOWN";
            case SCIP_PARAMETERWRONGTYPE -> "SCIP_PARAMETERWRONGTYPE";
            case SCIP_PARAMETERWRONGVAL  -> "SCIP_PARAMETERWRONGVAL";
            case SCIP_KEYALREADYEXISTING -> "SCIP_KEYALREADYEXISTING";
            case SCIP_MAXDEPTHLEVEL      -> "SCIP_MAXDEPTHLEVEL";
            case SCIP_BRANCHERROR        -> "SCIP_BRANCHERROR";
            case SCIP_NOTIMPLEMENTED     -> "SCIP_NOTIMPLEMENTED";
            default                      -> "unknown SCIP_RETCODE";
        };
        return name + " (" + retcode + ")";
    }

    private static MethodHandle downcallHandle(final String name, final FunctionDescriptor descriptor) {

        MemorySegment symbol = LOOKUP.find(name).orElseThrow(() -> new UnsatisfiedLinkError("Symbol not found: " + name));
        return LINKER.downcallHandle(symbol, descriptor);
    }

    private static MethodHandle downcallHandleOptional(final String name, final FunctionDescriptor descriptor) {

        return LOOKUP.find(name).map(symbol -> LINKER.downcallHandle(symbol, descriptor)).orElse(null);
    }

    static void addCons(final MemorySegment scip, final MemorySegment cons) {

        int retcode;
        try {
            retcode = (int) SCIP_ADD_CONS.invokeExact(scip, cons);
        } catch (Throwable t) {
            throw new RuntimeException("SCIPaddCons failed", t);
        }
        SCIPBindings.check(retcode, "SCIPaddCons");
    }

    static void addVar(final MemorySegment scip, final MemorySegment var) {

        int retcode;
        try {
            retcode = (int) SCIP_ADD_VAR.invokeExact(scip, var);
        } catch (Throwable t) {
            throw new RuntimeException("SCIPaddVar failed", t);
        }
        SCIPBindings.check(retcode, "SCIPaddVar");
    }

    static MemorySegment allocateDoubleArray(final Arena arena, final double[] values) {

        MemorySegment segment = arena.allocate(ValueLayout.JAVA_DOUBLE, values.length);
        MemorySegment.copy(values, 0, segment, ValueLayout.JAVA_DOUBLE, 0, values.length);
        return segment;
    }

    static MemorySegment allocatePointerArray(final Arena arena, final MemorySegment[] pointers) {

        MemorySegment segment = arena.allocate(ValueLayout.ADDRESS, pointers.length);
        for (int i = 0; i < pointers.length; i++) {
            segment.setAtIndex(ValueLayout.ADDRESS, i, pointers[i]);
        }
        return segment;
    }

    static MemorySegment allocateString(final Arena arena, final String value) {
        return arena.allocateFrom(value);
    }

    static void chgVarLb(final MemorySegment scip, final MemorySegment var, final double newbound) {

        int retcode;
        try {
            retcode = (int) SCIP_CHG_VAR_LB.invokeExact(scip, var, newbound);
        } catch (Throwable t) {
            throw new RuntimeException("SCIPchgVarLb failed", t);
        }
        SCIPBindings.check(retcode, "SCIPchgVarLb");
    }

    static void chgVarUb(final MemorySegment scip, final MemorySegment var, final double newbound) {

        int retcode;
        try {
            retcode = (int) SCIP_CHG_VAR_UB.invokeExact(scip, var, newbound);
        } catch (Throwable t) {
            throw new RuntimeException("SCIPchgVarUb failed", t);
        }
        SCIPBindings.check(retcode, "SCIPchgVarUb");
    }

    static void create(final MemorySegment scipPtr) {

        int retcode;
        try {
            retcode = (int) SCIP_CREATE.invokeExact(scipPtr);
        } catch (Throwable t) {
            throw new RuntimeException("SCIPcreate failed", t);
        }
        SCIPBindings.check(retcode, "SCIPcreate");
    }

    static void createConsBasicLinear(final MemorySegment scip, final MemorySegment consPtr, final MemorySegment name, final int nvars,
            final MemorySegment vars, final MemorySegment vals, final double lhs, final double rhs) {

        int retcode;
        try {
            retcode = (int) SCIP_CREATE_CONS_BASIC_LINEAR.invokeExact(scip, consPtr, name, nvars, vars, vals, lhs, rhs);
        } catch (Throwable t) {
            throw new RuntimeException("SCIPcreateConsBasicLinear failed", t);
        }
        SCIPBindings.check(retcode, "SCIPcreateConsBasicLinear");
    }

    static void createConsBasicQuadraticNonlinear(final MemorySegment scip, final MemorySegment consPtr, final MemorySegment name, final int nlinvars,
            final MemorySegment linvars, final MemorySegment lincoefs, final int nquadterms, final MemorySegment quadvars1, final MemorySegment quadvars2,
            final MemorySegment quadcoefs, final double lhs, final double rhs) {

        int retcode;
        try {
            retcode = (int) SCIP_CREATE_CONS_BASIC_QUADRATIC_NONLINEAR.invokeExact(scip, consPtr, name, nlinvars, linvars, lincoefs, nquadterms, quadvars1,
                    quadvars2, quadcoefs, lhs, rhs);
        } catch (Throwable t) {
            throw new RuntimeException("SCIPcreateConsBasicQuadraticNonlinear failed", t);
        }
        SCIPBindings.check(retcode, "SCIPcreateConsBasicQuadraticNonlinear");
    }

    static void createProbBasic(final MemorySegment scip, final MemorySegment name) {

        int retcode;
        try {
            retcode = (int) SCIP_CREATE_PROB_BASIC.invokeExact(scip, name);
        } catch (Throwable t) {
            throw new RuntimeException("SCIPcreateProbBasic failed", t);
        }
        SCIPBindings.check(retcode, "SCIPcreateProbBasic");
    }

    static void createVarBasic(final MemorySegment scip, final MemorySegment varPtr, final MemorySegment name, final double lb, final double ub,
            final double obj, final int vartype) {

        int retcode;
        try {
            retcode = (int) SCIP_CREATE_VAR_BASIC.invokeExact(scip, varPtr, name, lb, ub, obj, vartype);
        } catch (Throwable t) {
            throw new RuntimeException("SCIPcreateVarBasic failed", t);
        }
        SCIPBindings.check(retcode, "SCIPcreateVarBasic");
    }

    static void free(final MemorySegment scipPtr) {

        int retcode;
        try {
            retcode = (int) SCIP_FREE.invokeExact(scipPtr);
        } catch (Throwable t) {
            throw new RuntimeException("SCIPfree failed", t);
        }
        SCIPBindings.check(retcode, "SCIPfree");
    }

    static void freeTransform(final MemorySegment scip) {

        int retcode;
        try {
            retcode = (int) SCIP_FREE_TRANSFORM.invokeExact(scip);
        } catch (Throwable t) {
            throw new RuntimeException("SCIPfreeTransform failed", t);
        }
        SCIPBindings.check(retcode, "SCIPfreeTransform");
    }

    static MemorySegment getBestSol(final MemorySegment scip) {

        try {
            return (MemorySegment) SCIP_GET_BEST_SOL.invokeExact(scip);
        } catch (Throwable t) {
            throw new RuntimeException("SCIPgetBestSol failed", t);
        }
    }

    static double getSolOrigObj(final MemorySegment scip, final MemorySegment sol) {

        try {
            return (double) SCIP_GET_SOL_ORIG_OBJ.invokeExact(scip, sol);
        } catch (Throwable t) {
            throw new RuntimeException("SCIPgetSolOrigObj failed", t);
        }
    }

    static double getSolVal(final MemorySegment scip, final MemorySegment sol, final MemorySegment var) {

        try {
            return (double) SCIP_GET_SOL_VAL.invokeExact(scip, sol, var);
        } catch (Throwable t) {
            throw new RuntimeException("SCIPgetSolVal failed", t);
        }
    }

    /**
     * Note that this returns a {@code SCIP_STATUS}, not a {@code SCIP_RETCODE} - there is nothing to check.
     */
    static int getStatus(final MemorySegment scip) {

        try {
            return (int) SCIP_GET_STATUS.invokeExact(scip);
        } catch (Throwable t) {
            throw new RuntimeException("SCIPgetStatus failed", t);
        }
    }

    static boolean hasSolveConcurrent() {
        return SCIP_SOLVE_CONCURRENT != null;
    }

    static void includeDefaultPlugins(final MemorySegment scip) {

        int retcode;
        try {
            retcode = (int) SCIP_INCLUDE_DEFAULT_PLUGINS.invokeExact(scip);
        } catch (Throwable t) {
            throw new RuntimeException("SCIPincludeDefaultPlugins failed", t);
        }
        SCIPBindings.check(retcode, "SCIPincludeDefaultPlugins");
    }

    static void releaseCons(final MemorySegment scip, final MemorySegment consPtr) {

        int retcode;
        try {
            retcode = (int) SCIP_RELEASE_CONS.invokeExact(scip, consPtr);
        } catch (Throwable t) {
            throw new RuntimeException("SCIPreleaseCons failed", t);
        }
        SCIPBindings.check(retcode, "SCIPreleaseCons");
    }

    static void releaseVar(final MemorySegment scip, final MemorySegment varPtr) {

        int retcode;
        try {
            retcode = (int) SCIP_RELEASE_VAR.invokeExact(scip, varPtr);
        } catch (Throwable t) {
            throw new RuntimeException("SCIPreleaseVar failed", t);
        }
        SCIPBindings.check(retcode, "SCIPreleaseVar");
    }

    static void setIntParam(final MemorySegment scip, final MemorySegment name, final int value) {

        int retcode;
        try {
            retcode = (int) SCIP_SET_INT_PARAM.invokeExact(scip, name, value);
        } catch (Throwable t) {
            throw new RuntimeException("SCIPsetIntParam failed", t);
        }
        SCIPBindings.check(retcode, "SCIPsetIntParam");
    }

    static void setLongintParam(final MemorySegment scip, final MemorySegment name, final long value) {

        int retcode;
        try {
            retcode = (int) SCIP_SET_LONGINT_PARAM.invokeExact(scip, name, value);
        } catch (Throwable t) {
            throw new RuntimeException("SCIPsetLongintParam failed", t);
        }
        SCIPBindings.check(retcode, "SCIPsetLongintParam");
    }

    static void setObjsense(final MemorySegment scip, final int objsense) {

        int retcode;
        try {
            retcode = (int) SCIP_SET_OBJ_SENSE.invokeExact(scip, objsense);
        } catch (Throwable t) {
            throw new RuntimeException("SCIPsetObjsense failed", t);
        }
        SCIPBindings.check(retcode, "SCIPsetObjsense");
    }

    static void setRealParam(final MemorySegment scip, final MemorySegment name, final double value) {

        int retcode;
        try {
            retcode = (int) SCIP_SET_REAL_PARAM.invokeExact(scip, name, value);
        } catch (Throwable t) {
            throw new RuntimeException("SCIPsetRealParam failed", t);
        }
        SCIPBindings.check(retcode, "SCIPsetRealParam");
    }

    static void solve(final MemorySegment scip) {

        int retcode;
        try {
            retcode = (int) SCIP_SOLVE.invokeExact(scip);
        } catch (Throwable t) {
            throw new RuntimeException("SCIPsolve failed", t);
        }
        SCIPBindings.check(retcode, "SCIPsolve");
    }

    static void solveConcurrent(final MemorySegment scip) {

        int retcode;
        try {
            retcode = (int) SCIP_SOLVE_CONCURRENT.invokeExact(scip);
        } catch (Throwable t) {
            throw new RuntimeException("SCIPsolveConcurrent failed", t);
        }
        SCIPBindings.check(retcode, "SCIPsolveConcurrent");
    }

    private SCIPBindings() {}

}
