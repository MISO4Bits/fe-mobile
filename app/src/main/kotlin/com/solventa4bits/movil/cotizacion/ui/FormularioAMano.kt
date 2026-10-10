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

/** Figma 12C: el banco no reporto la hipoteca y el cliente escribe los datos. */
@Composable
internal fun FormularioAMano(
    estado: EstadoNuevaCotizacion,
    alCambiarMonto: (String) -> Unit,
    alCambiarSaldo: (String) -> Unit,
    alCambiarMeses: (String) -> Unit,
) {
    Text(
        text = stringResource(R.string.cotizacion_sin_conexion),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    CampoDePesos(stringResource(R.string.cotizacion_monto_total), estado.montoEscrito, alCambiarMonto)
    CampoDePesos(
        nombre = stringResource(R.string.cotizacion_cuanto_debes),
        valor = estado.saldoEscrito,
        alCambiar = alCambiarSaldo,
        error = if (estado.saldoMayorQueMonto) stringResource(R.string.cotizacion_saldo_mayor) else null,
    )
    OutlinedTextField(
        value = estado.mesesEscritos,
        onValueChange = alCambiarMeses,
        label = { Text(stringResource(R.string.cotizacion_meses_faltan)) },
        suffix = { Text(stringResource(R.string.cotizacion_sufijo_meses)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
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
private fun CampoDePesos(nombre: String, valor: String, alCambiar: (String) -> Unit, error: String? = null) {
    OutlinedTextField(
        value = valor,
        onValueChange = alCambiar,
        label = { Text(nombre) },
        prefix = { Text("$") },
        singleLine = true,
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
}
