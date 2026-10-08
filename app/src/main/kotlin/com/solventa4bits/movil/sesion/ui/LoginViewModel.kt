package com.solventa4bits.movil.sesion.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solventa4bits.movil.sesion.dominio.AlmacenDeSesion
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
) {
    val puedeEnviar: Boolean
        get() = correo.isNotBlank() && contrasena.isNotBlank() && !cargando
}

enum class ErrorLogin { CREDENCIALES_INVALIDAS, FALLO }

/** Decide que pasa en la pantalla de inicio de sesion. La pantalla solo dibuja. */
class LoginViewModel(
    private val servicio: ServicioDeSesion,
    private val almacen: AlmacenDeSesion,
) : ViewModel() {

    // Si ya hay una sesion guardada, la app arranca adentro.
    var estado by mutableStateOf(EstadoLogin(sesionIniciada = almacen.leer() != null))
        private set

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
            estado = when (val resultado = servicio.iniciarSesion(estado.correo, estado.contrasena)) {
                is ResultadoInicioSesion.Exito -> {
                    almacen.guardar(resultado.sesion)
                    estado.copy(cargando = false, sesionIniciada = true)
                }
                ResultadoInicioSesion.CredencialesInvalidas ->
                    estado.copy(cargando = false, error = ErrorLogin.CREDENCIALES_INVALIDAS)
                ResultadoInicioSesion.Fallo ->
                    estado.copy(cargando = false, error = ErrorLogin.FALLO)
            }
        }
    }

    /** Borra los datos locales de sesion y vuelve al inicio de sesion. */
    fun cerrarSesion() {
        almacen.borrar()
        estado = EstadoLogin()
    }
}
