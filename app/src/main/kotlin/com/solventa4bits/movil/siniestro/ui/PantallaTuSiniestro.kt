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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.solventa4bits.movil.R
import com.solventa4bits.movil.siniestro.dominio.DetalleDeSiniestro
import com.solventa4bits.movil.siniestro.dominio.EstadoDeSiniestro
import com.solventa4bits.movil.siniestro.dominio.TipoDeSiniestro
import com.solventa4bits.movil.solicitud.ui.fechaCorta
import com.solventa4bits.movil.solicitud.ui.fechaCortaDelDia
import com.solventa4bits.movil.ui.BarraSolventa

/**
 * M09V: en que va un siniestro. Lo propio de cada estado (lo que falta, el pago, el motivo del
 * rechazo) es de BITS-383 y BITS-384; aqui va lo que comparten todos.
 */
@Composable
fun PantallaTuSiniestro(
    estado: EstadoMisSiniestros,
    nombreDeLaPoliza: String,
    alReintentar: () -> Unit,
    alVolver: () -> Unit,
    alCerrarSesion: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        BarraSolventa(stringResource(R.string.tu_siniestro_titulo), alCerrarSesion, alVolver = alVolver)
        val detalle = estado.detalle
        when {
            detalle != null -> Detalle(detalle, nombreDeLaPoliza)
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
private fun Detalle(detalle: DetalleDeSiniestro, nombreDeLaPoliza: String) {
    val siniestro = detalle.resumen
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Datos {
            siniestro.estado?.let { Etiqueta(nombreDelEstado(it)) }
            Text(siniestro.numero, style = MaterialTheme.typography.headlineSmall)
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
        val plazo = detalle.fechaLimite?.let { fechaCorta(it) }
        if (siniestro.estado == EstadoDeSiniestro.EN_ESTUDIO && plazo != null) EnEstudio(siniestro.numero, plazo)
        TituloDeSeccion(stringResource(R.string.tu_siniestro_lo_que_reportaste))
        Datos {
            FilaDeDato(stringResource(R.string.revisa_poliza), nombreDeLaPoliza)
            FilaDeDato(
                stringResource(R.string.tu_siniestro_ocurrio),
                fechaCortaDelDia(detalle.fechaOcurrencia) ?: detalle.fechaOcurrencia,
            )
            FilaDeDato(stringResource(R.string.tu_siniestro_archivos), textoDeArchivos(detalle))
            // En Vida el pago va al banco; el destino de Tu casa no viene en el detalle.
            if (siniestro.tipo == TipoDeSiniestro.INCAPACIDAD_TOTAL) {
                FilaDeDato(stringResource(R.string.tu_siniestro_pagamos), stringResource(R.string.tu_siniestro_abono))
            }
        }
    }
}

@Composable
private fun EnEstudio(numero: String, plazo: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Outlined.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column {
                Text(
                    text = stringResource(R.string.tu_siniestro_revisando, numero),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(stringResource(R.string.tu_siniestro_plazo, plazo), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun textoDeArchivos(detalle: DetalleDeSiniestro): String = listOfNotNull(
    detalle.documentos.takeIf { it > 0 }
        ?.let { pluralStringResource(R.plurals.tu_siniestro_documentos, it, it) },
    detalle.fotos.takeIf { it > 0 }?.let { pluralStringResource(R.plurals.tu_siniestro_fotos, it, it) },
).joinToString(" · ")
