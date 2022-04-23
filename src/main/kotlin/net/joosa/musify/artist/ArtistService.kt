package net.joosa.musify.artist

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import net.joosa.musify.clients.CAAClient
import net.joosa.musify.clients.MBClient
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientException
import java.net.URI
import java.util.*

@Service
class ArtistService(
    private val mbClient: MBClient,
    private val caaClient: CAAClient
) {
    suspend fun getArtist(mbid: UUID): Artist {
        val mbArtist = mbClient.getArtist(mbid)

        val albums = runBlocking {
            mbArtist.albums.map { album ->
                async {
                    Album(
                        id = album.id,
                        title = album.title,
                        imageUrl = try {
                            caaClient.getPrimaryImageUrl(album.id)
                        } catch (e: WebClientException) {
                            null
                        }
                    )
                }
            }.awaitAll()
        }

        return Artist(
            mbid = mbArtist.mbid,
            name = mbArtist.name,
            gender = mbArtist.gender,
            country = mbArtist.country,
            disambiguation = mbArtist.disambiguation,
            description = null, // TODO
            albums = albums
        )
    }
}

data class Artist(
    val mbid: UUID,
    val name: String,
    val gender: String?,
    val country: String?,
    val disambiguation: String?,
    val description: String?,
    val albums: List<Album>
)

data class Album(
    val id: UUID,
    val title: String,
    val imageUrl: URI?
)
