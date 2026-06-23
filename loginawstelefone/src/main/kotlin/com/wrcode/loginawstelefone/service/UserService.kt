package com.wrcode.loginawstelefone.service

import com.wrcode.loginawstelefone.dto.UpdateUserRequest
import com.wrcode.loginawstelefone.model.ConfirmationCode
import com.wrcode.loginawstelefone.model.User
import com.wrcode.loginawstelefone.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.security.SecureRandom
import java.time.Instant
import java.util.UUID

/** Resultado de uma tentativa de login. */
sealed interface LoginResult {
    /** Telefone+uuid conferem com um usuário ativo: login efetuado. */
    data class Authenticated(val user: User) : LoginResult
    /** Foi necessário enviar um código de confirmação por SMS (HTTP 202). */
    data object ConfirmationRequired : LoginResult
}

/** Lançada quando não há código pendente para o par telefone+uuid (HTTP 404). */
class ConfirmationNotFoundException(message: String) : RuntimeException(message)
/** Lançada quando o código informado é inválido ou expirou (HTTP 400). */
class InvalidConfirmationCodeException(message: String) : RuntimeException(message)
/** Lançada quando um usuário não é encontrado (HTTP 404). */
class UserNotFoundException(message: String) : RuntimeException(message)

@Service
class UserService(
    private val repository: UserRepository,
    private val smsService: SmsService,
) {
    private val log = LoggerFactory.getLogger(UserService::class.java)
    private val random = SecureRandom()

    /**
     * POST /users/login
     * - Se telefone+uuid pertencem a um usuário ATIVO -> autentica.
     * - Caso o telefone não exista, ou exista com outro uuid, ou não esteja ativo
     *   -> gera código, envia SMS e sinaliza que é preciso confirmar (202).
     */
    fun login(phone: String, uuid: String): LoginResult {
        val user = repository.findByPhone(phone)
        if (user != null && user.uuid == uuid && user.active) {
            log.info("Login efetuado para telefone {}", phone)
            return LoginResult.Authenticated(user)
        }
        // Necessário confirmar via SMS
        val code = generateCode()
        repository.saveCode(ConfirmationCode(phone = phone, uuid = uuid, code = code))
        smsService.sendConfirmationCode(phone, code)
        log.info("Código de confirmação gerado para {} (uuid={})", phone, uuid)
        return LoginResult.ConfirmationRequired
    }

    /**
     * POST /users/confirm
     * - Sem código pendente para telefone+uuid -> 404.
     * - Código correto -> ativa novo usuário ou substitui o uuid do antigo.
     */
    fun confirm(phone: String, uuid: String, code: String): User {
        val pending = repository.findCode(phone, uuid)
            ?: throw ConfirmationNotFoundException("Nenhum código de confirmação para telefone $phone e uuid informado")

        if (pending.isExpired()) {
            repository.removeCode(phone, uuid)
            throw InvalidConfirmationCodeException("Código expirado, solicite o login novamente")
        }
        if (pending.code != code) {
            throw InvalidConfirmationCodeException("Código de confirmação inválido")
        }

        val existing = repository.findByPhone(phone)
        val user = if (existing != null) {
            // Substitui o uuid do usuário antigo e garante que está ativo
            existing.uuid = uuid
            existing.active = true
            existing.updatedAt = Instant.now()
            repository.save(existing)
        } else {
            // Cria e ativa um novo usuário
            repository.save(
                User(id = UUID.randomUUID().toString(), phone = phone, uuid = uuid, active = true)
            )
        }
        repository.removeCode(phone, uuid)
        log.info("Usuário confirmado/ativado: {}", user.id)
        return user
    }

    /** PUT /users/{id} — atualiza os demais dados do usuário (nome, email, senha, etc). */
    fun update(id: String, request: UpdateUserRequest): User {
        val user = repository.findById(id)
            ?: throw UserNotFoundException("Usuário $id não encontrado")
        request.name?.let { user.name = it }
        request.email?.let { user.email = it }
        request.password?.takeIf { it.isNotBlank() }?.let { user.passwordHash = PasswordHasher.hash(it) }
        request.description?.let { user.description = it }
        request.avatar?.let { user.avatar = it }
        user.updatedAt = Instant.now()
        return repository.save(user)
    }

    fun findById(id: String): User =
        repository.findById(id) ?: throw UserNotFoundException("Usuário $id não encontrado")

    private fun generateCode(): String = String.format("%06d", random.nextInt(1_000_000))
}
