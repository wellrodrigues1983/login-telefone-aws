package br.tec.wrcoder.loginphoneaws.data

import android.content.Context
import java.util.UUID

/**
 * Gera e persiste um uuid do dispositivo (conforme o enunciado: no Android o uuid
 * é gerado e armazenado no aplicativo). Fica em SharedPreferences, sobrevive a reinícios.
 */
object DeviceId {

    private const val PREFS = "device_prefs"
    private const val KEY_UUID = "device_uuid"

    fun get(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val existing = prefs.getString(KEY_UUID, null)
        if (existing != null) return existing
        val novo = UUID.randomUUID().toString()
        prefs.edit().putString(KEY_UUID, novo).apply()
        return novo
    }
}
