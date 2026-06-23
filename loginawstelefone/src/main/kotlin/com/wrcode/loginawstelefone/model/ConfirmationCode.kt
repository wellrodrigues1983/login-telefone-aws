package com.wrcode.loginawstelefone.model

import java.time.Instant

/**
 * Código de confirmação enviado por SMS para um par telefone + uuid.
 * Fica pendente até ser confirmado no endpoint POST /users/confirm.
 */
data class ConfirmationCode(
    val phone: String,
    val uuid: String,
    val code: String,
    val createdAt: Instant = Instant.now(),
    val expiresAt: Instant = Instant.now().plusSeconds(600), // 10 minutos
) {
    fun isExpired(): Boolean = Instant.now().isAfter(expiresAt)
}
