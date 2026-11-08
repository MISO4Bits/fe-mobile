package com.solventa4bits.movil.siniestro.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.solventa4bits.movil.R
import com.solventa4bits.movil.siniestro.dominio.EstadoDeSiniestro
import com.solventa4bits.movil.siniestro.dominio.SiniestroResumen
import com.solventa4bits.movil.siniestro.dominio.TipoDeSiniestro
import com.solventa4bits.movil.siniestro.dominio.enCurso
import com.solventa4bits.movil.solicitud.dominio.Solicitud
import com.solventa4bits.movil.solicitud.ui.fechaCorta

/**
 * M01 y M01N: los siniestros de la poliza, por cobertura, en "Mi seguro". La entrada para reportar
 * desaparece mientras esa cobertura tiene uno En estudio o en Falta informacion (BITS-111, AC-2A).
 */
@Composable
fun SiniestrosDelSeguro(
    poliza: Solicitud,
    estado: EstadoMisSiniestros,
    alReportarIncapacidad: () -> Unit,
    alVerSiniestro: (SiniestroResumen) -> Unit,
) {
    if (!estado.listos) return
    val deLaPoliza = estado.siniestros.filter { it.polizaId == poliza.id && it.tipo != null }
    val (deTuCasa, deVida) = deLaPoliza.partition { checkNotNull(it.tipo).deTuCasa }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TituloDeSeccion(stringResource(R.string.siniestro_seccion_vida))
        if (deLaPoliza.enCurso(poliza.id, TipoDeSiniestro.INCAPACIDAD_TOTAL) == null) {
            Tarjeta(alTocar = alReportarIncapacidad) {
                IconoDeTipo(TipoDeSiniestro.INCAPACIDAD_TOTAL)
                Text(
                    text = stringResource(R.string.siniestro_reportar_incapacidad),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                Flecha()
            }
        }
        deVida.forEach { TarjetaDeSiniestro(it, alVerSiniestro) }
        if (poliza.conTuCasa || deTuCasa.isNotEmpty()) {
            TituloDeSeccion(stringResource(R.string.siniestro_seccion_casa))
            deTuCasa.forEach { TarjetaDeSiniestro(it, alVerSiniestro) }
        }
    }
}

@Composable
private fun TarjetaDeSiniestro(siniestro: SiniestroResumen, alVerSiniestro: (SiniestroResumen) -> Unit) {
    Tarjeta(alTocar = { alVerSiniestro(siniestro) }) {
        IconoDeTipo(siniestro.tipo)
        Column(modifier = Modifier.weight(1f)) {
            Text(nombreDelTipo(siniestro.tipo), style = MaterialTheme.typography.bodyLarge)
            TextoTenue(stringResource(R.string.siniestro_radicado, siniestro.numero))
            // El automatico nadie lo reporto: se dice cuando se pago.
            val cuando = if (siniestro.automatico) {
                stringResource(R.string.siniestro_pagado_el, fechaCorta(siniestro.actualizadoEn).orEmpty())
            } else {
                stringResource(R.string.siniestro_reportado_el, fechaCorta(siniestro.reportadoEn).orEmpty())
            }
            TextoTenue(cuando)
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            siniestro.estado?.let { Etiqueta(nombreDelEstado(it)) }
            if (siniestro.automatico) Etiqueta(stringResource(R.string.siniestro_automatico))
        }
        Flecha()
    }
}

@Composable
internal fun Tarjeta(alTocar: (() -> Unit)? = null, contenido: @Composable RowScope.() -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (alTocar != null) Modifier.clickable(onClick = alTocar) else Modifier),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            content = contenido,
        )
    }
}

@Composable
internal fun TituloDeSeccion(texto: String) {
    Text(
        text = texto,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp),
    )
}

/** Como la etiqueta "Vigente" de la poliza. */
@Composable
internal fun Etiqueta(texto: String) {
    Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.secondaryContainer) {
        Text(
            text = texto,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}

@Composable
internal fun TextoTenue(texto: String) {
    Text(texto, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun Flecha() {
    Icon(
        Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun IconoDeTipo(tipo: TipoDeSiniestro?) {
    val icono: ImageVector = if (tipo?.deTuCasa == true) Icons.Outlined.Home else Icons.Outlined.FavoriteBorder
    Icon(icono, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
}

@Composable
internal fun nombreDelTipo(tipo: TipoDeSiniestro?): String = stringResource(
    when (tipo) {
        TipoDeSiniestro.INCAPACIDAD_TOTAL -> R.string.siniestro_tipo_incapacidad
        TipoDeSiniestro.INCENDIO_TERREMOTO -> R.string.siniestro_tipo_incendio
        TipoDeSiniestro.SISMO_PARAMETRICO -> R.string.siniestro_tipo_sismo
        null -> R.string.siniestro_tipo_otro
    },
)

@Composable
internal fun nombreDelEstado(estado: EstadoDeSiniestro): String = stringResource(
    when (estado) {
        EstadoDeSiniestro.EN_ESTUDIO -> R.string.siniestro_estado_en_estudio
        EstadoDeSiniestro.FALTA_INFORMACION -> R.string.siniestro_estado_falta_informacion
        EstadoDeSiniestro.PAGADO -> R.string.siniestro_estado_pagado
        EstadoDeSiniestro.RECHAZADO -> R.string.siniestro_estado_rechazado
    },
)
