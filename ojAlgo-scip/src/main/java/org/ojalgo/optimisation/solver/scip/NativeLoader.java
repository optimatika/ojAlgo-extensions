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
import java.lang.foreign.Linker;
import java.lang.foreign.SymbolLookup;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Handles loading of the SCIP native library from the system.
 * <p>
 * SCIP must be installed on the system (e.g. via Homebrew on macOS, or a package manager on Linux). The
 * library is located by searching well-known installation paths for the current platform.
 * <p>
 * If SCIP is already loaded in the process (e.g. by OR-Tools), the existing symbols are reused to avoid
 * loading a second copy of the library.
 */
final class NativeLoader {

    private static final String PROBE_SYMBOL = "SCIPcreate";

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
                    brewPrefix + "/opt/scip/lib/libscip.dylib",
                    brewPrefix + "/lib/libscip.dylib",
                    "/usr/local/lib/libscip.dylib"
            };

        } else if (osName.contains("linux")) {

            searchPaths = new String[]{
                    "/usr/lib/libscip.so",
                    "/usr/local/lib/libscip.so",
                    "/usr/lib/x86_64-linux-gnu/libscip.so",
                    "/usr/lib/aarch64-linux-gnu/libscip.so"
            };

        } else if (osName.contains("windows")) {

            searchPaths = new String[]{
                    "C:\\Program Files\\SCIPOptSuite\\bin\\libscip.dll"
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

        throw new UnsatisfiedLinkError("SCIP library not found. Install SCIP on the system (e.g. 'brew install scip' on macOS).");
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
