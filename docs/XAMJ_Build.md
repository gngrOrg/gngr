# XAMJ_Build Design Overview

Scope: src/XAMJ_Build/ (0 Java files; shared third-party JAR dependency store for all modules + legacy Lobo 0.98.5 / IzPack packaging and documentation artifacts).
Active role: Supplies compile-time @NonNull/@Nullable annotations to every module and runtime CSS/JS extension JARs (ext/) loaded by Platform_Core's ExtensionManager (-Dext.dirs=../XAMJ_Build/ext) and Render_Harness.
Legacy role: Retains disabled Ant/IzPack installer scripts (build.xml.disabled, install.xml) and historical Lobo project docs; **not executed by src/build.xml**.

## 1. Active JAR Inventory & Module Consumers

| File (src/XAMJ_Build/) | Size | Library / Role | Compile / Runtime Consumers |
|---|---:|---|---|
| org.eclipse.jdt.annotation_2.0.100.v20150311-1658.jar | 17 KB | Eclipse JDT @NonNull / @Nullable annotations | **All 6 Java modules** (Common, Platform_Public_API, Platform_Core, HTML_Renderer, Primary_Extension, Render_Harness) |
| ext/js.jar | 1.2 MB | Mozilla Rhino JavaScript engine | HTML_Renderer (compile/runtime), Primary_Extension (SimpleDocument imports Scriptable), Render_Harness (runtime), Platform_Core (-Dext.dirs) |
| ext/jstyleparser-4.0.1.jar | 711 KB | jStyleParser CSS parser & cascade engine (cz.vutbr.web.css.*) | HTML_Renderer, Render_Harness, Platform_Core (-Dext.dirs) |
| ext/antlr4-runtime-4.13.2.jar | 319 KB | ANTLR 4 runtime (required by jStyleParser 4.x) | Runtime for HTML_Renderer via Platform_Core (-Dext.dirs) and Render_Harness (build.xml) |
| ext/unbescape-1.1.5.RELEASE.jar | 162 KB | Unbescape HTML/CSS/JS unescaping (used by jStyleParser / HTML_Renderer) | HTML_Renderer, Render_Harness, Platform_Core (-Dext.dirs) |
| ext/slf4j-api-1.7.25.jar | 41 KB | SLF4J API facade (used by jStyleParser) | Runtime for HTML_Renderer via Platform_Core (-Dext.dirs) and Render_Harness (build.xml) |
| ext/jstyleDomBridge.jar | 18 KB | co.uproot.css.domimpl.* (JStyleSheetWrapper, StyleSheetBridge) | HTML_Renderer, Render_Harness, Platform_Core (-Dext.dirs) |
| ext/sac.jar | 15 KB | W3C SAC (Simple API for CSS) interfaces | HTML_Renderer, Render_Harness, Platform_Core (-Dext.dirs) |

*(Note: Core runtime dependencies — okhttp-3.13.1, okhttp-urlconnection-3.13.1, okio-1.17.3, h2-1.4.196, jooq-3.4.2, javatuples-1.2, public-suffix-list — live in src/Platform_Core/lib/, not XAMJ_Build/.)*

## 2. How XAMJ_Build Integrates with Build & Runtime

```
Compile time (Ant & Eclipse):
  Common / Platform_Public_API / Platform_Core ──► XAMJ_Build/org.eclipse.jdt.annotation_*.jar
  HTML_Renderer / Primary_Extension / Render_Harness ──► annotations + XAMJ_Build/ext/*.jar

Runtime (Browser — Platform_Core/build.xml EntryPoint & misc/gngrDev.launch):
  java -Dext.dirs=../XAMJ_Build/ext -Dext.files=../HTML_Renderer/bin,../Primary_Extension/bin ...
    └─ ExtensionManager scans XAMJ_Build/ext/*.jar:
         • JARs lacking gngr-extension.properties join shared librariesCL (URLClassLoader)
         • librariesCL is parent ClassLoader of HTML_Renderer & Primary_Extension
    └─ LocalSecurityPolicy.getPermissions(CodeSource):
         • Grants per-JAR least-privilege permissions matched by exact filename suffix

Runtime (Render_Harness — Render_Harness/build.xml run):
  • Places all 8 XAMJ_Build JARs directly on JVM classpath (no ExtensionManager or SecurityManager)
```

## 3. Legacy Packaging & Documentation Artifacts

