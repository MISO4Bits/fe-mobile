package com.solventa4bits.movil.cotizacion.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.solventa4bits.movil.R
import com.solventa4bits.movil.cotizacion.dominio.CreditoHipotecario
import com.solventa4bits.movil.cotizacion.dominio.EntidadFinanciera
import com.solventa4bits.movil.ui.BarraSolventa
import com.solventa4bits.movil.ui.tema.TemaSolventa

/**
 * Primer paso de una cotizacion: el cliente elige su banco y ve los datos de
 * su credito hipotecario, o los escribe (Figma 12, 12A, 12B y 12C). Solo
 * dibuja el [estado].
 */
@Composable
fun PantallaNuevaCotizacion(
    estado: EstadoNuevaCotizacion,
    acciones: AccionesDeCotizacion,
    alVolver: () -> Unit,
    alCerrarSesion: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        BarraSolventa(stringResource(R.string.cotizacion_titulo), alCerrarSesion)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.cotizacion_tu_credito),
                style = MaterialTheme.typography.headlineSmall,
            )
            when {
                estado.cargando -> Consultando()
                // Sin respuesta del BFF no hay ni lista de bancos: se informa
                // y el cliente decide si reintenta o escribe todo.
                estado.fallo && !estado.aMano -> NoSePudoConsultar(acciones, conEscribir = true)
                else -> BancoYCredito(estado, acciones)
            }
        }
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            // La cotizacion es la siguiente historia (BITS-293): aun no lleva a ninguna parte.
            Button(onClick = {}, enabled = estado.puedeCotizar, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.cotizacion_ver_cotizacion))
            }
            TextButton(onClick = alVolver) {
                Text(stringResource(R.string.cotizacion_atras))
            }
        }
    }
}

@Composable
private fun Consultando() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CircularProgressIndicator()
        Text(
            text = stringResource(R.string.cotizacion_consultando),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(R.string.cotizacion_consultando_detalle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun NoSePudoConsultar(acciones: AccionesDeCotizacion, conEscribir: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(R.string.cotizacion_fallo),
            style = MaterialTheme.typography.bodyMedium,
        )
        OutlinedButton(onClick = acciones::reintentar) {
            Text(stringResource(R.string.cotizacion_reintentar))
        }
        if (conEscribir) {
            TextButton(onClick = acciones::escribirAMano) {
                Text(stringResource(R.string.cotizacion_escribir_yo))
            }
        }
    }
}

@Composable
private fun BancoYCredito(estado: EstadoNuevaCotizacion, acciones: AccionesDeCotizacion) {
    // El BFF respondio pero la fuente no: el banco elegido se conserva y el
    // cliente puede reintentar o seguir escribiendo abajo.
    if (estado.fuenteNoDisponible) NoSePudoConsultar(acciones, conEscribir = false)

    Etiqueta(stringResource(R.string.cotizacion_tu_banco))
    if (estado.entidades.isNotEmpty()) SelectorDeBanco(estado, acciones)
    if (estado.otroBanco) {
        OutlinedTextField(
            value = estado.bancoEscrito,
            onValueChange = acciones::cambiarBanco,
            label = { Text(stringResource(R.string.cotizacion_escribe_banco)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    val elegida = estado.entidadElegida
    val credito = estado.creditoElegido
    when {
        estado.pideDatosAMano -> {
            Etiqueta(stringResource(R.string.cotizacion_datos_credito))
            FormularioAMano(estado, acciones)
        }
        elegida != null && credito != null -> {
            Etiqueta(stringResource(R.string.cotizacion_datos_credito))
            DatosDelCredito(elegida, credito, estado.fechaConsulta)
            TextButton(onClick = acciones::escribirAMano) {
                Text(stringResource(R.string.cotizacion_prefiero_escribir))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectorDeBanco(estado: EstadoNuevaCotizacion, acciones: AccionesDeCotizacion) {
    var abierto by remember { mutableStateOf(false) }
    val otroBanco = stringResource(R.string.cotizacion_otro_banco)
    Text(
        text = stringResource(R.string.cotizacion_elige_banco),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    ExposedDropdownMenuBox(expanded = abierto, onExpandedChange = { abierto = it }) {
        OutlinedTextField(
            value = if (estado.otroBanco) otroBanco else estado.entidadElegida?.nombre.orEmpty(),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.cotizacion_banco_o_entidad)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = abierto) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            estado.entidades.forEach { entidad ->
                DropdownMenuItem(
                    text = { Text(entidad.nombre) },
                    onClick = {
                        acciones.elegirEntidad(entidad)
                        abierto = false
                    },
                )
            }
            DropdownMenuItem(
                text = { Text(otroBanco) },
                onClick = {
                    acciones.elegirOtroBanco()
                    abierto = false
                },
            )
        }
    }
}

/** Figma 12B: los datos que el banco reporto, solo lectura, con su origen y fecha. */
@Composable
private fun DatosDelCredito(entidad: EntidadFinanciera, credito: CreditoHipotecario, fechaConsulta: String?) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.cotizacion_traido_de, entidad.nombre).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Dato(stringResource(R.string.cotizacion_monto_total), enPesos(credito.valorCredito))
            Dato(stringResource(R.string.cotizacion_saldo_hoy), enPesos(credito.saldoInsoluto))
            Dato(
                stringResource(R.string.cotizacion_meses_faltan),
                stringResource(R.string.cotizacion_meses, credito.plazoRestanteMeses),
            )
            HorizontalDivider()
            fechaConsulta?.let(::fechaLegible)?.let { fecha ->
                Text(
                    text = stringResource(R.string.cotizacion_consultado_el, fecha),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = stringResource(R.string.cotizacion_solo_lectura),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
    Text(
        text = stringResource(R.string.cotizacion_origen_datos, entidad.nombre),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun Dato(nombre: String, valor: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(nombre, style = MaterialTheme.typography.bodyLarge)
        Text(valor, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun Etiqueta(texto: String) {
    Text(
        text = texto.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Preview(showBackground = true)
@Composable
private fun PantallaNuevaCotizacionPreview() {
    val bancolombia = EntidadFinanciera("bancolombia", "Bancolombia")
    val sinAcciones = object : AccionesDeCotizacion {
        override fun reintentar() = Unit
        override fun elegirEntidad(entidad: EntidadFinanciera) = Unit
        override fun elegirOtroBanco() = Unit
        override fun escribirAMano() = Unit
        override fun cambiarBanco(texto: String) = Unit
        override fun cambiarMonto(texto: String) = Unit
        override fun cambiarSaldo(texto: String) = Unit
        override fun cambiarMeses(texto: String) = Unit
    }
    TemaSolventa {
        PantallaNuevaCotizacion(
            estado = EstadoNuevaCotizacion(
                cargando = false,
                entidades = listOf(bancolombia),
                creditos = listOf(
                    CreditoHipotecario(
                        entidadId = "bancolombia",
                        entidadNombre = "BANCOLOMBIA S.A.",
                        valorCredito = 380_000_000.0,
                        saldoInsoluto = 320_000_000.0,
                        plazoRestanteMeses = 180,
                        cuotaMensual = 3_100_000.0,
                    ),
                ),
                fechaConsulta = "2026-10-09T12:00:00Z",
                entidadElegida = bancolombia,
            ),
            acciones = sinAcciones,
            alVolver = {},
            alCerrarSesion = {},
        )
    }
}
