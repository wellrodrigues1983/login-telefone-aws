package com.wrcode.loginawstelefone.repository

import com.wrcode.loginawstelefone.model.ConfirmationCode
import com.wrcode.loginawstelefone.model.User
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap

/**
 * Repositório in-memory. Para produção, troque por DynamoDB/JPA mantendo a mesma interface.
 * Os códigos de confirmação são guardados por chave "telefone|uuid".
 */
@Repository
class UserRepository {

    private val usersById = ConcurrentHashMap<String, User>()
    private val pendingCodes = ConcurrentHashMap<String, ConfirmationCode>()

    private fun key(phone: String, uuid: String) = "$phone|$uuid"

    // ---- Usuários ----

    fun save(user: User): User {
        usersById[user.id] = user
        return user
    }

    fun findById(id: String): User? = usersById[id]

    fun findByPhone(phone: String): User? =
        usersById.values.firstOrNull { it.phone == phone }

    fun all(): List<User> = usersById.values.toList()

    // ---- Códigos de confirmação ----

    fun saveCode(code: ConfirmationCode) {
        pendingCodes[key(code.phone, code.uuid)] = code
    }

    fun findCode(phone: String, uuid: String): ConfirmationCode? =
        pendingCodes[key(phone, uuid)]

    fun removeCode(phone: String, uuid: String) {
        pendingCodes.remove(key(phone, uuid))
    }
}
