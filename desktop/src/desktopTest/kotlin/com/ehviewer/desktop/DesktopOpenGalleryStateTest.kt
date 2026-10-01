package com.ehviewer.desktop

import androidx.compose.ui.input.key.Key
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class DesktopOpenGalleryStateTest {

    @Test
    fun parseInput_standardUrl_returnsGidAndToken() {
        val input1 = "https://e-hentai.org/g/1234567/abcdef1234/"
        val target1 = DesktopOpenGalleryState.parseInput(input1)
        assertNotNull(target1)
        assertEquals(1234567L, target1.gid)
        assertEquals("abcdef1234", target1.token)

        // ExHentai URL 与末尾无斜杠
        val input2 = "https://exhentai.org/g/9876543/fedcba4321"
        val target2 = DesktopOpenGalleryState.parseInput(input2)
        assertNotNull(target2)
        assertEquals(9876543L, target2.gid)
        assertEquals("fedcba4321", target2.token)
    }

    @Test
    fun parseInput_pureGidTokenWithVariousDelimiters_returnsTarget() {
        // 斜杠分隔
        val targetSlash = DesktopOpenGalleryState.parseInput("1234567/abcdef1234")
        assertNotNull(targetSlash)
        assertEquals(1234567L, targetSlash.gid)
        assertEquals("abcdef1234", targetSlash.token)

        // 空格分隔
        val targetSpace = DesktopOpenGalleryState.parseInput("  9876543   fedcba4321  ")
        assertNotNull(targetSpace)
        assertEquals(9876543L, targetSpace.gid)
        assertEquals("fedcba4321", targetSpace.token)

        // 逗号分隔
        val targetComma = DesktopOpenGalleryState.parseInput("555555, 1122334455")
        assertNotNull(targetComma)
        assertEquals(555555L, targetComma.gid)
        assertEquals("1122334455", targetComma.token)
    }

    @Test
    fun parseInput_invalidOrBlank_returnsNull() {
        assertNull(DesktopOpenGalleryState.parseInput(null))
        assertNull(DesktopOpenGalleryState.parseInput(""))
        assertNull(DesktopOpenGalleryState.parseInput("   "))
        assertNull(DesktopOpenGalleryState.parseInput("https://google.com"))
        // Token 长度不足 10 位
        assertNull(DesktopOpenGalleryState.parseInput("123456/abc"))
        // 非十六进制 Token
        assertNull(DesktopOpenGalleryState.parseInput("123456/abcdefghij"))
    }

    @Test
    fun validate_reportsErrorKindsForI18n() {
        // 空输入 → EmptyInput
        assertEquals(DesktopOpenGalleryError.EmptyInput, DesktopOpenGalleryState.validate(null))
        assertEquals(DesktopOpenGalleryError.EmptyInput, DesktopOpenGalleryState.validate(""))
        assertEquals(DesktopOpenGalleryError.EmptyInput, DesktopOpenGalleryState.validate("   "))

        // 非法输入 → InvalidInput
        assertEquals(DesktopOpenGalleryError.InvalidInput, DesktopOpenGalleryState.validate("not a valid link"))
        assertEquals(DesktopOpenGalleryError.InvalidInput, DesktopOpenGalleryState.validate("12345/nothex"))
        assertEquals(DesktopOpenGalleryError.InvalidInput, DesktopOpenGalleryState.validate("https://google.com"))

        // 合法输入 → null
        assertNull(DesktopOpenGalleryState.validate("https://e-hentai.org/g/123456/abcdef1234/"))
        assertNull(DesktopOpenGalleryState.validate("123456/abcdef1234"))
    }

    @Test
    fun createGalleryInfo_buildsExpectedModel() {
        val target = GalleryParsedTarget(gid = 778899L, token = "1234567890")
        val infoDefault = DesktopOpenGalleryState.createGalleryInfo(target)
        assertEquals(778899L, infoDefault.gid)
        assertEquals("1234567890", infoDefault.token)
        assertEquals("Gallery 778899", infoDefault.title)

        val infoCustom = DesktopOpenGalleryState.createGalleryInfo(target, customTitle = "My Custom Title")
        assertEquals("My Custom Title", infoCustom.title)

        // 三角验证：空标题或空白标题回退默认
        val infoBlank = DesktopOpenGalleryState.createGalleryInfo(target, customTitle = "   ")
        assertEquals("Gallery 778899", infoBlank.title)
    }

    @Test
    fun resolveKeyAction_ctrlO_resolvesToOpenLinkDialog() {
        val action = resolveKeyAction(
            isKeyDown = true,
            isCtrlPressed = true,
            key = Key.O,
        )
        assertEquals(DesktopKeyAction.OpenLinkDialog, action)

        // KeyUp 状态忽略
        val actionKeyUp = resolveKeyAction(
            isKeyDown = false,
            isCtrlPressed = true,
            key = Key.O,
        )
        assertEquals(DesktopKeyAction.None, actionKeyUp)

        // 未按 Ctrl 忽略
        val actionNoCtrl = resolveKeyAction(
            isKeyDown = true,
            isCtrlPressed = false,
            key = Key.O,
        )
        assertEquals(DesktopKeyAction.None, actionNoCtrl)
    }
}
