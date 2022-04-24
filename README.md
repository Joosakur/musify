# Musify

This is a coding challenge exercise.

## Prerequisites
- Java 11+

## Building and running
- `./gradlew bootRun` to run the application
- `./gradlew test` to run tests

## Logic
The main flow is:
- Fetch artist from MusicBrainz
- In parallel
  - Task 1: if artist has wikidata link fetch data from there and if it contains entry for English wikipedia fetch summary html from there
  - Task 2: try fetching cover art for each album in parallel
    - Catch errors so that issue with one cover does not fail the whole request
    - Prefer approved front images if there are multiple options
- Combine the data to a response

Each successful client call result is cached separately with a possibility to have different TTL and other config based on e.g. estimated update frequency.

Each client call has a few quick auto-retries for 5xx errors with an exponential backoff and some jitter.

## Used tools
- Gradle
- Kotlin
- Spring Boot
- WebClient from Spring WebFlux as HTTP client
- Caffeine as local in-memory cache
- JUnit 5 as testing framework
- okhttp MockWebServer for mocking responses
- ktlint for code formatting

## Todo
- Discuss whether partial responses or errors are preferred
- Consider having 2nd level shared cache in e.g. Redis
- Circuit breaker for 3rd party APIs?
- Rate limiting?
- Figure out if possible to query less data from wikidata
- Consider distributed tracing
- Add OpenAPI definition

## Example urls
- http://localhost:8081/musify/music-artist/details/f27ec8db-af05-4f36-916e-3d57f91ecf5e
- http://localhost:8081/musify/music-artist/details/83d91898-7763-47d7-b03b-b92132375c47
- http://localhost:8081/musify/music-artist/details/cc197bad-dc9c-440d-a5b5-d52ba2e14234
