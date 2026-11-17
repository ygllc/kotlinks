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

import com.yg.kotlinks.ShortcutSet
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import kotlinx.html.*

internal suspend fun ApplicationCall.respondNotFound(path: String, shortcuts: ShortcutSet) {
    respondHtmlPage(
        title = "404 - Page Not Found",
        cssFile = "not-found.css",
        status = HttpStatusCode.NotFound,
    ) {
        m3eTheme {
            div(classes = "container") {
                div(classes = "hero") {
                    div(classes = "error-code") { +"404" }
                    p(classes = "error-message") { +"This shortcut doesn't exist" }
                    span(classes = "error-path") {
                        id = "requested-path"
                        +"go/$path"
                    }
                }

                div(classes = "search-section") {
                    p {
                        style = "font-size: 14px; color: var(--m3e-theme-on-surface-variant); " +
                            "margin-bottom: 16px;"
                        +"Search for a valid shortcut:"
                    }
                    m3eCard {
                        div(classes = "search-container") {
                            div(classes = "input-wrapper") {
                                input(type = InputType.text) {
                                    id = "shortcut-search"
                                    placeholder = " "
                                    autoComplete = "off"
                                }
                                label {
                                    htmlFor = "shortcut-search"
                                    +"Search shortcuts..."
                                }
                            }
                            m3eAutocomplete(
                                forId = "shortcut-search",
                                leadingIcon = "search",
                                strict = true,
                            ) {
                                shortcutOptions(shortcuts.links.keys)
                            }
                        }
                    }
                }

                div(classes = "shortcuts-section") {
                    id = "shortcuts-section"
                    p {
                        style = "font-size: 14px; color: var(--m3e-theme-on-surface-variant); " +
                            "margin-bottom: 16px;"
                        +"Or browse by category:"
                    }
                    div {
                        id = "categories-container"
                        categorySections(shortcuts.sections)
                    }
                }

                div(classes = "footer") {
                    m3eButton(id = "go-home", variant = "filled", shape = "square") {
                        m3eIcon(name = "home", slot = "icon")
                        +"Go Home"
                    }
                }
            }
        }

        script(src = "/static/not-found.js") {}
    }
}
