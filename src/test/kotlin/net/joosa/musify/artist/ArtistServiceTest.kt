package net.joosa.musify.artist

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.runBlocking
import net.joosa.musify.clients.CAAClient
import net.joosa.musify.clients.MBClient
import net.joosa.musify.clients.WikidataClient
import net.joosa.musify.clients.WikipediaClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.net.URI
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringBootTest
class ArtistServiceTest {
    val mbMockServer = MockWebServer()
    val wikidataMockServer = MockWebServer()
    val wikipediaMockServer = MockWebServer()
    val caaMockServer = MockWebServer()

    @Autowired
    lateinit var artistService: ArtistService

    @Autowired
    lateinit var objectMapper: ObjectMapper

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

    final val mbid = UUID.randomUUID()
    val wikiId = "Q1234"

    val mbArtist = MBClient.ArtistResponse(
        id = mbid,
        name = "Donald",
        gender = null,
        country = "US",
        disambiguation = null,
        relations = listOf(
            MBClient.Relation(
                type = "wikidata",
                url = MBClient.RelationUrl(
                    resource = URI("https://www.wikidata.org/wiki/$wikiId")
                )
            )
        ),
        releaseGroups = listOf("1", "2", "3", "4", "5").map {
            MBClient.ReleaseGroup(
                id = UUID.randomUUID(),
                title = it,
                primaryType = "Album"
            )
        }
    )

    @Test
    fun `request are sent in parallel when possible`() {
        mbMockServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody(objectMapper.writeValueAsString(mbArtist))
                .setBodyDelay(1, TimeUnit.SECONDS)
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
                .setBodyDelay(1, TimeUnit.SECONDS)
        )

        wikipediaMockServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody(
                    objectMapper.writeValueAsString(
                        WikipediaClient.WikipediaResponse(
                            html = "<p>Best singing duck ever</p>"
                        )
                    )
                )
                .setBodyDelay(1, TimeUnit.SECONDS)
        )

        mbArtist.releaseGroups.forEach {
            val release = CAAClient.ReleaseGroupResponse(
                images = emptyList()
            )
            caaMockServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .addHeader("Content-Type", "application/json")
                    .setBody(objectMapper.writeValueAsString(release))
                    .setBodyDelay(1, TimeUnit.SECONDS)
            )
        }

        val startTime = System.currentTimeMillis()
        runBlocking {
            artistService.getArtist(mbid)
        }
        val endTime = System.currentTimeMillis()

        // with correctly implemented parallelization the request should take around 3s
        assertTrue { endTime - startTime < 3700 }

        assertEquals(1, mbMockServer.requestCount)
        assertEquals(1, wikidataMockServer.requestCount)
        assertEquals(1, wikipediaMockServer.requestCount)
        assertEquals(5, caaMockServer.requestCount)

        assertEquals("$wikiId.json", wikidataMockServer.takeRequest().path!!.split("/").last())
        assertEquals("Donald_Duck", wikipediaMockServer.takeRequest().path!!.split("/").last())
    }
}
