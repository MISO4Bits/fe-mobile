package com.solventa4bits.movil.siniestro.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.solventa4bits.movil.R
import com.solventa4bits.movil.siniestro.dominio.DetalleDeSiniestro
import com.solventa4bits.movil.siniestro.dominio.EstadoDeSiniestro
import com.solventa4bits.movil.siniestro.dominio.TipoDeSiniestro
import com.solventa4bits.movil.solicitud.ui.fechaCorta
import com.solventa4bits.movil.solicitud.ui.fechaCortaDelDia
import com.solventa4bits.movil.ui.BarraSolventa

/**
 * M09: en que va un siniestro (BITS-118). Arriba lo que comparten todos; despues, lo de su estado:
 * el plazo, lo que falta, el pago o el motivo del rechazo. El pago automatico por sismo muestra el
 * sismo en vez de "Lo que reportaste", porque nadie lo reporto. En Falta informacion, el boton
 * para enviar lo que falta (AC-4).
 */
@Composable
fun PantallaTuSiniestro(
    estado: EstadoMisSiniestros,
    nombreDeLaPoliza: String,
    bancoDelCredito: String?,
    alEnviarLoQueFalta: (DetalleDeSiniestro) -> Unit,
    alReintentar: () -> Unit,
    alVolver: () -> Unit,
    alCerrarSesion: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        BarraSolventa(stringResource(R.string.tu_siniestro_titulo), alCerrarSesion, alVolver = alVolver)
        val detalle = estado.detalle
        when {
            detalle != null -> {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Encabezado(detalle)
                    LoDeSuEstado(detalle, bancoDelCredito)
                    detalle.evento?.let { ElSismo(it) }
                    if (!detalle.resumen.automatico) LoQueReportaste(detalle, nombreDeLaPoliza)
                }
                val siniestro = detalle.resumen
                val faltaAlgo = siniestro.estado == EstadoDeSiniestro.FALTA_INFORMACION && siniestro.tipo != null
                if (faltaAlgo && detalle.evidenciaPedida != null) {
                    Button(
                        onClick = { alEnviarLoQueFalta(detalle) },
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                    ) {
                        Text(stringResource(R.string.tu_siniestro_enviar_lo_que_falta))
                    }
                }
            }
            estado.falloDetalle -> Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                TextoTenue(stringResource(R.string.tu_siniestro_fallo))
                TextButton(onClick = alReintentar) { Text(stringResource(R.string.tu_siniestro_reintentar)) }
            }
            else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        }
    }
}

@Composable
private fun Encabezado(detalle: DetalleDeSiniestro) {
    val siniestro = detalle.resumen
    val pagadoEn = detalle.pago?.pagadoEn?.let { fechaCorta(it) }
    Datos {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            siniestro.estado?.let { Etiqueta(nombreDelEstado(it)) }
            if (siniestro.automatico) Etiqueta(stringResource(R.string.siniestro_automatico))
        }
        Text(siniestro.numero, style = MaterialTheme.typography.headlineSmall)
        if (siniestro.automatico && pagadoEn != null) {
            TextoTenue(stringResource(R.string.tu_siniestro_pagado_el, nombreDelTipo(siniestro.tipo), pagadoEn))
        } else {
            Column {
                TextoTenue(
                    stringResource(
                        R.string.tu_siniestro_reportado,
                        nombreDelTipo(siniestro.tipo),
                        fechaCorta(siniestro.reportadoEn).orEmpty(),
                    ),
                )
                TextoTenue(
                    stringResource(R.string.tu_siniestro_ultimo_cambio, fechaCorta(siniestro.actualizadoEn).orEmpty()),
                )
            }
        }
    }
}

@Composable
private fun LoDeSuEstado(detalle: DetalleDeSiniestro, bancoDelCredito: String?) {
    when (detalle.resumen.estado) {
        EstadoDeSiniestro.EN_ESTUDIO ->
            detalle.fechaLimite?.let { fechaCorta(it) }?.let { EnEstudio(detalle.resumen.numero, it) }
        EstadoDeSiniestro.FALTA_INFORMACION -> detalle.evidenciaPedida?.let { Pendientes(it) }
        EstadoDeSiniestro.PAGADO -> detalle.pago?.let { TuPago(it, bancoDelCredito) }
        EstadoDeSiniestro.RECHAZADO -> NuestraRespuesta(detalle.motivoRechazo)
        null -> Unit
    }
}

@Composable
private fun LoQueReportaste(detalle: DetalleDeSiniestro, nombreDeLaPoliza: String) {
    val siniestro = detalle.resumen
    TituloDeSeccion(stringResource(R.string.tu_siniestro_lo_que_reportaste))
    Datos {
        FilaDeDato(stringResource(R.string.revisa_poliza), nombreDeLaPoliza)
        FilaDeDato(
            stringResource(R.string.tu_siniestro_ocurrio),
            fechaCortaDelDia(detalle.fechaOcurrencia) ?: detalle.fechaOcurrencia,
        )
        // En Falta informacion lo que importa es lo que falta, no lo que ya llego.
        if (siniestro.estado != EstadoDeSiniestro.FALTA_INFORMACION) {
            FilaDeDato(stringResource(R.string.tu_siniestro_archivos), textoDeArchivos(detalle))
        }
        // En Vida el pago va al banco; el destino de Tu casa no viene en el detalle.
        if (siniestro.tipo == TipoDeSiniestro.INCAPACIDAD_TOTAL && siniestro.estado == EstadoDeSiniestro.EN_ESTUDIO) {
            FilaDeDato(stringResource(R.string.tu_siniestro_pagamos), stringResource(R.string.tu_siniestro_abono))
        }
    }
}

@Composable
private fun textoDeArchivos(detalle: DetalleDeSiniestro): String = listOfNotNull(
    detalle.documentos.takeIf { it > 0 }
        ?.let { pluralStringResource(R.plurals.tu_siniestro_documentos, it, it) },
    detalle.fotos.takeIf { it > 0 }?.let { pluralStringResource(R.plurals.tu_siniestro_fotos, it, it) },
).joinToString(" · ")
