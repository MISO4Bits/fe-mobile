package com.solventa4bits.movil.inicio.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solventa4bits.movil.cotizacion.dominio.Cotizacion
import com.solventa4bits.movil.cotizacion.dominio.ResultadoCotizacion
import com.solventa4bits.movil.cotizacion.dominio.ServicioDeCotizaciones
import com.solventa4bits.movil.solicitud.dominio.EstadoDeSolicitud
import com.solventa4bits.movil.solicitud.dominio.ResultadoLista
import com.solventa4bits.movil.solicitud.dominio.ServicioDeSolicitudes
import com.solventa4bits.movil.solicitud.dominio.Solicitud
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

data class EstadoInicio(
    /** La cotizacion en curso del cliente, para retomarla (BITS-95, AC-6). Null si no hay. */
    val cotizacionEnCurso: Cotizacion? = null,
    /** La solicitud mas reciente, para volver a verla (BITS-97, AC-2). Null si no hay o si vencio. */
    val solicitudReciente: Solicitud? = null,
    val sesionVencida: Boolean = false,
)

/** Decide que muestra el Home: ofrecer cotizar, la cotizacion en curso y la solicitud enviada. */
class InicioViewModel(
    private val cotizaciones: ServicioDeCotizaciones,
    private val solicitudes: ServicioDeSolicitudes,
) : ViewModel() {

    var estado by mutableStateOf(EstadoInicio())
        private set

    /**
     * Consulta la cotizacion activa y las solicitudes cada vez que se muestra el
     * Home. Mientras llegan, y si fallan, el Home ofrece cotizar. Una cotizacion
     * vencida se trata como si no hubiera, y una solicitud vencida tambien: el
     * cliente cotiza de nuevo.
     */
    fun consultar() {
        estado = EstadoInicio()
        viewModelScope.launch {
            val cotizacion = async { cotizaciones.consultarActual() }
            val lista = async { solicitudes.listar() }
            val resultado = cotizacion.await()
            val enviadas = lista.await()
            estado = if (resultado == ResultadoCotizacion.SesionVencida || enviadas == ResultadoLista.SesionVencida) {
                EstadoInicio(sesionVencida = true)
            } else {
                EstadoInicio(
                    cotizacionEnCurso = (resultado as? ResultadoCotizacion.Exito)?.cotizacion?.takeIf { it.vigente },
                    solicitudReciente = (enviadas as? ResultadoLista.Exito)?.solicitudes?.firstOrNull()
                        ?.takeIf { it.estado != EstadoDeSolicitud.VENCIDA },
                )
            }
        }
    }
}
