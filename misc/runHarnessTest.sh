#!/bin/bash
set -e

REPO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_DIR"

PROFILE_DIR="$REPO_DIR/.test-harness/profile"
SCREENSHOT_PATH="$REPO_DIR/.test-harness/screenshot.png"
TEST_HTML="file://$REPO_DIR/misc/test.html"

mkdir -p "$PROFILE_DIR"
rm -f "$PROFILE_DIR/port.dat" "$SCREENSHOT_PATH"

GRINDER_KEY="$(python3 -c 'import secrets; print(secrets.token_hex(32))')"

echo "Building project..."
ant -f src/build.xml build

cleanup() {
  if [ -n "$HARNESS_PID" ] && kill -0 "$HARNESS_PID" 2>/dev/null; then
    echo "Stopping render harness (PID $HARNESS_PID)..."
    kill "$HARNESS_PID" 2>/dev/null || true
  fi
  rm -rf "$REPO_DIR/.test-harness"
}
trap cleanup EXIT INT TERM

echo "Starting Render Harness under Xvfb..."
xvfb-run -a -s "-dpi 96 -screen 0 900x900x24" \
  ant -f src/build.xml -Dgngr.grinder.key="$GRINDER_KEY" -Dgngr.profile="$PROFILE_DIR" run-harness &
HARNESS_PID=$!

echo "Waiting for Render Harness to start and write port.dat..."
TRIES=0
MAX_TRIES=60
while [ ! -f "$PROFILE_DIR/port.dat" ]; do
  if ! kill -0 "$HARNESS_PID" 2>/dev/null; then
    echo "Error: Render harness process exited prematurely."
    exit 1
  fi
  sleep 0.5
  TRIES=$((TRIES + 1))
  if [ "$TRIES" -ge "$MAX_TRIES" ]; then
    echo "Error: Timed out waiting for port.dat in $PROFILE_DIR"
    exit 1
  fi
done

echo "port.dat found. Connecting with GrinderTestClient..."
GRINDER_KEY="$GRINDER_KEY" java misc/GrinderTestClient.java \
  "$PROFILE_DIR" "$TEST_HTML" "$SCREENSHOT_PATH" 800 600

echo "Waiting for Render Harness to exit on QUIT..."
wait "$HARNESS_PID" 2>/dev/null || true
unset HARNESS_PID

if [ -s "$SCREENSHOT_PATH" ]; then
  SCREENSHOT_SIZE="$(wc -c < "$SCREENSHOT_PATH")"
  echo "SUCCESS: Screenshot generated successfully ($SCREENSHOT_SIZE bytes)!"
else
  echo "FAILURE: Screenshot file is missing or empty."
  exit 1
fi
