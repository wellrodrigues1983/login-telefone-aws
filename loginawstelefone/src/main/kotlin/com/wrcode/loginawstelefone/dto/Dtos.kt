package com.wrcode.loginawstelefone.dto

import com.wrcode.loginawstelefone.model.User
import jakarta.validation.constraints.NotBlank

/** Corpo de POST /users/login */
data class LoginRequest(
    @field:NotBlank(message = "telefone é obrigatório")
    val phone: String,
    @field:NotBlank(message = "uuid é obrigatório")
    val uuid: String,
)

/** Corpo de POST /users/confirm */
data class ConfirmRequest(
    @field:NotBlank(message = "telefone é obrigatório")
    val phone: String,
    @field:NotBlank(message = "uuid é obrigatório")
    val uuid: String,
    @field:NotBlank(message = "código é obrigatório")
    val code: String,
)

/** Corpo de PUT /users/{id} — todos os campos são opcionais. */
data class UpdateUserRequest(
    val name: String? = null,
    val email: String? = null,
    val password: String? = null,
    val description: String? = null,
    val avatar: String? = null,
)

/** Representação pública do usuário retornada nas respostas (sem a senha). */
data class UserResponse(
    val id: String,
    val phone: String,
    val uuid: String,
    val active: Boolean,
    val name: String?,
    val email: String?,
    val description: String?,
    val avatar: String?,
) {
    companion object {
        fun from(user: User) = UserResponse(
            id = user.id,
            phone = user.phone,
            uuid = user.uuid,
            active = user.active,
            name = user.name,
            email = user.email,
            description = user.description,
            avatar = user.avatar,
        )
    }
}
