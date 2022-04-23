package net.joosa.musify.artist

import kotlinx.coroutines.runBlocking
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/musify/music-artist")
class ArtistController(
    private val artistService: ArtistService
) {

    @GetMapping("/details/{mbid}")
    fun getArtistDetails(
        @PathVariable mbid: UUID
    ): Artist {
        return runBlocking {
            artistService.getArtist(mbid)
        }
    }
}
