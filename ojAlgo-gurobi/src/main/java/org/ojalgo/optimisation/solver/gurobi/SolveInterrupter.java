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
package org.ojalgo.optimisation.solver.gurobi;

import org.ojalgo.netio.BasicLogger;

/**
 * Turns {@link Thread#interrupt()} of the thread that started a solve into a call to the solver's own "stop
 * now" function.
 * <p>
 * That indirection is necessary because the solving thread is inside native code for the whole solve: it
 * cannot notice its own interrupt, and the JVM cannot unwind a native frame. Something else has to watch the
 * flag, so this polls it from a separate thread and stops the solver on the solver's own terms.
 * <p>
 * The interrupt is never cleared, and neither is anything thrown here. Whoever set the flag decides what an
 * interrupted solve means; this only makes sure it stops.
 */
final class SolveInterrupter implements AutoCloseable {

    private static final long POLL_MILLIS = 100L;

    /**
     * Starts watching the calling thread. Call this immediately before handing the model to the solver, and
     * {@link #close()} as soon as it returns – which a try-with-resources does.
     *
     * @param stopSolver How to tell the solver to stop. Called repeatedly, from the watching thread, so it
     *        must be safe to call while the solver is running and to call more than once.
     */
    static SolveInterrupter watch(final Runnable stopSolver) {

        Thread solving = Thread.currentThread();

        Thread watching = new Thread(() -> {
            try {

                while (!solving.isInterrupted()) {
                    Thread.sleep(POLL_MILLIS);
                }

                // Asked repeatedly rather than once. A solver that is asked to stop before it has properly
                // started can lose the request while resetting its own state for the solve, and the interrupt
                // that prompted this stays set – so keep asking until the solve returns.
                while (true) {
                    stopSolver.run();
                    Thread.sleep(POLL_MILLIS);
                }

            } catch (InterruptedException cause) {
                // The solve finished on its own – there is nothing left to stop.
            } catch (RuntimeException cause) {
                BasicLogger.error("Failed to stop the solver!", cause);
            }
        }, "ojAlgo-gurobi-interrupt");

        watching.setDaemon(true);
        watching.start();

        return new SolveInterrupter(watching);
    }

    private final Thread myWatching;

    private SolveInterrupter(final Thread watching) {
        super();
        myWatching = watching;
    }

    @Override
    public void close() {
        myWatching.interrupt();
    }

}
