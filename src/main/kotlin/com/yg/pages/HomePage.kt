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

import com.yg.ShortcutSet
import io.ktor.server.application.ApplicationCall
import kotlinx.html.*

internal suspend fun ApplicationCall.respondHome(shortcuts: ShortcutSet) {
    respondHtmlPage(title = "Kotlinks - Shortcuts", cssFile = "home.css") {
        m3eTheme {
            div(classes = "container") {
                div(classes = "header") {
                    div(classes = "logo") { +"🔗" }
                    h1(classes = "title") { +"Kotlinks" }
                    p(classes = "subtitle") { +"${shortcuts.count} shortcuts available" }
                }

                div(classes = "search-section") {
                    div(classes = "search-container") {
                        m3eFormField {
                            label {
                                htmlFor = "shortcut-search"
                                attributes["slot"] = "label"
                                +"Search shortcuts..."
                            }
                            m3eIcon(name = "search", slot = "prefix")
                            input(type = InputType.text) {
                                id = "shortcut-search"
                                placeholder = " "
                                autoComplete = "off"
                            }
                        }
                        m3eAutocomplete(forId = "shortcut-search") {
                            shortcutOptions(shortcuts.links.keys)
                        }
                    }
                }

                div(classes = "shortcuts-container") {
                    id = "shortcuts-container"
                    categorySections(shortcuts.sections)
                }

                div(classes = "footer") {
                    m3eButton(id = "reload-btn", variant = "outlined", shape = "square") {
                        m3eIcon(name = "refresh", slot = "icon")
                        +"Reload"
                    }
                }
            }

            div(classes = "toast") {
                id = "toast"
                +"Shortcuts reloaded"
            }
        }

        script { unsafe { +"window.SHORTCUTS = ${shortcutsJson(shortcuts.links)};" } }
        script(src = "/static/home.js") {}
    }
}
