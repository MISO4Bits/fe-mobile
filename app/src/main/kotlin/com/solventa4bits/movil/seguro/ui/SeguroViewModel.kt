package com.solventa4bits.movil.seguro.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solventa4bits.movil.solicitud.dominio.EstadoDeSolicitud
import com.solventa4bits.movil.solicitud.dominio.ResultadoConsulta
import com.solventa4bits.movil.solicitud.dominio.ResultadoDocumento
import com.solventa4bits.movil.solicitud.dominio.ServicioDeSolicitudes
import com.solventa4bits.movil.solicitud.dominio.Solicitud
import kotlinx.coroutines.launch

/** Lo que se le dice al cliente al pedir el documento. */
enum class AvisoDeDocumento { NINGUNO, PIDIENDO, GENERANDO, FALLO, SIN_CONEXION }

data class EstadoSeguro(
    val seguro: Solicitud? = null,
    val aviso: AvisoDeDocumento = AvisoDeDocumento.NINGUNO,
    /** El enlace del documento, listo para abrirlo una vez. */
    val documentoPorAbrir: String? = null,
    val sesionVencida: Boolean = false,
    /** Sin conexion: cuando se guardo lo que se ve, en ISO 8601 (BITS-217, AC-2). Null con conexion. */
    val guardadoEn: String? = null,
    /** Se esta trayendo la poliza a la que lleva un aviso (BITS-229). */
    val cargando: Boolean = false,
    /** La poliza del aviso no se pudo mostrar: no existe, no esta emitida o no hay conexion. */
    val noDisponible: Boolean = false,
)

/** El detalle de una poliza emitida y su documento (BITS-104, AC-1). */
class SeguroViewModel(private val solicitudes: ServicioDeSolicitudes) : ViewModel() {

    var estado by mutableStateOf(EstadoSeguro())
        private set

    /** Lo muestra con lo que ya llego en la lista del Inicio, o con lo guardado si no hay conexion. */
    fun mostrar(seguro: Solicitud, guardadoEn: String? = null) {
        estado = EstadoSeguro(seguro = seguro, guardadoEn = guardadoEn)
    }

    /**
     * Abre la poliza a la que lleva un aviso: la trae del servidor, porque no viene de la lista del
     * Inicio. Solo se muestra si esta emitida; si no, [EstadoSeguro.noDisponible] y se vuelve al Inicio.
     */
    fun abrir(id: String) {
        estado = EstadoSeguro(cargando = true)
        viewModelScope.launch {
            estado = when (val resultado = solicitudes.consultar(id)) {
                is ResultadoConsulta.Exito ->
                    if (resultado.solicitud.estado == EstadoDeSolicitud.EMITIDA) {
                        EstadoSeguro(seguro = resultado.solicitud)
                    } else {
                        EstadoSeguro(noDisponible = true)
                    }
                ResultadoConsulta.SesionVencida -> EstadoSeguro(sesionVencida = true)
                ResultadoConsulta.NoExiste, ResultadoConsulta.Fallo -> EstadoSeguro(noDisponible = true)
            }
        }
    }

    fun verDocumento() {
        val seguro = estado.seguro ?: return
        if (estado.guardadoEn != null) {
            estado = estado.copy(aviso = AvisoDeDocumento.SIN_CONEXION)
            return
        }
        estado = estado.copy(aviso = AvisoDeDocumento.PIDIENDO)
        viewModelScope.launch {
            estado = when (val resultado = solicitudes.documento(seguro.id)) {
                is ResultadoDocumento.Listo ->
                    estado.copy(aviso = AvisoDeDocumento.NINGUNO, documentoPorAbrir = resultado.url)
                ResultadoDocumento.Generando -> estado.copy(aviso = AvisoDeDocumento.GENERANDO)
                ResultadoDocumento.SesionVencida -> estado.copy(aviso = AvisoDeDocumento.NINGUNO, sesionVencida = true)
                ResultadoDocumento.Fallo -> estado.copy(aviso = AvisoDeDocumento.FALLO)
            }
        }
    }

    fun documentoAbierto() {
        estado = estado.copy(documentoPorAbrir = null)
    }
}
