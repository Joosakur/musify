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

fun WebClient.ResponseSpec.mapErrors(context: String) = this
    .onStatus(HttpStatus::is4xxClientError) { res -> throw HttpClientError(res.statusCode().value(), context) }
    .onStatus(HttpStatus::is5xxServerError) { res -> throw HttpServerError(res.statusCode().value(), context) }

fun <T> Mono<T>.defaultRetries() = retryWhen(
    Retry
        .backoff(3, Duration.ofMillis(500)).jitter(0.5)
        .filter { it is HttpServerError }
)

sealed class HttpError(status: Int, context: String) : RuntimeException("Request $context failed with status $status")
class HttpClientError(status: Int, context: String) : HttpError(status, context)
class HttpServerError(status: Int, context: String) : HttpError(status, context)
