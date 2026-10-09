package com.solventa4bits.movil.sesion.dominio

/**
 * Lo que la app necesita de la autenticacion, sin saber como se hace por
 * dentro. Permite reemplazar la red por un doble en las pruebas.
 */
interface ServicioDeSesion {
    suspend fun iniciarSesion(correo: String, contrasena: String): ResultadoInicioSesion

    /**
     * Cambia el token de refresco por una sesion nueva, sin pedir contrasena.
     * Si el token ya vencio responde [ResultadoInicioSesion.CredencialesInvalidas].
     */
    suspend fun refrescar(tokenDeRefresco: String): ResultadoInicioSesion
}
