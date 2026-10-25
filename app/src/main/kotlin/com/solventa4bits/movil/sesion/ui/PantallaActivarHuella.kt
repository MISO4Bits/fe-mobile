package com.solventa4bits.movil.sesion.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.solventa4bits.movil.R
import com.solventa4bits.movil.ui.tema.TemaSolventa

/**
 * Figma 02 "Activar huella": se muestra una sola vez, justo despues de entrar
 * con contrasena en un telefono con sensor (BITS-353, AC-1). Solo dibuja;
 * las decisiones estan en [LoginViewModel].
 */
@Composable
fun PantallaActivarHuella(
    cargando: Boolean,
    alActivar: () -> Unit,
    alAhoraNo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(Modifier.height(64.dp))
        Image(
            painter = painterResource(R.drawable.logo_solventa),
            contentDescription = stringResource(R.string.app_name),
            modifier = Modifier.height(40.dp),
        )
        Spacer(Modifier.height(40.dp))
        Text(
            text = stringResource(R.string.huella_activar_titulo),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.huella_activar_detalle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        IconoDeHuella()
        Spacer(Modifier.height(40.dp))
        NotaSobreLaHuella()
        Spacer(Modifier.weight(1f))
        // Activar abre el dialogo del sistema una vez, para confirmar que el
        // sensor reconoce al cliente.
        Button(onClick = alActivar, enabled = !cargando, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.huella_activar))
        }
        TextButton(onClick = alAhoraNo, enabled = !cargando) {
            Text(stringResource(R.string.huella_ahora_no))
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Preview(showBackground = true)
@Composable
private fun PantallaActivarHuellaPreview() {
    TemaSolventa { PantallaActivarHuella(cargando = false, alActivar = {}, alAhoraNo = {}) }
}
