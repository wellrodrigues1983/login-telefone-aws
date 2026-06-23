package br.tec.wrcoder.loginphoneaws.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun LoginScreen(viewModel: LoginViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Login por telefone", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(24.dp))

        when (state.step) {
            Step.PHONE -> PhoneStep(
                phone = state.phone,
                loading = state.loading,
                onPhoneChange = viewModel::onPhoneChange,
                onSubmit = viewModel::submitPhone,
            )
            Step.CODE -> CodeStep(
                code = state.code,
                loading = state.loading,
                onCodeChange = viewModel::onCodeChange,
                onSubmit = viewModel::submitCode,
                onBack = viewModel::backToPhone,
            )
            Step.LOGGED_IN -> LoggedInStep(
                state = state,
                onSave = viewModel::saveProfile,
                onLogout = viewModel::logout,
            )
        }

        Spacer(Modifier.height(16.dp))
        state.info?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
private fun PhoneStep(
    phone: String,
    loading: Boolean,
    onPhoneChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    OutlinedTextField(
        value = phone,
        onValueChange = onPhoneChange,
        label = { Text("Telefone (ex: +5541999990000)") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(16.dp))
    Button(onClick = onSubmit, enabled = !loading, modifier = Modifier.fillMaxWidth()) {
        if (loading) CircularProgressIndicator(Modifier.height(20.dp)) else Text("Entrar")
    }
}

@Composable
private fun CodeStep(
    code: String,
    loading: Boolean,
    onCodeChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit,
) {
    OutlinedTextField(
        value = code,
        onValueChange = onCodeChange,
        label = { Text("Código de confirmação") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(16.dp))
    Button(onClick = onSubmit, enabled = !loading, modifier = Modifier.fillMaxWidth()) {
        if (loading) CircularProgressIndicator(Modifier.height(20.dp)) else Text("Confirmar")
    }
    TextButton(onClick = onBack, enabled = !loading) {
        Text("Voltar / trocar telefone")
    }
}

@Composable
private fun LoggedInStep(
    state: LoginUiState,
    onSave: (String, String) -> Unit,
    onLogout: () -> Unit,
) {
    val user = state.user
    Text("Bem-vindo!", style = MaterialTheme.typography.titleLarge)
    Spacer(Modifier.height(8.dp))
    Text("Telefone: ${user?.phone ?: "-"}")
    Text("ID: ${user?.id ?: "-"}")
    Spacer(Modifier.height(24.dp))

    var name by rememberSaveable { mutableStateOf(user?.name.orEmpty()) }
    var description by rememberSaveable { mutableStateOf(user?.description.orEmpty()) }

    Text("Completar perfil", style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
        value = name,
        onValueChange = { name = it },
        label = { Text("Nome") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
        value = description,
        onValueChange = { description = it },
        label = { Text("Descrição") },
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(16.dp))
    Button(
        onClick = { onSave(name, description) },
        enabled = !state.loading,
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (state.loading) CircularProgressIndicator(Modifier.height(20.dp)) else Text("Salvar perfil")
    }
    TextButton(onClick = onLogout) { Text("Sair") }
}
