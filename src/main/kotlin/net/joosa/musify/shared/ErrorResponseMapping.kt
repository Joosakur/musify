package net.joosa.musify.shared

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import javax.servlet.http.HttpServletRequest

@ControllerAdvice
class ErrorResponseMapping : ResponseEntityExceptionHandler() {

    @ExceptionHandler(value = [NotFoundException::class])
    fun notFound(req: HttpServletRequest, ex: NotFoundException): ResponseEntity<Unit> {
        logger.warn("Handling NotFoundException: ${ex.message}")
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build()
    }
}
