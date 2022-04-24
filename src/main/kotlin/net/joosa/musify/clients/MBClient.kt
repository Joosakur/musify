package net.joosa.musify.clients

import com.fasterxml.jackson.annotation.JsonProperty
import com.github.benmanes.caffeine.cache.Caffeine
import kotlinx.coroutines.reactor.awaitSingle
import net.joosa.musify.shared.ArtistNotFoundException
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.bodyToMono
import java.net.URI
import java.util.UUID
import java.util.concurrent.TimeUnit

@Component
class MBClient(
    @Value("\${app.clients.mb.base-url}") baseUrl: String
) {
    private val client = jsonClientBuilder(baseUrl).build()

    private val cache = Caffeine.newBuilder()
        .expireAfterWrite(1, TimeUnit.DAYS)
        .maximumSize(100_000)
        .build<UUID, MBArtist>()

    suspend fun getArtist(mbid: UUID): MBArtist {
        return cache.getIfPresent(mbid) ?: client
            .get()
            .uri("/artist/{mbid}?fmt=json&inc=url-rels+release-groups", mbid)
            .retrieve()
            .onStatus({ status -> status.value() == 404 }) { throw ArtistNotFoundException(mbid.toString()) }
            .mapErrors("to music brainz")
            .bodyToMono<ArtistResponse>()
            .defaultRetries()
            .map { res ->
                MBArtist(
                    mbid = res.id,
                    name = res.name,
                    gender = res.gender,
                    country = res.country,
                    disambiguation = res.disambiguation,
                    wikiDataId = res.relations
                        .firstOrNull { it.type == "wikidata" }
                        ?.url?.resource?.path?.split("/")?.last(),
                    albums = res.releaseGroups
                        .filter { it.primaryType == "Album" }
                        .map { MBAlbum(it.id, it.title) }
                )
            }
            .awaitSingle()
            .also { cache.put(mbid, it) }
    }

    data class ArtistResponse(
        val id: UUID,
        val name: String,
        val gender: String?,
        val country: String?,
        val disambiguation: String?,
        val relations: List<Relation>,
        @JsonProperty("release-groups")
        val releaseGroups: List<ReleaseGroup>
    )

    data class Relation(
        val type: String,
        val url: RelationUrl
    )

    data class RelationUrl(
        val resource: URI
    )

    data class ReleaseGroup(
        val id: UUID,
        val title: String,
        @JsonProperty("primary-type")
        val primaryType: String
    )

    data class MBArtist(
        val mbid: UUID,
        val name: String,
        val gender: String?,
        val country: String?,
        val disambiguation: String?,
        val wikiDataId: String?,
        val albums: List<MBAlbum>
    )

    data class MBAlbum(
        val id: UUID,
        val title: String
    )
}
