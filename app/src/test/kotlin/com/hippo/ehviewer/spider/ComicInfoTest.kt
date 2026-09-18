package com.hippo.ehviewer.spider

import kotlin.test.Test
import kotlin.test.assertTrue

class ComicInfoTest {

    @Test
    fun serializeComicInfo_containsMangaReadingDirectionAndAgeRating() {
        val info = ComicInfo(
            series = "Test Series",
            alternateSeries = null,
            writer = null,
            penciller = null,
            genre = null,
            web = "https://e-hentai.org",
            pageCount = 10,
            languageISO = "en",
            characters = null,
            teams = null,
            communityRating = "4.5",
        )

        val xmlStr = serializeComicInfo(info)

        assertTrue(
            xmlStr.contains("<Manga>YesAndRightToLeft</Manga>"),
            "Expected ComicInfo XML to contain Manga reading direction element",
        )
        assertTrue(
            xmlStr.contains("<AgeRating>Adult</AgeRating>"),
            "Expected ComicInfo XML to contain AgeRating element",
        )
    }
}
