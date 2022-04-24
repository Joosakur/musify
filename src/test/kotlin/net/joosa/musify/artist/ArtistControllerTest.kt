package net.joosa.musify.artist

import com.fasterxml.jackson.databind.ObjectMapper
import net.joosa.musify.clients.CAAClient
import net.joosa.musify.clients.MBClient
import net.joosa.musify.clients.WikidataClient
import net.joosa.musify.clients.WikipediaClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Test
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpStatus
import java.net.URI
import java.util.*
import kotlin.test.assertEquals

@SpringBootTest
class ArtistControllerTest {
    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    @Autowired
    lateinit var objectMapper: ObjectMapper

    val mbMockServer = MockWebServer()
    val wikidataMockServer = MockWebServer()
    val wikipediaMockServer = MockWebServer()
    val caaMockServer = MockWebServer()

    @BeforeEach
    internal fun setUp() {
        mbMockServer.start(9001)
        wikidataMockServer.start(9002)
        wikipediaMockServer.start(9003)
        caaMockServer.start(9004)
    }

    @AfterEach
    internal fun tearDown() {
        mbMockServer.shutdown()
        wikidataMockServer.shutdown()
        wikipediaMockServer.shutdown()
        caaMockServer.shutdown()
    }

    @Test
    fun `test request`() {
        val mbid = UUID.randomUUID()
        val wikiId = "Q1234"

        val mbArtist = MBClient.ArtistResponse(
            id = mbid,
            name = "Donald",
            gender = "Male",
            country = "US",
            disambiguation = "King of Ducks",
            relations = listOf(
                MBClient.Relation(
                    type = "wikidata",
                    url = MBClient.RelationUrl(
                        resource = URI("https://www.wikidata.org/wiki/$wikiId")
                    )
                )
            ),
            releaseGroups = listOf(
                MBClient.ReleaseGroup(
                    id = UUID.randomUUID(),
                    title = "Best of Duckburg",
                    primaryType = "Album"
                )
            )
        )

        mbMockServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody(objectMapper.writeValueAsString(mbArtist))
        )

        wikidataMockServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody(
                    objectMapper.writeValueAsString(
                        WikidataClient.WikidataResponse(
                            entities = mapOf(
                                wikiId to WikidataClient.Entity(
                                    sitelinks = WikidataClient.SiteLinks(
                                        enwiki = WikidataClient.Site(
                                            url = URI("https://en.wikipedia.org/wiki/Donald_Duck")
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
        )

        val description = "<p>Best singing duck ever</p>"
        wikipediaMockServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody(
                    objectMapper.writeValueAsString(
                        WikipediaClient.WikipediaResponse(
                            html = description
                        )
                    )
                )
        )

        val imageUrl = URI("https://upload.wikimedia.org/wikipedia/fi/5/55/Duckburg.jpg")
        caaMockServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody(
                    objectMapper.writeValueAsString(
                        CAAClient.ReleaseGroupResponse(
                            images = listOf(
                                CAAClient.Image(
                                    approved = false,
                                    front = true,
                                    image = imageUrl
                                )
                            )
                        )
                    )
                )
        )

        val artist = restTemplate.getForEntity(
            "http://localhost:8081/musify/music-artist/details/cc197bad-dc9c-440d-a5b5-d52ba2e14232",
            Artist::class.java
        )

        assertEquals(HttpStatus.OK, artist.statusCode)
        assertEquals(
            expected = Artist(
                mbid = mbid,
                name = mbArtist.name,
                gender = mbArtist.gender,
                country = mbArtist.country,
                disambiguation = mbArtist.disambiguation,
                description = description,
                albums = listOf(
                    Album(
                        id = mbArtist.releaseGroups.first().id,
                        title = mbArtist.releaseGroups.first().title,
                        imageUrl = imageUrl
                    )
                )
            ),
            actual = artist.body
        )
    }
}
