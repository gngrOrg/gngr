# Common Design Overview

Scope: src/Common/ (51 Java files, ~5.4k LOC; shared foundational utility library for concurrency, caches, events, URLs, strings, DOM/HTML helpers, AWT/Swing rendering/layouts, classloading, and stream/network I/O).
Prefix shorthand: o.l.u. = org.lobobrowser.util..
Build dependencies: JDK 1.8 (java.*, javax.swing.*, org.w3c.dom.*) + org.eclipse.jdt.annotation (@NonNull/@Nullable). **Zero internal module dependencies.**

## 1. Package & Resource Map

| Package | Files | LOC | Role |
|---|---:|---:|---|
| o.l.util | 40 | ~3.9k | Concurrency (SimpleThreadPool, SimpleThreadPoolTask, JoinableTask, Threads), caching/maps (LRUCache, WeakValueHashMap, Items, History), event dispatch (EventDispatch, EventDispatch2, GenericEventListener, GenericPropertyChangeListener, RemovalEvent, RemovalListener, InputProgressEvent), URLs/strings/HTML/DOM (Urls, Strings, Html, Nodes, DOMExceptions, Domains [stub]), classloading/security/reflection (BaseClassLoader, MultiplexClassLoader, SecurityUtil, Bean, Objects), streams/wrappers/misc (MonitoredInputStream, EmptyInputStream, GenericURLConnection, ArrayUtilities, CollectionUtilities, ID, OS, Timing, Diagnostics, BoxedObject, NameValuePair, ObjectFilter, WrapperException, NotImplementedYetException) |
| o.l.util.gui | 5 | ~1.0k | AWT/Swing colors, fonts, drawing & layouts: ColorFactory, FontFactory, GUITasks, CenterLayout, WrapperLayout |
| o.l.util.io | 6 | ~0.6k | Stream/file/network helpers: IORoutines, RecordedInputStream, BufferExceededException, EmptyReader, Files, NetRoutines |

## 2. Cross-Module Consumers

```
Common (0 internal deps)
  ├──► Platform_Core      uses SimpleThreadPool, LRUCache, Urls, EventDispatch*, IORoutines,
  │                       RecordedInputStream, MonitoredInputStream, ID, BaseClassLoader,
  │                       MultiplexClassLoader, SecurityUtil, NetRoutines, CollectionUtilities
  ├──► HTML_Renderer      uses ColorFactory, FontFactory, GUITasks, Urls, Strings, Nodes,
  │                       DOMExceptions, WeakValueHashMap, Bean, Objects, ArrayUtilities,
  │                       CollectionUtilities, IORoutines, RecordedInputStream
  └──► Primary_Extension  uses Strings, Html, Urls, IORoutines, Files, OS, GUITasks,
                          CenterLayout, WrapperLayout, Items
```
*(Neither Platform_Public_API nor Render_Harness depends on Common.)*

## 3. Subsystem Details

### 3.1 Concurrency & Thread Pools (o.l.util)
- **SimpleThreadPool(name, minShrinkToThreads, maxThreads, idleAliveMillis):** Daemon worker pool managing a FIFO LinkedList<SimpleThreadPoolTask> (taskList) and active HashSet<SimpleThreadPoolTask> (runningSet) under taskMonitor. Spawns threads lazily up to maxThreads; excess threads above minThreads exit after idleAliveMillis. Temporarily renames executing thread to <baseName>:<task.toString()>. cancel(task) removes from taskList and calls task.cancel(); cancelAll() clears taskList and calls cancel() on all entries in runningSet (cooperative — no thread interrupts).
- **JoinableTask (implements SimpleThreadPoolTask):** Wraps abstract execute() with a done monitor flag; supports join() (wait() loop), forceDone(), and cancel() (forceDone()).
- **Threads:** dumpStack(maxStacks) logging helper and sleep(ms).

### 3.2 Caches, Maps & History (o.l.util)
- **LRUCache (Serializable):** Thread-safe approximate-byte-budget LRU cache backed by an access-ordered LinkedHashMap<Object, CacheEntry> guarded by an internal lock. Putting an entry larger than approxMaxSize removes any existing stale entry for that key and drops the oversized value; shrinking approxMaxSize via setApproxMaxSize() immediately evicts excess LRU entries. Evictions, removals, and value-changing overwrites fire RemovalEvent to RemovalListeners outside the internal lock through a transient EventDispatch2 (restored along with the lock in readObject()).
- **WeakValueHashMap<K, V> (Map):** Wraps HashMap<K, LocalWeakReference> + ReferenceQueue<V>. checkQueue() polls GC'd references and removes their keys during get, put, putAll, remove, clear, and values. Rejects null values; not thread-safe.
- **Items:** Static synchronized WeakHashMap<Object, Map<String, Object>> associating arbitrary named metadata with host objects without retaining the host object.
- **History (Serializable, marked TODO: Looks like it is not used):** Transient navigation sequence list (back()/forward()) paired with serialized recency (TreeSet<TimedEntry>) and lexicographic (TreeSet<String>) sets for prefix search (getHeadMatchItems).

