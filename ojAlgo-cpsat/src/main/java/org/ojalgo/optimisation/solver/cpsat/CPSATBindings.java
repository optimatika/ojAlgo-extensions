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
package org.ojalgo.optimisation.solver.cpsat;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;

/**
 * FFM bindings for Google CP-SAT via OR-Tools' C API.
 * <p>
 * OR-Tools exports a small, protobuf-based C API for the CP-SAT solver from
 * {@code ortools/sat/c_api/cp_solver_c.h}. That translation unit is compiled straight into
 * {@code libortools} (see its {@code CMakeLists.txt}: "Will be merge in libortools.so") - there is no
 * SWIG/JNI layer and no full OR-Tools Java jar involved. A model is handed over as a serialized
 * {@code CpModelProto}, solved with a serialized {@code SatParameters}, and the solver hands back a
 * serialized {@code CpSolverResponse}: three flat byte buffers cross the native boundary, nothing else.
 * <p>
 * This class only deals with those bytes and the raw native entry points. Building the request messages
 * and parsing the response is {@link SolverCPSAT}'s job - it needs to know what the bytes mean, this class
 * does not.
 *
 * @see <a href="https://developers.google.com/optimization/cp/cp_solver">CP-SAT</a>
 * @see <a href=
 *      "https://github.com/google/or-tools/blob/main/ortools/sat/c_api/cp_solver_c.h">cp_solver_c.h</a>
 */
final class CPSATBindings {

    /**
     * {@code CpSolverStatus} enum values, see {@code cp_model.proto}.
     */
    static final int STATUS_UNKNOWN = 0;
    static final int STATUS_MODEL_INVALID = 1;
    static final int STATUS_FEASIBLE = 2;
    static final int STATUS_INFEASIBLE = 3;
    static final int STATUS_OPTIMAL = 4;

    private static final Linker LINKER = Linker.nativeLinker();
    private static final SymbolLookup LOOKUP;

    private static final MethodHandle SOLVE_CP_MODEL_WITH_PARAMETERS;
    private static final MethodHandle SOLVE_CP_NEW_ENV;
    private static final MethodHandle SOLVE_CP_DESTROY_ENV;
    private static final MethodHandle SOLVE_CP_STOP_SEARCH;
    private static final MethodHandle SOLVE_CP_INTERRUPTIBLE;

    /**
     * {@code free(3)}, used to release the response buffer the native side allocates with
     * {@code strings::memdup} (a thin wrapper over {@code malloc} - see {@code cp_solver_c.cc}). {@code null}
     * on a platform where the process' default symbol lookup does not expose it, in which case the response
     * buffer is deliberately leaked rather than risking a crash by freeing through the wrong allocator.
     */
    private static final MethodHandle FREE;

    static {

        LOOKUP = NativeLoader.load();

        // void SolveCpModelWithParameters(const void* creq, int creq_len, const void* cparams,
        //                                 int cparams_len, void** cres, int* cres_len)
        SOLVE_CP_MODEL_WITH_PARAMETERS = CPSATBindings.downcallHandle("SolveCpModelWithParameters",
                FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.JAVA_INT, // creq, creq_len
                        ValueLayout.ADDRESS, ValueLayout.JAVA_INT, // cparams, cparams_len
                        ValueLayout.ADDRESS, ValueLayout.ADDRESS)); // cres (void**), cres_len (int*)

        // void* SolveCpNewEnv()
        SOLVE_CP_NEW_ENV = CPSATBindings.downcallHandle("SolveCpNewEnv", FunctionDescriptor.of(ValueLayout.ADDRESS));

