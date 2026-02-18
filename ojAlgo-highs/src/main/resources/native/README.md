# HiGHS Native Libraries

This directory contains platform-specific HiGHS shared libraries bundled with ojAlgo-highs.

## Directory Structure

```
native/
├── macos-arm64/
│   └── libhighs.dylib
├── macos-x64/
│   └── libhighs.dylib
├── linux-x64/
│   └── libhighs.so
├── linux-arm64/
│   └── libhighs.so
├── linux-ppc64le/
│   └── libhighs.so
└── windows-x64/
    └── highs.dll
```

## Updating Binaries

Run the update script to download the latest binaries from conda-forge:

```bash
./scripts/update-highs-binaries.sh [VERSION]
```

Example:
```bash
./scripts/update-highs-binaries.sh 1.13.1
```

## Obtaining HiGHS Binaries

The easiest way to obtain shared libraries for all platforms is via **conda-forge**.

### Conda-forge (recommended)

Conda-forge provides pre-built shared libraries for all platforms:

```bash
# Download packages (no conda installation required)
curl -LO "https://anaconda.org/conda-forge/highs/1.13.1/download/osx-arm64/highs-1.13.1-h3feff0a_0.conda"
curl -LO "https://anaconda.org/conda-forge/highs/1.13.1/download/osx-64/highs-1.13.1-ha6bc089_0.conda"
curl -LO "https://anaconda.org/conda-forge/highs/1.13.1/download/linux-64/highs-1.13.1-h171cf75_0.conda"
curl -LO "https://anaconda.org/conda-forge/highs/1.13.1/download/linux-aarch64/highs-1.13.1-hdc560ac_0.conda"
curl -LO "https://anaconda.org/conda-forge/highs/1.13.1/download/win-64/highs-1.13.1-h477610d_0.conda"

# Extract (each .conda is a zip containing a .tar.zst)
unzip package.conda
zstd -d pkg-*.tar.zst -o pkg.tar
tar -xf pkg.tar

# Libraries are in:
# - lib/libhighs.1.13.dylib (macOS)
# - lib/libhighs.so.1.13.1 (Linux)
# - Library/bin/highs.dll (Windows)
```

Browse available versions at: https://anaconda.org/conda-forge/highs/files

### Alternative: Build from source

On each target platform:
```bash
git clone https://github.com/ERGO-Code/HiGHS.git
cd HiGHS && mkdir build && cd build
cmake .. -DCMAKE_BUILD_TYPE=Release -DBUILD_SHARED_LIBS=ON
cmake --build . --config Release
```

Note: HiGHS official releases only include static libraries (`.a` files) for most platforms.

## Version

Current bundled version: **HiGHS 1.13.1** (from conda-forge)