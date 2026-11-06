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

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ServerTest {

    @Test
    fun `home page renders shortcuts without placeholders`() = testApplication {
        configure()

        val response = client.get("/")
        val body = response.bodyAsText()

        assertEquals(HttpStatusCode.OK, response.status)
        assertContains(body, "Kotlinks")
        assertContains(body, "mymileage/github")
        assertFalse(body.contains("{{"), "Unresolved template placeholder left in home page")
    }

    @Test
    fun `shortcut redirects with 302`() = testApplication {
        configure()
        val noRedirect = createClient { followRedirects = false }

        val response = noRedirect.get("/mymileage/github")

        assertEquals(HttpStatusCode.Found, response.status)
        assertEquals(
            "https://github.com/ygllc/MyMileage",
            response.headers[HttpHeaders.Location],
        )
    }

    @Test
    fun `alias key redirects with 302`() = testApplication {
        configure()
        val noRedirect = createClient { followRedirects = false }

        val response = noRedirect.get("/mm/gh")

        assertEquals(HttpStatusCode.Found, response.status)
        assertEquals(
            "https://github.com/ygllc/MyMileage",
            response.headers[HttpHeaders.Location],
        )
    }

    @Test
    fun `unknown path returns 404 page`() = testApplication {
        configure()

        val response = client.get("/does-not-exist")
        val body = response.bodyAsText()

        assertEquals(HttpStatusCode.NotFound, response.status)
        assertContains(body, "does-not-exist")
        assertFalse(body.contains("{{"), "Unresolved template placeholder left in 404 page")
    }

    @Test
    fun `reload page renders the shortcut count`() = testApplication {
        configure()

        val response = client.get("/reload")
        val body = response.bodyAsText()

        assertEquals(HttpStatusCode.OK, response.status)
        assertContains(body, "Shortcuts Reloaded")
        assertContains(body, "mymileage/github")
        assertFalse(body.contains("{{"), "Unresolved template placeholder left in reload page")
    }

    @Test
    fun `service worker and manifest are served`() = testApplication {
        configure()

        val sw = client.get("/sw.js")
        assertEquals(HttpStatusCode.OK, sw.status)
        assertContains(sw.headers[HttpHeaders.ContentType] ?: "", "javascript")
        assertContains(sw.bodyAsText(), "CACHE_NAME")

        val manifest = client.get("/manifest.json")
        assertEquals(HttpStatusCode.OK, manifest.status)
        assertContains(manifest.headers[HttpHeaders.ContentType] ?: "", "json")
        assertContains(manifest.bodyAsText(), "Kotlinks")
    }
}
