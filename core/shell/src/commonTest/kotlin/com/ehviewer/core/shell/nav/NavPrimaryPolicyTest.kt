package com.ehviewer.core.shell.nav

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NavPrimaryPolicyTest {
    private val policy = NavPrimaryPolicy(
        primaryKeys = listOf("home", "subscription", "whatshot", "favorites", "downloads", "settings"),
        mergeAliases = mapOf("toplist" to "whatshot", "history" to "downloads"),
    )

    @Test
    fun `直接主项解析到自身`() {
        assertEquals("home", policy.resolvePrimary("home"))
        assertEquals("settings", policy.resolvePrimary("settings"))
    }

    @Test
    fun `别名主项归并到主键`() {
        assertEquals("whatshot", policy.resolvePrimary("toplist"))
        assertEquals("downloads", policy.resolvePrimary("history"))
    }

    @Test
    fun `非主项返回 null`() {
        assertNull(policy.resolvePrimary("nav_items"))
        assertNull(policy.resolvePrimary(null))
    }

    @Test
    fun `主项索引与底栏顺序一致`() {
        assertEquals(0, policy.primaryIndexOf("home"))
        assertEquals(2, policy.primaryIndexOf("whatshot"))
        assertEquals(5, policy.primaryIndexOf("settings"))
    }

    @Test
    fun `别名索引归并到对应主项`() {
        assertEquals(2, policy.primaryIndexOf("toplist"))
        assertEquals(4, policy.primaryIndexOf("history"))
    }

    @Test
    fun `未知项索引为 -1`() {
        assertEquals(-1, policy.primaryIndexOf("whatever"))
        assertEquals(-1, policy.primaryIndexOf(null))
    }
}
