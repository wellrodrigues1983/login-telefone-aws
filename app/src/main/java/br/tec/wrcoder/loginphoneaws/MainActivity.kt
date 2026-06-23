package br.tec.wrcoder.loginphoneaws

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import br.tec.wrcoder.loginphoneaws.data.DeviceId
import br.tec.wrcoder.loginphoneaws.ui.LoginScreen
import br.tec.wrcoder.loginphoneaws.ui.LoginViewModel
import br.tec.wrcoder.loginphoneaws.ui.LoginViewModelFactory
import br.tec.wrcoder.loginphoneaws.ui.theme.LoginPhoneAwsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val deviceUuid = DeviceId.get(applicationContext)
        setContent {
            LoginPhoneAwsTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val viewModel: LoginViewModel = viewModel(
                        factory = LoginViewModelFactory(deviceUuid)
                    )
                    LoginScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }
        }
    }
}
