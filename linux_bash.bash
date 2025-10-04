#!/bin/bash

# Define source and output directories
SOURCE_DIR="src"
OUTPUT_DIR="obj"

# Clean up previous compilation artifacts
echo "Cleaning up previous compilation artifacts in $OUTPUT_DIR/..."
rm -rf "$OUTPUT_DIR"
mkdir -p "$OUTPUT_DIR"

# Find all Java source files and compile them
echo "Compiling Java source files from $SOURCE_DIR/..."
find "$SOURCE_DIR" -name "*.java" -print0 | xargs -0 javac -source 1.8 -target 1.8 -sourcepath "$SOURCE_DIR" -d "$OUTPUT_DIR"

# Check if compilation was successful
if [ $? -eq 0 ]; then
    echo "Compilation successful! .class files are in $OUTPUT_DIR/."
else
    echo "Compilation failed. Please check the error messages above."
    exit 1
fi
