# HTML_Renderer Design Overview

Scope: src/HTML_Renderer/org/ (~233 Java files, ~50k LOC; Cobra/Lobo-derived HTML/CSS/DOM/JS/Swing engine used by Primary_Extension and Render_Harness).
Prefix shorthand: o.l. = org.lobobrowser., o.l.h. = org.lobobrowser.html.. (org/w3c/COPYRIGHT.html is license notice only.)

## 1. Package Map

| Package | Files | Role |
|---|---:|---|
| o.l.h | 6 | Host embedding contracts: HtmlRendererContext, AbstractHtmlRendererContext, BrowserFrame, FormInput, HtmlObject, ReadyStateChangeListener |
| o.l.h.parser | 6 | HTML/XHTML parser: DocumentBuilderImpl, HtmlParser, ElementInfo, InputSourceImpl, LocatorImpl, StopException |
| o.l.h.io | 1 | WritableLineReader (LineNumberReader with prepended write buffer for document.write()) |
| o.l.h.domimpl | 84 | W3C DOM + HTML elements, document lifecycle/jobs, image dedup cache, style-sheet manager, notifications |
| o.l.h.style | 26 | CSS cascade bridge (jStyleParser), computed/local JStyleProperties, RenderState chain, unit/value parsers |
| o.l.h.renderer | 75 | Box/line/float/positioned/table layout, painting, Swing form/img/frame UIControls, HtmlController input dispatch |
| o.l.h.gui | 6 | Swing host components: HtmlPanel, HtmlBlockPanel, FrameSetPanel, DocumentNotification, selection events |
| o.l.h.js | 18 | Browser JS runtime: Window (+ JSScheduler), Executor, EventTargetManager, Event, XMLHttpRequest, JSURL (URL), URLSearchParams, Location, History, Navigator, Screen, Storage, Rhino security controllers |
| o.l.js | 11 | Generic Java↔Rhino reflection bridge: JavaScript, JavaObjectWrapper, JavaClassWrapper, JavaFunctionObject, JavaConstructorObject, ScriptableDelegate, @HideFromJS |

**External dependencies:**
- Internal modules: o.l.ua.{UserAgentContext,NetworkRequest,ImageResponse}, o.l.util.*, o.l.main.DefferedLayoutSupport, o.l.request.DomainValidation, co.uproot.css.domimpl.{JStyleSheetWrapper,StyleSheetBridge}
- Third-party: W3C DOM (org.w3c.dom.*), jStyleParser (cz.vutbr.web.css.*), Mozilla Rhino (org.mozilla.javascript.*), Java AWT/Swing (java.awt.*, javax.swing.*)

## 2. End-to-End Pipeline

```
Host (HtmlClientlet / HarnessServer)
  │ DocumentBuilderImpl.createDocument(InputSourceImpl, contentType)
  ▼
HTMLDocumentImpl (layoutBlocked=true) ◄── Window (per HtmlRendererContext)
  │ document.load() ──► HtmlParser.parse(WritableLineReader)
  │   ├─ Builds DOM via ElementFactory / HTMLElementBuilder
  │   └─ Element attachment queues jobs via doc.addJob(fn, layoutBlocker):
  │        <link rel=stylesheet>: layoutBlocker=true
  │        <script>, <img>, <iframe>: layoutBlocker=false
  │
  │ Host calls doc.finishModifications()
  │   ├─ StyleElements.normalizeHTMLTree(doc)
  │   ├─ Background thread runs queued jobs; scripts submit to Window.JSScheduler
  │   ├─ layoutBlockingJobs == 0 ─► layoutBlocked=false, doc.allInvalidated()
  │   └─ registeredJobs == 0     ─► dispatchLoadEvent() (DOMContentLoaded),
  │                                  rcontext.jobsFinished(), window.jobsFinished()
  ▼
DocumentNotificationListener ──► HtmlPanel (coalesces on EDT: 10ms Timer / invokeLater)
  │
  ├─ FRAMESET doc ─► FrameSetPanel (nested JSplitPanes + BrowserFrame)
  └─ Normal doc   ─► HtmlBlockPanel (Swing JComponent, root RBlock)
       ├─ doLayout(): returns early if doc.layoutBlocked; else doc.primeNodeData()
       │              → rblock.layout(...) → updateWidgetBounds() → updateGUIComponents()
       │              → layoutCompleted.complete(true)
       ├─ paint():    paints root RBlock + selection (paintChildren is NOP)
       └─ Mouse/Key ─► RBlock hit-test ─► HtmlController ─► JS handlers / rcontext / nav
```

