package com.wrcode.loginawstelefone.service

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.HexFormat

/**
 * Hash de senha simples (SHA-256 com salt) — suficiente para fins acadêmicos.
 * Em produção, prefira BCrypt/Argon2 (ex.: spring-security-crypto).
 * Formato armazenado: "saltHex:hashHex".
 */
object PasswordHasher {

    private val random = SecureRandom()

    fun hash(password: String): String {
        val salt = ByteArray(16).also { random.nextBytes(it) }
        val hash = digest(password, salt)
        return "${HexFormat.of().formatHex(salt)}:${HexFormat.of().formatHex(hash)}"
    }

    fun matches(password: String, stored: String): Boolean {
        val parts = stored.split(":")
        if (parts.size != 2) return false
        val salt = HexFormat.of().parseHex(parts[0])
        val expected = HexFormat.of().parseHex(parts[1])
        return digest(password, salt).contentEquals(expected)
    }

    private fun digest(password: String, salt: ByteArray): ByteArray {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(salt)
        return md.digest(password.toByteArray(Charsets.UTF_8))
    }
}
