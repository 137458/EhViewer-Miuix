package com.ehviewer.core.database.client

import com.ehviewer.core.model.GalleryInfo
import com.ehviewer.core.util.unescapeXml
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// E-Hentai 官方 JSON API（api.php method=gdata）响应解析。
// 与 Android 侧 EhUtils 的分类常量/命名保持一致；HTML 列表解析依赖 Rust 库，不在此范围。
object GalleryMetadataParser {
    val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    // yyyy-MM-dd HH:mm
    private val postedFormatter = kotlinx.datetime.LocalDateTime.Format {
        year()
        char('-')
        monthNumber()
        char('-')
        day()
        char(' ')
        hour()
        char(':')
        minute()
    }

    fun parse(body: String, galleryInfoList: List<GalleryInfo>) {
        val result = json.decodeFromString<Result>(body)
        result.items.forEach { item ->
            val gi = galleryInfoList.find { it.gid == item.gid } ?: return@forEach
            gi.apply {
                title = item.title.unescapeXml()
                titleJpn = item.titleJpn.unescapeXml()
                category = getCategory(item.category)
                thumbKey = getThumbKey(item.thumb)
                uploader = item.uploader?.unescapeXml()
                posted = postedFormatter.format(
                    Instant.fromEpochMilliseconds(item.posted * 1000)
                        // 与 Android ParserUtils.formatDate（默认 UTC）语义一致，委托重构不得改变现网行为
                        .toLocalDateTime(TimeZone.UTC),
                )
                rating = item.rating
                simpleTags = item.tags
                pages = item.pages
                generateSLang()
            }
        }
    }

    @Serializable
    data class Result(@SerialName("gmetadata") val items: List<Item>)

    @Serializable
    data class Item(
        val gid: Long,
        val title: String,
        @SerialName("title_jpn")
        val titleJpn: String,
        val category: String,
        val thumb: String,
        // Null in some old galleries
        val uploader: String?,
        val posted: Long,
        @SerialName("filecount")
        val pages: Int,
        val rating: Float,
        val tags: List<String>,
    )
}

// 分类位掩码（与 EhUtils 对齐）
const val CATEGORY_MISC = 0x1
const val CATEGORY_DOUJINSHI = 0x2
const val CATEGORY_MANGA = 0x4
const val CATEGORY_ARTIST_CG = 0x8
const val CATEGORY_GAME_CG = 0x10
const val CATEGORY_IMAGE_SET = 0x20
const val CATEGORY_COSPLAY = 0x40
const val CATEGORY_ASIAN_PORN = 0x80
const val CATEGORY_NON_H = 0x100
const val CATEGORY_WESTERN = 0x200
const val CATEGORY_PRIVATE = 0x400
const val CATEGORY_UNKNOWN = 0x800

private val CATEGORY_STRINGS = listOf(
    CATEGORY_MISC to arrayOf("misc"),
    CATEGORY_DOUJINSHI to arrayOf("doujinshi"),
    CATEGORY_MANGA to arrayOf("manga"),
    CATEGORY_ARTIST_CG to arrayOf("artistcg", "Artist CG Sets", "Artist CG"),
    CATEGORY_GAME_CG to arrayOf("gamecg", "Game CG Sets", "Game CG"),
    CATEGORY_IMAGE_SET to arrayOf("imageset", "Image Sets", "Image Set"),
    CATEGORY_COSPLAY to arrayOf("cosplay"),
    CATEGORY_ASIAN_PORN to arrayOf("asianporn", "Asian Porn"),
    CATEGORY_NON_H to arrayOf("non-h"),
    CATEGORY_WESTERN to arrayOf("western"),
    CATEGORY_PRIVATE to arrayOf("private"),
    CATEGORY_UNKNOWN to arrayOf("unknown"),
)

fun getCategory(type: String?): Int {
    for (entry in CATEGORY_STRINGS) {
        for (str in entry.second) {
            if (str.equals(type, ignoreCase = true)) {
                return entry.first
            }
        }
    }
    return CATEGORY_UNKNOWN
}

fun getCategoryName(category: Int): String = when (category) {
    CATEGORY_MISC -> "misc"
    CATEGORY_DOUJINSHI -> "doujinshi"
    CATEGORY_MANGA -> "manga"
    CATEGORY_ARTIST_CG -> "artistcg"
    CATEGORY_GAME_CG -> "gamecg"
    CATEGORY_IMAGE_SET -> "imageset"
    CATEGORY_COSPLAY -> "cosplay"
    CATEGORY_ASIAN_PORN -> "asianporn"
    CATEGORY_NON_H -> "non-h"
    CATEGORY_WESTERN -> "western"
    CATEGORY_PRIVATE -> "private"
    else -> "unknown"
}

fun getCategoryDisplayName(category: Int): String = when (category) {
    CATEGORY_MISC -> "Misc"
    CATEGORY_DOUJINSHI -> "Doujinshi"
    CATEGORY_MANGA -> "Manga"
    CATEGORY_ARTIST_CG -> "Artist CG"
    CATEGORY_GAME_CG -> "Game CG"
    CATEGORY_IMAGE_SET -> "Image Set"
    CATEGORY_COSPLAY -> "Cosplay"
    CATEGORY_ASIAN_PORN -> "Asian Porn"
    CATEGORY_NON_H -> "Non-H"
    CATEGORY_WESTERN -> "Western"
    CATEGORY_PRIVATE -> "Private"
    else -> "Unknown"
}

// 与 EhCacheKeyFactory 对齐
const val URL_PREFIX_THUMB_E = "https://ehgt.org/"
const val URL_PREFIX_THUMB_EX = "https://s.exhentai.org/"
private const val URL_PREFIX_V1_THUMB_EX = URL_PREFIX_THUMB_EX + "t/"

fun getThumbKey(url: String): String = url.removePrefix(URL_PREFIX_THUMB_E).removePrefix(URL_PREFIX_V1_THUMB_EX).removePrefix(URL_PREFIX_THUMB_EX)

fun keyToThumbUrl(key: String, isExHentai: Boolean = false): String = if (key.startsWith("https:")) {
    key
} else {
    val prefix = if (key.endsWith("webp")) {
        if (isExHentai) URL_PREFIX_THUMB_EX else URL_PREFIX_THUMB_E
    } else {
        if (isExHentai) URL_PREFIX_V1_THUMB_EX else URL_PREFIX_THUMB_E
    }
    prefix + key
}

val GalleryInfo.thumbUrl: String?
    get() = thumbKey?.let { keyToThumbUrl(it) }