## 3. Subsystem Details

### 3.1 Host Embedding (o.l.h)
- HtmlRendererContext: callback interface from renderer/JS to browser chrome (navigation, linkClicked, submitForm, createBrowserFrame, getHtmlObject, mouse hooks, dialogs alert/confirm/prompt, scrolling, history, cursor, jobsFinished).
- UserAgentContext (in Platform_Public_API): network (createHttpRequest), policy (isRequestPermitted, isScriptingEnabled, isInternalCSSEnabled, isExternalCSSEnabled, isMedia), cookies, UA metadata.

### 3.2 Parser & I/O (o.l.h.parser, o.l.h.io)
- DocumentBuilderImpl extends javax.xml.parsers.DocumentBuilder: createDocument() wraps stream/reader in WritableLineReader and constructs HTMLDocumentImpl without parsing (for incremental setup); parse() creates and calls document.load().
- HtmlParser: recursive-descent parser driven by static ELEMENT_INFOS (ElementInfo specifies END_ELEMENT_REQUIRED|OPTIONAL|FORBIDDEN, childElementOk, stopTags, noScriptElement).
  - Optional end tags unwind recursion by throwing StopException(newElement) up to the matching ancestor frame.
  - Sets UserData MODIFYING_KEY = "cobra.suspend" (TRUE→FALSE) around element construction.
  - Supports HTML (tag uppercasing, auto-inserts HEAD/BODY, QUIRKS_MODE = true hardcoded) and XML/XHTML (doc.isXML() via XHTML doctype or application/xhtml+xml).
  - WritableLineReader prepends document.write()/writeln() text ahead of the underlying Reader.

### 3.3 DOM & Document Lifecycle (o.l.h.domimpl)
**Hierarchy:**
```
AbstractScriptableDelegate
 └─ NodeImpl (Node, ModelNode; holds treeLock, uiNode, nodeList, querySelectorAll)
     ├─ HTMLDocumentImpl (HTMLDocument, DocumentView, DocumentStyle, EventTarget)
     ├─ ElementImpl (Element, EventTarget; attributes map, id-map updates)
     │   ├─ DocumentFragmentImpl
     │   └─ HTMLElementImpl (HTMLElement, CSS2PropertiesContext; style cache, :hover, before/after)
     │       ├─ GeneratedElement (::before / ::after synthetic element)
     │       ├─ HTMLAbstractUIElement (on* event handler attributes)
     │       │   ├─ HTMLBaseInputElement ─► HTMLInput/Button/Select/TextAreaElementImpl
     │       │   ├─ HTMLBody/Div/P/Pre/Span/Heading/Form/Table/TableCell/TableHead/Image/Link/IFrame/Canvas/Object/Applet/UList/OList/LI/HR/Font/BaseFont/Center/GenericMarkup...
     │       └─ HTMLHtml/Base/Title/BR/Frame/FrameSet/Option/Quote/Script/Style/TableRow/NonStandard...
     ├─ CharacterDataImpl ─► TextImpl (─► CDataSectionImpl), CommentImpl
     ├─ AttrImpl, DocumentTypeImpl, AnonymousNodeImpl, HTMLProcessingInstruction
```
- ElementFactory + HTMLElementBuilder: maps ~45 uppercase tag names to subclasses; unknown tags become plain HTMLElementImpl. Note: "A", "ANCHOR", and "LINK" all build HTMLLinkElementImpl.
- HTMLDocumentImpl:
  - elementsById: WeakValueHashMap<String, Element> (first-wins crude doc-order check).
  - StyleSheetManager: scans DOM for LinkStyle (<style>, <link>), bridges via StyleSheetBridge to JStyleSheetWrapper, invalidates cached classifiedRules + triggers allInvalidated(true).
  - imageInfos: deduplicates concurrent async image loads (NetworkRequest, RequestKind.Image) per URL and fans out ImageEvent to ImageListeners.
  - Job queue (addJob, markJobsFinished, finishModifications, stopEverything): coordinates deferred resource loading; post-finishModifications jobs dispatch via window.addJSUniqueTask(...).
  - Notifications (DocumentNotificationListener): invalidated, sizeInvalidated, lookInvalidated, positionInvalidated, structureInvalidated, nodeLoaded, allInvalidated, externalScriptLoading.
