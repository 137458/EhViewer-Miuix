package com.ehviewer.desktop

import java.nio.file.Path
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.test.Test
import kotlin.test.assertTrue

// i18n 守卫：直接解析 core/i18n 的 moko-resources XML（不依赖 moko 运行时），防止两类回归：
// 1. 新增 desktop_* key 只补 base/zh-rCN，导致其他语言静默回落英文；
// 2. 各语言与 base 的格式化占位符不一致（运行期 WrongArgumentCount/错位填充）。
// 维护策略：desktop_* 要求 base + zh-rCN + zh-rTW + zh-rHK + ja 全覆盖；
// de/es/fr/ko/nb-rNO/th/tr 历史缺口大，允许对非 desktop_* key 回落 base。
class I18nResourceConsistencyTest {

    private val formatArg = Regex("%(\\d+\\$)?[-+ #0]*(\\d+)?(\\.\\d+)?[sdf]")

    private fun resourcesRoot(): Path {
        var dir = Path.of(System.getProperty("user.dir")).toAbsolutePath()
        repeat(5) {
            val candidate = dir.resolve("core/i18n/src/commonMain/moko-resources")
            if (candidate.toFile().isDirectory) return candidate
            val parent = dir.parent ?: error("moko-resources directory not found from ${System.getProperty("user.dir")}")
            dir = parent
        }
        error("moko-resources directory not found from ${System.getProperty("user.dir")}")
    }

    // key -> 格式化占位符列表（%% 转义不计）
    private fun parseFormatArgs(localeDir: Path): Map<String, List<String>> {
        val file = localeDir.resolve("strings.xml").toFile()
        if (!file.isFile) return emptyMap()
        val root = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val nodes = root.getElementsByTagName("string")
        val out = mutableMapOf<String, List<String>>()
        for (i in 0 until nodes.length) {
            val element = nodes.item(i) as org.w3c.dom.Element
            out[element.getAttribute("name")] = formatArg.findAll(element.textContent)
                .map { it.value }
                .filterNot { it == "%%" }
                .toList()
        }
        return out
    }

    @Test
    fun desktopSpecificKeys_coverAllMaintainedLocales() {
        val root = resourcesRoot()
        val desktopKeys = parseFormatArgs(root.resolve("base")).keys.filter { it.startsWith("desktop_") }
        assertTrue(desktopKeys.isNotEmpty(), "no desktop_* keys found in base locale")

        val missing = linkedMapOf<String, List<String>>()
        for (locale in REQUIRED_DESKTOP_LOCALES) {
            val keys = parseFormatArgs(root.resolve(locale)).keys
            val absent = desktopKeys.filterNot { it in keys }
            if (absent.isNotEmpty()) missing[locale] = absent
        }
        assertTrue(missing.isEmpty(), "desktop_* keys missing in maintained locales: $missing")
    }

    @Test
    fun sharedKeys_keepPlaceholderConsistencyWithBase() {
        val root = resourcesRoot()
        val base = parseFormatArgs(root.resolve("base"))
        val mismatch = mutableListOf<String>()
        val localeDirs = root.toFile().listFiles { f -> f.isDirectory }?.sortedBy { it.name } ?: emptyList()
        for (localeDir in localeDirs) {
            parseFormatArgs(localeDir.toPath()).forEach { (key, args) ->
                val baseArgs = base[key]
                if (baseArgs != null && baseArgs != args) {
                    mismatch += "${localeDir.name}:$key base=$baseArgs localized=$args"
                }
            }
        }
        assertTrue(mismatch.isEmpty(), "format placeholder mismatch vs base: $mismatch")
    }

    companion object {
        private val REQUIRED_DESKTOP_LOCALES = listOf("base", "zh-rCN", "zh-rTW", "zh-rHK", "ja")
    }
}
