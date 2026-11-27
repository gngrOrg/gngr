# Platform_Core Design Overview

Scope: src/Platform_Core/ (126 Java files, ~20.4k LOC; browser runtime, networking/OkHttp, extension loader, per-origin request firewall + Java security sandbox, Swing window/frame shell, H2/jOOQ + disk/RAM storage, Grinder test server).
Prefix shorthand: o.l. = org.lobobrowser..

## 1. Package & Resource Map

| Package / Path | Files | Role |
|---|---:|---|
| o.l.main | 13 | Boot & lifecycle: EntryPoint, PlatformInit, ReuseManager, ReuseServer, GrinderServer, ExtensionManager, Extension, ExtensionClassLoader, TrustManager, PlatformStreamHandlerFactory, ZipEntryHandler, DefferedLayoutSupport, OS |
| o.l.gui | 29 | Browser chrome & frames: FramePanel (NavigatorFrame), NavigationEngine, NavigatorWindowImpl, BrowserPanel, DefaultBrowserWindow, DefaultWindowFactory, ConsoleModel, AuthenticationDialog, ProgressWindow, listeners/events |
| o.l.request | 30 | HTTP/caching/cookies/UA: RequestEngine, ClientletRequestHandler, ClientletRequestImpl, ClientletResponseImpl, RedirectRequestHandler, CookieStore, CookieHandlerImpl, CookieParsing, DomainValidation, OkHostnameVerifier, VerifiedHostsStore, SilentUserAgentContextImpl, UserAgentContextImpl, UserAgentImpl, CacheInfo, MemoryCacheEntry |
| o.l.context | 7 | Clientlet & XHR/subresource runtime: NetworkRequestImpl, AlgorithmDigest (SRI), ClientletContextImpl, ClientletFactory, CoreClientletSelector, VolatileContentImpl, ClientletThreadGroupImpl |
| o.l.security | 12 | Privacy firewall & JVM sandbox: RequestManager, PermissionSystem, PermissionTable/PermissionCellButton, RequestRuleStore (DBRequestRuleStore, InMemoryRequestRuleStore), LocalSecurityPolicy, LocalSecurityManager, custom Permissions (GenericLocalPermission, StoreHostPermission, HistoryPermission) |
| o.l.store | 10 | Persistence & caches: StorageManager (H2 DB, settings, host stores), CacheManager (RAM LRU + disk cache sweeper), RestrictedStore (per-host quota), TempFileManager, ClassLoaderObjectInputStream |
| o.l.settings | 7 | Serializable settings singletons persisted via StorageManager: GeneralSettings, ConnectionSettings, CacheSettings, BooleanSettings, CookieSettings, AssociatedSettings, ManagedStoreSettings |
| o.l.protocol.{res,vc} | 4 | Custom URL schemes: res: (classpath resources) and vc: (VolatileContentImpl in-memory buffers) |
| o.l.async | 5 | Generic async callback primitives (AsyncResult, AsyncResultImpl, AsyncResultWrapper, AsyncResultListener, AsyncResultEvent) |
| info.gngr.db[.*] | 9 | jOOQ-generated schema classes (Tables, Keys, Public, Cookies, Globals, Permissions, *Record) from info/gngr/schema.sql |
| Resources / lib/ | — | info/gngr/schema.sql, trustStore.certs, properties/{logging,logging-debug,release}.properties, images/gngrLogo*.png; jars: okhttp-3.13.1, okhttp-urlconnection-3.13.1, okio-1.17.3, h2-1.4.196, jooq-3.4.2, javatuples-1.2, public-suffix-list |

**Internal module dependencies:** Platform_Public_API (o.l.ua.*, o.l.clientlet.*, o.l.io.*), Common (o.l.util.*). Platform_Core does **not** directly depend on HTML_Renderer or Primary_Extension; it discovers and invokes them via ExtensionManager and Clientlet.

## 2. Boot, Navigation & Request Pipelines

