#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
AGENT_SRC_DIR="$ROOT_DIR/agent/src"
AGENT_OUT_DIR="$ROOT_DIR/out"
AGENT_CLASSES_DIR="$AGENT_OUT_DIR/agent-classes"
AGENT_JAR="$AGENT_OUT_DIR/used-classes-agent.jar"

mkdir -p "$AGENT_CLASSES_DIR" "$ROOT_DIR/agent/META-INF" "$AGENT_OUT_DIR"

echo "Compiling agent (target: Java 6)..."
javac -source 1.6 -target 1.6 -Xlint:deprecation -Xlint:unchecked \
  -d "$AGENT_CLASSES_DIR" \
  "$AGENT_SRC_DIR/UsedClassesAgent.java"

echo "Packaging agent jar..."
jar cfm "$AGENT_JAR" "$ROOT_DIR/agent/META-INF/MANIFEST.MF" -C "$AGENT_CLASSES_DIR" .

echo "Built: $AGENT_JAR"
