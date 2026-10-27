package com.solventa4bits.movil.solicitud.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.solventa4bits.movil.R
import com.solventa4bits.movil.identidad.dominio.EstadoGuardado
import com.solventa4bits.movil.identidad.ui.colorDe
import com.solventa4bits.movil.identidad.ui.mensajeDe
import com.solventa4bits.movil.solicitud.dominio.CondicionesDelSeguro
import com.solventa4bits.movil.ui.BarraSolventa
import com.solventa4bits.movil.ui.tema.TemaSolventa

// La hoja de condiciones deja ver el texto completo sin tapar los botones.
private const val ALTO_MAXIMO_DEL_TEXTO = 360

/** Lo que el cliente puede hacer en "Revisa y envia tu solicitud". */
data class AccionesDeSolicitud(
    val alCambiarAceptacion: (Boolean) -> Unit,
    val alVerCondiciones: () -> Unit,
    val alCerrarCondiciones: () -> Unit,
    val alAceptarCondiciones: () -> Unit,
    val alEnviar: () -> Unit,
    val alReintentar: () -> Unit,
    val alIrAlInicio: () -> Unit,
)

/**
 * Figma 08 y 08D: los requisitos de la solicitud y su envio (BITS-278). El
 * cliente acepta las condiciones con la casilla o desde la hoja 08C.
 */
@Composable
fun PantallaSolicitud(
    estado: EstadoSolicitud,
    acciones: AccionesDeSolicitud,
    alCerrarSesion: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        BarraSolventa(stringResource(R.string.solicitud_titulo), alCerrarSesion)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column {
                Text(stringResource(R.string.solicitud_revisa), style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = stringResource(R.string.solicitud_revisa_detalle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            when {
                estado.cargando -> Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                estado.falloAlCargar -> FalloAlCargar(acciones.alReintentar)
                else -> Requisitos(estado.requisitos)
            }
        }
        PieDeEnvio(estado, acciones)
    }
    val condiciones = estado.condiciones
    if (estado.viendoCondiciones && condiciones != null) {
        HojaDeCondiciones(condiciones, acciones.alAceptarCondiciones, acciones.alCerrarCondiciones)
    }
}

@Composable
private fun FalloAlCargar(alReintentar: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.solicitud_fallo_carga),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
        TextButton(onClick = alReintentar, contentPadding = PaddingValues(0.dp)) {
            Text(stringResource(R.string.cotizacion_reintentar))
        }
    }
}

/** AC-1: frente, reverso y selfie, cada uno con su estado. */
@Composable
private fun Requisitos(requisitos: Map<Requisito, EstadoGuardado>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.large)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Requisito.entries.forEach { requisito ->
            val guardado = requisitos[requisito] ?: EstadoGuardado.PENDIENTE
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = colorDe(guardado))
                Column {
                    Text(stringResource(nombreDe(requisito)), style = MaterialTheme.typography.titleSmall)
                    Text(mensajeDe(guardado), style = MaterialTheme.typography.bodySmall, color = colorDe(guardado))
                }
            }
        }
    }
}

private fun nombreDe(requisito: Requisito): Int = when (requisito) {
    Requisito.FRENTE -> R.string.solicitud_frente
    Requisito.REVERSO -> R.string.solicitud_reverso
    Requisito.SELFIE -> R.string.solicitud_selfie
}

/** AC-2: la casilla, la autorizacion al banco y el envio. */
@Composable
private fun PieDeEnvio(estado: EstadoSolicitud, acciones: AccionesDeSolicitud) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Checkbox(
                checked = estado.aceptadas,
                onCheckedChange = acciones.alCambiarAceptacion,
                enabled = estado.condiciones != null,
            )
            Column(modifier = Modifier.padding(top = 12.dp)) {
                Text(stringResource(R.string.solicitud_acepto), style = MaterialTheme.typography.bodyMedium)
                TextButton(
                    onClick = acciones.alVerCondiciones,
                    enabled = estado.condiciones != null,
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Text(stringResource(R.string.solicitud_leer_condiciones))
                }
            }
        }
        Text(
            text = stringResource(R.string.solicitud_autorizas, estado.entidad),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        when (estado.errorAlEnviar) {
            ErrorAlEnviar.COTIZAR_DE_NUEVO -> {
                Aviso(stringResource(R.string.solicitud_cotizar_de_nuevo))
                Button(onClick = acciones.alIrAlInicio, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.solicitud_ir_al_inicio))
                }
            }
            ErrorAlEnviar.FALLO, null -> {
                if (estado.errorAlEnviar == ErrorAlEnviar.FALLO) Aviso(stringResource(R.string.solicitud_fallo_envio))
                Button(onClick = acciones.alEnviar, enabled = estado.puedeEnviar, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.solicitud_enviar))
                }
            }
        }
    }
}

@Composable
private fun Aviso(texto: String) {
    Text(texto, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
}

/** Figma 08C: las condiciones que manda el servidor, con su formato. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HojaDeCondiciones(condiciones: CondicionesDelSeguro, alAceptar: () -> Unit, alVolver: () -> Unit) {
    val contenido = remember(condiciones.contenido) { AnnotatedString.fromHtml(condiciones.contenido) }
    ModalBottomSheet(
        onDismissRequest = alVolver,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.condiciones_etiqueta),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(condiciones.titulo, style = MaterialTheme.typography.headlineSmall)
            Text(
                text = contenido,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.heightIn(max = ALTO_MAXIMO_DEL_TEXTO.dp).verticalScroll(rememberScrollState()),
            )
            condiciones.notaPie?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Button(onClick = alAceptar, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.condiciones_acepto))
                }
                TextButton(onClick = alVolver) {
                    Text(stringResource(R.string.condiciones_volver))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PantallaSolicitudPreview() {
    TemaSolventa {
        PantallaSolicitud(
            estado = EstadoSolicitud(
                cargando = false,
                requisitos = mapOf(
                    Requisito.FRENTE to EstadoGuardado.ACEPTADA,
                    Requisito.REVERSO to EstadoGuardado.ACEPTADA,
                    Requisito.SELFIE to EstadoGuardado.REVISION_MANUAL,
                ),
                condiciones = CondicionesDelSeguro("V1", "Condiciones de tu seguro", "<p>Texto</p>", null),
                entidad = "Bancolombia",
                aceptadas = true,
            ),
            acciones = AccionesDeSolicitud({}, {}, {}, {}, {}, {}, {}),
            alCerrarSesion = {},
        )
    }
}
