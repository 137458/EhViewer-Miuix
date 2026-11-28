package com.hippo.ehviewer.ui.tools

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IntentPolicyTest {
    private val restricted = setOf("com.hippo.ehviewer.action.INSTALL_UPDATE", "com.hippo.ehviewer.action.START_DOWNLOADSCENE")
    private val token = "process-token"
    private val installAction = "com.hippo.ehviewer.action.INSTALL_UPDATE"

    @Test
    fun restrictedActionWithMatchingTokenIsTrusted() {
        assertTrue(IntentPolicy.isInternalIntentTrusted(installAction, token, token, restricted))
    }

    @Test
    fun restrictedActionWithoutTokenIsRejected() {
        assertFalse(IntentPolicy.isInternalIntentTrusted(installAction, null, token, restricted))
    }

    @Test
    fun restrictedActionWithForeignTokenIsRejected() {
        assertFalse(IntentPolicy.isInternalIntentTrusted(installAction, "forged-token", token, restricted))
        assertFalse(IntentPolicy.isInternalIntentTrusted(installAction, "", token, restricted))
    }

    @Test
    fun unrelatedActionNeedsNoToken() {
        assertTrue(IntentPolicy.isInternalIntentTrusted("android.intent.action.VIEW", null, token, restricted))
        assertTrue(IntentPolicy.isInternalIntentTrusted(null, null, token, restricted))
    }

    @Test
    fun fileInsideProtectedDirIsRejected() {
        val dirs = listOf("/data/user/0/com.hippo.ehviewer/files", "/data/user/0/com.hippo.ehviewer/cache")
        assertTrue(IntentPolicy.isInsideAnyDir("/data/user/0/com.hippo.ehviewer/files/db/eh.db", dirs))
        assertTrue(IntentPolicy.isInsideAnyDir("/data/user/0/com.hippo.ehviewer/files", dirs))
        assertTrue(IntentPolicy.isInsideAnyDir("/data/user/0/com.hippo.ehviewer/cache/img.tmp", dirs))
    }

    @Test
    fun fileOutsideProtectedDirIsAccepted() {
        val dirs = listOf("/data/user/0/com.hippo.ehviewer/files")
        assertFalse(IntentPolicy.isInsideAnyDir("/storage/emulated/0/Download/a.cbz", dirs))
        // 同前缀但不是子目录
        assertFalse(IntentPolicy.isInsideAnyDir("/data/user/0/com.hippo.ehviewer/filesX/a", dirs))
        assertFalse(IntentPolicy.isInsideAnyDir(null, dirs))
        assertFalse(IntentPolicy.isInsideAnyDir("", dirs))
    }

    @Test
    fun relativeTraversalCannotEscapeProtectedDir() {
        val dirs = listOf("/data/user/0/com.hippo.ehviewer/files")
        assertFalse(IntentPolicy.isInsideAnyDir("/data/user/0/com.hippo.ehviewer/files/../../../sdcard/a", dirs))
        assertTrue(IntentPolicy.isInsideAnyDir("/data/user/0/com.hippo.ehviewer/files/./sub/../sub/b", dirs))
    }

    @Test
    fun windowsSeparatorsAreNormalized() {
        val dirs = listOf("/data/user/0/com.hippo.ehviewer/files")
        assertTrue(IntentPolicy.isInsideAnyDir("\\data\\user\\0\\com.hippo.ehviewer\\files\\a", dirs))
    }
}
