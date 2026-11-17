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

package com.yg.kotlinks.pages

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.html.respondHtml
import kotlinx.html.FlowContent
import kotlinx.html.HTMLTag
import kotlinx.html.body
import kotlinx.html.head
import kotlinx.html.link
import kotlinx.html.meta
import kotlinx.html.script
import kotlinx.html.title
import kotlinx.html.unsafe

private const val FONT_SANS =
    "https://fonts.googleapis.com/css2?family=Google+Sans:opsz,wght@9..40,400;9..40,500;9..40,700" +
        "&family=Google+Sans+Text:wght@400;500&display=swap"

private const val FONT_SYMBOLS =
    "https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@24,400,0..1,0"

private val IMPORT_MAP = """
{
  "imports": {
    "lit": "https://cdn.jsdelivr.net/npm/lit@3.3.0/+esm",
    "@m3e/web/theme": "https://cdn.jsdelivr.net/npm/@m3e/web@2.0.2/dist/theme.js",
    "@m3e/web/form-field": "https://cdn.jsdelivr.net/npm/@m3e/web@2.0.2/dist/form-field.js",
    "@m3e/web/autocomplete": "https://cdn.jsdelivr.net/npm/@m3e/web@2.0.2/dist/autocomplete.js",
    "@m3e/web/option": "https://cdn.jsdelivr.net/npm/@m3e/web@2.0.2/dist/option.js",
    "@m3e/web/card": "https://cdn.jsdelivr.net/npm/@m3e/web@2.0.2/dist/card.js",
    "@m3e/web/button": "https://cdn.jsdelivr.net/npm/@m3e/web@2.0.2/dist/button.js",
    "@m3e/web/icon": "https://cdn.jsdelivr.net/npm/@m3e/web@2.0.2/dist/icon.js",
    "@m3e/web/core": "https://cdn.jsdelivr.net/npm/@m3e/web@2.0.2/dist/core.js",
    "@m3e/web/core/a11y": "https://cdn.jsdelivr.net/npm/@m3e/web@2.0.2/dist/core-a11y.js",
    "@m3e/web/core/anchoring": "https://cdn.jsdelivr.net/npm/@m3e/web@2.0.2/dist/core-anchoring.js",
    "@m3e/web/core/bidi": "https://cdn.jsdelivr.net/npm/@m3e/web@2.0.2/dist/core-bidi.js",
    "@m3e/web/core/layout": "https://cdn.jsdelivr.net/npm/@m3e/web@2.0.2/dist/core-layout.js",
    "@m3e/web/core/platform": "https://cdn.jsdelivr.net/npm/@m3e/web@2.0.2/dist/core-platform.js",
    "@m3e/core": "https://cdn.jsdelivr.net/npm/@m3e/web@2.0.2/dist/core.js"
  }
}
""".trimIndent()

private val MODULE_IMPORTS = """
import "@m3e/web/theme";
import "@m3e/web/form-field";
import "@m3e/web/autocomplete";
import "@m3e/web/option";
import "@m3e/web/card";
import "@m3e/web/button";
import "@m3e/web/icon";
""".trimIndent()

/**
 * Renders the shared page chrome (head, fonts, Material 3 Expressive modules,
 * theme stylesheet and service-worker registration) around [content].
 */
internal suspend fun ApplicationCall.respondHtmlPage(
    title: String,
    cssFile: String,
    status: HttpStatusCode? = null,
    content: FlowContent.() -> Unit,
) {
    respondHtml(status ?: HttpStatusCode.OK) {
        attributes["lang"] = "en"
        head {
            meta(charset = "UTF-8")
            meta(name = "viewport", content = "width=device-width, initial-scale=1.0")
            title { +title }
            link { rel = "preconnect"; href = "https://fonts.googleapis.com" }
            link {
                rel = "preconnect"
                href = "https://fonts.gstatic.com"
                attributes["crossorigin"] = ""
            }
            link { rel = "stylesheet"; href = FONT_SANS }
            link { rel = "stylesheet"; href = FONT_SYMBOLS }
            link { rel = "stylesheet"; href = "/static/theme.css" }
            link { rel = "stylesheet"; href = "/static/$cssFile" }
            script(type = "importmap") { unsafe { +IMPORT_MAP } }
            script(type = "module") { unsafe { +MODULE_IMPORTS } }
        }
        body {
            content()
            script(src = "/static/app.js") {}
        }
    }
}

/**
 * Emits a custom element (for example an `m3e-*` web component) that
 * kotlinx.html does not model. A null attribute value renders a bare
 * boolean attribute.
 */
internal fun FlowContent.customElement(
    name: String,
    attrs: List<Pair<String, String?>> = emptyList(),
    content: FlowContent.() -> Unit = {},
) {
    val attrsText = attrs.joinToString(separator = "") { (key, value) ->
        if (value == null) " $key" else " $key=\"${escapeHtml(value)}\""
    }
    // Flow content inside a page is always an HTML tag, which is what `unsafe` needs.
    val tag = this as HTMLTag
    tag.unsafe { +"<$name$attrsText>" }
    content()
    tag.unsafe { +"</$name>" }
}

internal fun FlowContent.m3eTheme(content: FlowContent.() -> Unit) =
    customElement("m3e-theme", content = content)

internal fun FlowContent.m3eCard(content: FlowContent.() -> Unit) =
    customElement("m3e-card", content = content)

internal fun FlowContent.m3eFormField(content: FlowContent.() -> Unit) =
    customElement("m3e-form-field", content = content)

internal fun FlowContent.m3eAutocomplete(
    forId: String,
    leadingIcon: String? = null,
    strict: Boolean = false,
    content: FlowContent.() -> Unit,
) = customElement(
    name = "m3e-autocomplete",
    attrs = buildList<Pair<String, String?>> {
        add("for" to forId)
        leadingIcon?.let { add("leading-icon" to it) }
        if (strict) add("strict" to null)
    },
    content = content,
)

internal fun FlowContent.m3eIcon(name: String, slot: String? = null) =
    customElement(
        name = "m3e-icon",
        attrs = buildList<Pair<String, String?>> {
            slot?.let { add("slot" to it) }
            add("name" to name)
        },
    )

internal fun FlowContent.m3eButton(
    id: String? = null,
    variant: String? = null,
    shape: String? = null,
    type: String? = null,
    content: FlowContent.() -> Unit,
) = customElement(
    name = "m3e-button",
    attrs = buildList<Pair<String, String?>> {
        id?.let { add("id" to it) }
        variant?.let { add("variant" to it) }
        shape?.let { add("shape" to it) }
        type?.let { add("type" to it) }
    },
    content = content,
)

internal fun FlowContent.m3eOption(value: String, text: String) =
    customElement(name = "m3e-option", attrs = listOf("value" to value)) { +text }