### 2.1 Startup & Single-Instance Reuse
```
EntryPoint.main(args)  [checks stackDepth <= 11, enters doPrivileged]
  ├─ TrustManager.makeSSLSocketFactory(/trustStore.certs)  [JVM cacerts + extra certs]
  └─ ReuseManager.launch(args, sslInfo)
       ├─ Checks ~/.gngr/<profile>/port.dat on 127.0.0.1:
       │    Running instance found ─► sends "LAUNCH <arg>" / "LAUNCH_BLANK" ─► exits
       │    None found             ─► starts ReuseServer (random port 55000..64999), writes port.dat
       └─ PlatformInit:
            initLogging(debugOn)
            init(): checkReleaseDate (loads relProps, logs warning if >120d old) → initOtherProperties → initNative
                    → initSecurity (LocalSecurityPolicy + LocalSecurityManager)
                    → initProtocols (OkHttp HTTP/1.1+HTTP/2 + res:/vc:)
                    → initHTTP (AuthenticatorImpl + NOPCookieHandlerImpl)
                    → initLookAndFeel (Nimbus fallback System) → initConsole (unless -debug)
                    → initWindowFactory → ExtensionManager.initExtensions()
            start(args): captures -grinder-key=<key>, launches URL args or "about:welcome",
                         waits for DefaultWindowFactory.evtWindowShown
```

### 2.2 Frame Navigation & Clientlet Dispatch
```
FramePanel.navigate(url, method, paramInfo, targetType, requestType)
  ├─ ExtensionManager.dispatchBeforeNavigate(event)  [vetoable]
  ├─ Routes TargetType: SELF → navigateLocal | PARENT | TOP | BLANK → openWindow()
  └─ navigateLocal(event):
       ├─ requestManager.reset(url)  [fresh per-page PermissionSystem + host counters]
       ├─ dispatchBeforeLocalNavigate(event)  [vetoable]
       └─ Schedules ClientletRequestHandler on RequestEngine (SimpleThreadPool, 3..5 threads):
            1. Lookup RAM/disk cache (CacheManager) unless RELOAD/DOWNLOAD
            2. Open URLConnection (OkHttp via PlatformStreamHandlerFactory; proxy via ConnectionSettings)
            3. Add headers (User-Agent, Accept*, Referer [gated by RequestKind.Referrer],
               If-Modified-Since, Cookie [gated by RequestKind.Cookie])
            4. ExtensionManager.dispatchPreConnection(conn); write POST/multipart body
            5. Read response: store Set-Cookie [gated by RequestKind.Cookie];
               304 → reload cache; 301/302/303 → RedirectRequestHandler (max 30 hops)
            6. ExtensionManager.dispatchPostConnection(conn)
            7. ClientletFactory → CoreClientletSelector → ExtensionManager.getClientlet()
               (normal pass, then reverse-order last-resort pass)
            8. clientlet.process(ClientletContextImpl):
                 calls setResultingContent(ComponentContent) ─► FramePanel.replaceContent() on EDT
                 (swaps Swing component, updates NavigationEngine, calls content.navigatedNotify())
            9. Callback updates RAM + disk cache if response cacheable & valid
```

### 2.3 Subresource / XHR Requests (NetworkRequestImpl)
- Created via NavigatorFrame.createNetworkRequest() (from UserAgentContext.createHttpRequest()).
- send(content, Request) checks uaContext.isRequestPermitted(request) first; if denied, immediately calls abort() (STATE_ABORTED).
- Schedules LocalRequestHandler on RequestEngine (async via scheduleRequest, sync via inlineRequest).
- Supports Subresource Integrity (AlgorithmDigest.validate() for sha256/sha384/sha512, enforcing strongest present hash) before accepting/caching bytes.
- Caches CacheableResponse (raw bytes, lazy WeakReference<Image>, decoded text/XML) as transient cached object on ClientletResponse.

## 3. Subsystem Details

### 3.1 Extension System (o.l.main)
- ExtensionManager (singleton; gated by GenericLocalPermission.EXT_GENERIC) loads extensions in priority order (TreeSet<Extension>) from:
  1. Embedded /flat-extensions resource index (for fat-jar / Uno builds).
  2. ext.dirs system property (or <appDir>/ext), including nested jars via zipentry:// (ZipEntryHandler).
  3. ext.files / jnlp.ext.files system property.
