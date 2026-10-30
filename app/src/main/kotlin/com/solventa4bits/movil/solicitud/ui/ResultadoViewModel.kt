package com.solventa4bits.movil.solicitud.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solventa4bits.movil.solicitud.dominio.ResultadoConsulta
import com.solventa4bits.movil.solicitud.dominio.ServicioDeSolicitudes
import com.solventa4bits.movil.solicitud.dominio.Solicitud
import kotlinx.coroutines.launch

data class EstadoResultado(
    val cargando: Boolean = false,
    /** La solicitud a mostrar: en revision, no aprobada o aprobada (Figma 08, 08A y 08B). */
    val solicitud: Solicitud? = null,
    /** No se pudo consultar: el cliente puede reintentar. */
    val fallo: Boolean = false,
    /** El servidor ya no la tiene: el cliente vuelve al Inicio. */
    val noExiste: Boolean = false,
    val sesionVencida: Boolean = false,
)

/** Decide que muestra el resultado de la solicitud (BITS-97). La pantalla solo dibuja. */
class ResultadoViewModel(
    private val solicitudes: ServicioDeSolicitudes,
) : ViewModel() {

    var estado by mutableStateOf(EstadoResultado())
        private set

    private var id: String? = null

    /** Justo despues de enviarla: lo que respondio el envio, sin volver a llamar al servidor. */
    fun mostrar(solicitud: Solicitud) {
        id = solicitud.id
        estado = EstadoResultado(solicitud = solicitud)
    }

    /**
     * Desde el Inicio: la consulta de nuevo. Si estaba pendiente y ya se cumplio
     * lo ultimo, el servidor la responde emitida (AC-3).
     */
    fun abrir(id: String) {
        this.id = id
        estado = EstadoResultado()
        consultar()
    }

    fun reintentar() {
        consultar()
    }

    private fun consultar() {
        val actual = id ?: return
        estado = estado.copy(cargando = true, fallo = false)
        viewModelScope.launch {
            estado = when (val resultado = solicitudes.consultar(actual)) {
                is ResultadoConsulta.Exito -> EstadoResultado(solicitud = resultado.solicitud)
                ResultadoConsulta.NoExiste -> estado.copy(cargando = false, noExiste = true)
                ResultadoConsulta.SesionVencida -> estado.copy(cargando = false, sesionVencida = true)
                ResultadoConsulta.Fallo -> estado.copy(cargando = false, fallo = true)
            }
        }
    }
}
