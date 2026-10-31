package com.solventa4bits.movil.inicio.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.solventa4bits.movil.R
import com.solventa4bits.movil.seguro.ui.nombreDelSeguro
import com.solventa4bits.movil.solicitud.dominio.Solicitud

/** "Mis seguros" del Figma 10: las polizas emitidas de dos en dos, o el texto si aun no hay. */
@Composable
internal fun MisSeguros(seguros: List<Solicitud>, alVerSeguro: (Solicitud) -> Unit) {
    if (seguros.isEmpty()) {
        Text(
            text = stringResource(R.string.inicio_seguros_al_aprobar),
            style = MaterialTheme.typography.bodyMedium,
        )
        return
    }
    seguros.chunked(2).forEach { fila ->
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            fila.forEach { seguro ->
                TarjetaSeguro(seguro, Modifier.weight(1f)) { alVerSeguro(seguro) }
            }
            if (fila.size == 1) Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun TarjetaSeguro(seguro: Solicitud, modifier: Modifier, alVer: () -> Unit) {
    OutlinedCard(onClick = alVer, modifier = modifier) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(
                painterResource(R.drawable.ic_escudo),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            EtiquetaVigente()
            Text(nombreDelSeguro(seguro), style = MaterialTheme.typography.titleSmall)
            Text(
                text = seguro.numero.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = seguro.credito?.banco.orEmpty(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/** La etiqueta "Vigente" de una poliza emitida. */
@Composable
internal fun EtiquetaVigente() {
    Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.secondaryContainer) {
        Text(
            text = stringResource(R.string.seguro_vigente),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}
