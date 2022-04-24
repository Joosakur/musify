package net.joosa.musify.clients

import com.fasterxml.jackson.annotation.JsonProperty
import com.github.benmanes.caffeine.cache.Caffeine
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.bodyToMono
import java.util.concurrent.TimeUnit

@Component
class WikipediaClient(
    @Value("\${app.clients.wikipedia.base-url}") baseUrl: String
) {
    private val client = jsonClientBuilder(baseUrl, followRedirects = true).build()

    private val cache = Caffeine.newBuilder()
        .expireAfterWrite(2, TimeUnit.HOURS)
        .maximumSize(100_000)
        .build<String, String>()

    suspend fun getWikipediaSummary(pageTitle: String): String? {
        return cache.getIfPresent(pageTitle) ?: client
            .get()
            .uri("/page/summary/{title}", pageTitle)
            .retrieve()
            .mapErrors("to wikipedia")
            .bodyToMono<WikipediaResponse>()
            .defaultRetries()
            .awaitSingle()
            .html
            .also { cache.put(pageTitle, it) }
    }

    data class WikipediaResponse(
        @JsonProperty("extract_html")
        val html: String
    )
}
