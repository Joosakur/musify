package net.joosa.musify.clients

import com.github.benmanes.caffeine.cache.Caffeine
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.bodyToMono
import java.net.URI
import java.util.concurrent.TimeUnit

@Component
class WikidataClient(
    @Value("\${app.clients.wikidata.base-url}") baseUrl: String
) {
    private val client = jsonClientBuilder(baseUrl, followRedirects = true)
        .codecs { it.defaultCodecs().maxInMemorySize(5 * 1024 * 1024) } // 5 MB
        .build()

    private val cache = Caffeine.newBuilder()
        .expireAfterWrite(1, TimeUnit.DAYS)
        .maximumSize(100_000)
        .build<String, String>()

    suspend fun getWikipediaTitle(wikiId: String): String? {
        return cache.getIfPresent(wikiId) ?: client
            .get()
            .uri("/Special:EntityData/{wikiId}.json", wikiId)
            .retrieve()
            .mapErrors("to wikidata")
            .bodyToMono<WikidataResponse>()
            .defaultRetries()
            .awaitSingle()
            .let { res -> res.entities[wikiId]?.sitelinks?.enwiki?.url?.path?.split("/")?.last() }
            ?.also { cache.put(wikiId, it) }
    }

    data class WikidataResponse(
        val entities: Map<String, Entity>
    )

    data class Entity(
        val sitelinks: SiteLinks
    )

    data class SiteLinks(
        val enwiki: Site?
    )

    data class Site(
        val url: URI
    )
}
