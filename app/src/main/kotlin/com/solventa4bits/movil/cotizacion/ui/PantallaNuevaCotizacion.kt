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
 * su credito hipotecario (Figma 12, 12A y 12B). Solo dibuja el [estado].
 */
@Composable
fun PantallaNuevaCotizacion(
    estado: EstadoNuevaCotizacion,
    alElegirEntidad: (EntidadFinanciera) -> Unit,
    alReintentar: () -> Unit,
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
                estado.fallo -> NoSePudoConsultar(alReintentar)
                else -> BancoYCredito(estado, alElegirEntidad)
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            // La cotizacion es la siguiente historia (BITS-293): aun no lleva a ninguna parte.
            Button(
                onClick = {},
                enabled = estado.creditoElegido != null,
                modifier = Modifier.fillMaxWidth(),
            ) {
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
private fun NoSePudoConsultar(alReintentar: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.cotizacion_fallo),
            style = MaterialTheme.typography.bodyMedium,
        )
        OutlinedButton(onClick = alReintentar) {
            Text(stringResource(R.string.cotizacion_reintentar))
        }
    }
}

@Composable
private fun BancoYCredito(
    estado: EstadoNuevaCotizacion,
    alElegirEntidad: (EntidadFinanciera) -> Unit,
) {
    Text(
        text = stringResource(R.string.cotizacion_elige_banco),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Etiqueta(stringResource(R.string.cotizacion_tu_banco))
    SelectorDeBanco(estado.entidades, estado.entidadElegida, alElegirEntidad)

    val elegida = estado.entidadElegida ?: return
    Etiqueta(stringResource(R.string.cotizacion_datos_credito))
    val credito = estado.creditoElegido
    if (credito == null) {
        // El formulario para escribir los datos a mano llega en el siguiente paso de BITS-291.
        Text(
            text = stringResource(R.string.cotizacion_sin_credito, elegida.nombre),
            style = MaterialTheme.typography.bodyMedium,
        )
    } else {
        DatosDelCredito(elegida, credito)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectorDeBanco(
    entidades: List<EntidadFinanciera>,
    elegida: EntidadFinanciera?,
    alElegir: (EntidadFinanciera) -> Unit,
) {
    var abierto by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = abierto, onExpandedChange = { abierto = it }) {
        OutlinedTextField(
            value = elegida?.nombre.orEmpty(),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.cotizacion_banco_o_entidad)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = abierto) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            entidades.forEach { entidad ->
                DropdownMenuItem(
                    text = { Text(entidad.nombre) },
                    onClick = {
                        alElegir(entidad)
                        abierto = false
                    },
                )
            }
        }
    }
}

@Composable
private fun DatosDelCredito(entidad: EntidadFinanciera, credito: CreditoHipotecario) {
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
                entidadElegida = bancolombia,
            ),
            alElegirEntidad = {},
            alReintentar = {},
            alVolver = {},
            alCerrarSesion = {},
        )
    }
}
