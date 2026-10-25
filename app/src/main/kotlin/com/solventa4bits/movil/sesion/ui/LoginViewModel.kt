package com.solventa4bits.movil.sesion.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solventa4bits.movil.sesion.dominio.AlmacenDePreferencias
import com.solventa4bits.movil.sesion.dominio.AlmacenDeSesion
import com.solventa4bits.movil.sesion.dominio.AutenticadorDeHuella
import com.solventa4bits.movil.sesion.dominio.PreferenciaDeHuella
import com.solventa4bits.movil.sesion.dominio.ResultadoHuella
import com.solventa4bits.movil.sesion.dominio.ResultadoInicioSesion
import com.solventa4bits.movil.sesion.dominio.ServicioDeSesion
import kotlinx.coroutines.launch

/** Lo que la pantalla de inicio de sesion necesita para dibujarse. */
data class EstadoLogin(
    val correo: String = "",
    val contrasena: String = "",
    val cargando: Boolean = false,
    val error: ErrorLogin? = null,
    val sesionIniciada: Boolean = false,
    val paso: PasoLogin = PasoLogin.CREDENCIALES,
    /** Lo decidido sobre la huella en este telefono, o null si no tiene sensor. */
    val huella: PreferenciaDeHuella? = null,
) {
    val puedeEnviar: Boolean
        get() = correo.isNotBlank() && contrasena.isNotBlank() && !cargando

    /** Si entra con la huella; null cuando el telefono no tiene sensor. */
    val huellaActivada: Boolean?
        get() = huella?.let { it == PreferenciaDeHuella.ACTIVADA }
}

enum class ErrorLogin { CREDENCIALES_INVALIDAS, FALLO }

/** Que se le muestra al cliente mientras no ha entrado. */
enum class PasoLogin {
    CREDENCIALES,

    /** Acaba de entrar con contrasena y se le ofrece activar la huella (AC-1). */
    OFRECER_HUELLA,

    /** Tiene la huella activada: la sesion guardada espera detras del sensor (AC-2). */
    ENTRAR_CON_HUELLA,
}

/**
 * Decide que pasa en la pantalla de inicio de sesion. La pantalla solo dibuja.
 *
 * El [AutenticadorDeHuella] llega en cada llamada y no se guarda: el dialogo
 * biometrico pertenece a la Activity, que Android recrea al rotar, y el
 * ViewModel sobrevive a eso.
 *
 * @param conHuella el telefono tiene sensor y una huella registrada. Sin
 * sensor no se habla de huella: el inicio con credenciales funciona igual (AC-3).
 */
