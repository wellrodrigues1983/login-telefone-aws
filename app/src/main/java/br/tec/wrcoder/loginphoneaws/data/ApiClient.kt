package br.tec.wrcoder.loginphoneaws.data

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Fábrica do Retrofit.
 *
 * IMPORTANTE: no emulador Android, "localhost" do servidor é acessado por 10.0.2.2.
 * Para um dispositivo físico, troque por http://SEU_IP_DA_REDE:8080/.
 */
object ApiClient {

    // Emulador -> host. Ajuste conforme seu ambiente.
    private const val BASE_URL = "http://10.0.2.2:8080/"

    val authApi: AuthApi by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AuthApi::class.java)
    }
}
