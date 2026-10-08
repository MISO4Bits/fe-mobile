package com.solventa4bits.movil.sesion.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.solventa4bits.movil.R
import com.solventa4bits.movil.ui.tema.TemaSolventa

/**
 * Pantalla de inicio de sesion. Solo dibuja el [estado] que recibe y avisa
 * lo que hace el cliente: las decisiones estan en [LoginViewModel].
 */
@Composable
fun PantallaLogin(
    estado: EstadoLogin,
    alCambiarCorreo: (String) -> Unit,
    alCambiarContrasena: (String) -> Unit,
    alIniciarSesion: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(Modifier.height(96.dp))
        Image(
            painter = painterResource(R.drawable.logo_solventa),
            contentDescription = stringResource(R.string.app_name),
            modifier = Modifier.height(56.dp),
        )
        Spacer(Modifier.height(32.dp))
        Text(
            text = stringResource(R.string.login_lema),
            style = MaterialTheme.typography.bodyMedium,
        )
        estado.error?.let { MensajeDeError(it) }

        OutlinedTextField(
            value = estado.correo,
            onValueChange = alCambiarCorreo,
            label = { Text(stringResource(R.string.login_correo)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
        )
        val contrasenaConError = estado.error == ErrorLogin.CREDENCIALES_INVALIDAS
        OutlinedTextField(
            value = estado.contrasena,
            onValueChange = alCambiarContrasena,
            label = { Text(stringResource(R.string.login_contrasena)) },
            singleLine = true,
            isError = contrasenaConError,
            supportingText = if (contrasenaConError) {
                { Text(stringResource(R.string.login_revisa_contrasena)) }
            } else {
                null
            },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
        )
        // Recuperar la contrasena aun no tiene contrato en el BFF: no hace nada.
        TextButton(onClick = {}, modifier = Modifier.align(Alignment.End)) {
            Text(stringResource(R.string.login_olvidaste_contrasena))
        }
        Button(onClick = alIniciarSesion, enabled = estado.puedeEnviar) {
            Text(stringResource(R.string.login_iniciar_sesion))
        }
        Spacer(Modifier.height(48.dp))
        Text(
            text = stringResource(R.string.login_crea_tu_cuenta),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun MensajeDeError(error: ErrorLogin) {
    val texto = when (error) {
        ErrorLogin.CREDENCIALES_INVALIDAS -> R.string.login_error_credenciales
        ErrorLogin.FALLO -> R.string.login_error_fallo
    }
    Text(
        text = stringResource(texto),
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.errorContainer, MaterialTheme.shapes.medium)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    )
}

@Preview(showBackground = true)
@Composable
private fun PantallaLoginPreview() {
    TemaSolventa { PantallaLogin(EstadoLogin(), {}, {}, {}) }
}

@Preview(showBackground = true)
@Composable
private fun PantallaLoginConErrorPreview() {
    TemaSolventa {
        PantallaLogin(
            EstadoLogin(correo = "ana@correo.com", error = ErrorLogin.CREDENCIALES_INVALIDAS),
            {},
            {},
            {},
        )
    }
}
