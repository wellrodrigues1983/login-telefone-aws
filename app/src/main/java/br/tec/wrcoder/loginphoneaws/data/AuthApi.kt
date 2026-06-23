package br.tec.wrcoder.loginphoneaws.data

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/**
 * Interface Retrofit do AuthServer.
 * Usamos Response<...> para conseguir inspecionar o código HTTP
 * (200 = autenticado, 202 = confirmação por SMS necessária).
 */
interface AuthApi {

    @POST("users/login")
    suspend fun login(@Body body: LoginRequest): Response<UserResponse>

    @POST("users/confirm")
    suspend fun confirm(@Body body: ConfirmRequest): Response<UserResponse>

    @PUT("users/{id}")
    suspend fun updateUser(
        @Path("id") id: String,
        @Body body: UpdateUserRequest,
    ): Response<UserResponse>
}