- HTMLElementImpl:
  - Lazily computes & caches OrderedRule[] cachedRules, jStyleParser NodeData cachedNodeData, JStyleProperties currentStyle, and GeneratedElements (beforeNode/afterNode when content is set).
  - Tracks :hover via static MatchConditionOnElements elementMatchCondition; cachedHasHoverRule short-circuits descendant invalidation on mouseover.
  - Attribute changes call forgetStyle(true) + informInvalidRecursive().
- NodeImpl.querySelectorAll(query) parses query + " { display: none}" through jStyleParser and matches descendants (getElementsByClassName/getElementsByTagName delegate to it).
- HTMLCanvasElementImpl + CanvasPath2D: Java2D-backed 2D canvas context.

### 3.4 CSS & Style (o.l.h.style)
- **Cascade & Parsing:** powered by jStyleParser (cz.vutbr.web.css.*).
  - CSSNorm provides UA stylesheets (stdStyleSheet(), userStyleSheet()), parsed once in HTMLDocumentImpl (HTML + XML variants).
  - HTMLDocumentImpl.getClassifiedRules() merges UA sheets + StyleSheetManager.getEnabledJStyleSheets() via AnalyzerUtil.getClassifiedRules(sheets, MediaSpec("screen")).
  - Per element, HTMLElementImpl.getNodeData() appends synthetic attribute styles (StyleElements.convertAttributesToStyles() for TABLE/FONT/CANVAS/IMG) and inline style="" (CSSUtilities.jParseInlineStyle()), evaluates AnalyzerUtil.getApplicableRules() + getElementStyle(), then inherits & concretizes from parent NodeData.
  - CSSUtilities: fetches external CSS synchronously (in job thread) via NetworkRequest with SafeNetworkProcessor for @import, and parses via CSSParserFactory.
- **Style Properties:** JStyleProperties (implements CSS2Properties) has two concrete subclasses:
  - ComputedJStyleProperties: read-only view over jStyleParser NodeData (exposed internally as getCurrentStyle(), and to JS via window.getComputedStyle()).
  - LocalJStyleProperties: read/write view over the element's style attribute (element.style).
- **RenderState:** RenderState caches resolved AWT/layout values (Font, FontMetrics, Color, display, position, float, clear, overflow, visibility, whiteSpace, marginInsets, paddingInsets, borderInfo, backgroundInfo, WordInfo map, counters, cursor).
  - StyleSheetRenderState reads element.getCurrentStyle() and delegates inherited values to prevRenderState. Subclasses override defaults/attributes: BlockRenderState, BodyRenderState, TableRenderState, TableCellRenderState, ImageRenderState, IFrameRenderState; RenderStateDelegator (BaseFontRenderState) wraps a parent state.
  - HtmlValues parses CSS lengths (px, em, ex, pt, %, etc.), fonts, borders, backgrounds, insets (HtmlInsets), and HtmlLength (pixel/percent/multi-length * for framesets/tables).
  - RenderThreadState: ThreadLocal carrying overrideNoWrap during shrink-to-fit sizing passes.

### 3.5 Layout, Rendering & Input (o.l.h.renderer)
**Renderable Hierarchy:**
```
Renderable
 ├─ RStyleChanger (in-line Graphics/RenderState switch; non-boundable), RFloatInfo
 ├─ PositionedRenderable (wrapper with zIndex + ordinal for SortedSet painting/hit-test)
 └─ BoundableRenderable (BaseBoundableRenderable: bounds, origin, parent, hit-test, selection)
     ├─ Leaves: RWord (─► RTab), RBlank, RSpacing
     ├─ TranslatedRenderable (delegator for position:relative offset)
     └─ RCollection (BaseRCollection: child iteration, deep invalidation, mouse dispatch)
         ├─ RBlockViewport (block formatting context & scrollable canvas)
         ├─ RLine (single inline line box; baseline/vertical-align; throws OverflowException)
         └─ RElement (+ UINode) ─► BaseElementRenderable (+ RenderableContainer, ImageObserver)
             ├─ RInlineBlock
             ├─ RUIControl (─► RImgControl): wraps Swing UIControl
             └─ BaseBlockyRenderable
                 ├─ RBlock (margins/borders/padding/scrollbars; owns 1 RBlockViewport)
                 │   ├─ BaseRListElement ─► RList, RListItem
                 │   └─ RAbstractCell ─► RTableCell, RAnonTableCell
                 └─ RTable (owns TableMatrix + LayoutKey cache)
```
- **DOM↔Render link:** Block/replaced elements attach their RElement to NodeImpl.uiNode (setUINode). Inline nodes have no uiNode; NodeImpl.findUINode() walks ancestors to the nearest enclosing RElement.
- **Block layout (RBlock + RBlockViewport):**
  - RBlock.forceLayout() applies box style (applyStyle), computes declared/min/max width/height, runs optional pass-1 shrink-to-fit (RenderThreadState.overrideNoWrap = true when no declared width & !expandWidth), then lays out bodyLayout (RBlockViewport). If overflow-y: auto exceeds yLimit, RBlockViewport throws SizeExceededException, triggering a relayout with vertical scrollbar reserved.
  - RBlockViewport maintains:
    1. seqRenderables (ArrayList<BoundableRenderable>): in-flow RLines and RBlocks ordered by Y (binary-searchable via MarkupUtilities).
    2. positionedRenderables (TreeSet<PositionedRenderable> sorted by ZIndexComparator): floats and positioned descendants.
  - Element dispatch in RBlockViewport: static elementLayout map (MarkupLayout for BR, IMG, INPUT, TEXTAREA, SELECT, HR, OBJECT/APPLET/EMBED, IFRAME, CANVAS, NOSCRIPT, and NopLayout for SCRIPT/HEAD/TITLE/META/STYLE/LINK), falling back to CommonLayout which switches on RenderState.getDisplay().
  - Collapsible vertical margins tracked via currentCollapsibleMargin and getCollapsibleMarginTop/Bottom().
