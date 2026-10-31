package com.solventa4bits.movil.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.solventa4bits.movil.R
import com.solventa4bits.movil.red.GuardiaDeRed
import com.solventa4bits.movil.ui.tema.TemaSolventa

/**
 * Si una accion quedo esperando red, tapa la pantalla con "Sin conexion"
 * (Figma 11, BITS-217 AC-5). Lo de abajo sigue vivo: al reintentar con red
 * se retoma donde iba.
 */
@Composable
fun SinConexion(red: GuardiaDeRed, alInicio: () -> Unit, alCerrarSesion: () -> Unit, modifier: Modifier) {
    if (red.pendiente == null) return
    BackHandler(onBack = alInicio)
    Surface(modifier = Modifier.fillMaxSize()) {
        PantallaSinConexion(
            alReintentar = red::reintentar,
            alInicio = alInicio,
            alCerrarSesion = alCerrarSesion,
            modifier = modifier,
        )
    }
}

/** Lo que queda debajo de "Sin conexion" no se lee ni se toca mientras se ve. */
fun Modifier.tapadoSinRed(red: GuardiaDeRed): Modifier =
    if (red.pendiente == null) this else clearAndSetSemantics {}

@Composable
fun PantallaSinConexion(
    alReintentar: () -> Unit,
    alInicio: () -> Unit,
    alCerrarSesion: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        BarraSolventa(stringResource(R.string.app_name), alCerrarSesion)
        Column(
            modifier = Modifier.weight(1f).padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 48.dp)
                    .size(96.dp)
                    .background(MaterialTheme.colorScheme.errorContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(R.drawable.ic_reloj_de_arena),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(40.dp),
                )
            }
            Text(stringResource(R.string.sin_conexion_titulo), style = MaterialTheme.typography.headlineSmall)
            Text(
                text = stringResource(R.string.sin_conexion_detalle),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.sin_conexion_guardado),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 24.dp),
            )
        }
        Column(
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Button(onClick = alReintentar) { Text(stringResource(R.string.cotizacion_reintentar)) }
            TextButton(onClick = alInicio) { Text(stringResource(R.string.resultado_inicio)) }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PantallaSinConexionPreview() {
    TemaSolventa {
        PantallaSinConexion(alReintentar = {}, alInicio = {}, alCerrarSesion = {})
    }
}
