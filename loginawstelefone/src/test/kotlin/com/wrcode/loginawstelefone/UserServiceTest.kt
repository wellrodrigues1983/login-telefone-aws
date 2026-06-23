package com.wrcode.loginawstelefone

import com.wrcode.loginawstelefone.dto.UpdateUserRequest
import com.wrcode.loginawstelefone.repository.UserRepository
import com.wrcode.loginawstelefone.service.ConfirmationNotFoundException
import com.wrcode.loginawstelefone.service.InvalidConfirmationCodeException
import com.wrcode.loginawstelefone.service.LoginResult
import com.wrcode.loginawstelefone.service.SmsService
import com.wrcode.loginawstelefone.service.UserService
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Testes unitários do fluxo de login por telefone, sem Spring context nem AWS.
 * O SmsService usa enabled=false, então apenas registra o código no log (fallback).
 */
class UserServiceTest {

    private lateinit var repository: UserRepository
    private lateinit var service: UserService

    @BeforeEach
    fun setup() {
        repository = UserRepository()
        val sms = SmsService(snsEnabled = false, region = "us-east-1", senderId = "Test")
        service = UserService(repository, sms)
    }

    @Test
    fun `primeiro login de telefone novo exige confirmacao`() {
        val result = service.login("+5541999990000", "uuid-1")
        assertTrue(result is LoginResult.ConfirmationRequired)
    }

    @Test
    fun `confirmar com codigo correto cria e ativa usuario`() {
        service.login("+5541999990000", "uuid-1")
        val code = repository.findCode("+5541999990000", "uuid-1")!!.code

        val user = service.confirm("+5541999990000", "uuid-1", code)

        assertTrue(user.active)
        assertEquals("uuid-1", user.uuid)
        // código consumido
        assertEquals(null, repository.findCode("+5541999990000", "uuid-1"))
    }

    @Test
    fun `login de usuario ativo com mesmo uuid autentica direto`() {
        service.login("+5541999990000", "uuid-1")
        val code = repository.findCode("+5541999990000", "uuid-1")!!.code
        service.confirm("+5541999990000", "uuid-1", code)

        val result = service.login("+5541999990000", "uuid-1")
        assertTrue(result is LoginResult.Authenticated)
    }

    @Test
    fun `login do mesmo telefone com outro uuid exige nova confirmacao`() {
        service.login("+5541999990000", "uuid-1")
        val code1 = repository.findCode("+5541999990000", "uuid-1")!!.code
        service.confirm("+5541999990000", "uuid-1", code1)

        // dispositivo diferente
        val result = service.login("+5541999990000", "uuid-2")
        assertTrue(result is LoginResult.ConfirmationRequired)

        // ao confirmar, o uuid do usuário é substituído (mesmo id)
        val code2 = repository.findCode("+5541999990000", "uuid-2")!!.code
        val user = service.confirm("+5541999990000", "uuid-2", code2)
        assertEquals("uuid-2", user.uuid)
    }

    @Test
    fun `confirmar sem codigo pendente lanca 404`() {
        assertFailsWith<ConfirmationNotFoundException> {
            service.confirm("+5541999990000", "uuid-x", "000000")
        }
    }

    @Test
    fun `confirmar com codigo errado lanca excecao de codigo invalido`() {
        service.login("+5541999990000", "uuid-1")
        val real = repository.findCode("+5541999990000", "uuid-1")!!.code
        val wrong = if (real == "000000") "111111" else "000000"
        assertFailsWith<InvalidConfirmationCodeException> {
            service.confirm("+5541999990000", "uuid-1", wrong)
        }
    }

    @Test
    fun `atualizar dados do usuario`() {
        service.login("+5541999990000", "uuid-1")
        val code = repository.findCode("+5541999990000", "uuid-1")!!.code
        val user = service.confirm("+5541999990000", "uuid-1", code)

        val updated = service.update(user.id, UpdateUserRequest(name = "Wellington", description = "Dev"))
        assertEquals("Wellington", updated.name)
        assertEquals("Dev", updated.description)
    }

    @Test
    fun `cadastro salva email e guarda senha como hash`() {
        service.login("+5541999990000", "uuid-1")
        val code = repository.findCode("+5541999990000", "uuid-1")!!.code
        val user = service.confirm("+5541999990000", "uuid-1", code)

        val updated = service.update(
            user.id,
            UpdateUserRequest(name = "Wellington", email = "well@email.com", password = "secret123"),
        )
        assertEquals("well@email.com", updated.email)
        // a senha não é guardada em texto puro
        assertTrue(updated.passwordHash != null && updated.passwordHash != "secret123")
        // o hash confere com a senha original
        assertTrue(com.wrcode.loginawstelefone.service.PasswordHasher.matches("secret123", updated.passwordHash!!))
    }
}
