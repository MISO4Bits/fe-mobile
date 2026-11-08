package com.solventa4bits.movil.siniestro.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.solventa4bits.movil.R
import com.solventa4bits.movil.siniestro.dominio.ProblemaDeFecha
import com.solventa4bits.movil.siniestro.dominio.diasCubiertos
import com.solventa4bits.movil.solicitud.ui.fechaCorta
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * "Cuando ocurrio", en M03 y en M05: por defecto hoy (AC-1) y, si cae fuera de la vigencia, el
 * error en el campo y el recuadro con las fechas que si se pueden reportar (M03A, AC-2).
 */
@Composable
internal fun CuandoOcurrio(estado: EstadoReporte, alCambiarFecha: (LocalDate) -> Unit) {
    TituloDeSeccion(stringResource(R.string.reporte_cuando))
    CampoDeFecha(estado, alCambiarFecha)
    AvisoDeFecha(estado)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CampoDeFecha(estado: EstadoReporte, alCambiarFecha: (LocalDate) -> Unit) {
    var eligiendo by remember { mutableStateOf(false) }
    val problema = estado.problemaConLaFecha
    OutlinedTextField(
        value = fechaCorta(estado.fecha),
        onValueChange = {},
        readOnly = true,
        label = { Text(stringResource(R.string.reporte_fecha)) },
        isError = problema != null,
        supportingText = {
            when {
                problema != null -> Text(textoDelProblema(problema))
                estado.fecha == estado.hoy -> Text(stringResource(R.string.reporte_fecha_hoy))
            }
        },
        trailingIcon = {
            IconButton(onClick = { eligiendo = true }) {
                Icon(Icons.Outlined.DateRange, contentDescription = stringResource(R.string.reporte_fecha_cambiar))
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
    if (!eligiendo) return
    // El selector trabaja en milisegundos UTC: el dia elegido no depende de la zona del telefono.
    val calendario = rememberDatePickerState(
        initialSelectedDateMillis = estado.fecha.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
    )
    DatePickerDialog(
        onDismissRequest = { eligiendo = false },
        confirmButton = {
            TextButton(
                onClick = {
                    calendario.selectedDateMillis?.let {
                        alCambiarFecha(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    eligiendo = false
                },
            ) { Text(stringResource(R.string.reporte_aceptar)) }
        },
        dismissButton = {
            TextButton(onClick = { eligiendo = false }) { Text(stringResource(R.string.barra_cancelar)) }
        },
    ) { DatePicker(state = calendario) }
}

@Composable
private fun textoDelProblema(problema: ProblemaDeFecha): String = stringResource(
    when (problema) {
        ProblemaDeFecha.FUTURA -> R.string.reporte_fecha_futura
        ProblemaDeFecha.ANTES_DE_LA_POLIZA -> R.string.reporte_fecha_antes
        ProblemaDeFecha.DESPUES_DE_LA_POLIZA -> R.string.reporte_fecha_despues
    },
)

/** M03A: por que no puede seguir y entre que fechas si puede. */
@Composable
private fun AvisoDeFecha(estado: EstadoReporte) {
    if (estado.problemaConLaFecha == null) return
    val dias = estado.poliza?.diasCubiertos()
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Outlined.Info, contentDescription = null)
            Column {
                Text(
                    text = stringResource(R.string.reporte_fecha_no_puedes),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                if (dias != null) {
                    Text(
                        text = stringResource(
                            R.string.reporte_fecha_rango,
                            fechaCorta(dias.start),
                            fechaCorta(dias.endInclusive),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}
