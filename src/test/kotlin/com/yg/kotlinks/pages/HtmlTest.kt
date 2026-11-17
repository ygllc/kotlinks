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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class HtmlTest {
    @Test
    fun `json string escapes characters that can break out of a script element`() {
        val encoded = jsonString("a</script>&\u2028")

        assertFalse(encoded.contains("</"))
        assertFalse(encoded.contains("&"))
        assertFalse(encoded.contains("\u2028"))
        assertEquals("\"a\\u003c/script\\u003e\\u0026\\u2028\"", encoded)
    }
}
