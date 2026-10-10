package com.solventa4bits.movil.cotizacion.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solventa4bits.movil.cotizacion.dominio.CreditoHipotecario
import com.solventa4bits.movil.cotizacion.dominio.EntidadFinanciera
import com.solventa4bits.movil.cotizacion.dominio.ResultadoCreditos
import com.solventa4bits.movil.cotizacion.dominio.ServicioDeCreditos
import kotlinx.coroutines.launch

/** Lo que la pantalla de nueva cotizacion necesita para dibujarse. */
data class EstadoNuevaCotizacion(
    val cargando: Boolean = true,
    /** No se pudo consultar: el cliente puede reintentar. */
    val fallo: Boolean = false,
    val sesionVencida: Boolean = false,
    val entidades: List<EntidadFinanciera> = emptyList(),
    val creditos: List<CreditoHipotecario> = emptyList(),
    val entidadElegida: EntidadFinanciera? = null,
) {
    /** La hipoteca del cliente en el banco que eligio, si su banco la reporto. */
    val creditoElegido: CreditoHipotecario?
        get() = entidadElegida?.let { elegida -> creditos.find { it.entidadId == elegida.id } }
}

/** Decide que pasa en la pantalla de nueva cotizacion. La pantalla solo dibuja. */
class NuevaCotizacionViewModel(
    private val servicio: ServicioDeCreditos,
) : ViewModel() {

    var estado by mutableStateOf(EstadoNuevaCotizacion())
        private set

    /**
     * Trae las hipotecas y los bancos en una sola llamada. Elegir banco
     * despues no vuelve a llamar al BFF.
     */
    fun consultar() {
        estado = EstadoNuevaCotizacion()
        viewModelScope.launch {
            estado = when (val resultado = servicio.consultar()) {
                is ResultadoCreditos.Exito -> EstadoNuevaCotizacion(
                    cargando = false,
                    entidades = resultado.creditos.entidades,
                    creditos = resultado.creditos.creditos,
                )
                ResultadoCreditos.SesionVencida -> EstadoNuevaCotizacion(cargando = false, sesionVencida = true)
                ResultadoCreditos.Fallo -> EstadoNuevaCotizacion(cargando = false, fallo = true)
            }
        }
    }

    fun elegirEntidad(entidad: EntidadFinanciera) {
        estado = estado.copy(entidadElegida = entidad)
    }
}
