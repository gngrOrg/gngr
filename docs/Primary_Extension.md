# Primary_Extension Design Overview

Scope: src/Primary_Extension/ (61 Java files, ~9.3k LOC; default browser extension providing window chrome/menus/address bar, HTML/image/text/download clientlets, about:/data: URL protocols, history/bookmarks, search engines, and preferences UI).
Prefix shorthand: o.l. = org.lobobrowser., o.l.p. = org.lobobrowser.primary..
Extension manifest (gngr-extension.properties): extension.class=org.lobobrowser.primary.ext.ExtensionImpl, extension.priority=10 (highest/exclusive primary priority; discovered by Platform_Core's ExtensionManager via ext.files / ext.dirs / /flat-extensions).
Build dependencies:
- Internal modules: Platform_Public_API, Common, Platform_Core (GeneralSettings, ConnectionSettings, StorageManager, RequestEngine, AbstractRequestHandler, ClientletRequestHandler, ClientletRequestImpl, SilentUserAgentContextImpl, UserAgentImpl, DomainValidation, PlatformInit, OS, ConsoleModel, DefaultWindowFactory, NavigatorWindowImpl), HTML_Renderer (HtmlPanel, HTMLDocumentImpl, HtmlRendererContext, BrowserFrame, DocumentBuilderImpl, Window, JavaScript, DOM impls).
- External/JDK: JDK 1.8 (java.awt.*, javax.swing.*, org.w3c.dom.*), Rhino (org.mozilla.javascript.Scriptable in SimpleDocument), org.eclipse.jdt.annotation.
- **No other module has a compile-time dependency on Primary_Extension.**

## 1. Package & Resource Map

| Package / Path | Files | LOC | Role |
|---|---:|---:|---|
| o.l.p.ext | 19 | ~3.3k | Extension entrypoint (ExtensionImpl), browser chrome/menus/actions (ComponentSource, ActionPool, DirectorySource, AddressField, TextFieldComboBoxEditor, ProgressBar, IconFactory, MenuAdapter), protocol factory (PrimaryStreamHandlerFactory), history/bookmarks (BaseHistory, NavigationHistory, BookmarksHistory, BookmarkInfo, HistoryEntry, HostEntry, AddBookmarkDialog), viewers (TextViewerWindow, DomViewerWindow [unused]) |
| o.l.p.clientlets.html | 4 | ~1.4k | HTML clientlet & HTML_Renderer bridge: HtmlClientlet, HtmlContent, HtmlRendererContextImpl, BrowserFrameImpl |
| o.l.p.clientlets | 6 | ~0.8k | Dispatch (PrimaryClientletSelector), plain-text viewer (TextClientlet), stub DOM (SimpleDocument), legacy JAR classloader (ArchiveClassLoader, ArchiveCollection, ArchiveInfo [unused]) |
| o.l.p.clientlets.img | 2 | ~0.2k | Standalone image viewer: ImageClientlet (ImageContent), ImageScrollable |
| o.l.p.clientlets.download | 1 | ~0.1k | DownloadClientlet (speed probe + opens DownloadDialog, aborts frame load) |
| o.l.p.gui | 13 | ~1.2k | Reusable Swing form/list/dialog widgets: FormPanel, FormField, FieldType, ItemListControl, ItemEditorDialog, AbstractItemEditor, ItemEditorFactory, StringListControl, SimpleTextEditDialog, SearchDialog, CheckBoxPanel [unused], SwingTasks, ValidationException |
| o.l.p.gui.prefs | 9 | ~0.9k | Preferences dialog (PreferencesDialog, PreferencesTree, PreferencesPanel, SettingsInfo, AbstractSettingsUI) and tabs (GeneralSettingsUI, ConnectionSettingsUI, ToolsSettingsUI, SearchEngineEditor) |
| o.l.p.gui.download | 1 | ~0.5k | DownloadDialog (download progress UI + DownloadRequestHandler; save currently disabled) |
| o.l.p.settings | 2 | ~0.2k | ToolsSettings (search engines + chooser dirs via StorageManager), SearchEngine |
| o.l.protocol.about | 2 | ~0.5k | about: URL handler (Handler, AboutURLConnection) |
| o.l.protocol.data | 2 | ~0.2k | RFC 2397 data: URL handler (Handler, DataURLConnection) |
| Resources | — | — | gngr-extension.properties, README.txt (GPL), images/*.{gif,png} (11 icons: back, forward, go, go_disabled, refresh, refresh_disabled, stop, stop_disabled, info, warning, internet_search) |

## 2. Lifecycles & Processing Pipelines

### 2.1 Extension Init, Window Chrome & Error Handling
```
ExtensionImpl.init(NavigatorExtensionContext)
  ├─ addURLStreamHandlerFactory(PrimaryStreamHandlerFactory)  [about:, data:]
  ├─ addClientletSelector(PrimaryClientletSelector)
  ├─ addNavigatorErrorListener ──► ExtensionImpl.showError()  [renders about:error in HtmlPanel]
  └─ JavaScript.init()                                        [locks global Rhino ContextFactory]

ExtensionImpl.windowOpening(NavigatorWindow)
  └─ new ComponentSource(window) [owns ActionPool, DirectorySource, AddressField, ProgressBar]
       ├─ adds address-bar & status-bar components
       ├─ adds 9 menus: File, Edit, View, Navigation, Bookmarks, Directory, Page Services, Tools, Help
       ├─ registers as NavigatorWindowListener (updates address bar, progress, status, history, actions)
       └─ initializes with window.getCurrentNavigationEntry()

ExtensionImpl.windowClosing(NavigatorWindow)
  └─ NavigationHistory.getInstance().save()
```

### 2.2 Clientlet Selection (PrimaryClientletSelector)
```
Pass 1 — select(req, resp) [non-overridable priority-10 matches]:
  ├─ MIME text/html | image/svg+xml | application/xhtml+xml | application/xml ─► HtmlClientlet
  ├─ MIME image/{jpeg,jpg,gif,png,bmp,x-ms-bmp}                               ─► ImageClientlet
  └─ MIME null | application/octet-stream | content/unknown:
       ├─ ext .html | .htm | <none> ─► HtmlClientlet
       ├─ ext .gif | .jpg | .png    ─► ImageClientlet
       └─ otherwise                 ─► null (allow other extensions)

Pass 2 — lastResortSelect(req, resp) [fallback after all selectors return null]:
  ├─ MIME text/* | application/json                          ─► TextClientlet
  ├─ MIME application/xhtml+xml | ext .xhtml | MIME == null  ─► HtmlClientlet
  ├─ ext .txt | .xml | .svg | .rss | .xaml                   ─► TextClientlet
  └─ otherwise                                               ─► DownloadClientlet
```

### 2.3 HTML Clientlet & <meta http-equiv> Retry Pipeline
```
HtmlClientlet.process(ClientletContext)  [runs on RequestEngineThreadPool]
  ├─ Wraps response stream in RecordedInputStream (10 MiB buffer, mark())
  ├─ Charset priority: HTTP Content-Type charset > <meta http-equiv="content-type"> > "utf-8"
  ├─ DocumentBuilderImpl.createDocument(InputSourceImpl, contentType) ─► HTMLDocumentImpl
  ├─ Attaches LocalDocumentNotificationListener & calls document.load(false):
  │    ├─ On <head>/<script>/<html> loaded (if !charsetProvided and charset differs):
  │    │    throws HttpEquivRetryException ─► rin.reset() ─► single recursive processImpl() pass
  │    └─ Switches to rendering on: externalScriptLoading | >7s elapsed | parse completion
  │         └─ SwingUtilities.invokeLater:
  │              htmlPanel.setDocument(document, rcontext)
  │              ccontext.setResultingContent(HtmlContent)
  │                └─ FramePanel replaces content ─► HtmlContent.navigatedNotify()
  │                     └─ document.finishModifications()  [runs CSS/JS/img jobs, unblocks layout]
  └─ If HTTP Refresh or <meta http-equiv="refresh"> found:
       schedules one-shot Swing Timer ─► if frame.getComponentContent() == content:
         frame.navigate(destURL, RequestType.PROGRAMMATIC)
```

## 3. Subsystem Details

### 3.1 Browser Chrome, Address Bar & Actions (o.l.p.ext)
- **ExtensionImpl:** Entrypoint and error page generator (getErrorHtml() formats URL, exception type, human-readable getExceptionMeaning(), and—only when PlatformInit.getInstance().debugOn—full stack trace + root cause).
- **ComponentSource (NavigatorWindowListener):**
  - **Address bar:** Back, Forward, Stop, Refresh, AddressField (glue-stretched), Go, Search (tooltip shows active SearchEngine), Req Mgr (enabled in documentAccessed() only for http/https).
  - **Status bar:** Status JLabel (transient statusMessage overrides defaultStatusMessage; also displays ClientletRequestHandler.getProgressMessage() on progressUpdated()) + ProgressBar (% when max > 0, indeterminate + byte count when max == -1).
  - **Address-bar routing (navigateOrSearch()):** Empty/blank input is ignored; leading `?` invokes `search()` with selected `SearchEngine`; input without `^[a-z]+:.*` that has a space or no `.` navigates to `about:confirmSearch?<text>`; otherwise calls `window.getTopFrame().navigate(text, RequestType.ADDRESS_BAR)`.
  - **AddressField (JComboBox<String> + TextFieldComboBoxEditor):** Ctrl/Cmd+L focuses & selects URL; Ctrl/Cmd+K focuses & inserts "?"; dropdown loads up to 30 recent URLs on open, or filters getPotentialMatches() on typing ([a-zA-Z0-9./], trying http://, file:// for drive letters, then http://www.).
  - **Dynamic menus** (populated lazily on MenuAdapter.menuSelected()): Recent Bookmarks, Tagged Bookmarks (up to 20 tags × 20 entries), Back To, Forward To, Recent Hosts, Searching With (radio items mutating ToolsSettings).
  - **Static menus:** File (New Window, Cloned Window, disabled File..., Close), Edit (DefaultEditorKit Cut/Copy/Paste), View (Page Source, Console), Navigation (F5 + Ctrl+R reload), Directory (DirectorySource hardcoded links), Page Services (Google link:/related:, Wayback Machine), Tools, Help.
- **ActionPool:** Centralizes window Actions and tracks EnableableActions (back, forward, reload, backMore, forwardMore, recentHosts, source, plus UrlPrefixNavigateActions) refreshed by updateEnabling() on documentRendering() / setNavigationEntry().
- **TextViewerWindow:** Read-only JFrame (JTextArea + File/Edit menus) shared by View > Page Source and View > Console (ConsoleModel.getStandard() with scrollsOnAppends = true auto-scrolling on DocumentListener.insertUpdate).

### 3.2 History & Bookmarks (o.l.p.ext)
- **BaseHistory<T> (Serializable, capacity 1000):** Synchronized store keeping three parallel structures: historySortedSet (TreeSet<String> copied to array for Arrays.binarySearch prefix lookup), historyMap (HashMap<String, TimedEntry>), and historyTimedSet (TreeSet<TimedEntry> ordered newest-first; evicts last() over capacity).
- **NavigationHistory (BaseHistory<Object>):** Persisted via StorageManager (settings/NavigationHistory); populated in ComponentSource.documentAccessed() for GET requests with RequestType.ADDRESS_BAR or CLICK; saved on ExtensionImpl.windowClosing().
- **BookmarksHistory (BaseHistory<BookmarkInfo>):** Persisted via StorageManager (settings/BookmarksHistory); BookmarkInfo holds url, title, description, tags[]. Created/edited via AddBookmarkDialog (saved immediately); BookmarkNavigateAction calls touch(url).

### 3.3 Clientlets & HTML_Renderer Bridge (o.l.p.clientlets.*)
- **HtmlClientlet & HtmlContent:**
  - HtmlContent (ComponentContent): wraps HTMLDocument + HtmlPanel; serves getSourceCode() from RecordedInputStream.getBytesRead() (or literal error source); supports setProperty("defaultOverflowX"|"defaultOverflowY"); forwards isReadyToPaint() and disableRenderHints() to HtmlPanel for Grinder; triggers ((HTMLDocumentImpl) document).finishModifications() in navigatedNotify().
- **HtmlRendererContextImpl & BrowserFrameImpl:**
  - Cached 1-to-1 per NavigatorFrame in WeakHashMap<NavigatorFrame, WeakReference<HtmlRendererContextImpl>>; owns a persistent HtmlPanel and lazy SilentUserAgentContextImpl.
  - navigateImpl() resolves named targets against getTop().getFrames() (FrameNode → BrowserFrame.loadURL()) before mapping _blank/_parent/_top/default to TargetType.
  - Bridges form submission (LocalParameterInfo wrapping FormInput[] → RequestType.FORM), window.open (parses features via NavigatorWindowImpl.getPropertiesFromWindowFeatures), history, dialogs, cursor, scrolling, hover status text, middle-click (opens <a>/<img> in new window), right-click context menu (Open/Copy link & image + Back/Reload/Forward), and setJobFinishedHandler()/jobsFinished().
  - BrowserFrameImpl wraps child NavigatorFrames (<iframe>/<frame>) as BrowserFrame, loading with TargetType.SELF + RequestType.FRAME.
- **ImageClientlet & ImageScrollable:** Reads full response bytes into Toolkit.createImage(), wraps ImageScrollable (Scrollable, resizes/repaints on ALLBITS/FRAMEBITS) in JScrollPane; sets SimpleDocument on HtmlPanel and calls jobsFinished() on rcontext + Window in navigatedNotify(); calls image.flush() in removeNotify().
- **TextClientlet:** Loads stream into non-editable JTextArea inside JScrollPane, sets SimpleDocument on HtmlPanel, calls jobsFinished() in navigatedNotify().
- **DownloadClientlet & DownloadDialog:** Rejects local file:// and non-GET; reads response for up to 1 s to estimate transfer speed; opens DownloadDialog (JFrame) and throws CancelClientletException so the current frame is preserved. DownloadDialog includes DownloadRequestHandler (RequestEngine.inlineRequest() on a daemon thread, 1 s throttled progress updates, OS.launchPath() open/folder buttons), but SaveAction currently blocks execution with a security dialog.

### 3.4 Custom URL Protocols (o.l.protocol.{about,data})
- **PrimaryStreamHandlerFactory:** Maps "about" → o.l.protocol.about.Handler and "data" → o.l.protocol.data.Handler.
- **AboutURLConnection (text/html, UTF-8):**
  - about:blank (empty), about:welcome (release version/date from PlatformInit.relProps + Request Manager tip), about:bookmarks, about:bookmark-search?<q> (all space-separated keywords must match; scores title/tag +8/+6, URL/description +3/+2, ordered by score then recency), about:java-properties (System.getProperties()), about:confirmSearch?<q> (direct https:// link + ToolsSettings search engines), and fallback list for unknown paths.
- **DataURLConnection & data.Handler:** Parses data:[mediatype][;param=val][;base64],data (defaults to text/plain). Decodes Base64 (retrying after URLDecoder.decode(..., UTF-8) on IllegalArgumentException for GH #15) or custom decodeUrl() (%xx and + → space).

### 3.5 Settings, Preferences & Reusable GUI (o.l.p.settings, o.l.p.gui.*)
- **ToolsSettings (Serializable singleton via StorageManager):** Persists searchEngines (defaults: DuckDuckGo [selected], Wikipedia, Google, Yahoo!), selectedSearchEngine, downloadDirectory, openFileDirectory. SearchEngine.getURL(query) appends ?/& + queryParameter=URLEncoder.encode(query, UTF-8).
- **PreferencesDialog:** Left PreferencesTree (General, Connection, Tools) + right PreferencesPanel (WrapperLayout) + OK/Cancel/Apply/Restore Defaults:
  - GeneralSettingsUI → Platform_Core's GeneralSettings startup URLs (UI notes setting is currently ignored in favor of about:welcome; UA spoofing code commented out).
  - ConnectionSettingsUI → Platform_Core's ConnectionSettings (DIRECT/HTTP/SOCKS, host/port validation, local bypass, proxy auth).
  - ToolsSettingsUI + SearchEngineEditor → ToolsSettings search engine list via ItemListControl<SearchEngine>.
- **o.l.p.gui primitives:** FormPanel (lazy right-aligned label/editor rows), FormField (TEXT/PASSWORD), ItemListControl<T> + ItemEditorDialog<T> + AbstractItemEditor<T>, StringListControl + SimpleTextEditDialog, SearchDialog, SwingTasks (setNestedEnabled() tracking $nested.enabling client property to preserve inner disabled state, ancestor Frame/Dialog lookup).

## 4. Threading, Invariants & Known Quirks

- **Threads:**
  - Clientlets (HtmlClientlet, ImageClientlet, TextClientlet, DownloadClientlet) run on Platform_Core's RequestEngineThreadPool.
  - HtmlClientlet marshals htmlPanel.setDocument() + ccontext.setResultingContent() to EDT via SwingUtilities.invokeLater(); DownloadClientlet constructs and shows DownloadDialog directly on the worker thread.
  - DownloadDialog.startDownload() (when enabled) spawns daemon thread Download:<url> calling RequestEngine.inlineRequest() and marshals UI callbacks to EDT.
- **Known Quirks & Bugs:**
  1. **Intentionally disabled features:** DownloadDialog.SaveAction (DownloadDialog.java:387-393) pops a security notice instead of calling selectFile(); File > Open > File... is disabled (ComponentSource.java:186); GeneralSettings startup URLs are ignored in favor of about:welcome.
  2. **HtmlClientlet.NON_VISIBLE_ELEMENTS is never populated:** Empty HashSet (HtmlClientlet.java:69), so mayBeVisibleElement() returns true for every HTMLElement (including <head>/<meta>). getDefaultCharset() is dead code; missing charset defaults to "utf-8".
  3. **HtmlContent.getDescription() wrong attribute:** Reads element.getAttribute("description") instead of "content" on <meta name="description"> (HtmlContent.java:109).
  4. **TextClientlet hardcodes ISO-8859-1:** Ignores response.getCharset() (TextClientlet.java:48).
  5. **DataURLConnection parsing bugs:** removeSpaceCharacters() strips whitespace *before* checking base64 (DataURLConnection.java:79), removing spaces/tabs/newlines from non-base64 data: payloads; decodeUrl() maps + to ' '; getHeaderField(int n) has no bounds check (throws ArrayIndexOutOfBoundsException) and getHeaderFieldKey(int) is not overridden; data.Handler.parseURL() hardcodes port 80.
  6. **AboutURLConnection quirks:** welcome and confirmSearch use case-sensitive equals() (unlike blank/bookmarks/bookmark-search); ScoredEntry.compareTo() narrows timestamp subtraction to int (AboutURLConnection.java:385).
  7. **IconFactory never populates its cache:** getIcon() checks iconMap but never calls iconMap.put(...) (IconFactory.java:49-65), reloading icon bytes from classpath on every call.
  8. **Unregistered EnableableActions & unused legacy classes:** ActionPool.clonedWindowAction and copyAction are never added to enableableActions (ActionPool.java:94-103, and Edit menu uses DefaultEditorKit.CopyAction). Unused classes: ArchiveClassLoader, ArchiveCollection, ArchiveInfo, DomViewerWindow, CheckBoxPanel, plus unmounted ComponentSource.getExtensionsMenu() / empty ActionPool.ListExtensionsAction.
  9. **BaseHistory issues:** getExistingInfo() is unsynchronized (BaseHistory.java:188); getRecentHostEntries(max) adds to hosts before checking hosts.size() >= max, returning at most max - 1 items (BaseHistory.java:93-97); HistoryEntry.getTimetstamp() has an extra t.
  10. **Grinder paint readiness:** HtmlContent delegates isReadyToPaint() and disableRenderHints() to HtmlPanel; ImageContent and TextClientlet's SimpleComponentContent inherit default isReadyToPaint() == true (does not wait for async Toolkit.createImage() decode).

## 5. Task-to-File Quick Index

| If working on… | Start at (src/Primary_Extension/org/lobobrowser/) |
|---|---|
| Extension registration, error pages, lifecycle | primary/ext/ExtensionImpl.java, ../gngr-extension.properties |
| Clientlet MIME/extension routing | primary/clientlets/PrimaryClientletSelector.java |
| HTML parsing, <meta http-equiv> charset/refresh | primary/clientlets/html/HtmlClientlet.java, HtmlContent.java |
| HTML_Renderer callbacks, context menu, iframes | primary/clientlets/html/HtmlRendererContextImpl.java, BrowserFrameImpl.java |
| Standalone image, text, or download clientlets | primary/clientlets/img/ImageClientlet.java, ImageScrollable.java, primary/clientlets/TextClientlet.java, primary/clientlets/download/DownloadClientlet.java, primary/gui/download/DownloadDialog.java |
| Menus, toolbar, address bar, status/progress bar | primary/ext/ComponentSource.java, ActionPool.java, AddressField.java, ProgressBar.java, DirectorySource.java, IconFactory.java |
| Page source & console viewer windows | primary/ext/TextViewerWindow.java |
| Navigation history & bookmarks storage/UI | primary/ext/BaseHistory.java, NavigationHistory.java, BookmarksHistory.java, BookmarkInfo.java, AddBookmarkDialog.java |
| about: and data: URL schemes | primary/ext/PrimaryStreamHandlerFactory.java, protocol/about/AboutURLConnection.java, protocol/data/DataURLConnection.java, protocol/data/Handler.java |
| Search engines & ToolsSettings persistence | primary/settings/ToolsSettings.java, SearchEngine.java |
| Preferences dialog & settings panels | primary/gui/prefs/PreferencesDialog.java, PreferencesTree.java, GeneralSettingsUI.java, ConnectionSettingsUI.java, ToolsSettingsUI.java, SearchEngineEditor.java |
| Reusable Swing forms, lists & dialogs | primary/gui/FormPanel.java, FormField.java, ItemListControl.java, ItemEditorDialog.java, StringListControl.java, SwingTasks.java |
