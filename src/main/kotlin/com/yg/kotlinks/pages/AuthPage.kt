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

import io.ktor.server.application.ApplicationCall
import kotlinx.html.*

/**
 * Reserved password page. Not wired to a route yet, but kept as Kotlin so the
 * markup stays in sync with the other pages.
 */
internal suspend fun ApplicationCall.respondAuth(path: String, error: String? = null) {
    respondHtmlPage(title = "Password Required - Kotlinks", cssFile = "auth.css") {
        m3eTheme {
            div(classes = "container") {
                div(classes = "icon") { +"🔒" }
                h1(classes = "title") { +"Password Required" }
                p(classes = "description") { +"This shortcut is protected" }
                span(classes = "shortcut-path") { +"go/$path" }

                m3eCard {
                    form(action = "/auth/$path", method = FormMethod.post) {
                        id = "auth-form"
                        div {
                            style = "padding: 20px;"
                            div(classes = "error-message") {
                                id = "error-msg"
                                if (error == null) {
                                    style = "display: none;"
                                } else {
                                    +error
                                }
                            }
                            div(classes = "input-wrapper") {
                                input(type = InputType.password) {
                                    id = "password"
                                    name = "password"
                                    placeholder = " "
                                    autoComplete = "current-password"
                                }
                                label {
                                    htmlFor = "password"
                                    +"Password"
                                }
                            }
                            m3eButton(variant = "filled", type = "submit", id = "submit-btn") {
                                m3eIcon(name = "arrow_forward", slot = "icon")
                                +"Continue"
                            }
                        }
                    }
                }

                div(classes = "footer") {
                    m3eButton(variant = "text", id = "go-home") {
                        m3eIcon(name = "home", slot = "icon")
                        +"Go Home"
                    }
                }
            }
        }

        script(src = "/static/auth.js") {}
    }
}