- Non-extension jars are grouped into a shared URLClassLoader (librariesCL); each Extension gets an ExtensionClassLoader with librariesCL as parent and initializes in parallel via PlatformInit.scheduleTask() (MainThreadPool, 2..3 threads).
- Extension implements NavigatorExtensionContext: extensions register ClientletSelectors, NavigationListeners, ConnectionProcessors (pre/post), NavigatorErrorListeners, and URLStreamHandlerFactorys.

### 3.2 Grinder Test Control (ReuseServer + GrinderServer)
- ReuseServer listens on 127.0.0.1:<random> (port persisted to <appHome>/port.dat).
- Handles LAUNCH <url>, LAUNCH_BLANK, and GRINDER <passkey>:
  - PlatformInit.verifyAuth(reusePort, passkey) checks Base64(SHA-256(grinderKey + reusePort)) against -grinder-key=....
  - On success, opens about:blank frame and spawns GrinderServer on a 127.0.0.1 ephemeral port, returning the port int.
- GrinderServer handles TO <url> (navigates, waits for ProgressType.DONE + DefferedLayoutSupport.layoutCompletion(), then calls frame.allowAllFirstPartyRequests()), SET_SIZE <w> <h>, SCREENSHOT (waits isReadyToPaint(), disables render hints, paints component to PNG), CLOSE, and QUIT.

### 3.3 Security & Privacy Firewall (o.l.security)
Two distinct layers:
1. **Per-Origin Request Firewall (RequestManager + PermissionSystem + DBRequestRuleStore):**
   - Each FramePanel owns a RequestManager, reset on every top-level navigateLocal().
   - PermissionSystem stacks two PermissionBoards: global "*" fallback board → per-frame "*." + frameHost board. Each board has a header row ("") and per-requestHost rows; each row has a host cell + one cell per RequestKind (Permission: Allow | Deny | Undecided inheriting from parent/fallback cells).
   - Default rules initialized once in RequestRuleStore.HelperPrivate.initStore() (GLOBALS.permissionsInitialized): global default Deny, except Image and CSS = Allow.
   - Protocol gate: PermissionSystem.isRequestPermitted() permits only http, https, data (rewritten to frame host by RequestManager), and about:blank; blocks file:// and all other schemes.
   - Mixed-content check: https frame requesting http requires RequestKind.UnsecuredHTTP.
   - Persistence: DBRequestRuleStore stores 2-bit-per-kind bitmasks (0x3=Allow, 0x2=Deny, <2=Undecided) in H2 PERMISSIONS(frameHost, requestHost, permissions), indexing slots 1..15 by `RequestKind.persistenceId` (slot 0 = host default) rather than enum `ordinal()`.
   - UI: RequestManager.manageRequests() opens ManageDialog containing PermissionTable/PermissionCellButton; closing after any change triggers frame.reload().
2. **JVM Sandbox (LocalSecurityPolicy + LocalSecurityManager):**
   - LocalSecurityPolicy defines STORE_DIRECTORY (~/.gngr/default or -Dgngr.profile=...). Files under STORE_DIRECTORY are treated as untrusted (isLocal() == false).
   - getPermissions(CodeSource) rejects null CodeSource/location and grants tailored least-privilege sets by jar/path (core.jar/Common/Primary_Extension/HTML_Renderer, h2, jooq, okhttp, js.jar, jstyleparser, JDK jrt:). Remote code only gets StoreHostPermission for its valid domains + rhino.stack.style read.
   - PlatformInit.initSecurity() catches UnsupportedOperationException on Java 17+ where SecurityManager is disabled/removed.

