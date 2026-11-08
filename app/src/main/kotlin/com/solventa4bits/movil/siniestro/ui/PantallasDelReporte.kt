package com.solventa4bits.movil.siniestro.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.AccountBox
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.solventa4bits.movil.R
import com.solventa4bits.movil.siniestro.dominio.ArchivoDelReporte
import com.solventa4bits.movil.siniestro.dominio.ProblemaDeFecha
import com.solventa4bits.movil.siniestro.dominio.diasCubiertos
import com.solventa4bits.movil.solicitud.ui.fechaCorta
import com.solventa4bits.movil.solicitud.ui.fechaDeSolicitud
import com.solventa4bits.movil.ui.BarraSolventa
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * M03 y M03A: la fecha en que ocurrio, por defecto hoy, y el dictamen escaneado con la camara
 * (BITS-111, AC-1, AC-2 y AC-3). La historia clinica es opcional. Sin dictamen no se sigue.
 */
@Composable
fun PantallaTuDictamen(
    estado: EstadoReporte,
    acciones: AccionesDelDictamen,
    alCerrarSesion: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PasoDelReporte(
        titulo = stringResource(R.string.dictamen_titulo),
        subtitulo = stringResource(R.string.dictamen_subtitulo),
        boton = stringResource(R.string.reporte_continuar),
        botonActivo = estado.puedeContinuar,
        alBoton = acciones.alContinuar,
        alVolver = acciones.alVolver,
        alCerrarSesion = alCerrarSesion,
        modifier = modifier,
    ) {
        TituloDeSeccion(stringResource(R.string.reporte_cuando))
        CampoDeFecha(estado, acciones.alCambiarFecha)
        AvisoDeFecha(estado)
        Tarjeta(alTocar = { acciones.alEscanear(Escaneo.DICTAMEN) }.takeIf { estado.puedeEscanear }) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Outlined.AccountBox, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(stringResource(R.string.dictamen_documento), style = MaterialTheme.typography.titleSmall)
                TextoTenue(stringResource(R.string.dictamen_emitido_por))
                Text(
                    text = stringResource(
                        if (estado.dictamen.isEmpty()) R.string.reporte_escanear else R.string.reporte_agregar_pagina,
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        if (estado.preparando) TextoTenue(stringResource(R.string.reporte_preparando))
        if (estado.falloAlPreparar) {
            Text(
                text = stringResource(R.string.reporte_fallo_foto),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        if (estado.dictamen.isNotEmpty()) {
            TituloDeSeccion(stringResource(R.string.reporte_escaneado))
            ArchivoEscaneado(
                nombre = pluralStringResource(R.plurals.dictamen_paginas, estado.dictamen.size, estado.dictamen.size),
                paginas = estado.dictamen,
                alQuitar = { acciones.alQuitar(Escaneo.DICTAMEN) },
            )
            TextButton(onClick = acciones.alEscanearOtraVez, enabled = !estado.preparando) {
                Text(stringResource(R.string.reporte_escanear_otra_vez))
            }
        }
        TituloDeSeccion(stringResource(R.string.reporte_opcional))
        HistoriaClinica(estado, acciones)
    }
}

@Composable
private fun HistoriaClinica(estado: EstadoReporte, acciones: AccionesDelDictamen) {
    if (estado.historia.isEmpty()) {
        Tarjeta(alTocar = { acciones.alEscanear(Escaneo.HISTORIA_CLINICA) }.takeIf { estado.puedeEscanear }) {
            Icon(Icons.AutoMirrored.Outlined.List, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.historia_titulo), style = MaterialTheme.typography.titleSmall)
                TextoTenue(stringResource(R.string.historia_detalle))
            }
        }
        return
    }
    ArchivoEscaneado(
        nombre = pluralStringResource(R.plurals.historia_paginas, estado.historia.size, estado.historia.size),
        paginas = estado.historia,
        alQuitar = { acciones.alQuitar(Escaneo.HISTORIA_CLINICA) },
    )
    TextButton(onClick = { acciones.alEscanear(Escaneo.HISTORIA_CLINICA) }, enabled = estado.puedeEscanear) {
        Text(stringResource(R.string.reporte_agregar_pagina))
    }
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

/** Un documento escaneado: su nombre con las paginas, cuando se escaneo y la papelera (AC-6). */
@Composable
private fun ArchivoEscaneado(nombre: String, paginas: List<ArchivoDelReporte>, alQuitar: () -> Unit) {
    Tarjeta {
        Icon(Icons.AutoMirrored.Outlined.List, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column(modifier = Modifier.weight(1f)) {
            Text(nombre, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            paginas.lastOrNull()?.let { ultima ->
                fechaDeSolicitud(ultima.capturadoEn)?.let { cuando ->
                    TextoTenue(stringResource(R.string.reporte_escaneado_con_camara, cuando.lowercase()))
                }
            }
        }
        IconButton(onClick = alQuitar) {
            Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.reporte_quitar, nombre))
        }
    }
}

/**
 * M07V: lo que se va a enviar. El pago de Vida siempre va al banco, como abono al credito. Se
 * pueden quitar archivos; sin el dictamen no se envia (AC-6).
 */
@Composable
fun PantallaRevisaYEnvia(
    estado: EstadoReporte,
    nombreDeLaPoliza: String,
    alQuitar: (Escaneo) -> Unit,
    alEnviar: () -> Unit,
    alVolver: () -> Unit,
    alCerrarSesion: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PasoDelReporte(
        titulo = stringResource(R.string.revisa_titulo),
        subtitulo = stringResource(R.string.revisa_subtitulo),
        boton = stringResource(R.string.revisa_enviar),
        botonActivo = estado.puedeContinuar && !estado.enviando,
        alBoton = alEnviar,
        alVolver = alVolver,
        alCerrarSesion = alCerrarSesion,
        modifier = modifier,
    ) {
        Datos {
            FilaDeDato(stringResource(R.string.revisa_poliza), nombreDeLaPoliza)
            FilaDeDato(stringResource(R.string.revisa_que_paso), stringResource(R.string.siniestro_tipo_incapacidad))
            FilaDeDato(stringResource(R.string.revisa_cuando), fechaCorta(estado.fecha))
            FilaDeDato(stringResource(R.string.revisa_pago), stringResource(R.string.revisa_pago_banco))
        }
        TituloDeSeccion(stringResource(R.string.revisa_archivos))
        if (estado.dictamen.isNotEmpty()) {
            val nombre = pluralStringResource(R.plurals.dictamen_paginas, estado.dictamen.size, estado.dictamen.size)
            ArchivoParaEnviar(nombre) { alQuitar(Escaneo.DICTAMEN) }
        }
        if (estado.historia.isNotEmpty()) {
            val nombre = pluralStringResource(R.plurals.historia_paginas, estado.historia.size, estado.historia.size)
            ArchivoParaEnviar(nombre) { alQuitar(Escaneo.HISTORIA_CLINICA) }
        }
        estado.problemaAlEnviar?.let { problema ->
            Text(
                text = stringResource(
                    when (problema) {
                        ProblemaAlEnviar.FALLO -> R.string.revisa_fallo
                        ProblemaAlEnviar.FUERA_DE_VIGENCIA -> R.string.revisa_fuera_de_vigencia
                        ProblemaAlEnviar.EN_CURSO -> R.string.revisa_en_curso
                    },
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun ArchivoParaEnviar(nombre: String, alQuitar: () -> Unit) {
    Tarjeta {
        Icon(Icons.AutoMirrored.Outlined.List, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(nombre, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        IconButton(onClick = alQuitar) {
            Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.reporte_quitar, nombre))
        }
    }
}

/** M08V: el numero de radicado y hasta cuando tiene Solventa para responder (AC-7). */
@Composable
fun PantallaReporteEnviado(
    numero: String,
    fechaLimite: String?,
    alVerSiniestro: () -> Unit,
    alCerrarSesion: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        BarraSolventa(stringResource(R.string.enviado_barra), alCerrarSesion)
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp),
            )
            Text(stringResource(R.string.enviado_titulo), style = MaterialTheme.typography.headlineSmall)
            Text(
                text = stringResource(R.string.enviado_radicado, numero),
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center,
            )
            Etiqueta(stringResource(R.string.siniestro_estado_en_estudio))
            Text(
                text = fechaLimite?.let { fechaCorta(it) }
                    ?.let { stringResource(R.string.enviado_plazo, it) }
                    ?: stringResource(R.string.enviado_sin_plazo),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
        }
        Button(onClick = alVerSiniestro, modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(stringResource(R.string.enviado_ver))
        }
    }
}
