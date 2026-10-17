package com.solventa4bits.movil.cotizacion.ui

import androidx.annotation.StringRes
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.solventa4bits.movil.R
import com.solventa4bits.movil.cotizacion.dominio.Cotizacion
import com.solventa4bits.movil.cotizacion.dominio.DatosDelCredito
import com.solventa4bits.movil.ui.BarraSolventa
import com.solventa4bits.movil.ui.tema.TemaSolventa

/**
 * "Lo que pagas": la cotizacion tal como la calculo el backend, con los datos
 * del credito a la vista (BITS-95, AC-2) y, segun la [variante], los avisos de
 * lo que falta para que el precio quede preaprobado (AC-5).
 */
@Composable
fun PantallaLoQuePagas(
    cotizacion: Cotizacion,
    alBuscarCredito: () -> Unit,
    alIrAMisSeguros: () -> Unit,
    alVolver: () -> Unit,
    alCerrarSesion: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val variante = cotizacion.variante()
    var explicandoElPrecio by rememberSaveable { mutableStateOf(false) }
    Column(modifier = modifier.fillMaxSize()) {
        BarraSolventa(stringResource(R.string.pagas_titulo), alCerrarSesion)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column {
                Text(
                    text = stringResource(R.string.pagas_esto_es_lo_que_pagas),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = stringResource(subtitulo(variante)),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            PrecioMensual(cotizacion)
            // AC-3 y AC-4: la explicacion solo existe si el precio aplica el perfil.
            if (cotizacion.personalizada) {
                TextButton(onClick = { explicandoElPrecio = true }) {
                    Text(stringResource(R.string.pagas_por_que))
                }
            }
            DatosALaVista(cotizacion)
            Avisos(variante)
        }
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BotonPrincipal(variante, alBuscarCredito, alIrAMisSeguros)
            TextButton(onClick = alVolver) {
                Text(stringResource(R.string.cotizacion_atras))
            }
        }
    }
    if (explicandoElPrecio) {
        PanelPorQueEstePrecio(cotizacion, alCerrar = { explicandoElPrecio = false })
    }
}

@StringRes
private fun subtitulo(variante: VarianteDeCotizacion): Int = when (variante) {
    VarianteDeCotizacion.PREAPROBADA -> R.string.pagas_subtitulo_preaprobada
    VarianteDeCotizacion.VERIFICADA_SIN_PERFIL -> R.string.pagas_subtitulo_banco
    VarianteDeCotizacion.DECLARADA_CON_PERFIL,
    VarianteDeCotizacion.DECLARADA_SIN_PERFIL,
    -> R.string.pagas_subtitulo_declarados
}

@Composable
private fun PrecioMensual(cotizacion: Cotizacion) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary, MaterialTheme.shapes.large)
            .padding(20.dp),
    ) {
        val sobreElColor = MaterialTheme.colorScheme.onPrimary
        Text(
            text = stringResource(R.string.pagas_cada_mes),
            style = MaterialTheme.typography.labelLarge,
            color = sobreElColor,
        )
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = enPesos(cotizacion.primaMensual),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = sobreElColor,
            )
            Text(
                text = stringResource(R.string.pagas_por_mes),
                style = MaterialTheme.typography.titleMedium,
                color = sobreElColor,
                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
            )
        }
        Text(
            text = stringResource(R.string.pagas_te_cubre, cotizacion.coberturaMeses),
            style = MaterialTheme.typography.bodySmall,
            color = sobreElColor,
        )
    }
}

