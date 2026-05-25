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
package org.ojalgo.optimisation.solver.highs;

import java.io.IOException;
import java.io.InputStream;
import java.lang.foreign.Arena;
import java.lang.foreign.SymbolLookup;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Handles loading of the HiGHS native library from bundled resources.
 * <p>
 * The native library is extracted from the JAR to a temporary directory and loaded via
 * {@link SymbolLookup#libraryLookup(Path, Arena)}. Platform detection is automatic based on system
 * properties.
 */
final class NativeLoader {

    private static volatile SymbolLookup LOADED_LOOKUP;

    private static final String RESOURCE_PREFIX = "/native/";

    /**
     * Detect the current platform identifier.
     *
     * @return platform string like "macos-arm64", "linux-x64", etc.
     */
    private static String detectPlatform() {

        String osName = System.getProperty("os.name").toLowerCase();
        String osArch = System.getProperty("os.arch").toLowerCase();

        String os;
        if (osName.contains("mac") || osName.contains("darwin")) {
            os = "macos";
        } else if (osName.contains("linux")) {
            os = "linux";
        } else if (osName.contains("windows")) {
            os = "windows";
        } else {
            throw new UnsatisfiedLinkError("Unsupported operating system: " + osName);
        }

        String arch;
        if ("aarch64".equals(osArch) || "arm64".equals(osArch)) {
            arch = "arm64";
        } else if ("amd64".equals(osArch) || "x86_64".equals(osArch)) {
            arch = "x64";
        } else if ("ppc64le".equals(osArch)) {
            arch = "ppc64le";
        } else {
            throw new UnsatisfiedLinkError("Unsupported architecture: " + osArch);
        }

        return os + "-" + arch;
    }

    /**
     * Extract the native library from JAR resources to a temporary file.
     */
    private static Path extractToTemp(final String resourcePath, final String libraryName) {

        try (InputStream in = NativeLoader.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new UnsatisfiedLinkError("Native library not found in JAR: " + resourcePath + ". Ensure HiGHS binaries are bundled for platform: "
                        + NativeLoader.detectPlatform());
            }

            Path tempDir = Files.createTempDirectory("highs-native");
            Path tempFile = tempDir.resolve(libraryName);

            Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);

            tempFile.toFile().deleteOnExit();
            tempDir.toFile().deleteOnExit();

            return tempFile;
        } catch (IOException e) {
            throw new UnsatisfiedLinkError("Failed to extract native library: " + e.getMessage());
        }
    }

    /**
     * Get the platform-specific library filename.
     */
    private static String getLibraryName() {

        String osName = System.getProperty("os.name").toLowerCase();

        if (osName.contains("mac") || osName.contains("darwin")) {
            return "libhighs.dylib";
        } else if (osName.contains("linux")) {
            return "libhighs.so";
        } else if (osName.contains("windows")) {
            return "highs.dll";
        } else {
            throw new UnsatisfiedLinkError("Unsupported operating system: " + osName);
        }
    }

    /**
     * Load the HiGHS native library and return a {@link SymbolLookup} for resolving symbols.
     * <p>
     * The library is loaded once and cached. Subsequent calls return the same lookup instance.
     *
     * @return symbol lookup for the loaded HiGHS library
     * @throws UnsatisfiedLinkError if the library cannot be loaded
     */
    static synchronized SymbolLookup load() {

        if (LOADED_LOOKUP != null) {
            return LOADED_LOOKUP;
        }

        String platform = NativeLoader.detectPlatform();
        String libraryName = NativeLoader.getLibraryName();
        String resourcePath = RESOURCE_PREFIX + platform + "/" + libraryName;

        Path tempFile = NativeLoader.extractToTemp(resourcePath, libraryName);

        LOADED_LOOKUP = SymbolLookup.libraryLookup(tempFile, Arena.ofAuto());

        return LOADED_LOOKUP;
    }

    private NativeLoader() {}

}
