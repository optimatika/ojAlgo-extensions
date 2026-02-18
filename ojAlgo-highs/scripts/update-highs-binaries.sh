#!/bin/bash
#
# Downloads HiGHS native libraries from conda-forge for all supported platforms.
#
# Usage:
#   ./scripts/update-highs-binaries.sh [VERSION]
#
# Example:
#   ./scripts/update-highs-binaries.sh 1.13.1
#
# If VERSION is not specified, defaults to 1.13.1
#
# Note: Package names may need to be updated when changing versions.
# Browse available packages at: https://anaconda.org/conda-forge/highs/files
#

set -e

VERSION="${1:-1.13.1}"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
NATIVE_DIR="$PROJECT_DIR/src/main/resources/native"
TEMP_DIR=$(mktemp -d)

echo "=== Updating HiGHS binaries to version $VERSION ==="
echo "Project directory: $PROJECT_DIR"
echo "Temp directory: $TEMP_DIR"
echo ""

# Check for required tools
for tool in curl unzip zstd tar; do
    if ! command -v $tool &> /dev/null; then
        echo "ERROR: Required tool '$tool' not found. Please install it."
        exit 1
    fi
done

# Function to download and extract library for a platform
process_platform() {
    local conda_platform=$1
    local our_platform=$2
    local lib_dir=$3
    local lib_pattern=$4
    local lib_name=$5
    local package_name=$6
    
    echo "Processing $conda_platform -> $our_platform"
    
    # Create target directory
    mkdir -p "$NATIVE_DIR/$our_platform"
    
    local pkg_dir="$TEMP_DIR/$conda_platform"
    mkdir -p "$pkg_dir"
    
    local full_url="https://anaconda.org/conda-forge/highs/${VERSION}/download/${conda_platform}/${package_name}"
    local pkg_file="$pkg_dir/package.conda"
    
    echo "  Downloading $package_name..."
    curl -sL -o "$pkg_file" "$full_url"
    
    if [ ! -f "$pkg_file" ] || [ ! -s "$pkg_file" ]; then
        echo "  WARNING: Failed to download package for $conda_platform"
        return 0
    fi
    
    # Verify it's a zip file
    if ! file "$pkg_file" | grep -q "Zip archive"; then
        echo "  WARNING: Downloaded file is not a valid conda package"
        echo "  URL: $full_url"
        head -c 200 "$pkg_file"
        echo ""
        return 0
    fi
    
    # Extract .conda (it's a zip)
    echo "  Extracting..."
    cd "$pkg_dir"
    unzip -q -o "$pkg_file"
    
    # Find and decompress the pkg tar.zst
    local pkg_tar_zst=$(ls pkg-*.tar.zst 2>/dev/null | head -1)
    if [ -z "$pkg_tar_zst" ]; then
        echo "  WARNING: No pkg-*.tar.zst found in package"
        return 0
    fi
    
    zstd -d -q "$pkg_tar_zst" -o pkg.tar
    tar -xf pkg.tar
    
    # Find and copy the library
    echo "  Copying library..."
    local lib_file=$(find . -path "./${lib_dir}/${lib_pattern}" -type f 2>/dev/null | head -1)
    
    if [ -z "$lib_file" ]; then
        echo "  WARNING: Library not found: ${lib_dir}/${lib_pattern}"
        ls -la "${lib_dir}/" 2>/dev/null || true
        return 0
    fi
    
    cp "$lib_file" "$NATIVE_DIR/$our_platform/$lib_name"
    
    local size=$(du -h "$NATIVE_DIR/$our_platform/$lib_name" | cut -f1)
    echo "  Done: $lib_name ($size)"
}

# Process all platforms
# Format: conda_platform our_platform lib_dir lib_pattern lib_name package_name
# 
# Package names for version 1.13.1 (update these when changing VERSION):
# Browse https://anaconda.org/conda-forge/highs/files?version=VERSION to find package names

process_platform "osx-arm64" "macos-arm64" "lib" "libhighs.*.dylib" "libhighs.dylib" "highs-${VERSION}-h3feff0a_0.conda"
echo ""

process_platform "osx-64" "macos-x64" "lib" "libhighs.*.dylib" "libhighs.dylib" "highs-${VERSION}-ha6bc089_0.conda"
echo ""

process_platform "linux-64" "linux-x64" "lib" "libhighs.so.*" "libhighs.so" "highs-${VERSION}-h171cf75_0.conda"
echo ""

process_platform "linux-aarch64" "linux-arm64" "lib" "libhighs.so.*" "libhighs.so" "highs-${VERSION}-hdc560ac_0.conda"
echo ""

process_platform "linux-ppc64le" "linux-ppc64le" "lib" "libhighs.so.*" "libhighs.so" "highs-${VERSION}-h7cb76f5_0.conda"
echo ""

process_platform "win-64" "windows-x64" "Library/bin" "highs.dll" "highs.dll" "highs-${VERSION}-h477610d_0.conda"
echo ""

# Cleanup
rm -rf "$TEMP_DIR"

echo "=== Summary ==="
echo ""
for dir in "$NATIVE_DIR"/*/; do
    if [ -d "$dir" ]; then
        name=$(basename "$dir")
        lib=$(ls "$dir" 2>/dev/null | head -1)
        if [ -n "$lib" ]; then
            size=$(du -h "$dir$lib" | cut -f1)
            echo "  $name: $lib ($size)"
        else
            echo "  $name: (empty)"
        fi
    fi
done
echo ""
echo "Total size:"
du -sh "$NATIVE_DIR"
echo ""
echo "Done! HiGHS $VERSION binaries updated."
