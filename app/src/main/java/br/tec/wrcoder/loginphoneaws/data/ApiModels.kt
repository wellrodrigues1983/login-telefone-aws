package br.tec.wrcoder.loginphoneaws.data

/** Corpo de POST /users/login */
data class LoginRequest(val phone: String, val uuid: String)

/** Corpo de POST /users/confirm */
data class ConfirmRequest(val phone: String, val uuid: String, val code: String)

/** Corpo de PUT /users/{id} */
data class UpdateUserRequest(
    val name: String? = null,
    val description: String? = null,
    val avatar: String? = null,
)

/** Usuário retornado pelo servidor. */
data class UserResponse(
    val id: String,
    val phone: String,
    val uuid: String,
    val active: Boolean,
    val name: String?,
    val description: String?,
    val avatar: String?,
)
