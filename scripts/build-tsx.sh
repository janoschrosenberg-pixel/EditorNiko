#!/bin/bash
set -e

# === Konfiguration ===
PROJECT_ROOT="$(cd "$(dirname "$0")/.." ; pwd)"
TSX_SRC="$PROJECT_ROOT/tree-sitter-typescript/tsx/src"
TREE_SITTER_CORE="$PROJECT_ROOT/tree-sitter/lib/include"
OUTPUT_DIR="$PROJECT_ROOT/resources/native"

OS_NAME=$(uname -s)

echo "Building TSX parser for $OS_NAME..."

case "$OS_NAME" in
    Darwin)
        OUT_FILE="$OUTPUT_DIR/macos/tree-sitter-tsx.dylib"
        mkdir -p "$OUTPUT_DIR/macos"
        gcc -shared -fPIC -undefined dynamic_lookup \
            -I "$TREE_SITTER_CORE" -I "$TSX_SRC" \
            -o "$OUT_FILE" \
            "$TSX_SRC/parser.c" "$TSX_SRC/scanner.c"
        ;;
    Linux)
        OUT_FILE="$OUTPUT_DIR/linux/tree-sitter-tsx.so"
        mkdir -p "$OUTPUT_DIR/linux"
        gcc -shared -fPIC \
            -I "$TREE_SITTER_CORE" -I "$TSX_SRC" \
            -o "$OUT_FILE" \
            "$TSX_SRC/parser.c" "$TSX_SRC/scanner.c"
        ;;
    MINGW*|MSYS*|CYGWIN*|Windows_NT)
        OUT_FILE="$OUTPUT_DIR/windows/tree-sitter-tsx.dll"
        mkdir -p "$OUTPUT_DIR/windows"
        cl /LD /I "$TREE_SITTER_CORE" /I "$TSX_SRC" "$TSX_SRC\parser.c" "$TSX_SRC\scanner.c" /Fe:"$OUT_FILE"
        ;;
    *)
        echo "Unsupported OS: $OS_NAME"
        exit 1
        ;;
esac

echo "TSX parser built: $OUT_FILE"