### 3.3 Event Dispatch (o.l.util)
- **EventDispatch:** Synchronized LinkedList<GenericEventListener> storage; fireEvent(EventObject) snapshots listeners to GenericEventListener[] under synchronized (this) and invokes processEvent() outside any lock.
- **EventDispatch2:** Abstract typed dispatcher using ArrayList<EventListener>; snapshots to array under lock and calls abstract dispatchEvent(listener, event) outside the lock. fireEvent() returns boolean (false when empty).
- **GenericPropertyChangeListener:** Adapts java.beans.PropertyChangeListener to GenericEventListener.

### 3.4 URLs, Strings, HTML & DOM Helpers (o.l.util)
- **Urls:** URL inspection/normalization and HTTP header parsing:
  - isLocalFile(url) (file scheme without host) and isLocal(url) (isLocalFile or recursive jar:<subUrl>!/...).
  - createURL(base, rel), getNoRefForm(url), sameNoRefURL(u1, u2), getPort(url) (explicit or getDefaultPort()).
  - getHeaders(conn) reads indexed headers (n = 0..); getCharset(conn) extracts Content-Type charset= (unquoted), defaulting to file.encoding for local files or "ISO-8859-1".
  - getExpiration(conn, baseTime) checks Cache-Control (must-revalidate → 0, max-age → baseTime + s*1000) → Expires (synchronized PATTERN_RFC1123 or int seconds) → fallback etag == null ? 0 : baseTime + 60_000.
  - encodeIllegalCharacters(url) (replaces spaces with "%20") and removeControlCharacters(url) (strips chars < 32).