### 3.4 Networking, TLS, Cookies & Caching (o.l.request)
- **HTTP stack:** PlatformInit.initProtocols() registers OkUrlFactory(OkHttpClient) (HTTP/1.1 + HTTP/2, 100s connect timeout, redirects disabled so RequestEngine controls them) into PlatformStreamHandlerFactory.
- **TLS:** TrustManager.CompositeX509TrustManager checks system trust store then falls back to /trustStore.certs. OkHostnameVerifier (RFC 2818 + DistinguishedNameParser) verifies hostnames; VerifiedHostsStore caches user-approved overrides.
- **Cookies:** Global CookieHandler is set to NOPCookieHandlerImpl so OkHttp/JDK don't bypass the request firewall; RequestEngine explicitly invokes CookieHandlerImpl → CookieStore gated by RequestKind.Cookie.
  - CookieParsing parses Set-Cookie; DomainValidation validates cookie domains using public-suffix-list (de.malkusch.whoisServerList).
  - CookieStore keeps all cookies in RAM (transientMapByHost) and persists cookies with expiration dates into H2 COOKIES table via jOOQ.
- **UserAgentContext variants:**
  - SilentUserAgentContextImpl delegates isRequestPermitted() to frame.isRequestPermitted() (RequestManager).
  - UserAgentContextImpl returns false from isRequestPermitted() by default.
- **Caching (RequestEngine + CacheInfo + CacheManager):**
  - Two-tier cache: RAM LRU (CacheManager.transientCache, default 1 MB, stores MemoryCacheEntry with altObject like CacheableResponse) + disk files under <appHome>/cache/<host>/content/<prefix>_<md5> (max 100 MB, swept every 5 min by daemon thread).
  - HISTORY requests never expire from cache; ADDRESS_BAR always revalidates (If-Modified-Since); SOFT_RELOAD/HARD_RELOAD/DOWNLOAD bypass cache lookup.

### 3.5 Storage, DB & Settings (o.l.store, o.l.settings, info.gngr.db)
- Profile root <appHome> = LocalSecurityPolicy.STORE_DIRECTORY (~/.gngr/default or -Dgngr.profile=...).
  - user.h2 (H2 embedded DB, lazy-opened via StorageManager.getDB() returning jOOQ DSLContext; auto-runs /info/gngr/schema.sql if PUBLIC has 0 tables). Tables: PERMISSIONS, COOKIES, GLOBALS.
  - settings/<ClassName>: Java-serialized settings singletons (GeneralSettings, ConnectionSettings, CacheSettings, BooleanSettings, CookieSettings, AssociatedSettings, ManagedStoreSettings), deserialized through ClassLoaderObjectInputStream.
  - cache/<host>/content/: persistent HTTP cache managed by CacheManager (per-file canonical-path interned lock).
  - HostStore/<host>/: RestrictedStore (ManagedStore with 200 KB quota via RestrictedOutputStream, gated by StoreHostPermission, periodic size sync on StorageManager daemon thread).
  - port.dat: active ReuseServer port.

### 3.6 Browser GUI Shell (o.l.gui)
- FramePanel (JPanel, NavigatorFrame): core frame container; owns NavigationEngine (back/forward history stack), RequestManager, ComponentContent, and listener lists (NavigationListener, ResponseListener, ContentListener). Supports nested subframes (knownParentFrame) and popup/window opening (openWindow()).
- NavigatorWindowImpl (NavigatorWindow, WindowCallback): manages a top-level AbstractBrowserWindow (DefaultBrowserWindow via DefaultWindowFactory), menus, toolbars, address bar, status bar, and extension window lifecycle (initExtensionsWindow/shutdownExtensionsWindow).
- BrowserPanel: self-contained embeddable Swing panel combining FramePanel + optional AddressBarPanel, SharedToolBarPanel, StatusBarPanel, and JMenuBar.
- ConsoleModel (PlainDocument): captures System.out/System.err when initConsole() runs (skipped if -debug flag passed).

## 4. Threading, Invariants & Known Quirks

- **Threads:**
  - RequestEngineThreadPool (3..5 threads): executes RequestHandlers (HTTP, cache, redirects, clientlet.process()).
  - MainThreadPool (2..3 threads, PlatformInit): parallel extension classloader & initExtension() startup.
  - Background daemons: ReuseServer, GrinderServer (per test session), CacheManager (disk sweeper, 5m interval), StorageManager (quota sync, 5m interval).
  - Swing EDT: FramePanel.replaceContent(), UI dialogs (AuthenticationDialog, ManageDialog), window chrome updates.
