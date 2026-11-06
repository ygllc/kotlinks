/* 
 *  
 *   Copyright (C) 2026 YG LLC
 *   
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *   
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *   
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <https://gnu.org>.
 *  
 */

package com.yg

import com.akuleshov7.ktoml.Toml
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.encodeURLPathPart
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.log
import io.ktor.server.request.path
import io.ktor.server.response.respondRedirect
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.File

// --------------------------------------------------------------------------- //
//     TOML model (mirrors the shortcuts.toml structure used by golinks)
// --------------------------------------------------------------------------- //

@Serializable
internal data class ShortcutsFile(
    @SerialName("Links")
    val links: Map<String, Map<String, LinkEntry>> = emptyMap(),
    @SerialName("Keys")
    val keys: Map<String, Map<String, String>> = emptyMap(),
)

@Serializable
internal data class LinkEntry(
    val link: String? = null,
    val key: String? = null,
    val password: String? = null,
    val status: Int? = null,
)

internal data class ShortcutChip(val shortcut: String, val url: String)

internal data class ShortcutSet(
    val links: Map<String, String>,
    val sections: Map<String, List<ShortcutChip>>,
) {
    val count: Int get() = links.size
}

// --------------------------------------------------------------------------- //
//                   Shortcut loading / in-memory store
// --------------------------------------------------------------------------- //

private object ShortcutStore {
    private val toml = Toml()

    @Volatile
    private var state: ShortcutSet = readAndParse()

    fun current(): ShortcutSet = state

    fun reload(): ShortcutSet = readAndParse().also { state = it }

    private fun readAndParse(): ShortcutSet = buildShortcuts(parseToml(readTomlText()))

    private fun parseToml(text: String): ShortcutsFile =
        toml.decodeFromString(ShortcutsFile.serializer(), text)

    /**
     * Resolves the shortcuts file in this order:
     *  1. `-Dgolinks.shortcuts=/path/to/shortcuts.toml`
     *  2. `SHORTCUTS_FILE=/path/to/shortcuts.toml`
     *  3. `shortcuts.toml` in the working directory (editable while running)
     *  4. `shortcuts.toml` bundled on the classpath
     */
    private fun readTomlText(): String {
        val configured = System.getProperty("golinks.shortcuts")
            ?: System.getenv("SHORTCUTS_FILE")

        if (!configured.isNullOrBlank()) {
            val file = File(configured)
            check(file.isFile) { "Shortcuts file not found: ${file.absolutePath}" }
            return file.readText(Charsets.UTF_8)
        }

        val workingDirFile = File("shortcuts.toml")
        if (workingDirFile.isFile) {
            return workingDirFile.readText(Charsets.UTF_8)
        }

        return ShortcutStore::class.java.classLoader
            .getResourceAsStream("shortcuts.toml")
            ?.bufferedReader(Charsets.UTF_8)
            ?.use { it.readText() }
            ?: error(
                "shortcuts.toml not found. Put it in the working directory, " +
                    "on the classpath, or set SHORTCUTS_FILE.",
            )
    }

    /**
     * Port of the original `loadLinks()`:
     *  - paths are lowercased
     *  - links without an explicit scheme get `https://`
     *  - `[Keys]` entries create aliases that resolve to the URL of their link
     */
    private fun buildShortcuts(file: ShortcutsFile): ShortcutSet {
        val links = LinkedHashMap<String, String>()
        val keyToUrl = HashMap<String, String>()
        val sections = LinkedHashMap<String, MutableList<ShortcutChip>>()

        file.links.forEach { (section, entries) ->
            entries.forEach entryLoop@ { (path, entry) ->
                val link = entry.link ?: return@entryLoop
                val url = normalizeUrl(link)
                val shortcut = path.lowercase()

                links[shortcut] = url
                sections.getOrPut(section) { mutableListOf() }
                    .add(ShortcutChip(shortcut, url))

                entry.key?.let { keyToUrl[it] = url }
            }
        }

        val aliases = mutableListOf<ShortcutChip>()
        file.keys.forEach { (_, entries) ->
            entries.forEach aliasLoop@ { (internalKey, alias) ->
                val url = keyToUrl[internalKey] ?: return@aliasLoop
                val shortcut = alias.lowercase()

                links[shortcut] = url
                aliases.add(ShortcutChip(shortcut, url))
            }
        }
        if (aliases.isNotEmpty()) {
            sections["Aliases"] = aliases
        }

        return ShortcutSet(links = links, sections = sections)
    }

    private fun normalizeUrl(raw: String): String {
        val trimmed = raw.trim()
        return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else {
            "https://$trimmed"
        }
    }
}

// --------------------------------------------------------------------------- //
//                            Ktor application
// --------------------------------------------------------------------------- //

fun Application.module() {
    // Load eagerly so a broken shortcuts.toml fails at startup, not on first request.
    ShortcutStore.current()

    routing {
        get("/") {
            call.respondText(renderHome(ShortcutStore.current()), ContentType.Text.Html)
        }

        get("/reload") {
            call.respondReloadPage()
        }

        get("/sw.js") {
            call.respondText(loadResource("sw.js"), ContentType.parse("text/javascript"))
        }

        get("/manifest.json") {
            call.respondText(
                loadResource("manifest.json"),
                ContentType.parse("application/manifest+json"),
            )
        }

        // Multi-segment shortcuts (e.g. /mymileage/github) and simple ones alike.
        get("/{path...}") {
            val path = call.request.path().trim('/').lowercase()

            if (path.isEmpty()) {
                call.respondText(renderHome(ShortcutStore.current()), ContentType.Text.Html)
                return@get
            }

            // Preserve the original server's behavior for /reload/ (trailing slash).
            if (path == "reload") {
                call.respondReloadPage()
                return@get
            }

            val shortcuts = ShortcutStore.current()
            val target = shortcuts.links[path]
            if (target != null) {
                call.application.log.info("Redirect: go/{} -> {}", path, target)
                call.respondRedirect(target, permanent = false)
            } else {
                call.respondText(
                    renderNotFound(path, shortcuts),
                    ContentType.Text.Html,
                    HttpStatusCode.NotFound,
                )
            }
        }
    }
}

