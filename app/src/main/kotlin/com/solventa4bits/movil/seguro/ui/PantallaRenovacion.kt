package com.solventa4bits.movil.seguro.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.solventa4bits.movil.R
import com.solventa4bits.movil.cotizacion.ui.enPesos
import com.solventa4bits.movil.solicitud.dominio.Renovacion
import com.solventa4bits.movil.solicitud.ui.fechaCorta
import java.time.OffsetDateTime
import java.util.Locale

/**
 * El modal de la renovacion (BITS-237), con el mismo dialogo de declinar la cotizacion (BITS-337):
 * la propuesta (M10, AC-2) o la confirmacion de no renovar (M10A, AC-4).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PanelDeRenovacion(
    paso: PasoDeRenovacion,
    renovacion: Renovacion,
    banco: String,
    estado: EstadoSeguro,
    seguro: SeguroViewModel,
) {
    ModalBottomSheet(
        onDismissRequest = seguro::cerrarRenovacion,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.renovacion_etiqueta),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            when (paso) {
                PasoDeRenovacion.PROPUESTA -> LaPropuesta(renovacion)
                PasoDeRenovacion.NO_RENOVAR -> NoRenovar(renovacion, banco)
            }
            if (estado.falloAlResponder) {
                Text(
                    text = stringResource(R.string.renovacion_fallo),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                val habilitado = !estado.respondiendo
                when (paso) {
                    PasoDeRenovacion.PROPUESTA -> {
                        Button(
                            onClick = { seguro.responder(aceptar = true) },
                            enabled = habilitado,
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(stringResource(R.string.renovacion_aceptar)) }
                        TextButton(onClick = seguro::pedirNoRenovar, enabled = habilitado) {
                            Text(stringResource(R.string.renovacion_no_renovar))
                        }
                    }
                    PasoDeRenovacion.NO_RENOVAR -> {
                        Button(
                            onClick = { seguro.responder(aceptar = false) },
                            enabled = habilitado,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(stringResource(R.string.renovacion_no_renovar)) }
                        TextButton(onClick = seguro::verPropuesta, enabled = habilitado) {
                            Text(stringResource(R.string.no_renovar_volver))
                        }
                    }
                }
            }
        }
    }
}

/** M10: hoy, la prima nueva desde la renovacion y hasta cuando puede decidir. */
@Composable
private fun LaPropuesta(renovacion: Renovacion) {
    val fecha = fechaCorta(renovacion.fechaRenovacion).orEmpty()
    // El IPC que sube la tarifa es el del año anterior a la renovacion.
    val anio = runCatching { OffsetDateTime.parse(renovacion.fechaRenovacion).year - 1 }.getOrNull()
    val ipc = String.format(Locale.forLanguageTag("es-CO"), "%.1f %%", renovacion.ipc * CIEN)
    Text(stringResource(R.string.renovacion_titulo, fecha), style = MaterialTheme.typography.headlineSmall)
    Text(
        text = stringResource(
            R.string.renovacion_cuerpo,
            enPesos(renovacion.primaActual),
            fecha,
            enPesos(renovacion.primaNueva),
            anio?.toString().orEmpty(),
            ipc,
        ),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            Prima(stringResource(R.string.renovacion_hoy), renovacion.primaActual, Modifier.weight(1f))
            Prima(
                stringResource(R.string.renovacion_desde, fecha),
                renovacion.primaNueva,
                Modifier.weight(1f),
                MaterialTheme.colorScheme.primary,
            )
        }
    }
    Text(
        text = stringResource(R.string.renovacion_nota, fechaCorta(renovacion.decidirHasta).orEmpty()),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun Prima(titulo: String, prima: Double, modifier: Modifier, color: Color = Color.Unspecified) {
    Column(modifier = modifier) {
        Text(titulo, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = stringResource(R.string.renovacion_al_mes, enPesos(prima)),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = color,
        )
    }
}

/** M10A: hasta cuando sigue activa, que hara el banco y hasta cuando puede cambiar de opinion. */
@Composable
private fun NoRenovar(renovacion: Renovacion, banco: String) {
    Text(stringResource(R.string.no_renovar_titulo), style = MaterialTheme.typography.headlineSmall)
    Text(
        text = stringResource(
            R.string.no_renovar_cuerpo,
            fechaCorta(renovacion.fechaRenovacion).orEmpty(),
            banco,
            fechaCorta(renovacion.decidirHasta).orEmpty(),
        ),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private const val CIEN = 100
