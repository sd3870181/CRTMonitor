#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
DEMO_SRC_DIR="$ROOT_DIR/demo/src"
OUT_DIR="$ROOT_DIR/out"
DEMO_CLASSES_DIR="$OUT_DIR/demo-classes"
DEMO_JAR="$OUT_DIR/demo-app.jar"

mkdir -p "$DEMO_CLASSES_DIR" "$OUT_DIR"

echo "Compiling demo (target: Java 6)..."
javac -source 1.6 -target 1.6 -Xlint:deprecation -Xlint:unchecked \
  -d "$DEMO_CLASSES_DIR" \
  "$DEMO_SRC_DIR/DemoApp.java"

echo "Packaging demo jar..."
jar cfe "$DEMO_JAR" DemoApp -C "$DEMO_CLASSES_DIR" .

echo "Built: $DEMO_JAR"
