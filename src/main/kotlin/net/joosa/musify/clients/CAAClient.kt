package net.joosa.musify.clients

import com.github.benmanes.caffeine.cache.Caffeine
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.bodyToMono
import java.net.URI
import java.util.UUID
import java.util.concurrent.TimeUnit

@Component
class CAAClient(
    @Value("\${app.clients.caa.base-url}") baseUrl: String
) {
    private val client = jsonClientBuilder(baseUrl, followRedirects = true).build()

    private val cache = Caffeine.newBuilder()
        .expireAfterWrite(2, TimeUnit.DAYS)
        .maximumSize(100_000)
        .build<UUID, URI>()

    suspend fun getPrimaryImageUrl(mbid: UUID): URI? {
        return cache.getIfPresent(mbid) ?: client
            .get()
            .uri("/release-group/{mbid}", mbid)
            .retrieve()
            .mapErrors("to cover art archive")
            .bodyToMono<ReleaseGroupResponse>()
            .defaultRetries()
            .awaitSingle()
            .let { res -> getPrimaryImage(res.images)?.image?.also { cache.put(mbid, it) } }
    }

    data class ReleaseGroupResponse(
        val images: List<Image>
    )

    data class Image(
        val approved: Boolean,
        val front: Boolean,
        val image: URI
    )
}

fun getPrimaryImage(images: List<CAAClient.Image>): CAAClient.Image? {
    return images.firstOrNull { it.approved && it.front }
        ?: images.firstOrNull { it.front }
        ?: images.firstOrNull { it.approved }
        ?: images.firstOrNull()
}
