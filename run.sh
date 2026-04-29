#!/bin/bash
# Run script for EditorNiko on Unix-like systems (macOS, Linux)

echo "Building EditorNiko..."
mvn clean package

if [ $? -eq 0 ]; then
    echo ""
    echo "Build successful! Running application..."
    echo ""
    java --enable-native-access=ALL-UNNAMED -jar target/EditorNiko-executable.jar
else
    echo "Build failed!"
    exit 1
fi
