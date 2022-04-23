package net.joosa.musify

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class MusifyApplication

fun main(args: Array<String>) {
    runApplication<MusifyApplication>(*args)
}
