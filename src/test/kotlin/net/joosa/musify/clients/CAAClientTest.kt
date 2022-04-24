package net.joosa.musify.clients

import java.net.URI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CAAClientTest {

    val image1 = CAAClient.Image(
        approved = false,
        front = false,
        image = URI("http://example.con/1.jpg")
    )

    val image2 = CAAClient.Image(
        approved = true,
        front = false,
        image = URI("http://example.con/2.jpg")
    )

    val image3 = CAAClient.Image(
        approved = false,
        front = true,
        image = URI("http://example.con/3.jpg")
    )

    val image4 = CAAClient.Image(
        approved = true,
        front = true,
        image = URI("http://example.con/4.jpg")
    )

    @Test
    fun `getPrimaryImage prefers approved front images`() {
        val res1 = getPrimaryImage(listOf(image1, image2, image3, image4))
        assertEquals(image4, res1)

        val res1b = getPrimaryImage(listOf(image1, image4, image2, image3))
        assertEquals(image4, res1b)

        val res2 = getPrimaryImage(listOf(image1, image2, image3))
        assertEquals(image3, res2)

        val res3 = getPrimaryImage(listOf(image1, image2))
        assertEquals(image2, res3)

        val res4 = getPrimaryImage(listOf(image1))
        assertEquals(image1, res4)

        val res5 = getPrimaryImage(emptyList())
        assertNull(res5)
    }
}
