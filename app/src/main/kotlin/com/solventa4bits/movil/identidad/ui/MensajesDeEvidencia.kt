package com.solventa4bits.movil.identidad.ui

import androidx.annotation.StringRes
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.solventa4bits.movil.R
import com.solventa4bits.movil.identidad.dominio.MotivoDeRechazo

// El tema no trae un ambar: es el de "revision manual" en Figma 03D y 08.
private val Ambar = Color(0xFF8A5A00)

/** Lo que dice una foto (cara de la cedula o selfie) segun su estado (BITS-275 y BITS-235). */
@Composable
internal fun mensajeDe(estado: EstadoDeCara): String = when (estado) {
    EstadoDeCara.SinFoto -> stringResource(R.string.cedula_sin_foto)
    EstadoDeCara.Revisando -> stringResource(R.string.cedula_revisando)
    EstadoDeCara.Aceptada -> stringResource(R.string.cedula_se_ve_bien)
    EstadoDeCara.RevisionManual -> stringResource(R.string.cedula_revision_manual)
    EstadoDeCara.NoSeEnvio -> stringResource(R.string.cedula_no_se_envio)
    is EstadoDeCara.Reintentar -> {
        val quedan = estado.intentosRestantes
        val intentos = pluralStringResource(R.plurals.cedula_intentos, quedan, quedan)
        estado.motivo?.let { stringResource(R.string.cedula_reintentar_por, stringResource(textoDe(it)), intentos) }
            ?: stringResource(R.string.cedula_reintentar, intentos)
    }
}

/** El color del mensaje: verde si sirve, ambar en revision, rojo si hay que repetir. */
@Composable
internal fun colorDe(estado: EstadoDeCara): Color = when (estado) {
    EstadoDeCara.Aceptada -> MaterialTheme.colorScheme.primary
    EstadoDeCara.RevisionManual -> Ambar
    is EstadoDeCara.Reintentar, EstadoDeCara.NoSeEnvio -> MaterialTheme.colorScheme.error
    EstadoDeCara.SinFoto, EstadoDeCara.Revisando -> MaterialTheme.colorScheme.onSurfaceVariant
}

@StringRes
private fun textoDe(motivo: MotivoDeRechazo): Int = when (motivo) {
    MotivoDeRechazo.BORROSA -> R.string.cedula_motivo_borrosa
    MotivoDeRechazo.OSCURA -> R.string.cedula_motivo_oscura
    MotivoDeRechazo.REFLEJO -> R.string.cedula_motivo_reflejo
    MotivoDeRechazo.RECORTADA -> R.string.cedula_motivo_recortada
    MotivoDeRechazo.NO_ES_CEDULA -> R.string.cedula_motivo_no_es_cedula
    MotivoDeRechazo.SIN_ROSTRO -> R.string.selfie_motivo_sin_rostro
    MotivoDeRechazo.VARIOS_ROSTROS -> R.string.selfie_motivo_varios_rostros
    MotivoDeRechazo.POSIBLE_SUPLANTACION -> R.string.selfie_motivo_posible_suplantacion
    MotivoDeRechazo.NO_COINCIDE_CON_CEDULA -> R.string.selfie_motivo_no_coincide
}
