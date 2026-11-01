#!/usr/bin/env bash
# build.sh — build the standalone Pinterest MIDlet (CLDC 1.1 / MIDP 2.0).
#
# Output:
#   dist/Pinterest.jar  — install this on the phone
#   dist/Pinterest.jad  — descriptor, keep it next to the jar
#
# Pipeline:
#   1. Compile  — JDK 8 javac, -source/-target 1.3, CLDC+MIDP+JSR-75 bootclasspath
#   2. Package  — compiled classes + res/ + manifest
#   3. ProGuard — -microedition preverification (and shrinking/obfuscation per midlets.pro)
#   4. JAD      — descriptor with the final jar size
#
# JDK 8 is required because CLDC 1.1 has StringBuffer but no StringBuilder:
# only -source 1.3/1.4 lowers string concatenation to StringBuffer. Newer
# javac versions emit StringBuilder and the MIDlet would crash on a real device.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

BUILD_DIR="build/standalone"
COMPILED_DIR="$BUILD_DIR/compiled"
DIST_DIR="dist"
VERSION="${VERSION:-1.0.0}"

RUN_JAVA="${RUN_JAVA:-$(command -v java)}"
PROGUARD_JAR="lib/proguard-ant.jar"
BOOTCP="lib/cldc_1.1.jar:lib/midp_2.0.jar:lib/jsr75_1.0.jar"

find_jdk8() {
  if [ -n "${JAVA8_HOME:-}" ] && [ -x "$JAVA8_HOME/bin/javac" ]; then
    echo "$JAVA8_HOME"
    return 0
  fi
  local p
  p="$(/usr/libexec/java_home -v 1.8 2>/dev/null || true)"
  if [ -n "$p" ] && [ -x "$p/bin/javac" ] && "$p/bin/javac" -version 2>&1 | grep -q '1\.8'; then
    echo "$p"
    return 0
  fi
  local cand
  for cand in \
      /usr/lib/jvm/temurin-8* \
      /usr/lib/jvm/java-8* \
      /Library/Java/JavaVirtualMachines/*/Contents/Home \
      /opt/homebrew/opt/openjdk@8/libexec/openjdk.jdk/Contents/Home \
      /opt/homebrew/Cellar/temurin@8/*/libexec/openjdk.jdk/Contents/Home \
      "$HOME/.sdkman/candidates/java/"*; do
    if [ -x "$cand/bin/javac" ] && "$cand/bin/javac" -version 2>&1 | grep -q '1\.8'; then
      echo "$cand"
      return 0
    fi
  done
  return 1
}

JDK8="$(find_jdk8 || true)"
if [ -z "$JDK8" ]; then
  cat >&2 <<'EOF'
✗ JDK 8 not found. It is required for a CLDC-compatible build.

Install it, then re-run. Examples:
    sudo apt install openjdk-8-jdk
    brew install --cask temurin@8
    sdk install java 8.0.392-zulu

Or point at it explicitly:
    export JAVA8_HOME=/path/to/jdk8
EOF
  exit 1
fi
JAVAC="$JDK8/bin/javac"
JAR_TOOL="$JDK8/bin/jar"
echo "• JDK 8 : $JDK8 ($("$JAVAC" -version 2>&1))"

for f in "$PROGUARD_JAR" lib/cldc_1.1.jar lib/midp_2.0.jar lib/jsr75_1.0.jar; do
  if [ ! -f "$f" ]; then
    echo "✗ Missing $f" >&2
    exit 1
  fi
done

manifest_body() {
  cat <<EOF
MIDlet-Name: Pinterest
MIDlet-Vendor: Mr Lima
MIDlet-Version: $VERSION
MIDlet-1: Pinterest, /pinterest.png, pinterest.PinterestMIDlet
MicroEdition-Profile: MIDP-2.0
MicroEdition-Configuration: CLDC-1.1
MIDlet-Permissions: javax.microedition.io.Connector.http, javax.microedition.io.Connector.file.read, javax.microedition.io.Connector.file.write, javax.microedition.io.Connector.file.delete
MIDlet-Permissions-Opt: javax.microedition.io.Connector.https
EOF
}

echo "• Clean"
rm -rf "$BUILD_DIR"
mkdir -p "$COMPILED_DIR" "$DIST_DIR"

echo "• Generate Lang.java"
python3 tools/build_lang.py

echo "• Collect sources"
find src -name '*.java' > "$BUILD_DIR/sources.txt"
echo "    $(wc -l < "$BUILD_DIR/sources.txt" | tr -d ' ') Java files"

echo "• Compile (JDK 8, -source/-target 1.3, CLDC bootclasspath)"
"$JAVAC" -encoding UTF-8 -source 1.3 -target 1.3 -g:none \
  -bootclasspath "$BOOTCP" \
  -d "$COMPILED_DIR" @"$BUILD_DIR/sources.txt"

echo "• Package $BUILD_DIR/Pinterest_midlet.jar"
manifest_body > "$BUILD_DIR/MANIFEST.MF"
"$JAR_TOOL" cfm "$BUILD_DIR/Pinterest_midlet.jar" "$BUILD_DIR/MANIFEST.MF" \
  -C "$COMPILED_DIR" . -C res .

echo "• ProGuard (shrink + -microedition preverify) → dist/Pinterest.jar"
"$RUN_JAVA" -cp "$PROGUARD_JAR" proguard.ProGuard @midlets.pro

if [ ! -f "$DIST_DIR/Pinterest.jar" ]; then
  echo "✗ ProGuard did not produce dist/Pinterest.jar" >&2
  exit 1
fi

echo "• Write dist/Pinterest.jad"
JAR_SIZE="$(wc -c < "$DIST_DIR/Pinterest.jar" | tr -d ' ')"
{
  manifest_body
  echo "MIDlet-Jar-URL: Pinterest.jar"
  echo "MIDlet-Jar-Size: $JAR_SIZE"
} > "$DIST_DIR/Pinterest.jad"

if command -v sha256sum >/dev/null 2>&1; then
  (cd "$DIST_DIR" && sha256sum Pinterest.jar Pinterest.jad > SHA256SUMS)
elif command -v shasum >/dev/null 2>&1; then
  (cd "$DIST_DIR" && shasum -a 256 Pinterest.jar Pinterest.jad > SHA256SUMS)
fi

echo
echo "✓ Done."
echo "    dist/Pinterest.jar  ($(du -h "$DIST_DIR/Pinterest.jar" | cut -f1), $JAR_SIZE bytes)"
echo "    dist/Pinterest.jad"
if [ -f "$DIST_DIR/SHA256SUMS" ]; then
  echo "    dist/SHA256SUMS"
fi
echo
echo "  Copy both files to the phone and install the .jad (or the .jar)."