        // void SolveCpDestroyEnv(void* cenv)
        SOLVE_CP_DESTROY_ENV = CPSATBindings.downcallHandle("SolveCpDestroyEnv", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

        // void SolveCpStopSearch(void* cenv)
        SOLVE_CP_STOP_SEARCH = CPSATBindings.downcallHandle("SolveCpStopSearch", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

        // void SolveCpInterruptible(void* cenv, const void* creq, int creq_len, const void* cparams,
        //                           int cparams_len, void** cres, int* cres_len)
        SOLVE_CP_INTERRUPTIBLE = CPSATBindings.downcallHandle("SolveCpInterruptible",
                FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, // cenv
                        ValueLayout.ADDRESS, ValueLayout.JAVA_INT, // creq, creq_len
                        ValueLayout.ADDRESS, ValueLayout.JAVA_INT, // cparams, cparams_len
                        ValueLayout.ADDRESS, ValueLayout.ADDRESS)); // cres, cres_len

        FREE = LINKER.defaultLookup().find("free")
                .map(symbol -> LINKER.downcallHandle(symbol, FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)))
                .orElse(null);
    }

    /**
     * Allocates {@code data.length} bytes (at least 1, so a zero-length array never produces a zero-sized
     * segment) and copies {@code data} into them.
     */
    private static MemorySegment allocateBytes(final Arena arena, final byte[] data) {

        MemorySegment segment = arena.allocate(Math.max(data.length, 1));
        if (data.length > 0) {
            MemorySegment.copy(data, 0, segment, ValueLayout.JAVA_BYTE, 0, data.length);
        }
        return segment;
    }

    /**
     * Copies the {@code CpSolverResponse} bytes out of the {@code void** cres}/{@code int* cres_len}
     * out-parameters written by the native call, then frees the native buffer.
     */
    private static byte[] copyAndFreeResponse(final MemorySegment cresPtr, final MemorySegment cresLenPtr) throws Throwable {

        MemorySegment resSeg = cresPtr.get(ValueLayout.ADDRESS, 0);
        int resLen = cresLenPtr.get(ValueLayout.JAVA_INT, 0);

        byte[] result = new byte[Math.max(resLen, 0)];

        if (resLen > 0 && !resSeg.equals(MemorySegment.NULL)) {

            MemorySegment.copy(resSeg.reinterpret(resLen), ValueLayout.JAVA_BYTE, 0, result, 0, resLen);

            if (FREE != null) {
                FREE.invokeExact(resSeg);
            }
        }

        return result;
    }

    private static MethodHandle downcallHandle(final String name, final FunctionDescriptor descriptor) {

        MemorySegment symbol = LOOKUP.find(name).orElseThrow(() -> new UnsatisfiedLinkError("Symbol not found: " + name));
        return LINKER.downcallHandle(symbol, descriptor);
    }

    /**
     * Destroys a solve environment created by {@link #newEnv()}.
     */
    static void destroyEnv(final MemorySegment env) {

        try {
            SOLVE_CP_DESTROY_ENV.invokeExact(env);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to destroy CP-SAT environment", t);
        }
    }

    /**
     * Creates a solve environment. Only needed in order to be able to interrupt a solve in progress (via
     * {@link #stopSearch(MemorySegment)}); a one-shot, non-interruptible solve can go through
     * {@link #solve(byte[], byte[])} instead, which creates and destroys its own throwaway environment
     * entirely on the native side.
     *
     * @return a native pointer to the new environment
     */
    static MemorySegment newEnv() {

        try {
            return (MemorySegment) SOLVE_CP_NEW_ENV.invokeExact();
        } catch (Throwable t) {
            throw new RuntimeException("Failed to create CP-SAT environment", t);
        }
    }

    /**
     * Solves a serialized {@code CpModelProto} with a serialized {@code SatParameters}, using a fresh
     * environment that is created and destroyed entirely on the native side. Cannot be interrupted once
     * called - use {@link #solveInterruptible(MemorySegment, byte[], byte[])} with an environment from
     * {@link #newEnv()} for that.
     *
     * @param modelProtoBytes  a serialized {@code CpModelProto}
     * @param paramsProtoBytes a serialized {@code SatParameters}
     * @return the serialized {@code CpSolverResponse}
     */
    static byte[] solve(final byte[] modelProtoBytes, final byte[] paramsProtoBytes) {

        try (Arena arena = Arena.ofConfined()) {

            MemorySegment reqSeg = CPSATBindings.allocateBytes(arena, modelProtoBytes);
            MemorySegment paramsSeg = CPSATBindings.allocateBytes(arena, paramsProtoBytes);
            MemorySegment cresPtr = arena.allocate(ValueLayout.ADDRESS);
            MemorySegment cresLenPtr = arena.allocate(ValueLayout.JAVA_INT);

            SOLVE_CP_MODEL_WITH_PARAMETERS.invokeExact(reqSeg, modelProtoBytes.length, paramsSeg, paramsProtoBytes.length, cresPtr, cresLenPtr);

            return CPSATBindings.copyAndFreeResponse(cresPtr, cresLenPtr);

        } catch (Throwable t) {
            throw new RuntimeException("Failed to solve CP-SAT model", t);
        }
    }

    /**
     * As {@link #solve(byte[], byte[])}, but against an explicit environment so that a concurrent call to
     * {@link #stopSearch(MemorySegment)} can interrupt it.
     *
     * @param env              a native pointer obtained from {@link #newEnv()}
     * @param modelProtoBytes  a serialized {@code CpModelProto}
     * @param paramsProtoBytes a serialized {@code SatParameters}
     * @return the serialized {@code CpSolverResponse}
     */
    static byte[] solveInterruptible(final MemorySegment env, final byte[] modelProtoBytes, final byte[] paramsProtoBytes) {

        try (Arena arena = Arena.ofConfined()) {

            MemorySegment reqSeg = CPSATBindings.allocateBytes(arena, modelProtoBytes);
            MemorySegment paramsSeg = CPSATBindings.allocateBytes(arena, paramsProtoBytes);
            MemorySegment cresPtr = arena.allocate(ValueLayout.ADDRESS);
            MemorySegment cresLenPtr = arena.allocate(ValueLayout.JAVA_INT);

            SOLVE_CP_INTERRUPTIBLE.invokeExact(env, reqSeg, modelProtoBytes.length, paramsSeg, paramsProtoBytes.length, cresPtr, cresLenPtr);

            return CPSATBindings.copyAndFreeResponse(cresPtr, cresLenPtr);

        } catch (Throwable t) {
            throw new RuntimeException("Failed to solve CP-SAT model", t);
        }
    }

    /**
     * Asks a solve started with {@link #solveInterruptible(MemorySegment, byte[], byte[])} against this
     * environment to stop. Safe to call from a thread other than the one solving, and safe to call more than
     * once.
     */
    static void stopSearch(final MemorySegment env) {

        try {
            SOLVE_CP_STOP_SEARCH.invokeExact(env);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to stop CP-SAT search", t);
        }
    }

    private CPSATBindings() {}

}
