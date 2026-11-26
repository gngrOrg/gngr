# Guidelines
* Don't attempt to access any files or processes outside this repository. If necessary, explain the reason and plan, and ask before going ahead.
* Don't try to disassemble binaries or jar files, unless explicitly commanded to do so
* Prefer running standalone or one-off Java source files directly with `java <file.java>` (supported in modern JDKs) instead of compiling them separately with `javac`.


# Documentation

Consult the relevant doc in docs/ before exploring a module or running UI/rendering tests:

- [docs/Common.md](docs/Common.md) — src/Common/: thread pools, caches, event dispatch, URL/string/DOM helpers, Swing color/font/layout utilities, I/O streams.
- [docs/Platform_Public_API.md](docs/Platform_Public_API.md) — src/Platform_Public_API/: zero-dependency public contracts (ua, clientlet, io) shared across runtime, extensions, renderer, and harness.
- [docs/Platform_Core.md](docs/Platform_Core.md) — src/Platform_Core/: startup/reuse/Grinder server, OkHttp networking/caching/cookies, extension loader, per-origin request firewall & JVM security policy, H2/jOOQ storage, Swing frame/window shell.
- [docs/HTML_Renderer.md](docs/HTML_Renderer.md) — src/HTML_Renderer/: HTML/XHTML parser, DOM, jStyleParser CSS cascade & RenderState, box/inline/float/table layout & Swing painting, Rhino JS runtime & bridge.
- [docs/Primary_Extension.md](docs/Primary_Extension.md) — src/Primary_Extension/: browser chrome/menus/address bar, HTML/image/text/download clientlets, about:/data: protocols, history/bookmarks, search engines, preferences UI.
- [docs/Render_Harness.md](docs/Render_Harness.md) — src/Render_Harness/: standalone offscreen Grinder rendering harness (run-harness) for fast CSS/layout tests without chrome, firewall, or JS.
- [docs/XAMJ_Build.md](docs/XAMJ_Build.md) — src/XAMJ_Build/: shared third-party JARs (JDT annotations, ext/*.jar), -Dext.dirs classloading, LocalSecurityPolicy version-sync notes, legacy Lobo packaging.
- [docs/render-n-ui-tests.md](docs/render-n-ui-tests.md) — Running one-off GUI and rendering tests under Xvfb with Grinder (misc/GrinderTestClient.java, misc/runTest.sh).
