package br.tec.wrcoder.loginphoneaws.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.tec.wrcoder.loginphoneaws.data.AuthRepository
import br.tec.wrcoder.loginphoneaws.data.ConfirmOutcome
import br.tec.wrcoder.loginphoneaws.data.LoginOutcome
import br.tec.wrcoder.loginphoneaws.data.UserResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Etapa atual do fluxo de login. */
enum class Step { PHONE, CODE, LOGGED_IN }

data class LoginUiState(
    val step: Step = Step.PHONE,
    val phone: String = "",
    val code: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val user: UserResponse? = null,
    val info: String? = null,
)

class LoginViewModel(
    private val deviceUuid: String,
    private val repository: AuthRepository = AuthRepository(),
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun onPhoneChange(value: String) = _state.update { it.copy(phone = value, error = null) }
    fun onCodeChange(value: String) = _state.update { it.copy(code = value, error = null) }

    fun submitPhone() {
        val phone = _state.value.phone.trim()
        if (phone.isEmpty()) {
            _state.update { it.copy(error = "Informe o telefone") }
            return
        }
        _state.update { it.copy(loading = true, error = null, info = null) }
        viewModelScope.launch {
            when (val outcome = repository.login(phone, deviceUuid)) {
                is LoginOutcome.Authenticated ->
                    _state.update {
                        it.copy(loading = false, step = Step.LOGGED_IN, user = outcome.user)
                    }
                LoginOutcome.ConfirmationRequired ->
                    _state.update {
                        it.copy(
                            loading = false,
                            step = Step.CODE,
                            info = "Enviamos um código por SMS para $phone",
                        )
                    }
                is LoginOutcome.Error ->
                    _state.update { it.copy(loading = false, error = outcome.message) }
            }
        }
    }

    fun submitCode() {
        val phone = _state.value.phone.trim()
        val code = _state.value.code.trim()
        if (code.isEmpty()) {
            _state.update { it.copy(error = "Informe o código recebido") }
            return
        }
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val outcome = repository.confirm(phone, deviceUuid, code)) {
                is ConfirmOutcome.Success ->
                    _state.update {
                        it.copy(loading = false, step = Step.LOGGED_IN, user = outcome.user)
                    }
                is ConfirmOutcome.Error ->
                    _state.update { it.copy(loading = false, error = outcome.message) }
            }
        }
    }

    fun saveProfile(name: String, description: String) {
        val user = _state.value.user ?: return
        _state.update { it.copy(loading = true, error = null, info = null) }
        viewModelScope.launch {
            repository.updateUser(user.id, name.ifBlank { null }, description.ifBlank { null })
                .onSuccess { updated ->
                    _state.update { it.copy(loading = false, user = updated, info = "Perfil atualizado!") }
                }
                .onFailure { e ->
                    _state.update { it.copy(loading = false, error = e.message) }
                }
        }
    }

    fun logout() {
        _state.update { LoginUiState() }
    }

    fun backToPhone() {
        _state.update { it.copy(step = Step.PHONE, code = "", error = null, info = null) }
    }
}
