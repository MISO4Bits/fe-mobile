package com.solventa4bits.movil.solicitud.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solventa4bits.movil.cotizacion.dominio.CodigoCobertura
import com.solventa4bits.movil.cotizacion.dominio.Cotizacion
import com.solventa4bits.movil.identidad.dominio.CaraDeCedula
import com.solventa4bits.movil.identidad.dominio.EstadoGuardado
import com.solventa4bits.movil.identidad.dominio.ResultadoEstado
import com.solventa4bits.movil.identidad.dominio.ServicioDeEvidencias
import com.solventa4bits.movil.solicitud.dominio.CondicionesDelSeguro
import com.solventa4bits.movil.solicitud.dominio.DatosDeLaSolicitud
import com.solventa4bits.movil.solicitud.dominio.ResultadoCondiciones
import com.solventa4bits.movil.solicitud.dominio.ResultadoEnvio
import com.solventa4bits.movil.solicitud.dominio.ServicioDeSolicitudes
import com.solventa4bits.movil.solicitud.dominio.SolicitudEnviada
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

/** Lo que se revisa antes de enviar la solicitud (BITS-278, AC-1). */
enum class Requisito { FRENTE, REVERSO, SELFIE }

enum class ErrorAlEnviar {
    /** No llego al servidor o el servidor fallo: se puede enviar otra vez. */
    FALLO,

    /** La cotizacion vencio o cambio: hay que cotizar de nuevo. */
    COTIZAR_DE_NUEVO,
}

data class EstadoSolicitud(
    val cargando: Boolean = true,
    /** No llegaron las fotos o las condiciones: sin ellas no se puede enviar. */
    val falloAlCargar: Boolean = false,
    val requisitos: Map<Requisito, EstadoGuardado> = emptyMap(),
    val condiciones: CondicionesDelSeguro? = null,
    /** El banco del credito, al que Solventa le pedira el cambio del seguro. */
    val entidad: String = "",
    /** Marco "He leido y acepto las condiciones del seguro". */
    val aceptadas: Boolean = false,
    val viendoCondiciones: Boolean = false,
    val enviando: Boolean = false,
    val errorAlEnviar: ErrorAlEnviar? = null,
    /** La solicitud quedo registrada: la pantalla sale de aqui. */
    val enviada: SolicitudEnviada? = null,
    val sesionVencida: Boolean = false,
) {
    /** AC-2: solo con las condiciones aceptadas, y con su version a la mano para enviarla. */
    val puedeEnviar: Boolean
        get() = aceptadas && condiciones != null && !cargando && !enviando
}

/** Decide que pasa en "Revisa y envia tu solicitud". La pantalla solo dibuja. */
class SolicitudViewModel(
    private val evidencias: ServicioDeEvidencias,
    private val solicitudes: ServicioDeSolicitudes,
) : ViewModel() {

    var estado by mutableStateOf(EstadoSolicitud())
        private set

    private var cotizacion: Cotizacion? = null
    private var conTuCasa = false

    /** Al entrar con la cotizacion que el cliente vio y las coberturas que eligio. */
    fun abrir(cotizacion: Cotizacion, conTuCasa: Boolean) {
        this.cotizacion = cotizacion
        this.conTuCasa = conTuCasa
        estado = EstadoSolicitud(entidad = cotizacion.datosCredito.entidadAcreedora)
        cargar()
    }

    /** Vuelve a pedir las fotos y las condiciones; conserva lo que el cliente marco. */
    fun reintentar() {
        cargar()
    }

    // Las dos consultas van a la vez: ninguna depende de la otra.
    private fun cargar() {
        estado = estado.copy(cargando = true, falloAlCargar = false)
        viewModelScope.launch {
            val fotos = async { evidencias.consultar() }
            val condiciones = async { solicitudes.condiciones() }
            val estadoDeFotos = fotos.await()
            val resultado = condiciones.await()
            estado = when {
                estadoDeFotos == ResultadoEstado.SesionVencida -> estado.copy(cargando = false, sesionVencida = true)
                estadoDeFotos is ResultadoEstado.Exito && resultado is ResultadoCondiciones.Exito -> estado.copy(
                    cargando = false,
                    requisitos = requisitosDe(estadoDeFotos),
                    condiciones = resultado.condiciones,
                )
                else -> estado.copy(cargando = false, falloAlCargar = true)
            }
        }
    }

    fun cambiarAceptacion(marcada: Boolean) {
        estado = estado.copy(aceptadas = marcada)
    }

    fun verCondiciones() {
        estado = estado.copy(viendoCondiciones = true)
    }

    fun cerrarCondiciones() {
        estado = estado.copy(viendoCondiciones = false)
    }

    /** "Acepto estas condiciones": marca la casilla y cierra la hoja. */
    fun aceptarDesdeCondiciones() {
        estado = estado.copy(aceptadas = true, viendoCondiciones = false)
    }

    /** AC-2: registra la solicitud con la version de las condiciones aceptadas. Si falla, no se pierde nada. */
    fun enviar() {
        val vista = cotizacion
        val condiciones = estado.condiciones
        if (vista == null || condiciones == null || !estado.puedeEnviar) return
        estado = estado.copy(enviando = true, errorAlEnviar = null)
        val datos = DatosDeLaSolicitud(
            coberturas = if (conTuCasa) {
                setOf(CodigoCobertura.VIDA, CodigoCobertura.HOGAR)
            } else {
                setOf(CodigoCobertura.VIDA)
            },
            cotizacionCreadaEn = vista.creadaEn,
            versionDeCondiciones = condiciones.version,
        )
        viewModelScope.launch {
            estado = when (val resultado = solicitudes.enviar(datos)) {
                is ResultadoEnvio.Enviada -> estado.copy(enviando = false, enviada = resultado.solicitud)
                ResultadoEnvio.CotizarDeNuevo ->
                    estado.copy(enviando = false, errorAlEnviar = ErrorAlEnviar.COTIZAR_DE_NUEVO)
                ResultadoEnvio.SesionVencida -> estado.copy(enviando = false, sesionVencida = true)
                ResultadoEnvio.Fallo -> estado.copy(enviando = false, errorAlEnviar = ErrorAlEnviar.FALLO)
            }
        }
    }

    private fun requisitosDe(fotos: ResultadoEstado.Exito): Map<Requisito, EstadoGuardado> = mapOf(
        Requisito.FRENTE to (fotos.caras[CaraDeCedula.FRENTE] ?: EstadoGuardado.PENDIENTE),
        Requisito.REVERSO to (fotos.caras[CaraDeCedula.REVERSO] ?: EstadoGuardado.PENDIENTE),
        Requisito.SELFIE to fotos.selfie,
    )
}
