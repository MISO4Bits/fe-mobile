package com.solventa4bits.movil.red

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Antes de una accion que necesita el servidor revisa que haya red (BITS-217,
 * AC-5). Sin red, la accion queda [pendiente] y se ve "Sin conexion" hasta
 * reintentar con red o volver al Inicio. Lo que el cliente llevaba no se toca.
 */
class GuardiaDeRed(private val conexion: Conexion) {

    var pendiente by mutableStateOf<(() -> Unit)?>(null)
        private set

    fun conRed(accion: () -> Unit) {
        if (conexion.hay()) accion() else pendiente = accion
    }

    /** Con red, sigue con la accion que quedo pendiente; sin red, no pasa nada. */
    fun reintentar() {
        val accion = pendiente ?: return
        if (conexion.hay()) {
            pendiente = null
            accion()
        }
    }

    fun descartar() {
        pendiente = null
    }
}