- **Inline layout (RLine):** words (RWord), spaces (RBlank), tabs (RTab), inline elements, RStyleChangers. Overflowing a line throws OverflowException(overflowRenderables); RBlockViewport starts a new RLine and re-adds them.
- **Floats:** FloatingBounds (FloatingViewportBounds, ShiftedFloatingBounds, CombinedFloatingBounds, ParentFloatingBoundsSource) constrain line X-margins and clear Y offsets. Floats overflowing a child viewport are published as ExportableFloats to ancestor viewports; isFloatLimit() decides whether floats expand container height.
- **Positioning (DelayedPair):** position: absolute|fixed|relative elements bubble up as DelayedPairs via RenderableContainer.addDelayedPair() to their containingBlock (RBlock or RTable), which positions them in positionDelayed()/importDelayedPair(). Relative elements wrap in TranslatedRenderable and mark the original isDelegated() so it paints only in its z-ordered slot.
- **Tables (RTable + TableMatrix):** TableMatrix builds RowGroup/Row/VirtualCell grid (handling colspan/rowspan and anonymous cells RAnonTableCell), measures column/row sizes, positions RTableCells, and paints backgrounds/borders.
- **Swing Widgets (UIControl):** RUIControl bridges UIControl (BaseControl → ImgControl, HrControl, BaseInputControl → InputText/Password/Button/Checkbox/Radio/File/Image/Select/TextAreaControl; BrowserFrameUIControl; UIControlWrapper). Components register up to HtmlBlockPanel via addComponent() and are synced via updateGUIComponents() (avoids focus loss on relayout).
- **Input (HtmlController):** singleton invoked by renderables on mouse/key events; updates immediate EDT UI state (`setMouseOver()` for CSS `:hover`, cursor, link press `overlayColor`) and delegates DOM events (inline `on*`, `addEventListener`, and post-dispatch default actions such as `HtmlRendererContext` hooks, `HTMLLinkElementImpl.navigate()`, and form submit/reset) to `EventTargetManager`.

### 3.6 Swing GUI Bridge (o.l.h.gui)
- HtmlPanel (JComponent, FrameContext, DefferedLayoutSupport):
  - Public entry: setDocument(Document, HtmlRendererContext) / setHtml(...) (marshals to EDT).
  - Detects top-level <frameset> and swaps between HtmlBlockPanel and FrameSetPanel (recursive JSplitPane layout of BrowserFrames).
  - Subscribes to DocumentNotificationListener; batches DocumentNotifications (EDT: invokeLater, off-EDT: 10ms coalescing Swing Timer).
- HtmlBlockPanel (JComponent, NodeRenderer, RenderableContainer, ClipboardOwner):
  - Owns root RBlock; handles DocumentNotification (GENERIC/SIZE → invalidate up+deep + revalidatePanel(); POSITION → parent invalidate up; LOOK → invalidateRenderStyle() + repaint()).
  - doLayout(): skips if doc.layoutBlocked.get(), primes all element styles (doc.primeNodeData()), runs rblock.layout(...), updates widget bounds & Swing children, scrolls to URL #ref once, completes layoutCompleted CompletableFuture.
  - Overrides paint() (not paintComponent(), for scrollbar repainting) and makes paintChildren() a NOP (renderables paint child controls directly).
  - Manages text selection (startSelection/endSelection RenderableSpots), clipboard copy, and nested scrollable RBlock mouse-wheel dispatch.
  - isReadyToPaint(): !window.hasPendingTasks() && rblock.isReadyToPaint().

