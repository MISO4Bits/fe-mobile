package com.solventa4bits.movil.inicio.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solventa4bits.movil.cotizacion.dominio.Cotizacion
import com.solventa4bits.movil.cotizacion.dominio.ResultadoCotizacion
import com.solventa4bits.movil.cotizacion.dominio.ServicioDeCotizaciones
import kotlinx.coroutines.launch

data class EstadoInicio(
    /** La cotizacion en curso del cliente, para retomarla (BITS-95, AC-6). Null si no hay. */
    val cotizacionEnCurso: Cotizacion? = null,
    val sesionVencida: Boolean = false,
)

/** Decide que muestra el Home: ofrecer cotizar, o la cotizacion en curso. */
class InicioViewModel(
    private val cotizaciones: ServicioDeCotizaciones,
) : ViewModel() {

    var estado by mutableStateOf(EstadoInicio())
        private set

    /**
     * Consulta la cotizacion activa cada vez que se muestra el Home. Mientras
     * llega, y si falla, el Home ofrece cotizar. Una cotizacion vencida se trata
     * como si no hubiera: el cliente cotiza de nuevo.
     */
    fun consultar() {
        estado = EstadoInicio()
        viewModelScope.launch {
            estado = when (val resultado = cotizaciones.consultarActual()) {
                is ResultadoCotizacion.Exito ->
                    EstadoInicio(cotizacionEnCurso = resultado.cotizacion.takeIf { it.vigente })
                ResultadoCotizacion.SesionVencida -> EstadoInicio(sesionVencida = true)
                ResultadoCotizacion.SinCotizacion,
                ResultadoCotizacion.Fallo,
                is ResultadoCotizacion.DatosRechazados,
                -> EstadoInicio()
            }
        }
    }
}
