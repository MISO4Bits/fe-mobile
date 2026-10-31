package com.solventa4bits.movil.inicio.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.solventa4bits.movil.R
import com.solventa4bits.movil.cotizacion.dominio.Cotizacion
import com.solventa4bits.movil.cotizacion.ui.enPesos
import com.solventa4bits.movil.solicitud.dominio.EstadoDeSolicitud
import com.solventa4bits.movil.solicitud.dominio.Solicitud
import com.solventa4bits.movil.solicitud.ui.PendienteDeLaSolicitud
import com.solventa4bits.movil.ui.BarraSolventa
import com.solventa4bits.movil.ui.tema.TemaSolventa

/**
 * Inicio del cliente: sin cotizaciones ofrece cotizar (Figma 02A); con una
 * cotizacion en curso la muestra para retomarla (02B, BITS-95 AC-6). Con
 * una solicitud enviada, ofrece volver a verla (BITS-97, AC-2); si esta en
 * revision, reemplaza a la cotizacion (02C).
 */
@Composable
fun PantallaInicio(
    cotizacionEnCurso: Cotizacion?,
    alCotizar: () -> Unit,
    alVerCotizacion: (Cotizacion) -> Unit,
    alCerrarSesion: () -> Unit,
    huellaActivada: Boolean? = null,
    alAlternarHuella: () -> Unit = {},
    solicitudReciente: Solicitud? = null,
    alVerSolicitud: (Solicitud) -> Unit = {},
    seguros: List<Solicitud> = emptyList(),
    alVerSeguro: (Solicitud) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val enRevision = solicitudReciente?.takeIf { it.estado == EstadoDeSolicitud.PENDIENTE }
    var avisando by remember { mutableStateOf(false) }
    if (avisando) {
        PanelNuevaCotizacion(
            alCotizar = {
                avisando = false
                alCotizar()
            },
            alVolver = { avisando = false },
        )
    }
    Column(modifier = modifier.fillMaxSize()) {
        BarraSolventa(stringResource(R.string.inicio_titulo), alCerrarSesion, huellaActivada, alAlternarHuella)
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column {
                Text(
                    text = stringResource(
                        when {
                            enRevision != null -> R.string.inicio_revision_titulo
                            cotizacionEnCurso != null -> R.string.inicio_en_curso
                            seguros.isNotEmpty() -> R.string.inicio_tus_seguros
                            else -> R.string.inicio_sin_seguros
                        },
                    ),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = stringResource(R.string.inicio_lema),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (enRevision != null) {
                ContenidoEnRevision(enRevision, { alVerSolicitud(enRevision) }, { avisando = true }) {
                    MisSeguros(seguros, alVerSeguro)
                }
            } else if (seguros.isNotEmpty()) {
                // Figma 10: la cotizacion va en "Tu cotizacion" y nunca en "Mis seguros" (BITS-104, AC-3).
                Seccion(stringResource(R.string.inicio_seccion_cotizacion))
                solicitudReciente?.let { TarjetaSolicitud(it) { alVerSolicitud(it) } }
                cotizacionEnCurso?.let { TarjetaCotizacionEnCurso(it) { alVerCotizacion(it) } }
                TarjetaNuevaCotizacion(alCotizar)
                Seccion(stringResource(R.string.inicio_seccion_seguros))
                MisSeguros(seguros, alVerSeguro)
            } else {
                solicitudReciente?.let { TarjetaSolicitud(it) { alVerSolicitud(it) } }
                if (cotizacionEnCurso == null) {
                    TarjetaCotizar(alCotizar)
                } else {
                    TarjetaCotizacionEnCurso(cotizacionEnCurso) { alVerCotizacion(cotizacionEnCurso) }
                }
                TarjetaNuevaCotizacion(alCotizar)
            }
        }
    }
}

/** Figma 02C: la solicitud en revision reemplaza a la cotizacion. */
@Composable
private fun ContenidoEnRevision(
    solicitud: Solicitud,
    alVerSolicitud: () -> Unit,
    alCotizar: () -> Unit,
    misSeguros: @Composable () -> Unit,
) {
    Seccion(stringResource(R.string.inicio_seccion_cotizacion))
    TarjetaEnRevision(solicitud, alVerSolicitud)
    Seccion(stringResource(R.string.inicio_seccion_seguros))
    misSeguros()
    TarjetaNuevaCotizacion(alCotizar)
}

@Composable
private fun Seccion(titulo: String) {
    Text(
        text = titulo,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun TarjetaEnRevision(solicitud: Solicitud, alVer: () -> Unit) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.inicio_vida_para_tu_credito),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.inverseSurface) {
                    Text(
                        text = stringResource(R.string.inicio_solicitud_en_revision),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }
            solicitud.primaMensual?.let {
                Text(
                    text = stringResource(R.string.inicio_al_mes, enPesos(it)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            solicitud.pendientes.forEach { PendienteDeLaSolicitud(it) }
            Spacer(Modifier.height(8.dp))
            Button(onClick = alVer) {
                Text(stringResource(R.string.inicio_ver_solicitud))
            }
        }
    }
}

@Composable
private fun TarjetaSolicitud(solicitud: Solicitud, alVer: () -> Unit) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.inicio_tu_solicitud),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(
                    when (solicitud.estado) {
                        EstadoDeSolicitud.EMITIDA -> R.string.inicio_solicitud_aprobada
                        EstadoDeSolicitud.RECHAZADA -> R.string.inicio_solicitud_no_aprobada
                        EstadoDeSolicitud.PENDIENTE, EstadoDeSolicitud.VENCIDA -> R.string.inicio_solicitud_en_revision
                    },
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Button(onClick = alVer) {
                Text(stringResource(R.string.inicio_ver_solicitud))
            }
        }
    }
}

@Composable
private fun TarjetaCotizar(alCotizar: () -> Unit) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.inicio_sin_seguros),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.inicio_cotiza_detalle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Button(onClick = alCotizar) {
                Text(stringResource(R.string.inicio_cotizar))
            }
        }
    }
}

@Composable
private fun TarjetaCotizacionEnCurso(cotizacion: Cotizacion, alVer: () -> Unit) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.inicio_cotizacion_lista),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.inicio_cotizacion_resumen, enPesos(cotizacion.primaMensual)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Button(onClick = alVer) {
                Text(stringResource(R.string.inicio_ver_cotizacion))
            }
        }
    }
}

@Composable
private fun TarjetaNuevaCotizacion(alCotizar: () -> Unit) {
    OutlinedCard(onClick = alCotizar, modifier = Modifier.width(168.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(
                Icons.Filled.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.inicio_nueva_cotizacion),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = stringResource(R.string.inicio_nueva_cotizacion_detalle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.inicio_empezar),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PantallaInicioPreview() {
    TemaSolventa { PantallaInicio(cotizacionEnCurso = null, alCotizar = {}, alVerCotizacion = {}, alCerrarSesion = {}) }
}
