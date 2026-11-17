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

internal suspend fun ApplicationCall.respondReload(shortcuts: ShortcutSet) {
    respondHtmlPage(title = "Shortcuts Reloaded", cssFile = "reload.css") {
        m3eTheme {
            div(classes = "container") {
                div(classes = "hero") {
                    div(classes = "success-icon") { +"♻️" }
                }

                h1(classes = "success-title") { +"Shortcuts Reloaded!" }
                p(classes = "success-message") {
                    +"Your shortcuts have been refreshed from shortcuts.toml"
                }

                div(classes = "shortcuts-section") {
                    div(classes = "shortcuts-title") {
                        +"Available Shortcuts "
                        span(classes = "count-badge") { +shortcuts.count.toString() }
                    }
                    div(classes = "shortcuts-list") {
                        id = "shortcuts-list"
                        shortcuts.links.keys.sorted().forEach { shortcutChip(it) }
                    }
                }

                div(classes = "footer") {
                    m3eButton(id = "go-home", variant = "filled", shape = "square") {
                        m3eIcon(name = "home", slot = "icon")
                        +"Go Home"
                    }
                    m3eButton(id = "reload", variant = "filled", shape = "square") {
                        m3eIcon(name = "refresh", slot = "icon")
                        +"Reload Again"
                    }
                }
            }
        }

        script(src = "/static/reload.js") {}
    }
}
