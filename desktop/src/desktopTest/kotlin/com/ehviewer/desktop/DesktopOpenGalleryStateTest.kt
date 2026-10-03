package com.ehviewer.desktop

import androidx.compose.ui.input.key.Key
import com.ehviewer.core.i18n.MR
import dev.icerock.moko.resources.desc.ResourceFormatted
import dev.icerock.moko.resources.desc.StringDesc
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
    fun parseInput_uppercaseHexUrl_returnsNormalizedTarget() {
        // 与裸 GID/Token 形式对齐：完整 URL 中大写 hex Token 亦应被接受并归一化为小写
        val target = DesktopOpenGalleryState.parseInput("https://e-hentai.org/g/1234567/ABCDEF1234/")
        assertNotNull(target)
        assertEquals(1234567L, target.gid)
        assertEquals("abcdef1234", target.token)
    }

    @Test
    fun parseInput_mpvUrl_returnsTarget() {
        // 上游 GalleryDetailUrlParser 严格模式支持 g|mpv，桌面入口不应拒绝 mpv 链接
        val target = DesktopOpenGalleryState.parseInput("https://e-hentai.org/mpv/2468135790/deadbeef00/")
        assertNotNull(target)
        assertEquals(2468135790L, target.gid)
        assertEquals("deadbeef00", target.token)
    }

    @Test
    fun parseInput_nonGalleryUrlWithGidLikeSegment_returnsNull() {
        // 画廊 URL 门控保留：非画廊站点链接即使携带 gid/token 形态片段也不误判
        assertNull(DesktopOpenGalleryState.parseInput("https://forums.example.org/thread/9999999999/deadbeef00"))
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
        // 占位标题经 moko 本地化（跟随系统语言），期望值经同一 API 解析保证断言与语言环境无关
        val fallbackTitle = StringDesc.ResourceFormatted(MR.strings.desktop_gallery_numbered, 778899L).localized()
        val infoDefault = DesktopOpenGalleryState.createGalleryInfo(target)
        assertEquals(778899L, infoDefault.gid)
        assertEquals("1234567890", infoDefault.token)
        assertEquals(fallbackTitle, infoDefault.title)

        val infoCustom = DesktopOpenGalleryState.createGalleryInfo(target, customTitle = "My Custom Title")
        assertEquals("My Custom Title", infoCustom.title)

        // 三角验证：空标题或空白标题回退默认
        val infoBlank = DesktopOpenGalleryState.createGalleryInfo(target, customTitle = "   ")
        assertEquals(fallbackTitle, infoBlank.title)
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
