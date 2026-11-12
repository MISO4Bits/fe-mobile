package com.solventa4bits.movil.siniestro.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.solventa4bits.movil.R
import com.solventa4bits.movil.siniestro.dominio.BANCOS_DEL_CHEQUE
import com.solventa4bits.movil.siniestro.dominio.TipoDeCuenta

/**
 * M06: como recibe el cliente el pago de Tu casa (BITS-116, AC-1). A quien se le paga (AC-2); la
 * cuenta para la transferencia, con el banco de la lista (AC-3), o el banco y la oficina del cheque
 * (AC-4). Si la lista de bancos no cargo, el banco se escribe a mano.
 */
@Composable
fun PantallaTuPago(
    estado: EstadoReporte,
    alCambiarPago: (DatosDePago) -> Unit,
    alElegirOficina: () -> Unit,
    alContinuar: () -> Unit,
    alVolver: () -> Unit,
    alCerrarSesion: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pago = estado.pago
    PasoDelReporte(
        titulo = stringResource(R.string.pago_titulo),
        subtitulo = "",
        boton = stringResource(R.string.reporte_continuar),
        botonActivo = pago.destino != null,
        alBoton = alContinuar,
        alVolver = alVolver,
        alCerrarSesion = alCerrarSesion,
        modifier = modifier,
    ) {
        Opcion(
            titulo = stringResource(R.string.pago_transferencia),
            detalle = stringResource(R.string.pago_transferencia_detalle),
            elegida = pago.forma == FormaDePago.TRANSFERENCIA,
        ) { alCambiarPago(pago.copy(forma = FormaDePago.TRANSFERENCIA)) }
        Opcion(
            titulo = stringResource(R.string.pago_cheque),
            detalle = stringResource(R.string.pago_cheque_detalle),
            elegida = pago.forma == FormaDePago.CHEQUE,
        ) { alCambiarPago(pago.copy(forma = FormaDePago.CHEQUE)) }
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Outlined.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(stringResource(R.string.pago_a_quien), style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (pago.forma == FormaDePago.CHEQUE) {
            TuOficina(pago, alCambiarPago, alElegirOficina)
        } else {
            TuCuenta(estado, alCambiarPago)
        }
    }
}

/** AC-3: la cuenta de la transferencia. */
@Composable
private fun TuCuenta(estado: EstadoReporte, alCambiarPago: (DatosDePago) -> Unit) {
    val pago = estado.pago
    TituloDeSeccion(stringResource(R.string.pago_tu_cuenta))
    Banco(pago.banco, estado.bancos) { alCambiarPago(pago.copy(banco = it.take(LARGO_DEL_BANCO))) }
    Row {
            Opcion(
                titulo = stringResource(R.string.pago_ahorros),
                elegida = pago.tipoCuenta == TipoDeCuenta.AHORROS,
                modifier = Modifier.weight(1f),
            ) { alCambiarPago(pago.copy(tipoCuenta = TipoDeCuenta.AHORROS)) }
            Opcion(
                titulo = stringResource(R.string.pago_corriente),
                elegida = pago.tipoCuenta == TipoDeCuenta.CORRIENTE,
                modifier = Modifier.weight(1f),
            ) { alCambiarPago(pago.copy(tipoCuenta = TipoDeCuenta.CORRIENTE)) }
        }
    OutlinedTextField(
        value = pago.numeroCuenta,
        onValueChange = { numero ->
            alCambiarPago(pago.copy(numeroCuenta = numero.filter(Char::isDigit).take(DIGITOS_DE_LA_CUENTA)))
        },
        label = { Text(stringResource(R.string.pago_numero)) },
        supportingText = { Text(stringResource(R.string.pago_numero_ayuda)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

/** AC-4: el banco del cheque y la oficina, que se elige en M06A. Cambiar de banco olvida la oficina. */
@Composable
private fun TuOficina(pago: DatosDePago, alCambiarPago: (DatosDePago) -> Unit, alElegirOficina: () -> Unit) {
    TituloDeSeccion(stringResource(R.string.pago_tu_oficina))
    Row {
        BANCOS_DEL_CHEQUE.forEach { banco ->
            Opcion(titulo = banco, elegida = pago.bancoDelCheque == banco, modifier = Modifier.weight(1f)) {
                alCambiarPago(pago.copy(bancoDelCheque = banco, oficina = pago.oficina?.takeIf { it.banco == banco }))
            }
        }
    }
    if (pago.bancoDelCheque == null) return
    val oficina = pago.oficina
    Tarjeta(alTocar = alElegirOficina) {
        Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column(modifier = Modifier.weight(1f)) {
            if (oficina == null) {
                Text(stringResource(R.string.pago_elegir_oficina), style = MaterialTheme.typography.bodyLarge)
            } else {
                Text(
                    text = nombreDeLaOficina(oficina),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                TextoTenue(oficina.direccion)
            }
        }
        if (oficina != null) {
            Text(
                text = stringResource(R.string.pago_cambiar),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/** El banco de la lista; sin lista, escrito a mano. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Banco(banco: String, bancos: List<String>, alCambiar: (String) -> Unit) {
    if (bancos.isEmpty()) {
        OutlinedTextField(
            value = banco,
            onValueChange = alCambiar,
            label = { Text(stringResource(R.string.pago_banco)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        return
    }
    var abierto by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = abierto, onExpandedChange = { abierto = it }) {
        OutlinedTextField(
            value = banco,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.pago_banco)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = abierto) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            bancos.forEach { nombre ->
                DropdownMenuItem(
                    text = { Text(nombre) },
                    onClick = {
                        alCambiar(nombre)
                        abierto = false
                    },
                )
            }
        }
    }
}

/** Una opcion con su circulo; el [detalle], si lo hay, va debajo en gris. */
@Composable
private fun Opcion(
    titulo: String,
    elegida: Boolean,
    modifier: Modifier = Modifier,
    detalle: String? = null,
    alElegir: () -> Unit,
) {
    Row(
        modifier = modifier.selectable(selected = elegida, onClick = alElegir),
        verticalAlignment = if (detalle == null) Alignment.CenterVertically else Alignment.Top,
    ) {
        RadioButton(selected = elegida, onClick = null)
        Column(modifier = Modifier.padding(start = 8.dp)) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (detalle == null) FontWeight.Normal else FontWeight.SemiBold,
            )
            detalle?.let { TextoTenue(it) }
        }
    }
}

private const val LARGO_DEL_BANCO = 100
private const val DIGITOS_DE_LA_CUENTA = 20
