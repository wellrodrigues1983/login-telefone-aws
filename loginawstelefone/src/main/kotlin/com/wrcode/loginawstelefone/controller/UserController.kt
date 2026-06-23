package com.wrcode.loginawstelefone.controller

import com.wrcode.loginawstelefone.dto.ConfirmRequest
import com.wrcode.loginawstelefone.dto.LoginRequest
import com.wrcode.loginawstelefone.dto.UpdateUserRequest
import com.wrcode.loginawstelefone.dto.UserResponse
import com.wrcode.loginawstelefone.service.LoginResult
import com.wrcode.loginawstelefone.service.UserService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/users")
class UserController(
    private val userService: UserService,
) {

    /**
     * POST /users/login
     * Body: { phone, uuid }
     * - 200 + usuário, se telefone+uuid pertencem a um usuário ativo.
     * - 202 (sem corpo), se foi enviado um SMS de confirmação.
     */
    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): ResponseEntity<UserResponse> {
        return when (val result = userService.login(request.phone, request.uuid)) {
            is LoginResult.Authenticated ->
                ResponseEntity.ok(UserResponse.from(result.user))
            LoginResult.ConfirmationRequired ->
                ResponseEntity.status(HttpStatus.ACCEPTED).build()
        }
    }

    /**
     * POST /users/confirm
     * Body: { phone, uuid, code }
     * - 200 + usuário, se o código confere (ativa novo usuário ou troca o uuid).
     * - 404, se não havia código pendente para telefone+uuid.
     */
    @PostMapping("/confirm")
    fun confirm(@Valid @RequestBody request: ConfirmRequest): ResponseEntity<UserResponse> {
        val user = userService.confirm(request.phone, request.uuid, request.code)
        return ResponseEntity.ok(UserResponse.from(user))
    }

    /**
     * PUT /users/{id}
     * Body: { name?, description?, avatar? }
     * Atualiza os demais dados do usuário.
     */
    @PutMapping("/{id}")
    fun update(
        @PathVariable id: String,
        @Valid @RequestBody request: UpdateUserRequest,
    ): ResponseEntity<UserResponse> {
        val user = userService.update(id, request)
        return ResponseEntity.ok(UserResponse.from(user))
    }

    @GetMapping("/{id}")
    fun get(@PathVariable id: String): ResponseEntity<UserResponse> =
        ResponseEntity.ok(UserResponse.from(userService.findById(id)))
}
