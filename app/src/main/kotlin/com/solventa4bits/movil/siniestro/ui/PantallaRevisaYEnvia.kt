package com.solventa4bits.movil.siniestro.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import com.solventa4bits.movil.R
import com.solventa4bits.movil.siniestro.dominio.TipoDeSiniestro
import com.solventa4bits.movil.solicitud.ui.fechaCorta

/**
 * M07V y M07: lo que se va a enviar. En Vida el pago va al banco, como abono al credito; en Tu
 * casa, a la cuenta del cliente, y se ve lo que conto. Se pueden quitar archivos (AC-6); sin lo
 * obligatorio no se envia.
 */
@Composable
fun PantallaRevisaYEnvia(
    estado: EstadoReporte,
    nombreDeLaPoliza: String,
    acciones: AccionesDeRevision,
    alCerrarSesion: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val deVida = estado.tipo == TipoDeSiniestro.INCAPACIDAD_TOTAL
    PasoDelReporte(
        titulo = stringResource(R.string.revisa_titulo),
        subtitulo = stringResource(R.string.revisa_subtitulo),
        boton = stringResource(R.string.revisa_enviar),
        botonActivo = estado.listoParaEnviar && !estado.enviando,
        alBoton = acciones.alEnviar,
        alVolver = acciones.alVolver,
        alCerrarSesion = alCerrarSesion,
        modifier = modifier,
    ) {
        Datos {
            FilaDeDato(stringResource(R.string.revisa_poliza), nombreDeLaPoliza)
            FilaDeDato(stringResource(R.string.revisa_que_paso), nombreDelTipo(estado.tipo))
            FilaDeDato(stringResource(R.string.revisa_cuando), fechaCorta(estado.fecha))
            FilaDeDato(
                stringResource(R.string.revisa_pago),
                if (deVida) {
                    stringResource(R.string.revisa_pago_banco)
                } else {
                    stringResource(R.string.revisa_pago_transferencia, estado.pago.destino?.terminadaEn.orEmpty())
                },
            )
            if (!deVida) {
                HorizontalDivider()
                TextoTenue(stringResource(R.string.revisa_lo_que_contaste))
                Text(estado.narracion.trim(), style = MaterialTheme.typography.bodyMedium)
            }
        }
        TituloDeSeccion(stringResource(R.string.revisa_archivos))
        if (deVida) ArchivosDeVida(estado, acciones) else ArchivosDeTuCasa(estado, acciones)
        estado.problemaAlEnviar?.let { problema ->
            Text(
                text = stringResource(
                    when (problema) {
                        ProblemaAlEnviar.FALLO -> R.string.revisa_fallo
                        ProblemaAlEnviar.FUERA_DE_VIGENCIA -> R.string.revisa_fuera_de_vigencia
                        ProblemaAlEnviar.EN_CURSO -> R.string.revisa_en_curso
                    },
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
internal fun ArchivosDeVida(estado: EstadoReporte, acciones: AccionesDeRevision) {
    val lista = Icons.AutoMirrored.Outlined.List
    if (estado.dictamen.isNotEmpty()) {
        val nombre = pluralStringResource(R.plurals.dictamen_paginas, estado.dictamen.size, estado.dictamen.size)
        ArchivoParaEnviar(lista, nombre) { acciones.alQuitar(Escaneo.DICTAMEN) }
    }
    if (estado.historia.isNotEmpty()) {
        val nombre = pluralStringResource(R.plurals.historia_paginas, estado.historia.size, estado.historia.size)
        ArchivoParaEnviar(lista, nombre) { acciones.alQuitar(Escaneo.HISTORIA_CLINICA) }
    }
}

@Composable
internal fun ArchivosDeTuCasa(estado: EstadoReporte, acciones: AccionesDeRevision) {
    val foto = ImageVector.vectorResource(R.drawable.ic_foto)
    estado.fotos.forEachIndexed { i, archivo ->
        val nombre = stringResource(
            if (archivo.latitud != null) R.string.revisa_foto_con_ubicacion else R.string.revisa_foto_sin_ubicacion,
            i + 1,
        )
        ArchivoParaEnviar(foto, nombre) { acciones.alQuitarUno(Escaneo.FOTO_DEL_DANO, i) }
    }
    estado.documentos.forEachIndexed { i, _ ->
        val nombre = stringResource(R.string.documento_numero, i + 1)
        ArchivoParaEnviar(Icons.AutoMirrored.Outlined.List, nombre) { acciones.alQuitarUno(Escaneo.DOCUMENTO, i) }
    }
}

@Composable
private fun ArchivoParaEnviar(icono: ImageVector, nombre: String, alQuitar: () -> Unit) {
    Tarjeta {
        Icon(icono, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(nombre, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        IconButton(onClick = alQuitar) {
            Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.reporte_quitar, nombre))
        }
    }
}
