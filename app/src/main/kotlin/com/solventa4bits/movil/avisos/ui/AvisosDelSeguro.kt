package com.solventa4bits.movil.avisos.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.solventa4bits.movil.avisos.dominio.ControlDeAvisos
import com.solventa4bits.movil.avisos.dominio.DestinoDeAviso
import com.solventa4bits.movil.seguro.ui.SeguroViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Los avisos push (BITS-229): pide el permiso de notificaciones una vez, en cuanto hay sesion (AC-1),
 * y lleva a "Tu seguro" cuando el cliente toca un aviso de poliza emitida. Sin sesion, el destino
 * espera a que el cliente inicie sesion; si la poliza no se puede mostrar, queda en el Inicio.
 */
@Composable
fun AvisosDelSeguro(
    sesionIniciada: Boolean,
    avisos: ControlDeAvisos,
    destino: DestinoDeAviso?,
    seguro: SeguroViewModel,
    alAbrirSeguro: () -> Unit,
    alNoPoderMostrarlo: () -> Unit,
    alAtenderElDestino: () -> Unit,
    alcance: CoroutineScope,
) {
    val pedirPermiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        if (concedido) alcance.launch { avisos.alConcederPermiso() }
    }
    LaunchedEffect(sesionIniciada) {
        if (sesionIniciada && avisos.alEntrar()) pedirPermiso.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
    LaunchedEffect(destino, sesionIniciada) {
        if (destino != null && sesionIniciada) {
            seguro.abrir(destino.id)
            alAbrirSeguro()
            alAtenderElDestino()
        }
    }
    LaunchedEffect(seguro.estado.noDisponible) { if (seguro.estado.noDisponible) alNoPoderMostrarlo() }
}
