package com.solventa4bits.movil.siniestro.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.AccountBox
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.solventa4bits.movil.R
import com.solventa4bits.movil.siniestro.dominio.ArchivoDelReporte
import com.solventa4bits.movil.solicitud.ui.fechaCorta
import com.solventa4bits.movil.solicitud.ui.fechaDeSolicitud
import com.solventa4bits.movil.ui.BarraSolventa

/**
 * M03 y M03A: la fecha en que ocurrio, por defecto hoy, y el dictamen escaneado con la camara
 * (BITS-111, AC-1, AC-2 y AC-3). La historia clinica es opcional. Sin dictamen no se sigue.
 * M03F: lo que falta del dictamen de un siniestro ya reportado (BITS-118, AC-4), sin la fecha ni
 * la historia clinica.
 */
@Composable
fun PantallaTuDictamen(
    estado: EstadoReporte,
    acciones: AccionesDelDictamen,
    alCerrarSesion: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val entrega = estado.entrega
    PasoDelReporte(
        titulo = stringResource(if (entrega != null) R.string.falta_titulo else R.string.dictamen_titulo),
        subtitulo = entrega?.let { stringResource(R.string.falta_subtitulo_dictamen, it.numero) }
            ?: stringResource(R.string.dictamen_subtitulo),
        boton = stringResource(R.string.reporte_continuar),
        botonActivo = estado.puedeContinuar,
        alBoton = acciones.alContinuar,
        alVolver = acciones.alVolver,
        alCerrarSesion = alCerrarSesion,
        modifier = modifier,
        barra = stringResource(if (entrega != null) R.string.tu_siniestro_titulo else R.string.reporte_titulo),
    ) {
        if (entrega == null) CuandoOcurrio(estado, acciones.alCambiarFecha)
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
        Preparando(estado)
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
        if (entrega == null) {
            TituloDeSeccion(stringResource(R.string.reporte_opcional))
            HistoriaClinica(estado, acciones)
        }
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

/** Mientras se prepara la ultima foto, y si no se pudo leer. */
@Composable
internal fun Preparando(estado: EstadoReporte) {
    if (estado.preparando) TextoTenue(stringResource(R.string.reporte_preparando))
    if (estado.falloAlPreparar) {
        Text(
            text = stringResource(R.string.reporte_fallo_foto),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

/** Un documento escaneado: su nombre, cuando se escaneo y la papelera (AC-6). */
@Composable
internal fun ArchivoEscaneado(nombre: String, paginas: List<ArchivoDelReporte>, alQuitar: () -> Unit) {
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
 * M08V y M08: el numero de radicado y hasta cuando tiene Solventa para responder (AC-7). M08F y
 * M08FV, si [loQueFaltaba]: llego lo que faltaba y el plazo empieza de nuevo (BITS-118, AC-4).
 */
@Composable
fun PantallaReporteEnviado(
    numero: String,
    fechaLimite: String?,
    alVerSiniestro: () -> Unit,
    alCerrarSesion: () -> Unit,
    modifier: Modifier = Modifier,
    loQueFaltaba: Boolean = false,
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
            Text(
                text = stringResource(if (loQueFaltaba) R.string.falta_recibido else R.string.enviado_titulo),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = stringResource(
                    if (loQueFaltaba) R.string.falta_siniestro else R.string.enviado_radicado,
                    numero,
                ),
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center,
            )
            Etiqueta(stringResource(R.string.siniestro_estado_en_estudio))
            Text(
                text = fechaLimite?.let { fechaCorta(it) }
                    ?.let { plazo ->
                        stringResource(if (loQueFaltaba) R.string.falta_plazo else R.string.enviado_plazo, plazo)
                    }
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