// --------------------------------------------------------------------------- //
//           Page rendering (exact `{{PLACEHOLDER}}` replacement) 
// --------------------------------------------------------------------------- //

private suspend fun ApplicationCall.respondReloadPage() {
    val shortcuts = try {
        ShortcutStore.reload()
    } catch (e: Exception) {
        application.log.error("Failed to reload shortcuts", e)
        respondText(
            "Failed to reload shortcuts: ${e.message}",
            ContentType.Text.Plain,
            HttpStatusCode.InternalServerError,
        )
        return
    }
    respondText(renderReload(shortcuts), ContentType.Text.Html)
}

private fun renderHome(shortcuts: ShortcutSet): String =
    loadResource("home.html")
        .replace("{{COUNT}}", shortcuts.count.toString())
        .replace("{{OPTIONS_HTML}}", buildOptions(shortcuts.links.keys))
        .replace("{{CATEGORIES_HTML}}", buildCategories(shortcuts.sections))
        .replace("{{SHORTCUTS_JSON}}", buildShortcutsJson(shortcuts.links))

private fun renderReload(shortcuts: ShortcutSet): String {
    val chips = shortcuts.links.keys.sorted().joinToString("") { buildChip(it) }
    return loadResource("reload.html")
        .replace("{{COUNT}}", shortcuts.count.toString())
        .replace("{{SHORTCUTS_CHIPS}}", chips)
}

private fun renderNotFound(path: String, shortcuts: ShortcutSet): String =
    loadResource("404.html")
        // JS string literal first, then the remaining HTML text occurrence.
        .replace("\"{{REQUESTED_PATH}}\"", "\"${escapeJs(path)}\"")
        .replace("{{REQUESTED_PATH}}", escapeHtml(path))
        .replace("{{OPTIONS_HTML}}", buildOptions(shortcuts.links.keys))
        .replace("{{CATEGORIES_HTML}}", buildCategories(shortcuts.sections))
        .replace("{{SHORTCUTS_JSON}}", buildShortcutsJson(shortcuts.links))

private fun buildOptions(shortcuts: Collection<String>): String =
    shortcuts.sorted().joinToString("") { shortcut ->
        val href = shortcutHref(shortcut)
        """<m3e-option value="${escapeHtml(href)}">go/${escapeHtml(shortcut)}</m3e-option>"""
    }

private fun buildCategories(sections: Map<String, List<ShortcutChip>>): String =
    sections.entries
        .filter { it.value.isNotEmpty() }
        .joinToString("") { (name, chips) ->
            buildString {
                append("<div class=\"category-section\">")
                append("<div class=\"category-title\">").append(escapeHtml(name)).append("</div>")
                append("<div class=\"shortcuts-grid\">")
                chips.forEach { append(buildChip(it.shortcut)) }
                append("</div>")
                append("</div>")
            }
        }

private fun buildChip(shortcut: String): String =
    """<a href="${escapeHtml(shortcutHref(shortcut))}" class="shortcut-chip">go/${escapeHtml(shortcut)}</a>"""

private fun shortcutHref(shortcut: String): String =
    "/" + shortcut.split('/').joinToString("/") { it.encodeURLPathPart() }

private fun buildShortcutsJson(links: Map<String, String>): String =
    links.entries.joinToString(prefix = "[", postfix = "]") { (shortcut, url) ->
        """{"shortcut":${jsonString(shortcut)},"url":${jsonString(url)}}"""
    }

private fun loadResource(name: String): String =
    ShortcutStore::class.java.classLoader
        .getResourceAsStream(name)
        ?.bufferedReader(Charsets.UTF_8)
        ?.use { it.readText() }
        ?: error("Static resource not found on classpath: $name")

// --------------------------------------------------------------------------- //
// Small escaping helpers
// --------------------------------------------------------------------------- //

private fun escapeHtml(value: String): String = buildString(value.length) {
    value.forEach { c ->
        when (c) {
            '&' -> append("&amp;")
            '<' -> append("&lt;")
            '>' -> append("&gt;")
            '"' -> append("&quot;")
            '\'' -> append("&#39;")
            else -> append(c)
        }
    }
}

private fun escapeJs(value: String): String = buildString(value.length) {
    value.forEach { c ->
        when (c) {
            '\\' -> append("\\\\")
            '"' -> append("\\\"")
            '\'' -> append("\\'")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            '<' -> append("\\u003c")
            '>' -> append("\\u003e")
            '&' -> append("\\u0026")
            else -> append(c)
        }
    }
}

private fun jsonString(value: String): String = buildString(value.length + 2) {
    append('"')
    value.forEach { c ->
        when (c) {
            '"' -> append("\\\"")
            '\\' -> append("\\\\")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> if (c < ' ') {
                append("\\u%04x".format(c.code))
            } else {
                append(c)
            }
        }
    }
    append('"')
}
