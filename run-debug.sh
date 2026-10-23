#!/usr/bin/env bash
set -e
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

if [ -d "$SCRIPT_DIR/build/compose/binaries/main/app/pixelGram/lib/app" ]; then
    PGDIR="$SCRIPT_DIR"
elif [ -d "$SCRIPT_DIR/pixelGram/build/compose/binaries/main/app/pixelGram/lib/app" ]; then
    PGDIR="$SCRIPT_DIR/pixelGram"
else
    PGDIR="$HOME/Documents/AI/pgdev/pixelGram"
fi

APPDIR="$PGDIR/build/compose/binaries/main/app/pixelGram/lib/app"
RUNTIME="$PGDIR/build/compose/binaries/main/app/pixelGram/lib/runtime/bin/java"

if [ -x "$RUNTIME" ]; then
    JAVA_BIN="$RUNTIME"
elif [ -x "/usr/lib/jvm/java-21-openjdk/bin/java" ]; then
    JAVA_BIN="/usr/lib/jvm/java-21-openjdk/bin/java"
else
    JAVA_BIN="$(which java)"
fi

echo "Using Java: $JAVA_BIN"
echo "Using AppDir: $APPDIR"

exec "$JAVA_BIN" \
  -Dcompose.application.configure.swing.globals=true \
  -Dcompose.application.resources.dir="$APPDIR/resources" \
  -Dskiko.library.path="$APPDIR" \
  -Xmx1g \
  -XX:MinHeapFreeRatio=10 \
  -XX:MaxHeapFreeRatio=30 \
  -XX:G1PeriodicGCInterval=30000 \
  -cp "$APPDIR/*" \
  com.pixelgram.desktop.MainKt "$@"
