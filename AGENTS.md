# One-off GUI tests

## Running gngr without a physical display

Run GUI tests under Xvfb. Do not set `java.awt.headless=true`: gngr uses Swing
and needs a display server even when no monitor is attached.

```sh
GRINDER_KEY="$(python3 -c 'import secrets; print(secrets.token_hex(32))')"
printf 'Use this key in the test-client terminal: %s\n' "$GRINDER_KEY"
xvfb-run -a -s "-dpi 96 -screen 0 900x900x24" \
  ant -f src/build.xml -Dgngr.grinder.key="$GRINDER_KEY" run
```

Keep gngr running while the test client connects. The client does not need its
own X display unless it is capturing the virtual desktop. Stop the browser
after the test with Grinder's `CLOSE` or `QUIT` command, or stop the process
group started for the test. Avoid sharing a gngr process or profile between
parallel tests.

## Controlling and inspecting the browser

Passing `gngr.grinder.key` enables the Grinder test interface. Use the
standalone [`misc/GrinderTestClient.java`](misc/GrinderTestClient.java) as a
starting point for one-off browser tests; it uses only the Java standard
library and speaks the local Grinder protocol. See
[`misc/runTest.sh`](misc/runTest.sh) for the project's rendering-test setup.
The interface supports:

- `TO <url>` to navigate and wait for navigation/layout completion.
- `SET_SIZE <width> <height>` to set the browser viewport.
- `SCREENSHOT` to return a PNG of the page's content component after it is
  ready to paint.
- `CLOSE` to close the test window, or `QUIT` to shut down gngr.

It does not provide commands for querying DOM elements or retrieving logs.
Collect process output separately where useful, but note that gngr redirects
standard output/error to its in-app console during startup. The Grinder
screenshot is page content, not a capture of browser chrome, dialogs, or the
whole desktop. For those, use an X11 screenshot tool against the same Xvfb
`DISPLAY` (for example `xwd` or ImageMagick `import`).
gngr logs Grinder startup, authentication decisions, server ports, and
commands at INFO level; capture the launch process's console output when
diagnosing a failed connection. Authentication keys are not logged.

Compile and run the sample client with the same profile used by gngr. Its
`port.dat` is in `~/.gngr/default` by default, and the key must match the one
passed to Ant. Keep gngr running in the first terminal; set `GRINDER_KEY` to
the printed value in the test-client terminal:

```sh
export GRINDER_KEY="<same key passed to Ant>"
javac -d /tmp/gngr-grinder-client misc/GrinderTestClient.java
GRINDER_KEY="$GRINDER_KEY" java -cp /tmp/gngr-grinder-client GrinderTestClient \
  "$HOME/.gngr/default" http://example.test/ /tmp/gngr-page.png 900 900
```

The client sets the viewport when dimensions are supplied, navigates, saves a
page-content PNG, then sends `QUIT` to stop its one-off gngr session. Adapt it
when tests need different cleanup or assertions.

## Repeatability and isolation

Use the same Xvfb screen size, DPI, fonts, and gngr build when comparing
screenshots. The rendering harness in `misc/runTest.sh` is a reference for
font setup and the existing Grinder workflow.

Keep the test environment isolated: use a fresh, unpredictable Grinder key and
a disposable browser profile. Grinder uses a dynamic test-control port; block
external access and do not expose it to untrusted networks. The interface is
a test-control mechanism, not a general-purpose or production automation API.
