package com.solventa4bits.movil.siniestro.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.solventa4bits.movil.R
import com.solventa4bits.movil.cotizacion.ui.enPesos
import com.solventa4bits.movil.siniestro.dominio.Beneficiario
import com.solventa4bits.movil.siniestro.dominio.EventoSismo
import com.solventa4bits.movil.siniestro.dominio.EvidenciaPedida
import com.solventa4bits.movil.siniestro.dominio.PagoDelSiniestro
import com.solventa4bits.movil.solicitud.ui.fechaCorta
import com.solventa4bits.movil.solicitud.ui.fechaCortaDelDia

/** M09: el aviso de En estudio, con el plazo para responder (AC-3). */
@Composable
internal fun EnEstudio(numero: String, plazo: String) {
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

/** M09A: lo que pidio Solventa y que el plazo espera a que llegue (AC-2). */
@Composable
internal fun Pendientes(pedida: EvidenciaPedida) {
    TituloDeSeccion(stringResource(R.string.tu_siniestro_pendientes))
    Datos {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Outlined.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column {
                Text(pedida.detalle, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                fechaCorta(pedida.pedidaEn)?.let { TextoTenue(stringResource(R.string.tu_siniestro_pedido_el, it)) }
            }
        }
    }
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(painterResource(R.drawable.ic_reloj_de_arena), contentDescription = null)
            Text(stringResource(R.string.tu_siniestro_plazo_al_recibir), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** M09B y M09E: una tarjeta por linea del pago (AC-5). */
@Composable
internal fun TuPago(pago: PagoDelSiniestro, bancoDelCredito: String?) {
    TituloDeSeccion(stringResource(R.string.tu_siniestro_tu_pago))
    pago.lineas.forEach { linea ->
        Datos {
            val aQuien = when (linea.beneficiario) {
                Beneficiario.BANCO -> bancoDelCredito ?: stringResource(R.string.tu_siniestro_a_tu_banco)
                Beneficiario.CLIENTE -> stringResource(R.string.tu_siniestro_a_ti)
                null -> null
            }
            aQuien?.let { FilaDeDato(stringResource(R.string.tu_siniestro_pagamos_a), it) }
            FilaDeDato(stringResource(R.string.tu_siniestro_valor), enPesos(linea.valor))
            FilaDeDato(stringResource(R.string.tu_siniestro_destino), linea.destino)
            FilaDeDato(stringResource(R.string.tu_siniestro_fecha), fechaCorta(pago.pagadoEn).orEmpty())
        }
    }
}

/** M09C: por que no se paga y a donde escribir. */
@Composable
internal fun NuestraRespuesta(motivo: String?) {
    TituloDeSeccion(stringResource(R.string.tu_siniestro_nuestra_respuesta))
    Datos {
        Text(
            text = stringResource(R.string.tu_siniestro_no_podemos_pagar),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        motivo?.let {
            Text(stringResource(R.string.tu_siniestro_motivo, it), style = MaterialTheme.typography.bodyMedium)
        }
        Text(stringResource(R.string.tu_siniestro_dudas), style = MaterialTheme.typography.bodyMedium)
    }
}

/** M09E: el sismo que disparo el pago automatico (AC-6). */
@Composable
internal fun ElSismo(evento: EventoSismo) {
    TituloDeSeccion(stringResource(R.string.tu_siniestro_el_sismo))
    Datos {
        FilaDeDato(stringResource(R.string.tu_siniestro_fecha), fechaCortaDelDia(evento.fecha) ?: evento.fecha)
        FilaDeDato(stringResource(R.string.tu_siniestro_municipio), evento.municipio)
        FilaDeDato(stringResource(R.string.tu_siniestro_magnitud), evento.magnitud.toString().replace('.', ','))
    }
}
