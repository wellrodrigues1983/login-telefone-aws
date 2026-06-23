package br.tec.wrcoder.loginphoneaws.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/** Fornece o uuid do dispositivo ao LoginViewModel. */
class LoginViewModelFactory(private val deviceUuid: String) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return LoginViewModel(deviceUuid) as T
    }
}
