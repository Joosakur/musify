package net.joosa.musify.artist

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import net.joosa.musify.clients.CAAClient
import net.joosa.musify.clients.HttpError
import net.joosa.musify.clients.MBClient
import net.joosa.musify.clients.WikidataClient
import net.joosa.musify.clients.WikipediaClient
import org.springframework.stereotype.Service
import java.net.URI
import java.util.*

@Service
class ArtistService(
    private val mbClient: MBClient,
    private val wikidataClient: WikidataClient,
    private val wikipediaClient: WikipediaClient,
    private val caaClient: CAAClient
) {
    suspend fun getArtist(mbid: UUID): Artist = coroutineScope {
        val mbArtist = mbClient.getArtist(mbid)

        val albums = async { getAlbums(mbArtist) }
        val description = async { getWikiDescription(mbArtist) }

        Artist(
            mbid = mbArtist.mbid,
            name = mbArtist.name,
            gender = mbArtist.gender,
            country = mbArtist.country,
            disambiguation = mbArtist.disambiguation,
            description = description.await(),
            albums = albums.await()
        )
    }

    private suspend fun getAlbums(artist: MBClient.MBArtist) = coroutineScope {
        artist.albums.map { album ->
            async {
                Album(
                    id = album.id,
                    title = album.title,
                    imageUrl = try {
                        caaClient.getPrimaryImageUrl(album.id)
                    } catch (e: HttpError) {
                        null
                    }
                )
            }
        }.awaitAll()
    }
    private suspend fun getWikiDescription(artist: MBClient.MBArtist): String? {
        if (artist.wikiDataId == null) return null

        val pageTitle = wikidataClient.getWikipediaTitle(artist.wikiDataId) ?: return null

        return wikipediaClient.getWikipediaSummary(pageTitle)
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
