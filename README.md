# Kotlinks

A small, fast URL-shortcut ("go-link") server written in Kotlin with [Ktor](https://ktor.io). It is a Kotlin/Ktor port of [golinks](https://github.com/ygllc/golinks), the lightweight Bun/TypeScript implementation of the same idea.

Define shortcuts in a TOML file, then open them from the browser or address bar:

```
http://go/mm/gh   ->  https://github.com/ygllc/MyMileage
```

The server also ships a themed search page (Material 3 Expressive via [`@m3e/web`](https://matraic.github.io/m3e/)), a custom 404 page, a reload endpoint, plus a web-app manifest and service worker.

## Features

- **TOML-driven shortcuts** — organized into sections, editable without recompiling.
- **Aliases** — map a stable internal key to one or more short paths.
- **Case-insensitive, multi-segment paths** — `/MyMileage/GitHub` and `/mymileage/github` are the same shortcut.
- **Automatic `https://`** — links without a scheme are normalized.
- **Live reload** — `GET /reload` re-reads the TOML file and renders the updated shortcut list.
- **Server-rendered pages** — home and 404 pages are templates with exact `{{PLACEHOLDER}}` substitution.
- **Search/autocomplete** — case-insensitive substring filtering with keyboard navigation.
- **Web-app assets** — a `manifest.json` and `sw.js` are served at stable URLs.

## Requirements

- **JDK 21** (the Gradle toolchain resolves it automatically via the Foojay resolver).
- The bundled Gradle wrapper (Gradle 9.8.2). No separate Gradle install is needed.

## Quick start

```bash
./gradlew run
```

The server listens on `127.0.0.1:80` (configurable, see [Configuration](#configuration)).

### Make `http://go/` work

Port 80 lets you use a bare host name. Add an entry to your hosts file so `go` resolves locally:

```
127.0.0.1  go
```

- **Windows:** `C:\Windows\System32\drivers\etc\hosts` (edit as administrator)
- **Linux / macOS:** `/etc/hosts` (edit with `sudo`)

Then open <http://go/> in your browser.

> Binding to port 80 may require elevated privileges on some systems, and will fail if another process (IIS, nginx, a previous instance, …) already owns it.

## Shortcut configuration

Shortcuts live in `shortcuts.toml`. A minimal file:

```toml
[Links]
[Links.MyMileage]
"mymileage/github" = { link = "https://github.com/ygllc/MyMileage", key = "key.mymileage/github" }
"mymileage/jira"   = { link = "https://ygllc.atlassian.net/jira/software/projects/MILEAGE/summary", key = "key.mymileage/jira" }

[Links.FetalKick]
"fetalkick/github" = { link = "https://github.com/ygllc/FetalKick", key = "key.fetalkick/github" }

[Keys]
[Keys.MyMileage]
"key.mymileage/github" = "mm/gh"
"key.mymileage/jira"   = "mm/j"

[Keys.FetalKick]
"key.fetalkick/github" = "fk/gh"
```

This produces four shortcuts:

| Shortcut | Destination |
|---|---|
| `go/mymileage/github` | `https://github.com/ygllc/MyMileage` |
| `go/mymileage/jira` | the Jira project summary |
| `go/mm/gh` | `https://github.com/ygllc/MyMileage` (alias) |
| `go/fk/gh` | `https://github.com/ygllc/FetalKick` (alias) |

### Rules

- `[Links.<Section>]` groups shortcuts. Each entry needs a `link`; entries without one are skipped.
- Paths are **lowercased**.
- Links are trimmed, and `https://` is prepended if the link has no `http://`/`https://` scheme.
- A link's optional `key` registers a URL under an internal key.
- `[Keys.<Section>]` maps an internal key to an alias path. Unknown keys are ignored.
- Aliases appear under an **Aliases** section on the home page.
- `password` and `status` are accepted by the parser but are not enforced yet — redirects are always `302`.

### Where the file is loaded from

The first match wins:

1. JVM property: `-Dkotlinks.shortcuts=/path/to/shortcuts.toml`
2. Environment variable: `SHORTCUTS_FILE=/path/to/shortcuts.toml`
3. `shortcuts.toml` in the working directory
4. `shortcuts.toml` bundled on the classpath (`src/main/resources`)

The legacy property `-Dgolinks.shortcuts` is still accepted as a fallback.

```bash
# JVM property (pass it to the application JVM, not to Gradle)
java -Dkotlinks.shortcuts=/etc/kotlinks/shortcuts.toml -jar build/libs/kotlinks-all.jar

# Environment variable
SHORTCUTS_FILE=/etc/kotlinks/shortcuts.toml java -jar build/libs/kotlinks-all.jar
```

## HTTP routes

| Method | Path | Description |
|---|---|---|
| `GET` | `/` | Home page with search, categories, and shortcut count. |
| `GET` | `/reload` | Re-reads `shortcuts.toml`, then renders the reload page. Returns `500` on parse errors. |
| `GET` | `/sw.js` | Service worker (`text/javascript`). |
| `GET` | `/manifest.json` | Web app manifest (`application/manifest+json`). |
| `GET` | `/{path...}` | Looks up the shortcut. Found → `302` to its URL. Missing → 404 page with `404` status. |

Paths are normalized by trimming leading/trailing slashes and lowercasing before lookup, so trailing slashes and casing do not matter.

## Configuration

Server settings are in `src/main/resources/application.yaml`:

```yaml
ktor:
  deployment:
    host: 127.0.0.1
    port: 80
  application:
    modules:
      - com.yg.ApplicationKt.module
```

Change `host` to `0.0.0.0` to expose the server on your network, or change `port` if you cannot bind to 80.

## Building

| Task | Description |
|---|---|
| `./gradlew test` | Run the endpoint test suite. |
| `./gradlew build` | Compile, test, and assemble. |
| `./gradlew run` | Run the server from source. |
| `./gradlew shadowJar` | Build the self-contained jar at `build/libs/kotlinks-all.jar`. |
| `./gradlew installDist` | Install a runnable distribution under `build/install/`. |

Run the fat jar:

```bash
java -jar build/libs/kotlinks-all.jar
```

## Project structure

```
src/
├─ main/
│  ├─ kotlin/
│  │  └─ Application.kt          # TOML model, loader/store, rendering, routes
│  └─ resources/
│     ├─ application.yaml        # Ktor host/port and module entry point
│     ├─ home.html               # Home page template
│     ├─ 404.html                # Not-found page template
│     ├─ reload.html             # Reload success page template
│     ├─ auth.html               # Reserved password page (not wired up yet)
│     ├─ shortcuts.toml          # Bundled sample shortcuts
│     ├─ manifest.json           # PWA manifest
│     ├─ sw.js                   # Service worker
│     └─ logback.xml             # Logging configuration
└─ test/
   └─ kotlin/
      └─ ServerTest.kt           # Endpoint tests
```

## How it works

1. At startup `ShortcutStore` reads and parses `shortcuts.toml` with [ktoml](https://github.com/akuleshov7/ktoml).
2. `buildShortcuts()` builds an in-memory `Map<shortcut, url>`: section links first, then aliases resolved through `key → url`.
3. Requests to `/{path...}` look up the normalized path and issue a `302` redirect, or render `404.html` with the requested path, the shortcut list, and searchable options.
4. `/reload` swaps the in-memory map atomically (the store is `@Volatile`) so traffic keeps being served during a reload.

## Testing

```bash
./gradlew test
```

`ServerTest.kt` covers:

- home page rendering (including no unresolved `{{PLACEHOLDER}}`s)
- direct shortcut redirects
- alias redirects
- unknown paths returning the 404 page with a 404 status
- reload page rendering
- correct content types for `sw.js` and `manifest.json`

## Related project: golinks (Bun)

[golinks](https://github.com/ygllc/golinks) is the lightweight Bun + TypeScript implementation that Kotlinks was ported from. It targets exactly the same workflow — `shortcuts.toml`, aliases, `go/` redirects, and the same search/404 pages — but deliberately keeps everything in one ~110-line `index.ts`:

- `import shortcuts from "./shortcuts.toml"` — Bun parses TOML natively.
- `Bun.serve(...)` — built-in HTTP server, no web framework.
- `loadLinks()` — builds the shortcut map and alias table.
- `bun run index.ts` — no build step.

### Pros

| Strength | Why it matters |
|---|---|
| **Very small codebase** | One file (~110 lines) is quick to read, audit, copy, or fork. |
| **No build step** | Bun runs TypeScript directly, so edits take effect on the next run. Startup is near-instant. |
| **Native TOML + HTTP** | No parsing library, no Express/Koa; `Bun.serve` and the TOML loader are built in. |
| **Small runtime footprint** | A single Bun process, and the bundled `Dockerfile` produces a small `oven/bun` image. |
| **Fast iteration** | `bun --watch index.ts` restarts on changes with almost no overhead. |
| **Portable artifact** | `bun build --compile index.ts` can produce a standalone executable. |
| **Right-sized for personal use** | Exactly the features needed for local/team go-links, with little ceremony. |

### Cons

| Limitation | Impact |
|---|---|
| **Extra runtime dependency** | Requires Bun 1.3.7+, which is not preinstalled on most servers/CI images, unlike a JDK or Node. |
| **No automated tests** | Redirects, aliases, 404 handling, and reload have no regression safety net. |
| **Weak type safety around config** | The TOML data is cast with `as any`, so malformed or unexpected shapes fail at runtime rather than at compile time. |
| **Reload is limited** | `/reload` rebuilds the map from the **statically imported** TOML module, so edits made on disk after startup typically need a server restart to be picked up. |
| **Single-file coupling** | Routing, loading, HTML rendering, and redirect logic all live in `index.ts`; adding auth, custom status codes, or structured logging means restructuring. |
| **Fewer deployment options** | Needs Bun or Docker on the host; there is no `java -jar`-style fat jar and no JVM tooling/observability ecosystem. |
| **Younger ecosystem** | Fewer established conventions and libraries for testing, metrics, and long-term maintenance than the JVM. |

### Choosing between them

| Choose… | When… |
|---|---|
| **golinks (Bun)** | You already run Bun, want the smallest possible single-file tool, and do not need tests or layered structure. |
| **Kotlinks (Ktor)** | You want JVM deployment (`java -jar`), a typed TOML model, endpoint tests, file-based reload that actually re-reads disk, and clearer separation of concerns. |

Both projects share the same `shortcuts.toml` format and HTTP behavior, so you can switch between them without changing your configuration.

## License

No license file is included in this repository yet. Add one before distributing the project.
