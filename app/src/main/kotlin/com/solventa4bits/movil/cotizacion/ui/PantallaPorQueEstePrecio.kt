package com.solventa4bits.movil.cotizacion.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.solventa4bits.movil.R
import com.solventa4bits.movil.cotizacion.dominio.Cotizacion
import com.solventa4bits.movil.cotizacion.dominio.EfectoEnElPrecio
import com.solventa4bits.movil.cotizacion.dominio.FactorDelPrecio

/**
 * Figma 03C: por que el cliente paga lo que paga. Solo se ofrece cuando la
 * cotizacion aplica su perfil de riesgo (BITS-95, AC-3); sin perfil no se dice
 * nada del perfil (AC-4).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PanelPorQueEstePrecio(cotizacion: Cotizacion, alCerrar: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = alCerrar,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.porque_tu_precio).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(R.string.porque_titulo),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = stringResource(
                        R.string.porque_resumen,
                        enPesos(cotizacion.primaMensual),
                        enPesos(cotizacion.primaBaseMensual),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            val masPesa = cotizacion.factorQueMasPesa()
            cotizacion.factoresPorPeso().forEach { factor -> Factor(factor, esElQueMasPesa = factor == masPesa) }
            Explicacion(
                titulo = stringResource(R.string.porque_saldos_titulo),
                detalle = stringResource(R.string.porque_saldos_detalle),
            )
            Button(onClick = alCerrar, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text(stringResource(R.string.porque_entendido))
            }
        }
    }
}

@Composable
private fun Factor(factor: FactorDelPrecio, esElQueMasPesa: Boolean) {
    val efecto = stringResource(
        if (factor.efecto == EfectoEnElPrecio.BAJA) R.string.porque_baja else R.string.porque_sube,
    )
    val detalle = if (esElQueMasPesa) "$efecto ${stringResource(R.string.porque_mas_pesa)}" else efecto
    Explicacion(titulo = factor.descripcion, detalle = detalle)
}

@Composable
private fun Explicacion(titulo: String, detalle: String) {
    Column {
        Text(titulo, style = MaterialTheme.typography.titleSmall)
        Text(
            text = detalle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
