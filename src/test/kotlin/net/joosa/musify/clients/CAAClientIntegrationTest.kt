package net.joosa.musify.clients

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertThrows
import java.net.URI
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class CAAClientIntegrationTest {

    val mockServer = MockWebServer()
    val client = CAAClient("http://localhost:9000")

    @BeforeEach
    internal fun setUp() {
        mockServer.start(9000)
    }

    @AfterEach
    internal fun tearDown() {
        mockServer.shutdown()
    }

    val mbid = UUID.randomUUID()
    val responseBody = CAAClient.ReleaseGroupResponse(
        images = listOf(
            CAAClient.Image(
                approved = true,
                front = true,
                image = URI("http://example.com/123.jpg")
            )
        )
    )

    @Test
    fun `smoke test`() {
        mockServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(jacksonObjectMapper().writeValueAsString(responseBody))
                .addHeader("Content-Type", "application/json")
        )

        val received = runBlocking { client.getPrimaryImageUrl(mbid) }
        assertEquals(responseBody.images.first().image, received)
    }

    @Test
    fun `client errors are not retried`() {
        mockServer.enqueue(MockResponse().setResponseCode(400))
        mockServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(jacksonObjectMapper().writeValueAsString(responseBody))
                .addHeader("Content-Type", "application/json")
        )

        assertThrows<HttpClientError> {
            runBlocking { client.getPrimaryImageUrl(mbid) }
        }
    }

    @Test
    fun `server errors are retried`() {
        mockServer.enqueue(MockResponse().setResponseCode(500))
        mockServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(jacksonObjectMapper().writeValueAsString(responseBody))
                .addHeader("Content-Type", "application/json")
        )

        val received = runBlocking { client.getPrimaryImageUrl(mbid) }
        assertEquals(responseBody.images.first().image, received)
    }

    @Test
    fun `responses are cached`() {
        mockServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(jacksonObjectMapper().writeValueAsString(responseBody))
                .addHeader("Content-Type", "application/json")
        )

        val received1 = runBlocking { client.getPrimaryImageUrl(mbid) }
        val received2 = runBlocking { client.getPrimaryImageUrl(mbid) }
        assertEquals(responseBody.images.first().image, received1)
        assertEquals(responseBody.images.first().image, received2)

        assertEquals(1, mockServer.requestCount)
    }
}
