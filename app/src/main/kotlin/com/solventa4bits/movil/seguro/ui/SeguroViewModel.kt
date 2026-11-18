package com.solventa4bits.movil.seguro.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solventa4bits.movil.inicio.ui.DE_MIS_SEGUROS
import com.solventa4bits.movil.solicitud.dominio.EstadoDeRenovacion
import com.solventa4bits.movil.solicitud.dominio.EstadoDeSolicitud
import com.solventa4bits.movil.solicitud.dominio.ResultadoConsulta
import com.solventa4bits.movil.solicitud.dominio.ResultadoDocumento
import com.solventa4bits.movil.solicitud.dominio.ResultadoRenovacion
import com.solventa4bits.movil.solicitud.dominio.ServicioDeSolicitudes
import com.solventa4bits.movil.solicitud.dominio.Solicitud
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.OffsetDateTime

/** Lo que se le dice al cliente al pedir el documento. */
enum class AvisoDeDocumento { NINGUNO, PIDIENDO, GENERANDO, FALLO, SIN_CONEXION }

/** El modal de la renovacion que esta abierto: la propuesta (M10) o la confirmacion de no renovar (M10A). */
enum class PasoDeRenovacion { PROPUESTA, NO_RENOVAR }

/** La etiqueta de la poliza en Mis seguros y en su detalle (BITS-237). */
enum class EtiquetaDelSeguro { VIGENTE, POR_RENOVAR, NO_SE_RENUEVA, DESACTIVADA }

fun etiquetaDe(seguro: Solicitud): EtiquetaDelSeguro = when {
    seguro.estado == EstadoDeSolicitud.DESACTIVADA -> EtiquetaDelSeguro.DESACTIVADA
    seguro.renovacion?.estado == EstadoDeRenovacion.PROPUESTA -> EtiquetaDelSeguro.POR_RENOVAR
    seguro.renovacion?.estado == EstadoDeRenovacion.RECHAZADA -> EtiquetaDelSeguro.NO_SE_RENUEVA
    else -> EtiquetaDelSeguro.VIGENTE
}

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
    /** Aun no pasa el dia limite para responder la renovacion (BITS-237). */
    val puedeDecidir: Boolean = false,
    val renovando: PasoDeRenovacion? = null,
    val respondiendo: Boolean = false,
    val falloAlResponder: Boolean = false,
)

/** El detalle de una poliza emitida, su documento (BITS-104, AC-1) y su renovacion (BITS-237). */
class SeguroViewModel(
    private val solicitudes: ServicioDeSolicitudes,
    private val ahora: () -> Instant = Instant::now,
) : ViewModel() {

    var estado by mutableStateOf(EstadoSeguro())
        private set

    /** Lo muestra con lo que ya llego en la lista del Inicio, o con lo guardado si no hay conexion. */
    fun mostrar(seguro: Solicitud, guardadoEn: String? = null) {
        estado = conLaPoliza(seguro, guardadoEn)
    }

    /**
     * Abre la poliza a la que lleva un aviso: la trae del servidor, porque no viene de la lista del
     * Inicio. Solo se muestra si esta emitida o desactivada; si no, [EstadoSeguro.noDisponible] y se vuelve
     * al Inicio.
     */
    fun abrir(id: String) {
        estado = EstadoSeguro(cargando = true)
        viewModelScope.launch {
            estado = when (val resultado = solicitudes.consultar(id)) {
                is ResultadoConsulta.Exito ->
                    if (resultado.solicitud.estado in DE_MIS_SEGUROS) {
                        conLaPoliza(resultado.solicitud, guardadoEn = null)
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

    /** La propuesta (M10): al abrir el detalle, con "Quiero renovarla" o al volver de M10A. */
    fun verPropuesta() {
        estado = estado.copy(renovando = PasoDeRenovacion.PROPUESTA, falloAlResponder = false)
    }

    /** AC-4: "No renovar" pide confirmar (M10A). */
    fun pedirNoRenovar() {
        estado = estado.copy(renovando = PasoDeRenovacion.NO_RENOVAR, falloAlResponder = false)
    }

    fun cerrarRenovacion() {
        estado = estado.copy(renovando = null, falloAlResponder = false)
    }

    /**
     * Acepta o rechaza la renovacion y trae la poliza de nuevo para verla como quedo. Si ya no se
     * podia decidir (409), tambien se trae de nuevo: muestra lo que el servidor tiene.
     */
    fun responder(aceptar: Boolean) {
        val seguro = estado.seguro ?: return
        estado = estado.copy(respondiendo = true, falloAlResponder = false)
        viewModelScope.launch {
            when (solicitudes.responderRenovacion(seguro.id, aceptar)) {
                ResultadoRenovacion.Respondida, ResultadoRenovacion.YaNoSePuede -> {
                    val actual = (solicitudes.consultar(seguro.id) as? ResultadoConsulta.Exito)?.solicitud
                    estado = conLaPoliza(actual ?: seguro, guardadoEn = null).copy(renovando = null)
                }
                ResultadoRenovacion.SesionVencida -> estado = estado.copy(respondiendo = false, sesionVencida = true)
                ResultadoRenovacion.Fallo -> estado = estado.copy(respondiendo = false, falloAlResponder = true)
            }
        }
    }

    /** AC-2: con la propuesta sin responder, el modal se abre cada vez que se abre el detalle. */
    private fun conLaPoliza(seguro: Solicitud, guardadoEn: String?): EstadoSeguro {
        val limite = seguro.renovacion?.decidirHasta?.let { runCatching { OffsetDateTime.parse(it) }.getOrNull() }
        val puedeDecidir = guardadoEn == null && limite != null && !ahora().isAfter(limite.toInstant())
        val abierta = puedeDecidir && seguro.renovacion?.estado == EstadoDeRenovacion.PROPUESTA
        return EstadoSeguro(
            seguro = seguro,
            guardadoEn = guardadoEn,
            puedeDecidir = puedeDecidir,
            renovando = PasoDeRenovacion.PROPUESTA.takeIf { abierta },
        )
    }
}
