# Platform_Public_API Design Overview

Scope: src/Platform_Public_API/ (48 Java files, ~4.0k LOC; zero-dependency public contract layer between Platform_Core, extensions/clientlets, HTML_Renderer, and Render_Harness).
Prefix shorthand: o.l. = org.lobobrowser..
Build dependencies: JDK 11 (org.w3c.dom.Document, java.awt.*, javax.swing.JMenu) + org.eclipse.jdt.annotation (@NonNull/@Nullable). **Zero internal module dependencies** (does not depend on Common, Platform_Core, HTML_Renderer, or Primary_Extension).
License note: README.txt states FreeBSD-equivalent license for platform APIs, though newer files carry LGPL (ImageResponse.java) or GPL (NavigationEvent.java) headers.

## 1. Package & Resource Map

| Package / Path | Files | LOC | Role |
|---|---:|---:|---|
| o.l.ua | 28 | ~2.4k | Browser/UA contracts: NavigatorExtension, NavigatorExtensionContext, NavigatorWindow, NavigatorFrame, UserAgent, UserAgentContext (RequestKind, Request), NetworkRequest, ImageResponse (State), NavigationEntry, NavigationListener, NavigationEvent, NavigationVetoException, ConnectionProcessor, NavigatorErrorListener, NavigatorWindowListener, NetworkRequestListener, Parameter/ParameterInfo, events (NavigatorEvent, NavigatorResponseEvent, NavigatorProgressEvent, NavigatorWindowEvent, NavigatorExceptionEvent, NetworkRequestEvent), enums (RequestType, TargetType, ProgressType, NavigatorEventType) |
| o.l.clientlet | 16 | ~1.3k | Content-handler plugin API: Clientlet, ClientletSelector, ClientletContext, ClientletRequest, ClientletResponse, ClientletAccess, ClientletThreadGroup, ComponentContent, AbstractComponentContent, SimpleComponentContent, ContentBuffer, Header, exceptions (ClientletException, CancelClientletException, JavaVersionException, NavigatorVersionException) |
| o.l.io | 4 | ~0.3k | Sandboxed per-host virtual filesystem: ManagedStore, ManagedFile, ManagedFileFilter, QuotaExceededException |
| Resources | — | — | README.txt, org/lobobrowser/{ua,clientlet,io}/package.html (stale RequestStats*.class exist under bin/ with no .java source) |

**Cross-module consumers & concrete implementations:**

| Module | Usage & Key Implementations |
|---|---|
| Platform_Core | Implements all 3 packages: Extension (NavigatorExtensionContext), FramePanel (NavigatorFrame), NavigatorWindowImpl & BrowserPanel (NavigatorWindow), UserAgentImpl & OrigUserAgentImpl (UserAgent), SilentUserAgentContextImpl & UserAgentContextImpl (UserAgentContext), NetworkRequestImpl (NetworkRequest), ClientletContextImpl (ClientletContext), ClientletRequestImpl (ClientletRequest), ClientletResponseImpl (ClientletResponse), CoreClientletSelector (ClientletSelector), ClientletThreadGroupImpl (ClientletThreadGroup), VolatileContentImpl (ContentBuffer), NavigationAdapter (NavigationListener), RestrictedStore (ManagedStore), ManagedFileImpl (ManagedFile) |
| Primary_Extension | Implements o.l.ua + o.l.clientlet: ExtensionImpl (NavigatorExtension), PrimaryClientletSelector (ClientletSelector), HtmlClientlet/ImageClientlet/TextClientlet/DownloadClientlet (Clientlet), HtmlContent/ImageContent (ComponentContent), ComponentSource (NavigatorWindowListener), LocalParameterInfo (ParameterInfo) |
| HTML_Renderer | Consumes o.l.ua: UserAgentContext, NetworkRequest, NetworkRequestListener, NetworkRequestEvent, ImageResponse, TargetType, RequestType |
| Render_Harness | Implements/consumes o.l.ua: HarnessUserAgentContext (UserAgentContext), HarnessNetworkRequest (NetworkRequest) |

