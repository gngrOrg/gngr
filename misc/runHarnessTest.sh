#!/bin/bash
set -ex

REPO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_DIR"

TMP_DIR="$REPO_DIR/tmp-test-harness/"
PROFILE_DIR="$TMP_DIR/tmp-test-harness/profile"
SCREENSHOT_PATH="$TMP_DIR/test_screenshot.png"
TEST_HTML_FILE="$TMP_DIR/test.html"
TEST_HTML="localhost:8000/test.html"

mkdir -p "$PROFILE_DIR"
rm -f "$PROFILE_DIR/port.dat" "$SCREENSHOT_PATH"

if [ ! -f "$TEST_HTML_FILE" ]; then
  cat << 'EOF' > "$TEST_HTML_FILE"
<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8">
  <title>Render Harness Test</title>
  <style>
    body {
      background-color: #f0f0f0;
      font-family: sans-serif;
      margin: 20px;
    }
    h1 {
      color: #003366;
    }
    .box {
      width: 200px;
      height: 100px;
      background-color: #4CAF50;
      color: white;
      text-align: center;
      line-height: 100px;
      font-size: 20px;
    }
  </style>
</head>
<body>
  <h1>Render Harness Test</h1>
  <div class="box">Green Box</div>
</body>
</html>
EOF
fi

GRINDER_KEY="$(python3 -c 'import secrets; print(secrets.token_hex(32))')"

echo "Building project..."
ant -f src/build.xml build

cleanup() {
  if [ -n "$HARNESS_PID" ] && kill -0 "$HARNESS_PID" 2>/dev/null; then
    echo "Stopping render harness (PID $HARNESS_PID)..."
    kill "$HARNESS_PID" 2>/dev/null || true
  fi
  if [ -n "$HTTP_PID" ] && kill -0 "$HTTP_PID" 2>/dev/null; then
    kill "$HTTP_PID" 2>/dev/null || true
  fi
  rm -rf "$TMP_DIR/" "$SCREENSHOT_PATH"
}
trap cleanup EXIT INT TERM

echo "Starting local HTTP test server on port 8000..."
python3 -m http.server 8000 --directory "$TMP_DIR" &> /dev/null &
HTTP_PID=$!
sleep 1

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
  echo "Screenshot generated ($SCREENSHOT_SIZE bytes)."
  echo "Verifying image pixel content..."
  java misc/VerifyScreenshot.java "$SCREENSHOT_PATH"
else
  echo "FAILURE: Screenshot file is missing or empty."
  exit 1
fi
