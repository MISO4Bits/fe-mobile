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
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.solventa4bits.movil.R
import com.solventa4bits.movil.cotizacion.dominio.Cotizacion
import com.solventa4bits.movil.cotizacion.dominio.DatosDelCredito
import com.solventa4bits.movil.cotizacion.dominio.PlanElegido
import com.solventa4bits.movil.cotizacion.dominio.plan
import com.solventa4bits.movil.ui.BarraSolventa
import com.solventa4bits.movil.ui.tema.TemaSolventa

/**
 * "Lo que pagas": la cotizacion tal como la calculo el backend, con los datos
 * del credito a la vista (BITS-95, AC-2) y, segun la [variante], el aviso que
 * explica el precio. Toda variante acepta y continua (BITS-333, AC-1).
 */
@Composable
fun PantallaLoQuePagas(
    cotizacion: Cotizacion,
    sinAutorizacion: Boolean,
    conTuCasa: Boolean,
    alAceptar: () -> Unit,
    alVolver: () -> Unit,
    alCerrarSesion: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val variante = cotizacion.variante(sinAutorizacion)
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
            val plan = cotizacion.plan(conTuCasa)
            PrecioMensual(cotizacion, plan.total)
            // AC-3 y AC-4: la explicacion solo existe si el precio aplica el perfil.
            if (cotizacion.personalizada) {
                TextButton(onClick = { explicandoElPrecio = true }) {
                    Text(stringResource(R.string.pagas_por_que))
                }
            }
            Detalle(plan)
            DatosALaVista(cotizacion)
            Aviso(variante)
        }
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Button(onClick = alAceptar, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.pagas_aceptar_y_continuar))
            }
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
    VarianteDeCotizacion.SIN_AUTORIZACION,
    -> R.string.pagas_subtitulo_declarados
}

@Composable
private fun PrecioMensual(cotizacion: Cotizacion, total: Double) {
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
                text = enPesos(total),
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

/**
 * BITS-96, AC-3: la vida, "Tu casa", el IVA y el total por separado, cada uno
 * "/mes" (Figma 05 y 05D). La vida no lleva IVA.
 */
@Composable
private fun Detalle(plan: PlanElegido) {
    val plantilla = stringResource(R.string.pagas_valor_mes)
    val porMes = { valor: Double -> plantilla.format(enPesos(valor)) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = stringResource(R.string.pagas_detalle),
            style = MaterialTheme.typography.titleSmall,
        )
        Linea(stringResource(R.string.coberturas_vida_titulo), porMes(plan.vida))
        plan.tuCasa?.let { Linea(stringResource(R.string.coberturas_casa_titulo), porMes(it)) }
        Linea(stringResource(R.string.pagas_subtotal), porMes(plan.subtotal))
        Linea(stringResource(R.string.pagas_iva), porMes(plan.iva))
        HorizontalDivider()
        Linea(stringResource(R.string.pagas_total), porMes(plan.total), resaltada = true)
    }
}

@Composable
private fun Linea(nombre: String, valor: String, resaltada: Boolean = false) {
    val color = if (resaltada) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            text = nombre,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (resaltada) FontWeight.SemiBold else null,
        )
        Text(valor, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = color)
    }
}

/** El aviso azul de Figma: que significa el precio. La preaprobada no lleva aviso. */
@Composable
private fun Aviso(variante: VarianteDeCotizacion) {
    val (titulo, detalle) = when (variante) {
        VarianteDeCotizacion.PREAPROBADA -> return
        VarianteDeCotizacion.DECLARADA_SIN_PERFIL ->
            R.string.pagas_estimada_titulo to R.string.pagas_estimada_detalle
        VarianteDeCotizacion.DECLARADA_CON_PERFIL ->
            R.string.pagas_falta_confirmar_titulo to R.string.pagas_falta_confirmar_detalle
        VarianteDeCotizacion.VERIFICADA_SIN_PERFIL ->
            R.string.pagas_analizando_titulo to R.string.pagas_analizando_detalle
        VarianteDeCotizacion.SIN_AUTORIZACION ->
            R.string.pagas_tarifa_estandar_titulo to R.string.pagas_tarifa_estandar_detalle
    }
    val letra = MaterialTheme.colorScheme.onTertiaryContainer
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.shapes.medium)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(stringResource(titulo), style = MaterialTheme.typography.titleSmall, color = letra)
        Text(stringResource(detalle), style = MaterialTheme.typography.bodySmall, color = letra)
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
            sinAutorizacion = false,
            conTuCasa = false,
            alAceptar = {},
            alVolver = {},
            alCerrarSesion = {},
        )
    }
}