## 2. Contract Lifecycles & Interaction Flows

### 2.1 Extension & Window Lifecycle
```
NavigatorExtension.init(NavigatorExtensionContext)
  ├─ registers ClientletSelector, NavigationListener, ConnectionProcessor,
  │  NavigatorErrorListener, URLStreamHandlerFactory
  └─ per window: windowOpening(NavigatorWindow)
       ├─ addMenu / addToolBar / addSharedToolBarComponent / addAddressBarComponent / addStatusBarComponent
       └─ addNavigatorWindowListener(NavigatorWindowListener)
  ... windowClosing(NavigatorWindow) ... destroy()
```

### 2.2 Navigation → Clientlet Dispatch → Content Lifecycle
```
NavigatorFrame.navigate(...) / open(...) / linkClicked(...)
  ├─ NavigationListener.beforeNavigate / beforeLocalNavigate / beforeWindowOpen  [veto: NavigationVetoException]
  ├─ ConnectionProcessor.processPreConnection(conn) ─► HTTP I/O ─► processPostConnection(conn)
  ├─ ClientletSelector.select(ClientletRequest, ClientletResponse)  [fallback: lastResortSelect()]
  └─ Clientlet.process(ClientletContext)
       ├─ CancelClientletException ─► abort cleanly
       └─ context.setResultingContent(ComponentContent)
            └─ Frame replaces component:
                 addNotify() ─► update NavigationEntry ─► navigatedNotify() ─► [on replace] removeNotify()
```

### 2.3 Subresource / XHR Flow (UserAgentContext + NetworkRequest)
```
UserAgentContext.createHttpRequest() ─► NetworkRequest
  ├─ addNetworkRequestListener(NetworkRequestListener)
  ├─ open(method, url, asyncFlag, userName, password, integrity)
  ├─ optional addRequestedHeader(name, value)
  └─ send(content, new Request(url, RequestKind))  [gated by UserAgentContext.isRequestPermitted(Request)]
       ├─ States: STATE_UNINITIALIZED(0) ─► STATE_LOADING(1) ─► STATE_LOADED(2)
       │          ─► STATE_INTERACTIVE(3) ─► STATE_COMPLETE(4) | STATE_ABORTED(5)
       └─ Outputs: getResponseBytes() / getResponseText() / getResponseXML() / getResponseImage() [ImageResponse]
```

## 3. Subsystem Details

### 3.1 Extension, Window & Frame Shell (o.l.ua)
- **NavigatorFrame:** Frame navigation (navigate, open, linkClicked, reload), history (back, forward, canBack, canForward, getCurrentNavigationEntry, getPreviousNavigationEntry, getNextNavigationEntry, moveInHistory, navigateInHistory, getHistoryLength), frame hierarchy (getParentFrame, getTopFrame, getOpenerFrame, createFrame), UI dialogs (alert, confirm, prompt), content/window control (replaceContent, getComponentContent, getSourceCode, resizeWindowTo/By, closeWindow, invokeLater), subresources (createNetworkRequest), and gngr privacy hooks (isRequestPermitted, manageRequests, allowAllFirstPartyRequests).
- **NavigatorWindow:** Chrome extension points (addMenu/getMenu, addToolBar, addSharedToolBarComponent, addAddressBarComponent, addStatusBarComponent, createGlueComponent, createGap), navigation/clipboard actions (back/forward/reload/stop/copy/goTo), addNavigatorWindowListener/removeNavigatorWindowListener, getTopFrame(), getAwtWindow().
- **Enums:**
  - RequestType: CLICK, PROGRAMMATIC, PROGRAMMATIC_FROM_CLICK, ADDRESS_BAR, SOFT_RELOAD, HARD_RELOAD, HISTORY, DOWNLOAD, OPEN_WINDOW, OPEN_WINDOW_FROM_CLICK, FRAME, FORM, ELEMENT, NONE.
  - TargetType: BLANK, SELF, PARENT, TOP.
  - ProgressType: NONE, CONNECTING, SENDING, WAITING_FOR_RESPONSE, CONTENT_LOADING, BUILDING, DONE.
  - NavigatorEventType: DOCUMENT_ACCESSED, DOCUMENT_RENDERING, PROGRESS_UPDATED, ERROR_OCCURRED, STATUS_UPDATED, DEFAULT_STATUS_UPDATED.
