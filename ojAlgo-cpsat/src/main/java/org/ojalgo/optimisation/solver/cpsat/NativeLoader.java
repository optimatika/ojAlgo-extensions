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
import java.lang.foreign.Linker;
import java.lang.foreign.SymbolLookup;
import java.nio.file.Files;
import java.nio.file.Path;

final class NativeLoader {

    /**
     * {@code SolveCpModel} itself is a C++ function (declared in {@code cp_model_solver.h}, no
     * {@code extern "C"}), so its exported symbol name is mangled and cannot be probed for directly. The C
     * API in {@code ortools/sat/c_api/cp_solver_c.h} - compiled into the same {@code libortools} - exports
     * {@code SolveCpModelWithParameters} with C linkage instead; that is what {@link CPSATBindings} actually
     * calls, so it is what gets probed for here.
     */
    private static final String PROBE_SYMBOL = "SolveCpModelWithParameters";

    private static volatile SymbolLookup LOADED_LOOKUP;

    private static SymbolLookup findAlreadyLoaded() {

        SymbolLookup loaderLookup = SymbolLookup.loaderLookup();
        if (loaderLookup.find(PROBE_SYMBOL).isPresent()) {
            return loaderLookup;
        }

        SymbolLookup defaultLookup = Linker.nativeLinker().defaultLookup();
        if (defaultLookup.find(PROBE_SYMBOL).isPresent()) {
            return defaultLookup;
        }

        return null;
    }

    private static Path findLibrary() {

        String osName = System.getProperty("os.name").toLowerCase();
        String osArch = System.getProperty("os.arch").toLowerCase();

        String[] searchPaths;

        if (osName.contains("mac") || osName.contains("darwin")) {

            String brewPrefix = "aarch64".equals(osArch) || "arm64".equals(osArch) ? "/opt/homebrew" : "/usr/local";

            searchPaths = new String[]{
                    brewPrefix + "/lib/libortools.dylib",
                    "/usr/local/lib/libortools.dylib"
            };

        } else if (osName.contains("linux")) {

            searchPaths = new String[]{
                    "/usr/lib/libortools.so",
                    "/usr/local/lib/libortools.so",
                    "/usr/lib/x86_64-linux-gnu/libortools.so",
                    "/usr/lib/aarch64-linux-gnu/libortools.so"
            };

        } else if (osName.contains("windows")) {

            searchPaths = new String[]{
                    "C:\\Program Files\\or-tools\\bin\\ortools.dll"
            };

        } else {
            throw new UnsatisfiedLinkError("Unsupported operating system: " + osName);
        }

        for (String path : searchPaths) {
            Path candidate = Path.of(path);
            if (Files.exists(candidate)) {
                return candidate;
            }
        }

        throw new UnsatisfiedLinkError("OR-Tools native library not found. Install OR-Tools on the system.");
    }

    static synchronized SymbolLookup load() {

        if (LOADED_LOOKUP != null) {
            return LOADED_LOOKUP;
        }

        SymbolLookup alreadyLoaded = NativeLoader.findAlreadyLoaded();
        if (alreadyLoaded != null) {
            LOADED_LOOKUP = alreadyLoaded;
            return LOADED_LOOKUP;
        }

        Path libraryPath = NativeLoader.findLibrary();

        LOADED_LOOKUP = SymbolLookup.libraryLookup(libraryPath, Arena.ofAuto());

        return LOADED_LOOKUP;
    }

    private NativeLoader() {}

}
