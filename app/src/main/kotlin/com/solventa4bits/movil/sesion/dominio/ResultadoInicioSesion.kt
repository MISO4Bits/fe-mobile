package com.solventa4bits.movil.sesion.dominio

/**
 * Todo lo que puede pasar al intentar iniciar sesion. La pantalla decide que
 * mostrar a partir de esto y nunca ve una excepcion.
 */
sealed interface ResultadoInicioSesion {
    data class Exito(val sesion: Sesion) : ResultadoInicioSesion

    /** El BFF respondio 401: correo o contrasena incorrectos. */
    data object CredencialesInvalidas : ResultadoInicioSesion

    /** No se pudo completar: no hay red o el servidor fallo. */
    data object Fallo : ResultadoInicioSesion
}
