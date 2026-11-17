/*
 * Copyright (C) 2026 YG LLC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://gnu.org>.
 */

package com.yg.pages

import com.yg.ShortcutChip
import io.ktor.http.encodeURLPathPart
import kotlinx.html.FlowContent
import kotlinx.html.a
import kotlinx.html.div

/** Renders the server-side `<m3e-option>` list that powers search/autocomplete. */
internal fun FlowContent.shortcutOptions(shortcuts: Collection<String>) {
    shortcuts.sorted().forEach { shortcut ->
        m3eOption(value = shortcutHref(shortcut), text = "go/$shortcut")
    }
}

/** Renders one card per non-empty shortcut section. */
internal fun FlowContent.categorySections(sections: Map<String, List<ShortcutChip>>) {
    sections.entries
        .filter { it.value.isNotEmpty() }
        .forEach { (name, chips) ->
            div(classes = "category-section") {
                div(classes = "category-title") { +name }
                div(classes = "shortcuts-grid") {
                    chips.forEach { shortcutChip(it.shortcut) }
                }
            }
        }
}

internal fun FlowContent.shortcutChip(shortcut: String) {
    a(href = shortcutHref(shortcut), classes = "shortcut-chip") { +"go/$shortcut" }
}

/** Builds a go-link href, encoding each path segment separately. */
internal fun shortcutHref(shortcut: String): String =
    "/" + shortcut.split('/').joinToString("/") { it.encodeURLPathPart() }

/** Serializes the shortcut map as JSON for the `window.SHORTCUTS` client hook. */
internal fun shortcutsJson(links: Map<String, String>): String =
    links.entries.joinToString(prefix = "[", postfix = "]") { (shortcut, url) ->
        """{"shortcut":${jsonString(shortcut)},"url":${jsonString(url)}}"""
    }
