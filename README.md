# Musify

This is a coding challenge exercise.

## Prerequisites
- Java 11+

## Building and running
- `./gradlew bootRun` to run the application
- `./gradlew test` to run tests

## Used tools
- Gradle
- Kotlin
- Spring Boot
- WebClient from Spring WebFlux as HTTP client
- Caffeine as cache
- JUnit 5 as testing framework
- okhttp MockWebServer for mocking responses
- ktlint for code formatting

## Todo
- Proper logging
- Move some hard coded constants into config
- Consider integrating to distributed tracing solution
- Discuss whether partial responses or errors are preferred
- Add OpenAPI definition
- Consider having 2nd level shared cache in e.g. Redis
- Circuit breaker for 3rd party APIs?
- Rate limiting?