class LoginViewModel(
    private val servicio: ServicioDeSesion,
    private val almacen: AlmacenDeSesion,
    private val preferencias: AlmacenDePreferencias,
    private val conHuella: Boolean,
) : ViewModel() {

    private val huella: PreferenciaDeHuella?
        get() = preferencias.huella().takeIf { conHuella }

    // Si ya hay una sesion guardada, la app arranca adentro. Con la huella
    // activada, primero pide tocar el sensor.
    var estado by mutableStateOf(
        when {
            almacen.leer() == null -> EstadoLogin(huella = huella)
            huella == PreferenciaDeHuella.ACTIVADA -> EstadoLogin(paso = PasoLogin.ENTRAR_CON_HUELLA, huella = huella)
            else -> EstadoLogin(sesionIniciada = true, huella = huella)
        },
    )
        private set

    init {
        if (estado.sesionIniciada) viewModelScope.launch { refrescar() }
    }

    // Renueva la sesion por debajo, sin que el cliente lo note. Si no hay red
    // se conserva la sesion que habia. Responde si la sesion sigue sirviendo.
    private suspend fun refrescar(): Boolean {
        val guardada = almacen.leer() ?: return false
        return when (val resultado = servicio.refrescar(guardada.tokenDeRefresco)) {
            is ResultadoInicioSesion.Exito -> {
                almacen.guardar(resultado.sesion)
                true
            }
            // Mas de 24 horas sin abrir la app: se piden credenciales, pero la
            // huella sigue activada; solo cerrar sesion la apaga (AC-6).
            ResultadoInicioSesion.CredencialesInvalidas -> {
                cerrarSesion(olvidarHuella = false)
                false
            }
            ResultadoInicioSesion.Fallo -> true
        }
    }

    fun cambiarCorreo(correo: String) {
        estado = estado.copy(correo = correo)
    }

    fun cambiarContrasena(contrasena: String) {
        estado = estado.copy(contrasena = contrasena)
    }

    fun iniciarSesion() {
        if (!estado.puedeEnviar) return
        estado = estado.copy(cargando = true, error = null)
        viewModelScope.launch {
            // Un espacio que deja el teclado o el pegado al inicio o al final del
            // correo hace que el BFF rechace el formato. La contrasena no se toca:
            // un espacio puede ser parte de ella.
            estado = when (val resultado = servicio.iniciarSesion(estado.correo.trim(), estado.contrasena)) {
                is ResultadoInicioSesion.Exito -> {
                    almacen.guardar(resultado.sesion)
                    // Con sensor y sin decision previa se ofrece la huella (AC-1).
                    if (huella == PreferenciaDeHuella.SIN_DECIDIR) {
                        estado.copy(cargando = false, paso = PasoLogin.OFRECER_HUELLA)
                    } else {
                        estado.copy(cargando = false, sesionIniciada = true)
                    }
                }
                ResultadoInicioSesion.CredencialesInvalidas ->
                    estado.copy(cargando = false, error = ErrorLogin.CREDENCIALES_INVALIDAS)
                ResultadoInicioSesion.Fallo ->
                    estado.copy(cargando = false, error = ErrorLogin.FALLO)
            }
        }
    }

    /** Respuesta a la oferta de activar la huella: si o "ahora no" (AC-1). */
    fun decidirHuella(activar: Boolean, autenticador: AutenticadorDeHuella) {
        if (estado.cargando) return
        if (!activar) {
            preferencias.guardarHuella(PreferenciaDeHuella.RECHAZADA)
            entrar()
            return
        }
        estado = estado.copy(cargando = true)
        viewModelScope.launch {
            // Si el sensor no la reconoce o el cliente desiste, entra igual y se
            // le vuelve a ofrecer la proxima vez.
            if (autenticador.autenticar() == ResultadoHuella.EXITO) {
                preferencias.guardarHuella(PreferenciaDeHuella.ACTIVADA)
            }
            entrar()
        }
    }

    /** El cliente toco el sensor en "Entrar con tu huella" (AC-2). */
    fun entrarConHuella(autenticador: AutenticadorDeHuella) {
        if (estado.cargando) return
        estado = estado.copy(cargando = true)
        viewModelScope.launch {
            when (autenticador.autenticar()) {
                ResultadoHuella.EXITO -> if (refrescar()) entrar()
                // Cerro el dialogo: puede tocar el icono otra vez o usar la contrasena.
                ResultadoHuella.CANCELADO -> estado = estado.copy(cargando = false)
                // El sensor no la reconocio o esta bloqueado: entra con sus
                // credenciales. La sesion guardada se conserva.
                ResultadoHuella.FALLO -> estado = estado.copy(cargando = false, paso = PasoLogin.CREDENCIALES)
            }
        }
    }

    /** Desde "Entrar con tu huella" el cliente prefiere escribir la contrasena. */
    fun entrarConContrasena() {
        estado = estado.copy(paso = PasoLogin.CREDENCIALES)
    }

    /** Prende o apaga la huella desde el menu, ya adentro. */
    fun alternarHuella(autenticador: AutenticadorDeHuella) {
        if (!conHuella || estado.cargando) return
        if (preferencias.huella() == PreferenciaDeHuella.ACTIVADA) {
            preferencias.guardarHuella(PreferenciaDeHuella.RECHAZADA)
            estado = estado.copy(huella = huella)
            return
        }
        estado = estado.copy(cargando = true)
        viewModelScope.launch {
            if (autenticador.autenticar() == ResultadoHuella.EXITO) {
                preferencias.guardarHuella(PreferenciaDeHuella.ACTIVADA)
            }
            estado = estado.copy(cargando = false, huella = huella)
        }
    }

    private fun entrar() {
        estado = estado.copy(cargando = false, sesionIniciada = true, paso = PasoLogin.CREDENCIALES, huella = huella)
    }

    /**
     * Borra los datos locales de sesion y vuelve al inicio de sesion. Al
     * cerrarla el cliente, la huella queda desactivada en este telefono y se
     * vuelve a ofrecer al entrar con contrasena (AC-6).
     */
    fun cerrarSesion(olvidarHuella: Boolean = true) {
        almacen.borrar()
        if (olvidarHuella) preferencias.guardarHuella(PreferenciaDeHuella.SIN_DECIDIR)
        estado = EstadoLogin(huella = huella)
    }
}