### 3.7 JavaScript Engine & Bridge (o.l.js, o.l.h.js)
- **Java↔Rhino Bridge (o.l.js):**
  - JavaScript.getJavascriptObject(raw, scope): passes through primitives/Strings/arrays/Scriptables; caches JavaObjectWrapper directly on ScriptableDelegate instances (most DOM/BOM objects extend AbstractScriptableDelegate), or uses a weak-key/weak-value map otherwise. Note: raw Java arrays (`T[]`) pass through as Java arrays rather than JS `Array`s, so methods returning JS arrays should construct them via `Context.newArray(scope, ...)`.
  - JavaClassWrapper (cached by JavaClassWrapperFactory): reflects public methods/fields once per class:
    - Blocks methods declared in java.* (so host classes needing `.toString()` in JS must override `public String toString()` directly) and methods annotated @HideFromJS.
    - getX/isX (0 args) and setX (1 arg, void) become JS properties unless annotated @NotGetterSetter; @PropertyName overrides JS name.
    - item(i)/setItem → integer indexer; namedItem(s)/setNamedItem → name indexer.
    - Overloaded methods group into JavaFunctionObject; constructors use JavaConstructorObject + JavaInstantiator; public static final fields expose as constants. Note: `JavaConstructorObject` passes raw Rhino `Object[]` args to `JavaInstantiator` without calling `setScriptable()` on the delegate; wrapped host objects arrive as `JavaObjectWrapper` (whose Java `.toString()` returns `"JavaObjectWrapper[...]"`) so instantiators must convert string args via `ScriptRuntime.toString(arg)` and capture the window `Scriptable` scope explicitly when needed.
  - JavaScript.init() installs a throwing global ContextFactory so all execution must use a Window's ContextFactory.
- **Browser Runtime (o.l.h.js):**
  - Window: weakly cached per HtmlRendererContext (CONTEXT_WINDOWS). setDocument() runs onunload, calls clearState() (stops doc jobs, winds up JSScheduler, resets EventTargetManager, cancels timers, clears windowScope), initializes scope (initSafeStandardObjects, console, XMLHttpRequest, Path2D, Event, URL, URLSearchParams, localStorage/sessionStorage dummy Storage, element constructors Image/Script/IFrame/Option/Select/HTMLDivElement/Comment/Text), and starts JSScheduler.
  - Window.JSScheduler: dedicated thread draining a PriorityBlockingQueue<ScheduledTask> ordered by `creationTime` with a monotonic sequence tie-breaker for strict FIFO ordering (checks UserAgentContext.isRequestPermitted(..., JavaScript) and runs under captured AccessControlContext). Timers (setTimeout/setInterval) use Swing javax.swing.Timer whose ActionListener enqueues onto JSScheduler.
  - Window.MyContextFactory: Rhino VERSION_1_8, interpreter mode (optimizationLevel = -1), V8 extensions enabled, ClassShutter blocking java.* (except java.lang.Object and java.lang.*Exception), instruction observer every 100k ops throwing WindowClosingError on unload.
  - Executor: enters Window's ContextFactory, attaches PolicySecurityController (enforces non-null CodeSource per script), seals Context, invokes JS functions with event bound.
  - EventTargetManager: stores per-NodeImpl (IdentityHashMap) synchronized EventListener and Rhino Function lists; `dispatchEvent(node, evt, stopNode, defaultAction)` builds the target→ancestors propagation path and takes a fast path on the caller thread when no Java/JS/inline handlers apply (or JS is blocked), otherwise enqueues a `JSEventTask` on `Window.JSScheduler` (guarded against stale documents) that runs inline `on<type>` handlers (compiling lazily off-EDT; `return false` calls `preventDefault()`), `EventListener`s, and Rhino `Function` listeners per node along the path (`AT_TARGET` + `BUBBLING_PHASE`), sets `Event.currentTarget`, honors `stopPropagation()` and `stopImmediatePropagation()`, and invokes `defaultAction` afterwards iff `!evt.getDefaultPrevented()` (with Swing-bound actions like `onContextMenu` and `resetForm` marshaled to the EDT; capture phase & Window in path are TODO).
  - XMLHttpRequest: wraps UserAgentContext.createHttpRequest() (NetworkRequest), dispatches onreadystatechange onto JSScheduler.

