package com.solventa4bits.movil.siniestro.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.solventa4bits.movil.R

/**
 * M09D: lo que falta, antes de enviarlo (BITS-118, AC-4). Se pueden quitar archivos; al enviarlo
 * el siniestro vuelve a En estudio.
 */
@Composable
fun PantallaRevisaLoQueFalta(
    estado: EstadoReporte,
    acciones: AccionesDeRevision,
    alCerrarSesion: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val entrega = estado.entrega ?: return
    PasoDelReporte(
        titulo = stringResource(R.string.revisa_titulo),
        subtitulo = stringResource(R.string.falta_siniestro, entrega.numero),
        boton = stringResource(R.string.revisa_enviar),
        botonActivo = estado.listoParaEnviar && !estado.enviando,
        alBoton = acciones.alEnviar,
        alVolver = acciones.alVolver,
        alCerrarSesion = alCerrarSesion,
        modifier = modifier,
        barra = stringResource(R.string.tu_siniestro_titulo),
    ) {
        TituloDeSeccion(entrega.pedido.uppercase())
        if (estado.tipo.deTuCasa) ArchivosDeTuCasa(estado, acciones) else ArchivosDeVida(estado, acciones)
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Outlined.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(stringResource(R.string.falta_al_enviar), style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (estado.problemaAlEnviar != null) {
            Text(
                text = stringResource(R.string.revisa_fallo),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}