| File (src/XAMJ_Build/) | Status | Role |
|---|---|---|
| build.xml.disabled | Disabled | Legacy Lobo 0.98.5 Ant script (hardcoded /opt/XAMJ_Project, /opt/IzPack/, Java 1.4/1.5, removed modules cssparser/JWebContentExtension/Console/Detect_Ext/IzPack_RegistryPanel, AdSense Javadoc templates). Renamed .disabled so src/build.xml (includes="*/build.xml") skips it. |
| install.xml, process-panel.xml, default-shortcut-spec.xml, windows-shortcut-spec.xml | Unused | Legacy IzPack GUI installer config and Unix/Windows desktop shortcut specs for Lobo 0.98.5 (references legacy JavaFX 1.0 JARs and RegistryPanel.jar). |
| re_manifest.mf | Unused | Legacy executable JAR manifest for lobo.jar (Main-Class: org.lobobrowser.main.EntryPoint, Class-Path: lobo-pub.jar, Sealed: true). |
| .project, .externalToolBuilders/{Ant Builder,Installer Builder,API_Doc_Builder}.launch | Disabled | Legacy Eclipse external tool builders (all ATTR_BUILDER_ENABLED=false, hardcoded C:\opt\XAMJ_Project\XAMJ_Build\build.xml). |
| LICENSE.txt, README.txt, ACKNOWLEDGMENTS.txt, CONTRIBUTORS.txt, CHANGELOG.txt | Historical | GPLv2 license text, Lobo launch README, third-party acknowledgments (IzPack/Rhino/cssparser/JavaFX), Lobo contributors, and 1,869-line Lobo changelog through v0.98.5. |

## 4. Maintenance Invariants & Known Quirks

1. **Multi-file sync required when upgrading any JAR in XAMJ_Build/:**
   - Update src/HTML_Renderer/build.xml + .classpath, src/Primary_Extension/build.xml + .classpath, src/Render_Harness/build.xml + .classpath (and all 6 modules if updating org.eclipse.jdt.annotation_*.jar).
   - Update src/Platform_Core/org/lobobrowser/security/LocalSecurityPolicy.java (getPermissions()): hardcodes exact filename matches for slf4j-api-1.7.25.jar (line 375), jstyleparser-4.0.1.jar (line 378), sac.jar (line 433), and js.jar (line 435). A version bump without updating LocalSecurityPolicy silently strips permissions under a SecurityManager.
2. **Existing classpath drift across module build files:**
   - src/Primary_Extension/build.xml lines 45–46 reference **non-existent** jstyleparser-1.21-SNAPSHOT.jar and unbescape-1.1.0.RELEASE.jar (and omit antlr4-runtime and slf4j-api); compilation succeeds only because Primary_Extension does not directly import those packages.
   - src/HTML_Renderer/build.xml omits antlr4-runtime-4.13.2.jar and slf4j-api-1.7.25.jar (not needed at compile time; supplied at runtime by -Dext.dirs=../XAMJ_Build/ext in browser mode).
   - src/HTML_Renderer/.classpath still references **non-existent** antlr4-4.5.3.jar and jstyleparser-3.3-SNAPSHOT.jar and omits slf4j-api-1.7.25.jar.
   - src/Render_Harness/build.xml and .classpath are the only configs referencing all 8 current JAR filenames.
3. **ext/ is auto-loaded at browser runtime:** Any .jar placed in src/XAMJ_Build/ext/ is automatically scanned by ExtensionManager via -Dext.dirs=../XAMJ_Build/ext and added to librariesCL (or loaded as a NavigatorExtension if it contains gngr-extension.properties). Do not leave backup or duplicate version JARs in ext/.
4. **Permissions for unlisted ext/ JARs:** antlr4-runtime-4.13.2.jar, unbescape-1.1.5.RELEASE.jar, and jstyleDomBridge.jar have no dedicated branch in LocalSecurityPolicy.getPermissions(), so they receive no extra permissions under a SecurityManager.
5. **Do not rename build.xml.disabled to build.xml:** Root src/build.xml globs */build.xml and would attempt to run the broken legacy Lobo script.

## 5. Task-to-File Quick Index

| If working on… | Start at |
|---|---|
| Upgrading jStyleParser / Rhino / ANTLR / Unbescape / SLF4J | src/XAMJ_Build/ext/, src/HTML_Renderer/build.xml, src/Render_Harness/build.xml, src/Primary_Extension/build.xml, src/Platform_Core/org/lobobrowser/security/LocalSecurityPolicy.java |
| Upgrading @NonNull / @Nullable annotation JAR | src/XAMJ_Build/org.eclipse.jdt.annotation_*.jar + all 6 src/*/build.xml and .classpath files |
| Understanding how ext/ JARs are classloaded at runtime | src/Platform_Core/build.xml (EntryPoint -Dext.dirs), src/Platform_Core/org/lobobrowser/main/ExtensionManager.java |
| Inspecting legacy Lobo packaging / changelog history | src/XAMJ_Build/build.xml.disabled, install.xml, CHANGELOG.txt, ACKNOWLEDGMENTS.txt |
