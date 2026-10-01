package com.ehviewer.desktop

import com.ehviewer.core.model.BaseGalleryInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DesktopGalleryHydratorTest {

    private fun gdataBody(gid: Long = 778899L): String = """
        {
          "gmetadata": [
            {
              "gid": $gid,
              "token": "1234567890",
              "title": "Hydrated Title [Artist]",
              "title_jpn": "日本語",
              "category": "Manga",
              "thumb": "https://ehgt.org/ab/cd/efg.jpg",
              "uploader": "someone",
              "posted": 1700000000,
              "filecount": 20,
              "rating": 4.5,
              "tags": ["language:chinese", "artist:foo"]
            }
          ]
        }
    """.trimIndent()

    @Test
    fun gdataRequestBody_matchesOfficialApiContract() {
        assertEquals(
            """{"method":"gdata","gidlist":[[778899,"1234567890"]],"namespace":1}""",
            DesktopGalleryHydrator.gdataRequestBody(778899L, "1234567890"),
        )
    }

    @Test
    fun needsHydration_flagsPlaceholderWithoutMetadata() {
        assertTrue(DesktopGalleryHydrator.needsHydration(BaseGalleryInfo(gid = 1, token = "t")))
        assertFalse(
            DesktopGalleryHydrator.needsHydration(
                BaseGalleryInfo(gid = 1, token = "t", thumbKey = "ab/cd.jpg"),
            ),
        )
    }

    @Test
    fun fetchInfo_success_appliesMetadataOntoFreshInfo() {
        var capturedUrl: String? = null
        var capturedBody: String? = null
        val info = DesktopGalleryHydrator.fetchInfo(778899L, "1234567890") { url, body ->
            capturedUrl = url
            capturedBody = body
            DesktopResponse(200, gdataBody())
        }

        assertNotNull(info)
        assertEquals(DesktopGalleryHydrator.GDATA_URL, capturedUrl)
        assertEquals(DesktopGalleryHydrator.gdataRequestBody(778899L, "1234567890"), capturedBody)
        assertEquals("Hydrated Title [Artist]", info.title)
        assertEquals("日本語", info.titleJpn)
        assertEquals("ab/cd/efg.jpg", info.thumbKey)
        assertEquals(20, info.pages)
        assertEquals(4.5f, info.rating)
        assertEquals(listOf("language:chinese", "artist:foo"), info.simpleTags)
        assertEquals("someone", info.uploader)
    }

    @Test
    fun fetchInfo_nonSuccessHttpStatus_returnsNull() {
        assertNull(
            DesktopGalleryHydrator.fetchInfo(1L, "1234567890") { _, _ ->
                DesktopResponse(404, "not found")
            },
        )
    }

    @Test
    fun fetchInfo_emptyGmetadata_returnsNull() {
        assertNull(
            DesktopGalleryHydrator.fetchInfo(1L, "1234567890") { _, _ ->
                DesktopResponse(200, """{"gmetadata":[]}""")
            },
        )
    }

    @Test
    fun fetchInfo_malformedJson_returnsNull() {
        assertNull(
            DesktopGalleryHydrator.fetchInfo(1L, "1234567890") { _, _ ->
                DesktopResponse(200, "<html>gateway error</html>")
            },
        )
    }

    @Test
    fun fetchInfo_transportFailure_returnsNull() {
        assertNull(
            DesktopGalleryHydrator.fetchInfo(1L, "1234567890") { _, _ ->
                throw java.net.ConnectException("Connection refused")
            },
        )
    }
}