- **Strings:** Version comparison (compareVersions), blank/line/char utilities (isBlank, countLines, countChars, truncate, split, getCRLFString), Java identifiers/literals (isJavaIdentifier, getJavaIdentifier, getJavaStringLiteral), hashing (getMD5, getHash32, getHash64), unquoting (unquote, unquoteSingle), HTML/token encoding (strictHtmlEncode, trimForAlphaNumDash).
- **Html:** textToHTML(text) converts plain text to HTML-escaped markup (&, <, >, ", and \r/\n/\r\n → <br>).
- **Nodes:** DOM Node/NodeList helpers (getCommonAncestor, isSameOrAncestorOf, makeIterable, makeIterableElements, forEachNode).
- **DOMExceptions.ExtendedError:** Enum mapping HTML5/DOM4 codes 18..25 (SecurityError, NetworkError, AbortError, URLMismatchError, QuotaExceededError, TimeoutError, InvalidNodeTypeError, DataCloneError) to DOMException factories.
- **Domains:** Empty stub (methods commented out with @deprecated note pointing to Platform_Core's DomainValidation).

### 3.5 Classloading, Security, Reflection & Collections (o.l.util)
- **BaseClassLoader (SecureClassLoader) & MultiplexClassLoader:** Multi-parent classloader searching parentLoaders sequentially in loadClass() and getResource() (falling back to findSystemClass when empty, then findClass).
- **SecurityUtil.doPrivileged(PrivilegedAction):** Fast-path bypasses AccessController.doPrivileged() when System.getSecurityManager() == null.
- **Bean & Objects:** Bean caches Introspector PropertyDescriptors and walks dotted names via setPropertyForFQN(); Objects provides primitive/box-aware parameter matching (isBoxClass, areSameTo, isSameOrBox, areAssignableTo, isAssignableOrBox) for script-to-Java dispatch.
- **ArrayUtilities & CollectionUtilities:** Typed array conversion (copy, copySynched, contains, slice iterator) and lazy iterators/views (iteratorUnion, ListReverser, reverseIterator, FilterIterator, singletonIterator, emptyIterator, getIteratorEnumeration).
- **Misc:** ID (generateLong/generateInt, getMD5Bytes, getHexString, random), OS (isWindows, launchPath, launchBrowser), Timing (round1, getElapsedText), Diagnostics.Assert, BoxedObject, NameValuePair, ObjectFilter, WrapperException, NotImplementedYetException.

### 3.6 GUI Rendering & Layout Helpers (o.l.util.gui)
- **ColorFactory (singleton):** Parses 148 CSS named colors + transparent, #rgb/#rrggbb/#rrggbbaa hex, rgb(r,g,b), and rgba(r,g,b,a); memoizes parsed Colors in a synchronized HashMap<String, Color>.
- **FontFactory (singleton):** Snapshots GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames() at initialization; supports runtime font loading (registerFont(name, format, stream)/unregisterFont). Resolves comma-separated font-family (registeredFonts → system fontFamilies checked via canDisplayUpTo() against target Locales → "monospace" → fallback defaultFontName = "SansSerif"), applies bold/italic/size/TextAttribute.SUPERSCRIPT, and caches in fontMap keyed by FontKey (interned strings + identity comparison).
- **GUITasks:** getTopFrame(), drawDashed() (axis-aligned only), drawDotted() (BasicStroke round/butt dot pattern), addEscapeListener(JDialog).
- **CenterLayout & WrapperLayout:** Singleton LayoutManagers that center or stretch a single child component within container insets.

### 3.7 I/O & Network Helpers (o.l.util.io + stream classes in o.l.util)
- **IORoutines:** Doubling-buffer loaders (load, loadExact, loadAsText), file helpers (save, equalContent, touch, saveStrings, loadStrings), unbuffered readLine(InputStream), and gzip/deflate stream wrappers (getDecodedStream, getDecodedErrorStream).
- **RecordedInputStream:** Wraps an InputStream, teeing bytes into a ByteArrayOutputStream up to maxBufferSize so callers can mark()/reset(), consumeToEOF(), and later inspect buffered content via getBytesRead() or getString(encoding) (throws BufferExceededException if exceeded).
- **MonitoredInputStream (o.l.util):** Emits throttled InputProgressEvents on evtProgress (EventDispatch) every minProgressEventGap ms during reads.
- **EmptyInputStream, EmptyReader, GenericURLConnection** (wraps an InputStream + URL; getHeaderField* returns null).
- **Files (getContentType handles .txt/.html/.htm with file.encoding fallback application/octet-stream, joinPaths) & NetRoutines.isLocalAddress(host)** (checks InetAddress anyLocal/linkLocal/loopback/siteLocal).

## 4. Threading, Invariants & Known Quirks

1. **WeakValueHashMap stale metadata & unsupported ops:** size(), isEmpty(), containsKey(), and keySet() do not call checkQueue(), so GC'd keys stay counted/visible until a mutating method or get()/values() drains ReferenceQueue. Rejects null values; containsValue() and entrySet() throw UnsupportedOperationException. Not thread-safe.
2. **ColorFactory edge cases:** isColor() ignores rgba( (checks #, rgb(, colorMap only); unknown specs in getColor() return Color.RED; malformed rgba(...) stores null in colorMap; colorMap grows unbounded.
3. **FontFactory edge cases:** Static initializer queries GraphicsEnvironment (requires Xvfb/display when not headless); the "monospace" branch in createFont_Impl() has no break so trailing font-family tokens overwrite it; only "bold"/"bolder" weights are handled (numeric weights ignored); fontVariant is unused; fontMap grows unbounded.
4. **Urls.getExpiration() return contract:** Always returns Long (0 when no Cache-Control/Expires/Etag, or baseTime + 60_000 when Etag is present), never null despite Javadoc. Urls.encodeIllegalCharacters() only escapes spaces (%20).
5. **ID randomness & hex formatting:** Uses three seeded java.util.Random instances (not cryptographically secure); ID.getHexString() strips leading zero bytes via BigInteger.toString(16) (commented as frozen for cache-key compatibility).
6. **Legacy/unused classes:** Domains.java is empty (use Platform_Core's DomainValidation); History.java is marked unused; o.l.util.OS is separate from Platform_Core's o.l.main.OS (BrowserPanel/DefaultWindowFactory).
7. **SimpleThreadPool lock scope:** cancelAll() calls task.cancel() while holding taskMonitor; threadGroup is hardcoded null (SimpleThreadPool.java:38).
8. **Lock-free event dispatch:** EventDispatch and EventDispatch2 copy listener collections inside synchronized (this) before invoking listeners outside the lock.

## 5. Task-to-File Quick Index

| If working on… | Start at (src/Common/org/lobobrowser/util/) |
|---|---|
| Background thread pools & joinable tasks | SimpleThreadPool.java, SimpleThreadPoolTask.java, JoinableTask.java, Threads.java |
| Byte-bounded LRU cache & weak-value map | LRUCache.java, WeakValueHashMap.java, Items.java, RemovalEvent.java, RemovalListener.java |
| Listener/event dispatch base classes | EventDispatch.java, EventDispatch2.java, GenericEventListener.java, GenericPropertyChangeListener.java |
| URL normalization, cache expiry & charset parsing | Urls.java, io/NetRoutines.java, GenericURLConnection.java |
| CSS color & font resolution/caching | gui/ColorFactory.java, gui/FontFactory.java |
| Swing layouts, dashed/dotted strokes, dialog ESC | gui/CenterLayout.java, gui/WrapperLayout.java, gui/GUITasks.java |
| Stream reading, replay buffering, progress, gzip | io/IORoutines.java, io/RecordedInputStream.java, io/BufferExceededException.java, MonitoredInputStream.java |
| DOM traversal & HTML5 DOMException codes | Nodes.java, DOMExceptions.java, Html.java |
| String/version helpers, hashing & ID generation | Strings.java, ID.java, Timing.java, NameValuePair.java |
| Multi-parent classloading, reflection & privileges | BaseClassLoader.java, MultiplexClassLoader.java, Bean.java, Objects.java, SecurityUtil.java |
| Array/collection helpers, files & OS utilities | ArrayUtilities.java, CollectionUtilities.java, io/Files.java, OS.java, Diagnostics.java |