## 4. Concurrency, Invariants & Known Quirks

- **Threads:**
  - *Parser thread* (host/clientlet): runs HtmlParser.parse(), mutates DOM, queues jobs.
  - *Job thread* (spawned in HTMLDocumentImpl.finishModifications()): fetches blocking CSS, external scripts, images, iframes.
  - *JSScheduler thread* (1 per Window): executes <script>, event listeners, timers, XHR callbacks sequentially.
  - *Swing EDT*: all HtmlPanel/HtmlBlockPanel notification handling, layout (doLayout), painting, UINode reads/writes, and HtmlController UI state (`:hover`, cursor, link overlay, `rcontext.onContextMenu`, `resetForm`); inline `on*` compilation & execution run on `JSScheduler`.
- **Lock ordering:** Always acquire NodeImpl.treeLock (the owning HTMLDocumentImpl) **before** element monitor synchronized (this) (see HTMLElementImpl.getNodeData()). Never invoke DocumentNotificationListener callbacks while holding locks.
- **Gotchas:**
  1. RBlock layout caching is currently disabled (value = null in RBlock.doLayout), whereas RTable caches LayoutKey→LayoutValue.
  2. HtmlBlockPanel overrides paint() and no-ops paintChildren(); Swing child widgets are painted by RUIControl / RBlock scrollbars.
  3. HTMLDocumentImpl and ElementImpl stub out W3C EventTarget (org.w3c.dom.events.EventListener) methods; JS addEventListener/dispatchEvent work via Rhino Function overloads in NodeImpl → EventTargetManager.
  4. HTMLLinkElementImpl is shared by <link> and <a> (ElementFactory).
  5. Window instances are reused across documents in the same HtmlRendererContext (reset via clearState()).
  6. HtmlBlockPanel.doLayout() is a no-op while doc.layoutBlocked is true (until finishModifications() and all layoutBlocker jobs finish).
  7. HTMLDocumentImpl sets `this.document = this`, so `document.getOwnerDocument()` returns `this` rather than `null` (non-standard W3C DOM, relied on by `NodeImpl.addEventListener()` and `Executor.executeFunction()` when dispatching events on `document`).

## 5. Task-to-File Quick Index

| If working on… | Start at (src/HTML_Renderer/org/lobobrowser/) |
|---|---|
| HTML parsing / tag closing / entities | html/parser/HtmlParser.java, ElementInfo.java, DocumentBuilderImpl.java |
| Adding an HTML tag or DOM API | html/domimpl/ElementFactory.java, HTMLElementBuilder.java, NodeImpl.java, HTMLElementImpl.java |
| Resource loading / DOMContentLoaded / layout blocking | html/domimpl/HTMLDocumentImpl.java (addJob, finishModifications, markJobsFinished), HTMLLinkElementImpl.java, HTMLScriptElementImpl.java |
| CSS parsing, cascade, UA defaults, :hover, ::before/::after | html/domimpl/HTMLElementImpl.java (getNodeData), HTMLDocumentImpl.java (StyleSheetManager), html/style/CSSUtilities.java, CSSNorm.java, StyleElements.java |
| CSS property resolution / units / fonts / borders | html/style/StyleSheetRenderState.java, ComputedJStyleProperties.java, LocalJStyleProperties.java, HtmlValues.java |
| Block / inline / float / positioning layout | html/renderer/RBlock.java, RBlockViewport.java, BaseElementRenderable.java, RLine.java, DelayedPair.java, FloatingViewportBounds.java |
| Table layout | html/renderer/RTable.java, TableMatrix.java, RTableCell.java, VirtualCell.java |
| Form controls, images, iframes, <hr> | html/renderer/RUIControl.java, BaseInputControl.java, Input*Control.java, ImgControl.java, BrowserFrameUIControl.java |
| Swing integration, scrolling, selection, repainting | html/gui/HtmlPanel.java, HtmlBlockPanel.java, FrameSetPanel.java |
| Click/hover/form events | html/renderer/HtmlController.java, html/js/EventTargetManager.java, Event.java |
| JS runtime, timers, XHR, Rhino sandbox | html/js/Window.java, Executor.java, PolicySecurityController.java, XMLHttpRequest.java, JSURL.java, URLSearchParams.java |
| Exposing/hiding Java methods to JS | js/JavaClassWrapper.java, JavaObjectWrapper.java, JavaScript.java, HideFromJS.java |
