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

/** Decide que pasa en la pantalla de inicio de sesion. La pantalla solo dibuja. */
class LoginViewModel(
    private val servicio: ServicioDeSesion,
    private val almacen: AlmacenDeSesion,
    private val autenticador: AutenticadorDeHuella,
    private val preferencias: AlmacenDePreferencias,
) : ViewModel() {

    private val conHuella = autenticador.disponible()

    // Sin sensor no se habla de huella: el inicio con credenciales funciona
    // igual (AC-3).
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
        if (estado.sesionIniciada) refrescar()
    }

    // Renueva la sesion por debajo, sin que el cliente lo note. Si no hay red
    // se conserva la sesion que habia.
    private fun refrescar() {
        val guardada = almacen.leer() ?: return
        viewModelScope.launch {
            when (val resultado = servicio.refrescar(guardada.tokenDeRefresco)) {
                is ResultadoInicioSesion.Exito -> almacen.guardar(resultado.sesion)
                ResultadoInicioSesion.CredencialesInvalidas -> cerrarSesion()
                ResultadoInicioSesion.Fallo -> Unit
            }
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
    fun decidirHuella(activar: Boolean) {
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
    fun entrarConHuella() {
        if (estado.cargando) return
        estado = estado.copy(cargando = true)
        viewModelScope.launch {
            if (autenticador.autenticar() == ResultadoHuella.EXITO) {
                entrar()
                refrescar()
            } else {
                // El telefono no la reconocio: entra con sus credenciales. La
                // sesion guardada se conserva por si vuelve a intentarlo.
                estado = estado.copy(cargando = false, paso = PasoLogin.CREDENCIALES)
            }
        }
    }

    /** Desde "Entrar con tu huella" el cliente prefiere escribir la contrasena. */
    fun entrarConContrasena() {
        estado = estado.copy(paso = PasoLogin.CREDENCIALES)
    }

    /** Prende o apaga la huella desde el menu, ya adentro. */
    fun alternarHuella() {
        if (!conHuella) return
        if (preferencias.huella() == PreferenciaDeHuella.ACTIVADA) {
            preferencias.guardarHuella(PreferenciaDeHuella.RECHAZADA)
            estado = estado.copy(huella = huella)
            return
        }
        viewModelScope.launch {
            if (autenticador.autenticar() == ResultadoHuella.EXITO) {
                preferencias.guardarHuella(PreferenciaDeHuella.ACTIVADA)
            }
            estado = estado.copy(huella = huella)
        }
    }

    private fun entrar() {
        estado = estado.copy(cargando = false, sesionIniciada = true, paso = PasoLogin.CREDENCIALES, huella = huella)
    }

    /** Borra los datos locales de sesion y vuelve al inicio de sesion. */
    fun cerrarSesion() {
        almacen.borrar()
        // La huella queda desactivada en este telefono y se vuelve a ofrecer
        // al entrar con contrasena (AC-6).
        preferencias.guardarHuella(PreferenciaDeHuella.SIN_DECIDIR)
        estado = EstadoLogin(huella = huella)
    }
}
