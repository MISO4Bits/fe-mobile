package com.solventa4bits.movil.cotizacion.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.solventa4bits.movil.R
import com.solventa4bits.movil.cotizacion.dominio.CodigoCobertura
import com.solventa4bits.movil.cotizacion.dominio.Cotizacion
import com.solventa4bits.movil.cotizacion.dominio.DatosDelCredito
import com.solventa4bits.movil.cotizacion.dominio.PrecioDeCobertura
import com.solventa4bits.movil.cotizacion.dominio.plan
import com.solventa4bits.movil.cotizacion.dominio.tuCasa
import com.solventa4bits.movil.ui.BarraSolventa
import com.solventa4bits.movil.ui.tema.TemaSolventa

/**
 * Figma 03 "Tus coberturas" (BITS-96): la vida va siempre y "Tu casa" se marca
 * o se quita. El total se actualiza al instante, sin llamar al servidor (AC-2).
 */
@Composable
fun PantallaCoberturas(
    cotizacion: Cotizacion,
    conTuCasa: Boolean,
    alCambiarTuCasa: (Boolean) -> Unit,
    alContinuar: () -> Unit,
    alVolver: () -> Unit,
    alCerrarSesion: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val plan = cotizacion.plan(conTuCasa)
    Column(modifier = modifier.fillMaxSize()) {
        BarraSolventa(stringResource(R.string.coberturas_titulo), alCerrarSesion)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column {
                Text(
                    text = stringResource(R.string.coberturas_elige),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = stringResource(R.string.coberturas_subtitulo),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Cobertura(
                marcada = true,
                alCambiar = null,
                etiqueta = stringResource(R.string.coberturas_va_siempre),
                titulo = stringResource(R.string.coberturas_vida_titulo),
                detalle = stringResource(R.string.coberturas_vida_detalle),
                precio = stringResource(R.string.coberturas_por_mes, enPesos(plan.vida)),
            )
            cotizacion.tuCasa?.let { casa ->
                Cobertura(
                    marcada = conTuCasa,
                    alCambiar = alCambiarTuCasa,
                    etiqueta = null,
                    titulo = stringResource(R.string.coberturas_casa_titulo),
                    detalle = stringResource(R.string.coberturas_casa_detalle, enPesos(casa.sumaAsegurada)),
                    precio = stringResource(R.string.coberturas_mas_por_mes, enPesos(casa.primaTotal)),
                )
            }
            Recuadro(
                titulo = stringResource(R.string.coberturas_pagas),
                destacado = true,
                valor = enPesos(plan.total),
                detalle = stringResource(R.string.coberturas_se_cobra),
            )
            Recuadro(
                titulo = stringResource(R.string.coberturas_cuanto_cubrimos),
                destacado = false,
                valor = stringResource(
                    R.string.coberturas_lo_que_debes,
                    enPesos(cotizacion.datosCredito.saldoInsoluto.toDouble()),
                ),
                detalle = null,
            )
        }
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Button(onClick = alContinuar, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.coberturas_continuar))
            }
            TextButton(onClick = alVolver) {
                Text(stringResource(R.string.cotizacion_atras))
            }
        }
    }
}

/** Una cobertura con su casilla. Sin [alCambiar] la casilla queda fija: la vida va siempre. */
@Composable
private fun Cobertura(
    marcada: Boolean,
    alCambiar: ((Boolean) -> Unit)?,
    etiqueta: String?,
    titulo: String,
    detalle: String,
    precio: String,
) {
    // Marcada se resalta como en Figma. La vida no se puede tocar, pero no se ve apagada.
    val fondo = if (marcada) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
    val colores = CardDefaults.outlinedCardColors(containerColor = fondo)
    val borde = if (marcada) {
        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    }
    val contenido: @Composable () -> Unit = {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            Checkbox(checked = marcada, onCheckedChange = alCambiar)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                etiqueta?.let {
                    Text(
                        text = it.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(titulo, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = detalle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = precio,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
    if (alCambiar == null) {
        OutlinedCard(modifier = Modifier.fillMaxWidth(), colors = colores, border = borde) { contenido() }
    } else {
        OutlinedCard(
            onClick = { alCambiar(!marcada) },
            modifier = Modifier.fillMaxWidth(),
            colors = colores,
            border = borde,
        ) { contenido() }
    }
}

@Composable
private fun Recuadro(titulo: String, destacado: Boolean, valor: String, detalle: String?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.shapes.medium)
            .padding(16.dp),
    ) {
        Text(titulo, style = MaterialTheme.typography.bodySmall)
        Text(
            text = valor,
            style = if (destacado) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        detalle?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
    }
}

@Preview(showBackground = true)
@Composable
private fun PantallaCoberturasPreview() {
    TemaSolventa {
        PantallaCoberturas(
            cotizacion = Cotizacion(
                vigente = true,
                datosCredito = DatosDelCredito(
                    entidadAcreedora = "Bancolombia",
                    valorCredito = 320_000_000,
                    saldoInsoluto = 320_000_000,
                    plazoRestanteMeses = 240,
                ),
                datosDeclarados = true,
                primaMensual = 49_100.0,
                primaBaseMensual = 49_100.0,
                sumaAsegurada = 320_000_000.0,
                coberturaMeses = 240,
                personalizada = false,
                factores = emptyList(),
                coberturas = listOf(
                    PrecioDeCobertura(
                        codigo = CodigoCobertura.HOGAR,
                        sumaAsegurada = 240_000_000.0,
                        primaNeta = 23_760.0,
                        iva = 4_514.0,
                        primaTotal = 28_274.0,
                    ),
                ),
            ),
            conTuCasa = true,
            alCambiarTuCasa = {},
            alContinuar = {},
            alVolver = {},
            alCerrarSesion = {},
        )
    }
}
