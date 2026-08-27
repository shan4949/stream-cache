#!/usr/bin/env bash
set -e

mkdir -p out

echo "Compiling..."
find src -name "*.java" | xargs javac -d out

echo ""
echo "Running tests..."
java -ea -cp out test.StreamCacheTest

echo ""
java -cp out benchmark.Benchmark
