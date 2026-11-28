# Render_Harness Design Overview

Scope: src/Render_Harness/ (5 Java files, ~1.9k LOC; standalone offscreen Grinder rendering harness that drives HTML_Renderer directly without browser chrome, Platform_Core's RequestEngine/cache/cookies, RequestManager firewall, SecurityManager, or JavaScript).
Prefix shorthand: o.l. = org.lobobrowser., o.l.h. = org.lobobrowser.harness..
Entrypoint & launch: o.l.h.RenderHarnessMain, invoked via ant -f src/build.xml run-harness (or ant -f src/Render_Harness/build.xml run); default target of misc/runTest.sh and misc/runHarnessTest.sh.
Dependencies:
- Direct imports: HTML_Renderer (HtmlPanel, HtmlBlockPanel, HtmlRendererContext, BrowserFrame, FormInput, HtmlObject, HTMLDocumentImpl, DocumentBuilderImpl, InputSourceImpl), Platform_Public_API (UserAgentContext, NetworkRequest, NetworkRequestEvent, NetworkRequestListener, ImageResponse), Common (org.lobobrowser.protocol.data.Handler), JDK 11 (java.awt.*, javax.swing.*, javax.imageio.ImageIO, java.net.*, org.w3c.dom.*), org.eclipse.jdt.annotation (@NonNull).
- Transitive build/runtime classpath (build.xml): Common, Platform_Core + Platform_Core/lib/*, and all 8 JARs in XAMJ_Build/ (needed by HTML_Renderer at runtime; Render_Harness itself imports nothing from Platform_Core or Primary_Extension).

## 1. File & Resource Map

| File (src/Render_Harness/) | LOC | Role |
|---|---:|---|
| org/lobobrowser/harness/RenderHarnessMain.java | 290 | CLI/sysprop entrypoint, `data:` URL handler registration (`registerProtocolHandlers`), port.dat creation & shutdown cleanup, 127.0.0.1 Grinder auth/discovery server, offscreen HtmlPanel factory (createOffscreenHtmlPanel) |
| org/lobobrowser/harness/HarnessServer.java | 652 | Grinder TCP command server (TO, SET_SIZE, SCREENSHOT, CLOSE, QUIT), URL/file path resolver (resolveURL), load/layout/paint synchronization, PNG capture |
| org/lobobrowser/harness/HarnessHtmlRendererContext.java | 487 | Minimal HtmlRendererContext: tracks currentURL, supports resizeTo/resizeBy, jobsFinished callback, getFrames() delegation, and offscreen BrowserFrame (<iframe>/<frame>) loading with per-frame child contexts |
| org/lobobrowser/harness/HarnessNetworkRequest.java | 333 | Synchronous NetworkRequest over java.net.URLConnection; tracks static ACTIVE_REQUESTS counter, pre-decodes images via ImageIO.read(); no caching, cookies, SRI, or security checks |
| org/lobobrowser/harness/HarnessUserAgentContext.java | 129 | Permissive UserAgentContext: isRequestPermitted() always true, external/internal CSS enabled, cookies & scripting disabled (false), isMedia() matches screen/all |
| build.xml, .classpath, .project | — | Ant build/run config (run forwards gngr.grinder.key and gngr.profile to RenderHarnessMain) and Eclipse metadata |

## 2. Discovery, Command & Rendering Pipeline

```
RenderHarnessMain.main(args)
  ├─ Calls registerProtocolHandlers() (registers Common's data: URLStreamHandlerFactory)
  ├─ Reads -grinder-key= / -profile= / -width= / -height= (fallback sysprops gngr.grinder.key, gngr.profile;
  │  defaults: 900×900, ~/.gngr/default, random 64-hex key if unset)
  ├─ Binds reuse ServerSocket on 127.0.0.1:0 ─► writes 4-byte int port to <profile>/port.dat
  ├─ EDT: creates single offscreen HtmlPanel (createOffscreenHtmlPanel) + HarnessUserAgentContext + HarnessHtmlRendererContext
  └─ Accept loop (15s socket timeout):
       receives "GRINDER <passkey>"
       verifies Base64(SHA-256(grinderKey + reusePort)) == passkey
         ├─ Fail ─► writes int -1
         └─ Pass ─► spawns HarnessServer(panel, ucontext, rcontext) on 127.0.0.1 ephemeral port
                    writes 4-byte int gsPort ─► reads client ACK line

HarnessServer (daemon thread "HarnessGrinderServer", 120s socket timeout, 1 command per TCP connection):
  ├─ TO <path|url>
  │    1. resolveURL(path) ─► URLConnection (10s connect, 15s read) ─► MIME/charset fallback
  │    2. DocumentBuilderImpl.createDocument(is, mimeType) ─► doc.load(false)
  │    3. EDT: panel.setSize/setBounds + panel.setDocument(doc, rcontext)
  │    4. doc.finishModifications()
  │    5. Poll (≤10s, 10ms) until !doc.layoutBlocked && activeRequests == 0 (timeout → status 1)
  │    6. EDT: recursive layoutFrameTree(panel) validate() & doLayout()
  │    7. panel.layoutCompletion().get(10s) (timeout/failure → status 1)
  │    8. Poll (≤5s, 20ms) until recursive isPanelTreeReadyToPaint() && activeRequests == 0 (timeout/failure → status 1)
  │    9. Write int 0 (or 1 on timeout/error) ─► read ACK line
  ├─ SET_SIZE <w> <h>   ─► update currentWidth/Height ─► EDT resize + layoutFrameTree ─► write 0 + ACK
  ├─ SCREENSHOT         ─► poll (≤5s, 20ms) recursive ready & activeRequests==0 (timeout/failure → write int -1, skip paint)
  │                       ─► EDT disableRenderHintsTree + layoutFrameTree + panel.paint(BufferedImage TYPE_INT_ARGB)
  │                       ─► (paint error → write int -1) ─► ImageIO PNG ─► write int len + bytes ─► read ACK line
  ├─ CLOSE              ─► reload blank doc, set done=true, close command socket
  └─ QUIT               ─► close command socket ─► RenderHarnessMain.shutdown() (delete port.dat, System.exit(0))
```

## 3. Subsystem Details

### 3.1 Offscreen Swing Layout & Subframes (RenderHarnessMain.createOffscreenHtmlPanel, HarnessHtmlRendererContext)
- Because the harness never attaches HtmlPanel to a realized JFrame, Component.isDisplayable() is false and Swing's standard revalidate() is a no-op.
- createOffscreenHtmlPanel() subclasses HtmlPanel.createHtmlBlockPanel() to return a custom HtmlBlockPanel whose validateAll() and revalidatePanel() explicitly invoke parent.doLayout() and this.doLayout() when !isDisplayable().
- HarnessHtmlRendererContext.createBrowserFrame() creates a HarnessBrowserFrame backed by createOffscreenHtmlPanel() and a child HarnessHtmlRendererContext (with parent/getTop() links and navigate() delegating self-targeted navigations to ownerFrame.loadURL()), parses with HarnessServer.resolveCharset()/resolveMimeType(), synchronizes setDocument() to the EDT, calls doc.finishModifications(), and delegates getFrames() to the current HTMLDocumentImpl.getFrames().
- Still requires a non-headless AWT graphics environment (e.g., Xvfb at 96 DPI in misc/runTest.sh and misc/runHarnessTest.sh) for Swing/font metrics.

### 3.2 URL, MIME & Charset Resolution (HarnessServer)
- resolveURL(path) checks in order:
  1. Explicit scheme (contains("://") or prefix file:, jar:, data:) → new URL(path) (ensuring the data: handler is registered via RenderHarnessMain.registerProtocolHandlers())
  2. Absolute path (/... or Windows X:\... / X:/...) → File.toURI().toURL()
  3. Relative prefix (./ or ../) → File.toURI().toURL()
  4. Existing local file/directory (new File(path).exists()) → File.toURI().toURL()
  5. Fallback → new URL("http://" + path)
- MIME fallback when Content-Type is null, content/unknown, or application/octet-stream: .xht/.xhtml/.xml → application/xhtml+xml, otherwise text/html. Default charset is UTF-8.

### 3.3 Synchronous Subresource Fetching (HarnessNetworkRequest & HarnessUserAgentContext)
- HarnessUserAgentContext allows all requests (isRequestPermitted() → true), enables internal/external CSS, sets isMedia() to screen/all, and disables cookies and scripting (isCookieEnabled()/isScriptingEnabled() → false, getSecurityPolicy() → null).
- HarnessNetworkRequest.send() executes **synchronously** on the caller thread (ignoring asyncFlag) using java.net.URLConnection (10s connect, 15s read, redirects enabled on HttpURLConnection) so subresources (<img>, CSS background images, stylesheets) finish before layout/paint.
- Wraps execution with static ACTIVE_REQUESTS (AtomicInteger) so HarnessServer can wait for all subresource fetches to drain. Eagerly runs ImageIO.read() on every completed response body to populate ImageResponse (loaded or error), firing STATE_LOADING → STATE_LOADED → STATE_COMPLETE (or STATE_ABORTED with status 500 on exception).

### 3.4 Differences vs. Full Browser Grinder (Platform_Core's GrinderServer)
- Speaks the same wire protocol (port.dat, GRINDER <passkey>, TO/SET_SIZE/SCREENSHOT/CLOSE/QUIT) so misc/GrinderTestClient.java and Grinder work unchanged against both targets.
- Faster and isolated for CSS/layout regression testing: skips PlatformInit, ExtensionManager, OkHttp, H2/jOOQ DB, RequestManager firewall, LocalSecurityPolicy, and browser chrome.

## 4. Threading, Invariants & Known Quirks

- **Threads:** main runs the reuse accept loop; each accepted GRINDER session spawns daemon thread HarnessGrinderServer; HTMLDocumentImpl.finishModifications() spawns its job thread; all Swing sizing/layout/painting/isReadyToPaint() calls are marshaled synchronously to EDT via SwingUtilities.invokeAndWait().
- **Known Quirks & Bugs:**
  1. **Single shared HtmlPanel:** RenderHarnessMain creates one HtmlPanel and passes it to every HarnessServer instance; concurrent Grinder sessions will contend on the panel and static HarnessNetworkRequest.ACTIVE_REQUESTS.
  2. **-width/-height CLI flags are immediately overwritten:** RenderHarnessMain sizes the initial panel from -width=/-height=, but HarnessServer initializes currentWidth = 900; currentHeight = 900; (HarnessServer.java:64-65) and resets the panel in loadBlankDocument() until SET_SIZE is received.
  3. **No about: URL handler:** Because PlatformInit and Primary_Extension are not loaded, about: URLs are unsupported (data: URLs are supported via Common's org.lobobrowser.protocol.data.Handler).
  4. **CLOSE / QUIT protocol asymmetry:** Unlike TO/SET_SIZE, handleClose() and handleQuit() do not write a status int or read an ACK line before closing the socket.
  5. **HarnessNetworkRequest limitations:** Ignores asyncFlag, POST content body, userName/password, SRI integrity, and Request kind; getResponseXML() always returns null; executeRequest() attempts ImageIO.read() on every response (including CSS), swallowing decode errors.
  6. **JS flag mismatch:** HarnessUserAgentContext.isScriptingEnabled() is false, while isRequestPermitted() returns true for all RequestKinds (including JavaScript).

## 5. Task-to-File Quick Index

| If working on… | Start at (src/Render_Harness/) |
|---|---|
| Startup flags, port.dat, Grinder auth handshake | org/lobobrowser/harness/RenderHarnessMain.java |
| Offscreen HtmlPanel/HtmlBlockPanel layout workaround | org/lobobrowser/harness/RenderHarnessMain.java (createOffscreenHtmlPanel) |
| Grinder commands (TO, SET_SIZE, SCREENSHOT, CLOSE, QUIT) | org/lobobrowser/harness/HarnessServer.java |
| Path/URL & MIME/charset resolution | org/lobobrowser/harness/HarnessServer.java (resolveURL, handleTo) |
| Subresource HTTP/file loading & active request tracking | org/lobobrowser/harness/HarnessNetworkRequest.java |
| UA capabilities, CSS/media/JS toggles | org/lobobrowser/harness/HarnessUserAgentContext.java |
| Renderer callbacks & <iframe> handling | org/lobobrowser/harness/HarnessHtmlRendererContext.java |
| Ant target & smoke/regression scripts | build.xml, ../build.xml (run-harness), ../../misc/runHarnessTest.sh, ../../misc/runTest.sh |
