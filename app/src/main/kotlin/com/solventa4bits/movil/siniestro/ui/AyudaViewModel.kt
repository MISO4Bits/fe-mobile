package com.solventa4bits.movil.siniestro.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solventa4bits.movil.siniestro.dominio.Asistencia
import com.solventa4bits.movil.siniestro.dominio.CatalogoDeSiniestros
import com.solventa4bits.movil.siniestro.dominio.DetalleDeSiniestro
import com.solventa4bits.movil.siniestro.dominio.FuenteDeUbicacion
import com.solventa4bits.movil.siniestro.dominio.Localidad
import com.solventa4bits.movil.siniestro.dominio.Prestador
import com.solventa4bits.movil.siniestro.dominio.PrestadorCercano
import com.solventa4bits.movil.siniestro.dominio.PuntoDeAyuda
import com.solventa4bits.movil.siniestro.dominio.ResultadoAyuda
import com.solventa4bits.movil.siniestro.dominio.ResultadoCatalogo
import com.solventa4bits.movil.siniestro.dominio.ServicioDeSiniestros
import com.solventa4bits.movil.siniestro.dominio.SolicitudDeAyuda
import com.solventa4bits.movil.siniestro.dominio.TipoDeAyuda
import kotlinx.coroutines.launch
import java.util.UUID

/** La direccion que escribe el cliente cuando no esta en la casa o el telefono no dio su ubicacion (AC-4). */
data class DireccionEscrita(val direccion: String = "", val localidad: Localidad? = null) {
    val completa: Boolean
        get() = direccion.isNotBlank() && localidad != null
}

data class EstadoAyuda(
    val siniestro: DetalleDeSiniestro? = null,
    val catalogo: CatalogoDeSiniestros = CatalogoDeSiniestros(),
    val cargando: Boolean = false,
    val fallo: Boolean = false,
    val sesionVencida: Boolean = false,
    val tipo: TipoDeAyuda? = null,
    /** A donde va la ayuda. Al empezar, donde se tomaron las fotos si alguna tiene ubicacion (AC-3). */
    val punto: PuntoDeAyuda? = null,
    val escrita: DireccionEscrita = DireccionEscrita(),
    val buscandoUbicacion: Boolean = false,
    /** El telefono no dio su ubicacion: hay que escribir la direccion (AC-4). */
    val sinUbicacion: Boolean = false,
    val pidiendo: Boolean = false,
    val falloAlPedir: Boolean = false,
    val noSePuede: Boolean = false,
    val pedida: Asistencia? = null,
) {
    /** AC-5: los prestadores del tipo elegido, del mas cercano al mas lejano. */
    val prestadores: List<PrestadorCercano>
        get() {
            val tipo = tipo
            val punto = punto
            if (tipo == null || punto == null) return emptyList()
            return catalogo.prestadoresCerca(tipo.id, punto.ubicacion).take(PRESTADORES_A_MOSTRAR)
        }

    /** El siniestro con la ayuda que se acaba de pedir, para verla en "Tu siniestro" (AC-7). */
    val siniestroConLaAyuda: DetalleDeSiniestro?
        get() = pedida?.let { nueva -> siniestro?.let { it.copy(asistencias = it.asistencias + nueva) } } ?: siniestro

    private companion object {
        const val PRESTADORES_A_MOSTRAR = 5
    }
}

/**
 * Pedir ayuda en sitio para la casa danada (BITS-239): el tipo (M13), a donde va (M13A), quien la
 * presta (M14) y la ayuda pedida (M15). La llave se repite mientras se reintenta lo mismo, para que
 * el servidor guarde una sola (AC-6).
 */
class AyudaViewModel(
    private val servicio: ServicioDeSiniestros,
    private val ubicacion: FuenteDeUbicacion,
    private val llaveNueva: () -> String = { UUID.randomUUID().toString() },
) : ViewModel() {

    var estado by mutableStateOf(EstadoAyuda())
        private set

    private var llave = ""
    private var ultimaSolicitud: SolicitudDeAyuda? = null

    /** El catalogo se trae una sola vez: no cambia mientras la app esta abierta. */
    fun empezar(siniestro: DetalleDeSiniestro) {
        ultimaSolicitud = null
        estado = EstadoAyuda(
            siniestro = siniestro,
            catalogo = estado.catalogo,
            punto = siniestro.ubicacionDeLasFotos?.let { PuntoDeAyuda.DondeTomoLasFotos(it) },
        )
        if (estado.catalogo.tiposAyuda.isEmpty()) cargar()
    }

    fun cargar() {
        estado = estado.copy(cargando = true, fallo = false)
        viewModelScope.launch {
            estado = when (val resultado = servicio.catalogo()) {
                is ResultadoCatalogo.Exito -> estado.copy(catalogo = resultado.catalogo, cargando = false)
                ResultadoCatalogo.SesionVencida -> estado.copy(cargando = false, sesionVencida = true)
                ResultadoCatalogo.Fallo -> estado.copy(cargando = false, fallo = true)
            }
        }
    }

    fun elegirTipo(tipo: TipoDeAyuda) {
        estado = estado.copy(tipo = tipo)
    }

    /** "Estoy en mi casa": la ayuda va a donde esta el telefono. Si no la da, se escribe la direccion. */
    fun usarUbicacionDelTelefono(alUbicar: () -> Unit) {
        estado = estado.copy(buscandoUbicacion = true, sinUbicacion = false)
        viewModelScope.launch {
            val donde = ubicacion.actual()
            estado = if (donde == null) {
                estado.copy(buscandoUbicacion = false, sinUbicacion = true)
            } else {
                estado.copy(buscandoUbicacion = false, punto = PuntoDeAyuda.DondeEstaElTelefono(donde))
            }
            if (donde != null) alUbicar()
        }
    }

    fun cambiarDireccion(escrita: DireccionEscrita) {
        estado = estado.copy(escrita = escrita.copy(direccion = escrita.direccion.take(LARGO_DE_LA_DIRECCION)))
    }

    fun usarDireccion() {
        val escrita = estado.escrita
        val localidad = escrita.localidad ?: return
        if (!escrita.completa) return
        estado = estado.copy(punto = PuntoDeAyuda.EnUnaDireccion(escrita.direccion.trim(), localidad))
    }

    fun pedir(prestador: Prestador, alPedir: () -> Unit) {
        val siniestro = estado.siniestro
        val tipo = estado.tipo
        val punto = estado.punto
        if (siniestro == null || tipo == null || punto == null) return
        if (estado.pidiendo) return
        val solicitud = SolicitudDeAyuda(tipo, prestador, punto)
        // Otra solicitud es otra ayuda: lleva otra llave.
        if (solicitud != ultimaSolicitud) llave = llaveNueva()
        ultimaSolicitud = solicitud
        estado = estado.copy(pidiendo = true, falloAlPedir = false)
        viewModelScope.launch {
            val resultado = servicio.pedirAyuda(siniestro.resumen.id, solicitud, llave)
            estado = when (resultado) {
                is ResultadoAyuda.Pedida -> estado.copy(pidiendo = false, pedida = resultado.asistencia)
                ResultadoAyuda.NoSePuede -> estado.copy(pidiendo = false, noSePuede = true)
                ResultadoAyuda.SesionVencida -> estado.copy(pidiendo = false, sesionVencida = true)
                ResultadoAyuda.Fallo -> estado.copy(pidiendo = false, falloAlPedir = true)
            }
            if (resultado is ResultadoAyuda.Pedida) alPedir()
        }
    }

    private companion object {
        const val LARGO_DE_LA_DIRECCION = 120
    }
}
