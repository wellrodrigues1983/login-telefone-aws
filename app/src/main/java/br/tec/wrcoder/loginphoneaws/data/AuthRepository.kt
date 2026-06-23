package br.tec.wrcoder.loginphoneaws.data

/** Resultado de uma tentativa de login. */
sealed interface LoginOutcome {
    data class Authenticated(val user: UserResponse) : LoginOutcome
    data object ConfirmationRequired : LoginOutcome
    data class Error(val message: String) : LoginOutcome
}

/** Resultado da confirmação por código. */
sealed interface ConfirmOutcome {
    data class Success(val user: UserResponse) : ConfirmOutcome
    data class Error(val message: String) : ConfirmOutcome
}

class AuthRepository(private val api: AuthApi = ApiClient.authApi) {

    suspend fun login(phone: String, uuid: String): LoginOutcome {
        return try {
            val response = api.login(LoginRequest(phone, uuid))
            when {
                response.code() == 202 -> LoginOutcome.ConfirmationRequired
                response.isSuccessful && response.body() != null ->
                    LoginOutcome.Authenticated(response.body()!!)
                else -> LoginOutcome.Error("Falha no login (HTTP ${response.code()})")
            }
        } catch (e: Exception) {
            LoginOutcome.Error("Erro de rede: ${e.message}")
        }
    }

    suspend fun confirm(phone: String, uuid: String, code: String): ConfirmOutcome {
        return try {
            val response = api.confirm(ConfirmRequest(phone, uuid, code))
            when {
                response.isSuccessful && response.body() != null ->
                    ConfirmOutcome.Success(response.body()!!)
                response.code() == 404 ->
                    ConfirmOutcome.Error("Nenhum código pendente. Solicite o login novamente.")
                response.code() == 400 ->
                    ConfirmOutcome.Error("Código inválido ou expirado.")
                else -> ConfirmOutcome.Error("Falha na confirmação (HTTP ${response.code()})")
            }
        } catch (e: Exception) {
            ConfirmOutcome.Error("Erro de rede: ${e.message}")
        }
    }

    suspend fun updateUser(id: String, name: String?, description: String?): Result<UserResponse> {
        return try {
            val response = api.updateUser(id, UpdateUserRequest(name = name, description = description))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Falha ao atualizar (HTTP ${response.code()})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