- **Security context propagation:** RequestEngine.scheduleRequest() captures AccessController.getContext() when a SecurityManager is active; ClientletRequestHandler temporarily sets ClientletAccess.setCurrentClientletContext() and thread context ClassLoader to the clientlet's classloader during clientlet.process().
- **Gotchas:**
  1. PlatformInit.checkReleaseDate() loads release.properties and logs a non-blocking warning if version.releaseDate is >120 days old; about:welcome displays the old-version warning banner.
  2. CookieHandler.setDefault() is intentionally NOPCookieHandlerImpl; cookies flow through RequestEngine + RequestKind.Cookie permission checks instead.
  3. UserAgentContextImpl.isRequestPermitted() hardcodes false; real frame-backed requests use SilentUserAgentContextImpl (which delegates to FramePanel → RequestManager).
  4. CookieStore.saveCookie() currently persists secure=true, httpOnly=true literals when merging into H2 COOKIES (see CookieStore.java:119).
  5. EntryPoint.main() aborts if Thread.currentThread().getStackTrace().length > 11.
  6. LocalSecurityPolicy hardcodes third-party jar filenames/versions (e.g., h2-1.4.196.jar, okhttp-3.13.1.jar, jstyleparser-4.0.1.jar); upgrading jars requires updating LocalSecurityPolicy.getPermissions().
  7. DefferedLayoutSupport is spelled with one r and two fs (used across Platform_Core, HTML_Renderer, and GrinderServer).

## 5. Task-to-File Quick Index

| If working on… | Start at (src/Platform_Core/) |
|---|---|
| Startup, CLI flags, profile dir, shutdown | org/lobobrowser/main/EntryPoint.java, PlatformInit.java, ReuseManager.java |
| Single-instance IPC or Grinder test protocol | org/lobobrowser/main/ReuseServer.java, GrinderServer.java, PlatformInit.java (verifyAuth) |
| Extension discovery, classloading, lifecycle hooks | org/lobobrowser/main/ExtensionManager.java, Extension.java, ExtensionClassLoader.java |
| HTTP fetching, redirects, headers, POST/multipart | org/lobobrowser/request/RequestEngine.java, RedirectRequestHandler.java, MultipartFormDataWriter.java |
| TLS trust store & hostname verification | org/lobobrowser/main/TrustManager.java, org/lobobrowser/request/OkHostnameVerifier.java, VerifiedHostsStore.java |
| Subresource/XHR requests & SRI (integrity=) | org/lobobrowser/context/NetworkRequestImpl.java, AlgorithmDigest.java |
| Clientlet selection & response context | org/lobobrowser/request/ClientletRequestHandler.java, ClientletResponseImpl.java, org/lobobrowser/context/ClientletFactory.java, ClientletContextImpl.java |
| Per-origin request permissions / firewall UI / DB | org/lobobrowser/security/RequestManager.java, PermissionSystem.java, DBRequestRuleStore.java, RequestRuleStore.java, PermissionTable.java |
| Java SecurityManager / jar permissions / profile path | org/lobobrowser/security/LocalSecurityPolicy.java, LocalSecurityManager.java |
| Cookies & public suffix validation | org/lobobrowser/request/CookieStore.java, CookieParsing.java, DomainValidation.java, CookieHandlerImpl.java |
| RAM/disk HTTP cache, H2 DB, settings, host quotas | org/lobobrowser/store/StorageManager.java, CacheManager.java, RestrictedStore.java, info/gngr/schema.sql, org/lobobrowser/settings/*Settings.java |
| Frame navigation, history, windows, embeddable panel | org/lobobrowser/gui/FramePanel.java, NavigationEngine.java, NavigatorWindowImpl.java, BrowserPanel.java, DefaultWindowFactory.java |
| Custom res: / vc: URL protocols | org/lobobrowser/protocol/res/ResURLConnection.java, org/lobobrowser/protocol/vc/VcURLConnection.java, org/lobobrowser/context/VolatileContentImpl.java |
