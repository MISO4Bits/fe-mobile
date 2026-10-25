package com.solventa4bits.movil.sesion.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
 * Figma 01 "Entrar con tu huella": la app abrio con la huella activada y la
 * sesion guardada espera detras del sensor (BITS-353, AC-2). Al abrirse
 * lanza el dialogo del sistema; el icono lo vuelve a lanzar si se cerro.
 */
@Composable
fun PantallaEntrarConHuella(
    alTocarSensor: () -> Unit,
    alEntrarConContrasena: () -> Unit,
    alUsarOtraCuenta: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(Unit) { alTocarSensor() }
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
        // La sesion guardada no trae el nombre del cliente: el saludo va sin el.
        Text(
            text = stringResource(R.string.huella_hola),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.huella_entrar_detalle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        IconoDeHuella(alTocar = alTocarSensor)
        Text(
            text = stringResource(R.string.huella_toca_el_sensor),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(24.dp))
        NotaSobreLaHuella()
        Spacer(Modifier.weight(1f))
        OutlinedButton(onClick = alEntrarConContrasena, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.huella_entrar_con_contrasena))
        }
        TextButton(onClick = alUsarOtraCuenta) {
            Text(stringResource(R.string.huella_usar_otra_cuenta))
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Preview(showBackground = true)
@Composable
private fun PantallaEntrarConHuellaPreview() {
    TemaSolventa { PantallaEntrarConHuella(alTocarSensor = {}, alEntrarConContrasena = {}, alUsarOtraCuenta = {}) }
}
