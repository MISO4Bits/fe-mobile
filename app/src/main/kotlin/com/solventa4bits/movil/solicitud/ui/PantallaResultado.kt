package com.solventa4bits.movil.solicitud.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.solventa4bits.movil.R
import com.solventa4bits.movil.solicitud.dominio.CodigoDePendiente
import com.solventa4bits.movil.solicitud.dominio.EstadoDeSolicitud
import com.solventa4bits.movil.solicitud.dominio.Pendiente
import com.solventa4bits.movil.solicitud.dominio.Solicitud
import com.solventa4bits.movil.ui.BarraSolventa
import com.solventa4bits.movil.ui.tema.TemaSolventa

// El tema no trae un ambar: es el de "en curso" en Figma 08.
private val Ambar = Color(0xFF8A5A00)

// En revision la barra va a la mitad: enviada, pero sin aprobar.
private const val AVANCE_EN_REVISION = 0.5f

/**
 * Figma 08, 08A y 09: el resultado de la solicitud segun su estado (BITS-97);
 * aprobada, con el valor del seguro dentro de la cuota (BITS-102).
 * Los pendientes y los motivos llegan del servidor listos para mostrar.
 */
@Composable
fun PantallaResultado(
    estado: EstadoResultado,
    alReintentar: () -> Unit,
    alInicio: () -> Unit,
    alCerrarSesion: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        BarraSolventa(stringResource(R.string.solicitud_titulo), alCerrarSesion)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            val solicitud = estado.solicitud
            when {
                estado.cargando -> CircularProgressIndicator(Modifier.padding(32.dp))
                estado.fallo -> {
                    Text(
                        text = stringResource(R.string.solicitud_fallo_carga),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 32.dp),
                    )
                    TextButton(onClick = alReintentar) { Text(stringResource(R.string.cotizacion_reintentar)) }
                }
                solicitud != null -> Resultado(solicitud)
            }
        }
        val alFinal = Modifier.align(Alignment.CenterHorizontally).padding(8.dp)
        if (estado.solicitud?.estado == EstadoDeSolicitud.EMITIDA) {
            Button(onClick = alInicio, modifier = alFinal) { Text(stringResource(R.string.resultado_ir_al_inicio)) }
        } else {
            TextButton(onClick = alInicio, modifier = alFinal) { Text(stringResource(R.string.resultado_inicio)) }
        }
    }
}

@Composable
private fun Resultado(solicitud: Solicitud) {
    Encabezado(solicitud)
    if (solicitud.estado == EstadoDeSolicitud.EMITIDA) {
        ResultadoAprobado(solicitud)
        return
    }
    if (solicitud.estado.enRevision) {
        LinearProgressIndicator(progress = { AVANCE_EN_REVISION }, modifier = Modifier.fillMaxWidth())
    }
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Paso(
            icono = { IconoListo() },
            titulo = stringResource(R.string.resultado_paso_enviada),
            detalle = fechaDeSolicitud(solicitud.creadaEn).orEmpty(),
        )
        PasoDeRevision(solicitud)
        PasoDeActivacion(solicitud.estado)
    }
    Text(
        text = stringResource(
            if (solicitud.estado == EstadoDeSolicitud.RECHAZADA) {
                R.string.resultado_nota_rechazada
            } else {
                R.string.resultado_nota_en_revision
            },
        ),
        style = MaterialTheme.typography.bodySmall,
        textAlign = TextAlign.Center,
    )
}

// Una solicitud vencida se ve como pendiente: el Inicio ya no la ofrece.
private val EstadoDeSolicitud.enRevision: Boolean
    get() = this == EstadoDeSolicitud.PENDIENTE || this == EstadoDeSolicitud.VENCIDA

/** "Revision de tu solicitud": no aprobada, o en curso con lo que falta. */
@Composable
private fun PasoDeRevision(solicitud: Solicitud) {
    if (solicitud.estado == EstadoDeSolicitud.RECHAZADA) {
        Paso(
            icono = { IconoRechazo(tamano = 22.dp) },
            titulo = stringResource(R.string.resultado_paso_revision),
            detalle = stringResource(R.string.resultado_no_aprobada),
            colorDelDetalle = MaterialTheme.colorScheme.error,
        )
    } else {
        Paso(
            icono = { Icon(painterResource(R.drawable.ic_reloj), contentDescription = null, tint = Ambar) },
            titulo = stringResource(R.string.resultado_paso_revision),
            detalle = stringResource(R.string.resultado_en_curso),
            colorDelDetalle = Ambar,
        ) {
            solicitud.pendientes.forEach { PendienteDeLaSolicitud(it) }
        }
    }
}

/** "Activamos tu seguro": no se emitio o pendiente. */
@Composable
private fun PasoDeActivacion(estado: EstadoDeSolicitud) {
    Paso(
        icono = { IconoVacio() },
        titulo = stringResource(R.string.resultado_paso_activamos),
        detalle = stringResource(
            if (estado == EstadoDeSolicitud.RECHAZADA) {
                R.string.resultado_no_se_emitio
            } else {
                R.string.resultado_pendiente
            },
        ),
    )
}

/** El icono grande, el titulo y la explicacion de cada estado. */
@Composable
private fun Encabezado(solicitud: Solicitud) {
    val (titulo, detalle) = when (solicitud.estado) {
        EstadoDeSolicitud.EMITIDA ->
            stringResource(R.string.resultado_aprobada_titulo) to stringResource(R.string.resultado_aprobada_detalle)
        EstadoDeSolicitud.RECHAZADA -> stringResource(R.string.resultado_rechazada_titulo) to
            stringResource(R.string.resultado_motivo, solicitud.motivosDeRechazo.joinToString(" "))
        EstadoDeSolicitud.PENDIENTE, EstadoDeSolicitud.VENCIDA ->
            stringResource(R.string.resultado_revision_titulo) to stringResource(R.string.resultado_revision_detalle)
    }
    val fondo = if (solicitud.estado == EstadoDeSolicitud.RECHAZADA) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.primaryContainer
    }
    Box(
        modifier = Modifier.padding(top = 24.dp).size(96.dp).background(fondo, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        when (solicitud.estado) {
            EstadoDeSolicitud.EMITIDA -> IconoListo(tamano = 44.dp)
            EstadoDeSolicitud.RECHAZADA -> IconoRechazo(tamano = 44.dp)
            EstadoDeSolicitud.PENDIENTE, EstadoDeSolicitud.VENCIDA -> Icon(
                painterResource(R.drawable.ic_reloj_de_arena),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(40.dp),
            )
        }
    }
    Text(titulo, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
    Text(
        text = detalle,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

@Preview(showBackground = true)
@Composable
private fun PantallaResultadoPreview() {
    TemaSolventa {
        PantallaResultado(
            estado = EstadoResultado(
                solicitud = Solicitud(
                    id = "poliza-1",
                    estado = EstadoDeSolicitud.PENDIENTE,
                    creadaEn = "2026-10-28T14:41:00Z",
                    emitidaEn = null,
                    pendientes = listOf(
                        Pendiente(CodigoDePendiente.CORREO_SIN_CONFIRMAR, "Revisa el enlace en tu correo"),
                        Pendiente(CodigoDePendiente.EVIDENCIA_EN_REVISION, "Un agente está revisando tus documentos"),
                    ),
                    motivosDeRechazo = emptyList(),
                ),
            ),
            alReintentar = {},
            alInicio = {},
            alCerrarSesion = {},
        )
    }
}
