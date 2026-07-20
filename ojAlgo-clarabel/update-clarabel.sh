#!/usr/bin/env bash
set -euo pipefail

REPO_DIR="$(cd "$(dirname "$0")/.." && pwd)/Clarabel.cpp"
INSTALL_DIR="/usr/local/lib"

if [ ! -d "${REPO_DIR}" ]; then
    echo "Cloning Clarabel.cpp..."
    git clone --recurse-submodules https://github.com/oxfordcontrol/Clarabel.cpp.git "${REPO_DIR}"
else
    echo "Updating Clarabel.cpp..."
    cd "${REPO_DIR}"
    git pull
    git submodule update --init --recursive
fi

echo "Building libclarabel_c..."
cd "${REPO_DIR}/rust_wrapper"
cargo build --release

echo "Installing to ${INSTALL_DIR}..."
sudo mkdir -p "${INSTALL_DIR}"
sudo cp "${REPO_DIR}/rust_wrapper/target/release/libclarabel_c.dylib" "${INSTALL_DIR}/"

echo "Done. Installed version:"
ls -l "${INSTALL_DIR}/libclarabel_c.dylib"
