package com.solventa4bits.movil.seguro.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.solventa4bits.movil.R
import com.solventa4bits.movil.inicio.ui.EtiquetaVigente
import com.solventa4bits.movil.solicitud.dominio.EstadoDeSolicitud
import com.solventa4bits.movil.solicitud.dominio.Solicitud
import com.solventa4bits.movil.solicitud.ui.IconoListo
import com.solventa4bits.movil.solicitud.ui.IconoRechazo
import com.solventa4bits.movil.solicitud.ui.fechaYHora
import com.solventa4bits.movil.ui.BarraSolventa
import com.solventa4bits.movil.ui.tema.TemaSolventa

// El tema no trae un ambar: es el del aviso sin conexion en Figma 05.
private val Ambar = Color(0xFF8A5A00)
private val FondoAmbar = Color(0xFFFFF1D6)

/**
 * Figma 05: sin conexion, las polizas guardadas en el telefono y cuando se
 * guardaron (BITS-217, AC-2). Tocar una abre su detalle con lo guardado.
 */
@Composable
fun PantallaMiPoliza(
    seguros: List<Solicitud>,
    actualizadosEn: String?,
    alVerSeguro: (Solicitud) -> Unit,
    alReintentar: () -> Unit,
    alCerrarSesion: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        BarraSolventa(stringResource(R.string.mi_poliza_titulo), alCerrarSesion)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            AvisoSinConexion()
            actualizadosEn?.let { ActualizadaEl(it) }
            if (seguros.isEmpty()) {
                Text(stringResource(R.string.mi_poliza_sin_guardados), style = MaterialTheme.typography.bodyMedium)
            }
            seguros.forEach { seguro -> TarjetaGuardada(seguro) { alVerSeguro(seguro) } }
            QuePuedesHacer()
        }
        OutlinedButton(
            onClick = alReintentar,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        ) {
            Text(stringResource(R.string.mi_poliza_reintentar))
        }
    }
}

/** El aviso amarillo de que no hay conexion y se ve lo guardado. */
@Composable
internal fun AvisoSinConexion() {
    Surface(color = FondoAmbar, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(R.drawable.ic_sin_senal), contentDescription = null, tint = Ambar)
            Text(
                text = stringResource(R.string.sin_conexion_aviso),
                style = MaterialTheme.typography.bodyMedium,
                color = Ambar,
            )
        }
    }
}

/** "ACTUALIZADA EL 19 OCT · 9:41 A. M." */
@Composable
internal fun ActualizadaEl(fechaIso: String) {
    Text(
        text = stringResource(R.string.sin_conexion_actualizada, fechaYHora(fechaIso).orEmpty()).uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun TarjetaGuardada(seguro: Solicitud, alVer: () -> Unit) {
    OutlinedCard(onClick = alVer, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(nombreDelSeguro(seguro), style = MaterialTheme.typography.titleMedium)
                Text(
                    text = seguro.numero.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            EtiquetaVigente()
        }
    }
}

@Composable
private fun QuePuedesHacer() {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.mi_poliza_sin_senal), style = MaterialTheme.typography.titleSmall)
            Opcion(stringResource(R.string.mi_poliza_ver)) { IconoListo(tamano = 18.dp) }
            Opcion(stringResource(R.string.mi_poliza_cuando)) { IconoListo(tamano = 18.dp) }
            Opcion(stringResource(R.string.mi_poliza_cotizar)) { IconoRechazo(tamano = 18.dp) }
        }
    }
}

@Composable
private fun Opcion(texto: String, icono: @Composable () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        icono()
        Text(texto, style = MaterialTheme.typography.bodyMedium)
    }
}

@Preview(showBackground = true)
@Composable
private fun PantallaMiPolizaPreview() {
    TemaSolventa {
        PantallaMiPoliza(
            seguros = listOf(
                Solicitud(
                    id = "poliza-1",
                    estado = EstadoDeSolicitud.EMITIDA,
                    creadaEn = "2026-10-19T14:41:00Z",
                    emitidaEn = "2026-10-19T14:41:00Z",
                    pendientes = emptyList(),
                    motivosDeRechazo = emptyList(),
                    numero = "SOL-VH-2026-K7M2Q9",
                ),
            ),
            actualizadosEn = "2026-10-19T14:41:00Z",
            alVerSeguro = {},
            alReintentar = {},
            alCerrarSesion = {},
        )
    }
}
