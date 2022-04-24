package net.joosa.musify.shared

open class NotFoundException(msg: String = "Resource not found") : RuntimeException(msg)

class ArtistNotFoundException(id: String) : NotFoundException("Artist $id not found")
