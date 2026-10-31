package com.solventa4bits.movil.solicitud.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.solventa4bits.movil.R
import com.solventa4bits.movil.cotizacion.dominio.CodigoCobertura
import com.solventa4bits.movil.cotizacion.ui.enPesos
import com.solventa4bits.movil.solicitud.dominio.Solicitud

/**
 * Figma 09: la solicitud aprobada con el valor del seguro dentro de la cuota
 * (BITS-102). Solo datos del seguro: ni la cuota del credito ni cuanto cambia.
 * El seguro empieza el dia en que se emitio la poliza.
 */
@Composable
internal fun ResultadoAprobado(solicitud: Solicitud) {
    val alMes = solicitud.primaMensual?.let(::enPesos).orEmpty()
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Paso(
            icono = { IconoListo() },
            titulo = stringResource(R.string.resultado_paso_aprobada),
            detalle = solicitud.emitidaEn?.let(::fechaDeSolicitud).orEmpty(),
        )
        Paso(
            icono = { IconoListo() },
            titulo = stringResource(R.string.resultado_paso_emitida),
            detalle = solicitud.numero.orEmpty(),
        )
        Paso(
            icono = { IconoListo() },
            titulo = stringResource(R.string.resultado_paso_en_la_cuota),
            detalle = stringResource(
                R.string.resultado_al_mes_desde,
                alMes,
                solicitud.emitidaEn?.let(::fechaDelDia).orEmpty(),
            ),
        )
    }
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.resultado_mes_a_mes),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.resultado_lo_cobra, solicitud.credito?.banco.orEmpty()),
                    style = MaterialTheme.typography.titleSmall,
                )
                solicitud.primasPorCobertura[CodigoCobertura.VIDA]?.let {
                    Text(
                        stringResource(R.string.resultado_vida, enPesos(it)),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                solicitud.primasPorCobertura[CodigoCobertura.HOGAR]?.let {
                    Text(
                        stringResource(R.string.resultado_tu_casa, enPesos(it)),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                HorizontalDivider()
                Text(
                    text = stringResource(R.string.resultado_total, alMes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
