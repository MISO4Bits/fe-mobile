package com.solventa4bits.movil.inicio.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solventa4bits.movil.cotizacion.dominio.Cotizacion
import com.solventa4bits.movil.cotizacion.dominio.ResultadoCotizacion
import com.solventa4bits.movil.cotizacion.dominio.ServicioDeCotizaciones
import com.solventa4bits.movil.seguro.dominio.AlmacenDeSeguros
import com.solventa4bits.movil.solicitud.dominio.EstadoDeSolicitud
import com.solventa4bits.movil.solicitud.dominio.ResultadoLista
import com.solventa4bits.movil.solicitud.dominio.ServicioDeSolicitudes
import com.solventa4bits.movil.solicitud.dominio.Solicitud
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.time.Instant

data class EstadoInicio(
    /** La cotizacion en curso del cliente, para retomarla (BITS-95, AC-6). Null si no hay. */
    val cotizacionEnCurso: Cotizacion? = null,
    /**
     * La solicitud mas reciente, para volver a verla (BITS-97, AC-2). Null si
     * no hay, si vencio o si ya se emitio: emitida, va en [seguros].
     */
    val solicitudReciente: Solicitud? = null,
    /** Las polizas emitidas, para "Mis seguros" (BITS-104, AC-3). */
    val seguros: List<Solicitud> = emptyList(),
    val sesionVencida: Boolean = false,
    /** No se pudo llegar al servidor: [seguros] son los guardados en el telefono (BITS-217, AC-2). */
    val sinConexion: Boolean = false,
    /** Cuando se guardaron los [seguros] que se ven sin conexion, en ISO 8601. Null si llegaron del servidor. */
    val actualizadosEn: String? = null,
)

/** Decide que muestra el Home: ofrecer cotizar, la cotizacion en curso y la solicitud enviada. */
class InicioViewModel(
    private val cotizaciones: ServicioDeCotizaciones,
    private val solicitudes: ServicioDeSolicitudes,
    private val guardados: AlmacenDeSeguros,
    private val ahora: () -> Instant = Instant::now,
) : ViewModel() {

    var estado by mutableStateOf(EstadoInicio())
        private set

    /**
     * Consulta la cotizacion activa y las solicitudes cada vez que se muestra el
     * Home. Mientras llegan, y si fallan, el Home ofrece cotizar. Una cotizacion
     * vencida se trata como si no hubiera, y una solicitud vencida tambien: el
     * cliente cotiza de nuevo. Una cotizacion que ya va en una solicitud
     * pendiente o emitida tampoco se muestra.
     *
     * Las polizas emitidas que llegan se guardan en el telefono (BITS-217, AC-1
     * y AC-4). Si la lista no llega, se muestran las guardadas; si tampoco llega
     * la cotizacion, es que no hay conexion (AC-2).
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
                val enCurso = (resultado as? ResultadoCotizacion.Exito)?.cotizacion
                val lista = (enviadas as? ResultadoLista.Exito)?.solicitudes
                if (lista != null) {
                    guardados.guardar(lista.filter { it.estado == EstadoDeSolicitud.EMITIDA }, ahora().toString())
                    armar(enCurso, lista)
                } else {
                    conLoGuardado(enCurso, sinConexion = resultado == ResultadoCotizacion.Fallo)
                }
            }
        }
    }

    /** AC-6: al cerrar sesion no queda ninguna poliza en el telefono. */
    fun olvidarSeguros() {
        estado = EstadoInicio()
        viewModelScope.launch { guardados.borrar() }
    }

    private suspend fun conLoGuardado(enCurso: Cotizacion?, sinConexion: Boolean): EstadoInicio {
        val guardado = guardados.leer()
        return armar(enCurso, guardado?.seguros.orEmpty())
            .copy(sinConexion = sinConexion, actualizadosEn = guardado?.actualizadosEn)
    }

    private fun armar(cotizacion: Cotizacion?, enviadas: List<Solicitud>): EstadoInicio {
        val usadas = enviadas
            .filter { it.estado == EstadoDeSolicitud.PENDIENTE || it.estado == EstadoDeSolicitud.EMITIDA }
            .mapNotNull { it.cotizacionCreadaEn }
        return EstadoInicio(
            cotizacionEnCurso = cotizacion?.takeIf { it.vigente && it.creadaEn !in usadas },
            solicitudReciente = enviadas.firstOrNull()
                ?.takeIf { it.estado == EstadoDeSolicitud.PENDIENTE || it.estado == EstadoDeSolicitud.RECHAZADA },
            seguros = enviadas.filter { it.estado == EstadoDeSolicitud.EMITIDA },
        )
    }
}