/** AC-2: la entidad, el valor, el saldo y el plazo, junto a la suma asegurada. */
@Composable
private fun DatosALaVista(cotizacion: Cotizacion) {
    val credito = cotizacion.datosCredito
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = stringResource(R.string.pagas_tu_credito),
            style = MaterialTheme.typography.titleSmall,
        )
        Linea(stringResource(R.string.pagas_entidad), credito.entidadAcreedora)
        Linea(stringResource(R.string.cotizacion_monto_total), enPesos(credito.valorCredito.toDouble()))
        Linea(stringResource(R.string.cotizacion_saldo_hoy), enPesos(credito.saldoInsoluto.toDouble()))
        Linea(
            stringResource(R.string.cotizacion_meses_faltan),
            stringResource(R.string.cotizacion_meses, credito.plazoRestanteMeses),
        )
        Linea(stringResource(R.string.pagas_suma_asegurada), enPesos(cotizacion.sumaAsegurada))
    }
}

@Composable
private fun Linea(nombre: String, valor: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(nombre, style = MaterialTheme.typography.bodyMedium)
        Text(valor, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

/** Los dos avisos de Figma: que significa el precio (azul) y que queda en espera (claro). */
@Composable
private fun Avisos(variante: VarianteDeCotizacion) {
    val (informativo, enEspera) = when (variante) {
        VarianteDeCotizacion.PREAPROBADA -> return
        VarianteDeCotizacion.DECLARADA_SIN_PERFIL ->
            (R.string.pagas_estimada_titulo to R.string.pagas_estimada_detalle) to
                (R.string.pagas_trae_credito_titulo to R.string.pagas_trae_credito_detalle)
        VarianteDeCotizacion.DECLARADA_CON_PERFIL ->
            (R.string.pagas_falta_confirmar_titulo to R.string.pagas_falta_confirmar_detalle) to
                (R.string.pagas_necesitamos_banco_titulo to R.string.pagas_necesitamos_banco_detalle)
        VarianteDeCotizacion.VERIFICADA_SIN_PERFIL ->
            (R.string.pagas_analizando_titulo to R.string.pagas_analizando_detalle) to
                (R.string.pagas_en_espera_titulo to R.string.pagas_en_espera_detalle)
    }
    Aviso(informativo, MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
    Aviso(enEspera, MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
}

@Composable
private fun Aviso(textos: Pair<Int, Int>, fondo: Color, letra: Color) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(fondo, MaterialTheme.shapes.medium)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(stringResource(textos.first), style = MaterialTheme.typography.titleSmall, color = letra)
        Text(stringResource(textos.second), style = MaterialTheme.typography.bodySmall, color = letra)
    }
}

@Composable
private fun BotonPrincipal(
    variante: VarianteDeCotizacion,
    alBuscarCredito: () -> Unit,
    alIrAMisSeguros: () -> Unit,
) {
    val (texto, accion) = when (variante) {
        VarianteDeCotizacion.DECLARADA_SIN_PERFIL,
        VarianteDeCotizacion.DECLARADA_CON_PERFIL,
        -> R.string.pagas_buscar_credito to alBuscarCredito
        VarianteDeCotizacion.VERIFICADA_SIN_PERFIL -> R.string.pagas_ir_a_mis_seguros to alIrAMisSeguros
        // Continuar lleva a elegir coberturas, que es otra historia (BITS-96): aun no hace nada.
        VarianteDeCotizacion.PREAPROBADA -> R.string.pagas_continuar to {}
    }
    Button(onClick = accion, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(texto))
    }
}

@Preview(showBackground = true)
@Composable
private fun PantallaLoQuePagasPreview() {
    TemaSolventa {
        PantallaLoQuePagas(
            cotizacion = Cotizacion(
                vigente = true,
                datosCredito = DatosDelCredito(
                    entidadAcreedora = "Bancolombia",
                    valorCredito = 250_000_000,
                    saldoInsoluto = 238_500_000,
                    plazoRestanteMeses = 180,
                ),
                datosDeclarados = true,
                primaMensual = 39_734.0,
                primaBaseMensual = 46_746.0,
                sumaAsegurada = 238_500_000.0,
                coberturaMeses = 180,
                personalizada = true,
                factores = emptyList(),
            ),
            alBuscarCredito = {},
            alIrAMisSeguros = {},
            alVolver = {},
            alCerrarSesion = {},
        )
    }
}
