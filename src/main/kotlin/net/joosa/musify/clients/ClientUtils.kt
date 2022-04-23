package net.joosa.musify.clients

import io.netty.channel.ChannelOption
import io.netty.handler.timeout.ReadTimeoutHandler
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import reactor.netty.http.client.HttpClient
import reactor.util.retry.Retry
import java.time.Duration
import java.util.concurrent.TimeUnit

fun jsonClientBuilder(baseurl: String, timeoutMillis: Int = 10_000, followRedirects: Boolean = false): WebClient.Builder {
    val httpClient = HttpClient.create()
        .followRedirect(followRedirects)
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeoutMillis)
        .responseTimeout(Duration.ofMillis(timeoutMillis.toLong()))
        .doOnConnected { connection ->
            connection.addHandlerFirst(ReadTimeoutHandler(timeoutMillis.toLong(), TimeUnit.MILLISECONDS))
        }

    return WebClient.builder()
        .clientConnector(ReactorClientHttpConnector(httpClient))
        .baseUrl(baseurl)
        .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
}

fun WebClient.ResponseSpec.mapErrors() = this
    .onStatus(HttpStatus::is4xxClientError) { a -> throw HttpClientError(a.statusCode().value()) }
    .onStatus(HttpStatus::is5xxServerError) { a -> throw HttpServerError(a.statusCode().value()) }

fun <T> Mono<T>.defaultRetries() = retryWhen(
    Retry
        .backoff(3, Duration.ofMillis(500)).jitter(0.5)
        .filter { it is HttpServerError }
)

class HttpClientError(status: Int) : RuntimeException("Request failed with status $status")
class HttpServerError(status: Int) : RuntimeException("Request failed with status $status")
