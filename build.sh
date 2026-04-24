#!/bin/bash

echo "Cleaning build directory..."
rm -rf build
mkdir -p build

echo "Compiling Java files..."

# 编译整个 vsue 目录（更稳）
javac -d build $(find vsue -name "*.java")

if [ $? -ne 0 ]; then
    echo "Compilation failed!"
    exit 1
fi

echo "Compilation successful ✅"