- **Event hierarchy:** EventObject → NavigatorEvent (getNavigatorFrame, getEventType) → NavigatorProgressEvent (getMethod, getProgressType, getUrl, getCurrentValue, getMaxValue) | NavigatorResponseEvent (getResponse, getUrl, getMethod, getRequestType) → NavigatorWindowEvent (getMessage) | NavigatorExceptionEvent (getException). Separate: NavigationEvent (vetoable) and NetworkRequestEvent (getState).

### 3.2 UserAgentContext, Permissions & NetworkRequest (o.l.ua)
- **UserAgentContext.RequestKind:** Image("Img", 1), CSS("CSS", 2), Cookie("Cookie", 3), JavaScript("JS", 4), Frame("Frame", 5), XHR("XHR", 6), Referrer("Referrer", 7), UnsecuredHTTP("Unsecured HTTP", 8); exposes shortName, persistenceId (1..15, stable H2 bitmask slot), forOrdinal(int), and numKinds().
- **UserAgentContext.Request:** Immutable (URL url, RequestKind kind) checked via isRequestPermitted(Request) and passed to NetworkRequest.send(content, requestType).
- **UserAgentContext:** Browser identity (getAppCodeName/getAppName/getAppVersion/getAppMinorVersion, getUserAgent, getPlatform, getVendor, getProduct, getBrowserLanguage), cookies (getCookie/setCookie), security/scripting (getSecurityPolicy, getScriptingOptimizationLevel, isMedia), plus legacy feature flags marked TODO: Remove (isCookieEnabled, isScriptingEnabled, isExternalCSSEnabled, isInternalCSSEnabled).
- **NetworkRequest:** XHR-like contract supporting SRI integrity on open(...) overloads, custom headers (addRequestedHeader), header filtering (getAllResponseHeaders(excludedHeadersLowerCase)), and typed response getters.
- **ImageResponse:** Value class with State (loading, loaded, error) and @Nullable Image img. isDecoded() requires state == loaded with width >= 0 && height >= 0; isReadyToPaint() returns isDecoded() when loaded, or state != loading (so error is ready to paint).
- **ParameterInfo / Parameter:** Form submission encoding + array of text or file parameters.

### 3.3 Clientlet & ComponentContent API (o.l.clientlet)
- **Clientlet & ClientletSelector:** select(req, resp) matches primary handlers; lastResortSelect(req, resp) handles fallback handlers (e.g., plain text/download) after primary selectors decline.
- **ClientletContext:** Exposes ClientletRequest, ClientletResponse, UserAgent, NavigatorFrame, per-context item map (getItem/setItem), ManagedStore (getManagedStore()/getManagedStore(host)), volatile buffers (createContentBuffer(...) → ContentBuffer.getURL()), content handoff (setResultingContent/getResultingContent/isResultingContentSet), progress updates (setProgressEvent), window property overrides (overrideWindowProperties/getOverriddingWindowProperties), navigate, alert, and createNavigatorFrame.
- **ClientletResponse:** Status/headers/body/charset/MIME (matches(mimeType, fileExtension[])), cache metadata (isFromCache, isCacheable, isNewNavigationAction, getDate), and two-tier cache attachment (getPersistentCachedObject/setNewPersistentCachedObject for Serializable; getTransientCachedObject/setNewTransientCachedObject(obj, approxSize) for RAM).
- **ComponentContent:** Holds AWT Component, title/description/source/MIME/contentObject, clipboard (canCopy/copy), lifecycle (addNotify, navigatedNotify, removeNotify), plus Java 8 default methods disableRenderHints() (NOP) and isReadyToPaint() (true) used by Grinder screenshot capture. AbstractComponentContent and SimpleComponentContent provide partial base classes.
- **ClientletAccess:** Looks up current ClientletContext via ThreadLocal, falling back to Thread.currentThread().getThreadGroup() instanceof ClientletThreadGroup.
- **Exceptions:** ClientletException (optionally carries sourceCode); subclasses CancelClientletException, JavaVersionException, and NavigatorVersionException (latter two carry expectingVersion).

