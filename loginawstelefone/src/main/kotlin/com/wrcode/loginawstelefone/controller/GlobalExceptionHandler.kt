package com.wrcode.loginawstelefone.controller

import com.wrcode.loginawstelefone.service.ConfirmationNotFoundException
import com.wrcode.loginawstelefone.service.InvalidConfirmationCodeException
import com.wrcode.loginawstelefone.service.UserNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(ConfirmationNotFoundException::class, UserNotFoundException::class)
    fun handleNotFound(ex: RuntimeException): ResponseEntity<Map<String, String?>> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to ex.message))

    @ExceptionHandler(InvalidConfirmationCodeException::class)
    fun handleInvalidCode(ex: RuntimeException): ResponseEntity<Map<String, String?>> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mapOf("error" to ex.message))

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<Map<String, String?>> {
        val message = ex.bindingResult.fieldErrors.joinToString(", ") { "${it.field}: ${it.defaultMessage}" }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mapOf("error" to message))
    }
}
