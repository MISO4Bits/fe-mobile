package com.solventa4bits.movil.seguro.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.solventa4bits.movil.R
import com.solventa4bits.movil.cotizacion.dominio.CodigoCobertura
import com.solventa4bits.movil.cotizacion.ui.enPesos
import com.solventa4bits.movil.solicitud.dominio.CreditoAsegurado
import com.solventa4bits.movil.solicitud.dominio.EstadoDeSolicitud
import com.solventa4bits.movil.solicitud.dominio.Solicitud
import com.solventa4bits.movil.solicitud.dominio.Vigencia
import com.solventa4bits.movil.solicitud.ui.fechaCorta
import com.solventa4bits.movil.ui.BarraSolventa
import com.solventa4bits.movil.ui.tema.TemaSolventa

/**
 * El detalle de una poliza emitida. El documento se abre con [abrirEnlace], o
 * en el navegador si es null.
 */
@Composable
fun TuSeguro(
    seguro: SeguroViewModel,
    abrirEnlace: ((String) -> Unit)?,
    alInicio: () -> Unit,
    alCerrarSesion: () -> Unit,
    modifier: Modifier,
) {
    val enlaces = LocalUriHandler.current
    val abrir = abrirEnlace ?: { url -> enlaces.openUri(url) }
    val estado = seguro.estado
    LaunchedEffect(estado.sesionVencida) { if (estado.sesionVencida) alCerrarSesion() }
    LaunchedEffect(estado.documentoPorAbrir) {
        estado.documentoPorAbrir?.let {
            abrir(it)
            seguro.documentoAbierto()
        }
    }
    BackHandler(onBack = alInicio)
    PantallaSeguro(
        estado = estado,
        alVerDocumento = seguro::verDocumento,
        alInicio = alInicio,
        alCerrarSesion = alCerrarSesion,
        modifier = modifier,
    )
}

/** Figma 11: el detalle de una poliza emitida y su documento (BITS-104, AC-1). */
@Composable
fun PantallaSeguro(
    estado: EstadoSeguro,
    alVerDocumento: () -> Unit,
    alInicio: () -> Unit,
    alCerrarSesion: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val seguro = estado.seguro ?: return
    Column(modifier = modifier.fillMaxSize()) {
        BarraSolventa(stringResource(R.string.seguro_titulo), alCerrarSesion)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            estado.guardadoEn?.let {
                AvisoSinConexion()
                ActualizadaEl(it)
            }
            Column {
                Text(nombreDelSeguro(seguro), style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = stringResource(R.string.seguro_numero, seguro.numero.orEmpty()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Recuadro(stringResource(R.string.seguro_condiciones)) {
                Fila(stringResource(R.string.seguro_pagas), seguro.primaMensual?.let(::enPesos).orEmpty())
                HorizontalDivider()
                Fila(stringResource(R.string.seguro_te_cubre), seguro.vigencia?.let { textoDeVigencia(it) }.orEmpty())
            }
            seguro.credito?.let { credito ->
                Recuadro(stringResource(R.string.seguro_tu_credito)) {
                    Fila(stringResource(R.string.seguro_banco), credito.banco)
                    HorizontalDivider()
                    Fila(stringResource(R.string.seguro_debes), enPesos(credito.saldo))
                    HorizontalDivider()
                    Fila(
                        stringResource(R.string.seguro_plazo),
                        stringResource(R.string.seguro_meses, credito.plazoMeses),
                    )
                }
            }
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.seguro_nota),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(16.dp),
                )
            }
            AvisoDelDocumento(estado.aviso)
        }
        Column(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Button(onClick = alVerDocumento, enabled = estado.aviso != AvisoDeDocumento.PIDIENDO) {
                Text(stringResource(R.string.seguro_ver_documento))
            }
            TextButton(onClick = alInicio) { Text(stringResource(R.string.resultado_inicio)) }
        }
    }
}

/** "Vida y tu casa" si tomo "Tu casa"; si no, "Vida para tu credito". */
@Composable
fun nombreDelSeguro(seguro: Solicitud): String =
    stringResource(if (seguro.conTuCasa) R.string.seguro_vida_y_casa else R.string.inicio_vida_para_tu_credito)

@Composable
private fun textoDeVigencia(vigencia: Vigencia): String =
    stringResource(R.string.seguro_vigencia, fechaCorta(vigencia.desde).orEmpty(), fechaCorta(vigencia.hasta).orEmpty())

@Composable
private fun AvisoDelDocumento(aviso: AvisoDeDocumento) {
    val texto = when (aviso) {
        AvisoDeDocumento.GENERANDO -> R.string.seguro_generando
        AvisoDeDocumento.FALLO -> R.string.seguro_fallo_documento
        AvisoDeDocumento.SIN_CONEXION -> R.string.seguro_documento_sin_conexion
        AvisoDeDocumento.NINGUNO, AvisoDeDocumento.PIDIENDO -> return
    }
    Text(
        text = stringResource(texto),
        style = MaterialTheme.typography.bodyMedium,
        color = if (aviso == AvisoDeDocumento.FALLO) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
    )
}

@Composable
private fun Recuadro(titulo: String, contenido: @Composable () -> Unit) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(titulo, style = MaterialTheme.typography.titleSmall)
            contenido()
        }
    }
}

@Composable
private fun Fila(etiqueta: String, valor: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = etiqueta,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(valor, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

private const val PRIMA_VIDA_DE_EJEMPLO = 49_100.0
private const val PRIMA_CASA_DE_EJEMPLO = 28_274.0

@Preview(showBackground = true)
@Composable
private fun PantallaSeguroPreview() {
    TemaSolventa {
        PantallaSeguro(
            estado = EstadoSeguro(
                seguro = Solicitud(
                    id = "poliza-1",
                    estado = EstadoDeSolicitud.EMITIDA,
                    creadaEn = "2026-10-22T14:41:00Z",
                    emitidaEn = "2026-10-22T15:00:00Z",
                    pendientes = emptyList(),
                    motivosDeRechazo = emptyList(),
                    numero = "SOL-VH-2026-K7M2Q9",
                    vigencia = Vigencia("2026-10-22T15:00:00Z", "2027-10-22T15:00:00Z"),
                    primaMensual = 77_374.0,
                    credito = CreditoAsegurado(banco = "Bancolombia S.A.", saldo = 320_000_000.0, plazoMeses = 180),
                    primasPorCobertura = mapOf(
                        CodigoCobertura.VIDA to PRIMA_VIDA_DE_EJEMPLO,
                        CodigoCobertura.HOGAR to PRIMA_CASA_DE_EJEMPLO,
                    ),
                ),
            ),
            alVerDocumento = {},
            alInicio = {},
            alCerrarSesion = {},
        )
    }
}
