package com.wrcode.loginawstelefone.model

import java.time.Instant

/**
 * Usuário do sistema. O login é feito por telefone + uuid do dispositivo.
 * - `active` indica se o usuário já confirmou o telefone via código SMS.
 * - `uuid` é o identificador do dispositivo atualmente vinculado ao usuário.
 */
data class User(
    val id: String,
    val phone: String,
    var uuid: String,
    var active: Boolean = false,
    var name: String? = null,
    var email: String? = null,
    /** Hash da senha (nunca armazenamos/retornamos a senha em texto puro). */
    var passwordHash: String? = null,
    var description: String? = null,
    var avatar: String? = null,
    val createdAt: Instant = Instant.now(),
    var updatedAt: Instant = Instant.now(),
)