### 3.4 Sandboxed Managed Storage (o.l.io)
- **ManagedStore:** Quota-bounded per-host storage (getQuota, getSize), virtual files (getManagedFile, getRootManagedDirectory), and serialized object I/O (saveObject, retrieveObject with optional ClassLoader).
- **ManagedFile / ManagedFileFilter:** Directory/file operations (exists, isDirectory, mkdir/mkdirs, listFiles, createNewFile, getParent, openInputStream/openOutputStream, delete). Exceeding quota throws QuotaExceededException (IOException).

## 4. Invariants & Known Quirks

1. **RequestKind persistence IDs vs. ordinals:** RequestKind.persistenceId (1..15, never changed or reused) indexes into Platform_Core's 2-bit-per-kind bitmask persisted in H2 PERMISSIONS; forOrdinal() and numKinds() are for transient in-memory indexing only.
2. **Legacy API spellings (do not rename without cross-module updates):**
   - NetworkRequest.isAsnyc() (transposed letters)
   - NavigatorErrorListener.errorOcurred(...) (single r)
   - ClientletContext.getOverriddingWindowProperties() (double d)
3. **AbstractComponentContent and SimpleComponentContent remain abstract:** Neither implements ComponentContent.navigatedNotify(), so callers must subclass and implement navigatedNotify().
4. **NetworkRequest.STATE_ABORTED = 5:** Extends beyond the older Javadoc comment ("a value between 0 and 4"); listeners must handle state 5 (triggered e.g. when isRequestPermitted() denies a request).
5. **UserAgentContext legacy flags:** isCookieEnabled(), isScriptingEnabled(), isExternalCSSEnabled(), and isInternalCSSEnabled() are marked TODO: Remove in favor of isRequestPermitted(Request).
6. **ClientletAccess context lookup:** Relies on ThreadLocal set by the runtime or ClientletThreadGroup on child threads; arbitrary worker threads outside a ClientletThreadGroup receive null.
7. **Strict module boundary:** Platform_Public_API must remain free of internal module imports (Common, Platform_Core, HTML_Renderer, Primary_Extension).

## 5. Task-to-File Quick Index

| If working on… | Start at (src/Platform_Public_API/org/lobobrowser/) |
|---|---|
| Extension registration & lifecycle hooks | ua/NavigatorExtension.java, ua/NavigatorExtensionContext.java |
| Clientlet selection, dispatch & context | clientlet/Clientlet.java, clientlet/ClientletSelector.java, clientlet/ClientletContext.java, clientlet/ClientletAccess.java |
| Rendered content wrapper & Grinder readiness | clientlet/ComponentContent.java, clientlet/AbstractComponentContent.java, clientlet/SimpleComponentContent.java |
| HTTP request/response & clientlet caching | clientlet/ClientletRequest.java, clientlet/ClientletResponse.java, ua/ParameterInfo.java, ua/Parameter.java |
| Request firewall kinds & UA capabilities | ua/UserAgentContext.java, ua/UserAgent.java |
| Subresource/XHR API & SRI integrity | ua/NetworkRequest.java, ua/NetworkRequestEvent.java, ua/NetworkRequestListener.java |
| Async image loading state & paint readiness | ua/ImageResponse.java |
| Frame navigation, history & veto events | ua/NavigatorFrame.java, ua/NavigationEvent.java, ua/NavigationListener.java, ua/NavigationEntry.java, ua/RequestType.java, ua/TargetType.java |
| Browser window chrome & progress/error events | ua/NavigatorWindow.java, ua/NavigatorEvent.java, ua/NavigatorProgressEvent.java, ua/NavigatorErrorListener.java, ua/ConnectionProcessor.java |
| Per-host sandboxed files & storage quotas | io/ManagedStore.java, io/ManagedFile.java, io/ManagedFileFilter.java, io/QuotaExceededException.java |
