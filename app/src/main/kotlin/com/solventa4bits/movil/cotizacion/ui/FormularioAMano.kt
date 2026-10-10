package com.solventa4bits.movil.cotizacion.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.solventa4bits.movil.R

/**
 * Figma 12C: el cliente escribe los datos de su credito. Cada campo senala
 * su rango permitido cuando el valor escrito queda por fuera.
 */
@Composable
internal fun FormularioAMano(estado: EstadoNuevaCotizacion, acciones: AccionesDeCotizacion) {
    Text(
        text = stringResource(R.string.cotizacion_escribe_datos),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    CampoNumerico(
        nombre = stringResource(R.string.cotizacion_monto_total),
        valor = estado.montoEscrito,
        alCambiar = acciones::cambiarMonto,
        error = if (estado.montoFueraDeRango) stringResource(R.string.cotizacion_rango_monto) else null,
        prefijo = "$",
    )
    CampoNumerico(
        nombre = stringResource(R.string.cotizacion_cuanto_debes),
        valor = estado.saldoEscrito,
        alCambiar = acciones::cambiarSaldo,
        error = if (estado.saldoFueraDeRango) stringResource(R.string.cotizacion_rango_saldo) else null,
        prefijo = "$",
    )
    CampoNumerico(
        nombre = stringResource(R.string.cotizacion_meses_faltan),
        valor = estado.mesesEscritos,
        alCambiar = acciones::cambiarMeses,
        error = if (estado.mesesFueraDeRango) stringResource(R.string.cotizacion_rango_meses) else null,
        sufijo = stringResource(R.string.cotizacion_sufijo_meses),
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.shapes.medium)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResource(R.string.cotizacion_declaras_titulo),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
        )
        Text(
            text = stringResource(R.string.cotizacion_declaras_detalle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
        )
    }
}

@Composable
private fun CampoNumerico(
    nombre: String,
    valor: String,
    alCambiar: (String) -> Unit,
    error: String?,
    prefijo: String? = null,
    sufijo: String? = null,
) {
    OutlinedTextField(
        value = valor,
        onValueChange = alCambiar,
        label = { Text(nombre) },
        prefix = prefijo?.let { { Text(it) } },
        suffix = sufijo?.let { { Text(it) } },
        singleLine = true,
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
}
