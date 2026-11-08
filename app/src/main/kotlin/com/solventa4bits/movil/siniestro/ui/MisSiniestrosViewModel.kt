package com.solventa4bits.movil.siniestro.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solventa4bits.movil.siniestro.dominio.DetalleDeSiniestro
import com.solventa4bits.movil.siniestro.dominio.ResultadoDetalle
import com.solventa4bits.movil.siniestro.dominio.ResultadoSiniestros
import com.solventa4bits.movil.siniestro.dominio.ServicioDeSiniestros
import com.solventa4bits.movil.siniestro.dominio.SiniestroResumen
import kotlinx.coroutines.launch

data class EstadoMisSiniestros(
    val siniestros: List<SiniestroResumen> = emptyList(),
    /** La lista ya llego, o fallo: las entradas para reportar se pueden mostrar. */
    val listos: Boolean = false,
    val detalle: DetalleDeSiniestro? = null,
    val cargandoDetalle: Boolean = false,
    val falloDetalle: Boolean = false,
    val sesionVencida: Boolean = false,
)

/** Los siniestros del cliente en "Mi seguro" (M01) y el de "Tu siniestro" (M09V). */
class MisSiniestrosViewModel(private val servicio: ServicioDeSiniestros) : ViewModel() {

    var estado by mutableStateOf(EstadoMisSiniestros())
        private set

    // El detalle que se pidio la ultima vez, para reintentar.
    private var idDelDetalle: String? = null

    /**
     * Cada vez que se abre "Mi seguro": un siniestro pudo cambiar de estado. Empieza de cero, para no
     * mostrar lo de otra sesion mientras llega. Si falla, la lista queda vacia.
     */
    fun listar() {
        estado = EstadoMisSiniestros()
        viewModelScope.launch {
            estado = when (val resultado = servicio.listar()) {
                is ResultadoSiniestros.Exito -> estado.copy(siniestros = resultado.siniestros, listos = true)
                ResultadoSiniestros.SesionVencida -> estado.copy(sesionVencida = true)
                ResultadoSiniestros.Fallo -> estado.copy(siniestros = emptyList(), listos = true)
            }
        }
    }

    fun abrir(id: String) {
        idDelDetalle = id
        estado = estado.copy(detalle = null, cargandoDetalle = true, falloDetalle = false, sesionVencida = false)
        viewModelScope.launch {
            estado = when (val resultado = servicio.consultar(id)) {
                is ResultadoDetalle.Exito -> estado.copy(detalle = resultado.detalle, cargandoDetalle = false)
                ResultadoDetalle.SesionVencida -> estado.copy(cargandoDetalle = false, sesionVencida = true)
                ResultadoDetalle.NoExiste, ResultadoDetalle.Fallo ->
                    estado.copy(cargandoDetalle = false, falloDetalle = true)
            }
        }
    }

    fun reintentarDetalle() {
        idDelDetalle?.let(::abrir)
    }
}
