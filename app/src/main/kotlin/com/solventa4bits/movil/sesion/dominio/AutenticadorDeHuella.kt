package com.solventa4bits.movil.sesion.dominio

/**
 * Comprueba con el telefono que quien lo tiene en la mano es su dueno. La app
 * nunca ve la huella: el sistema la compara y solo responde si paso o no.
 * Permite reemplazar el sensor por un doble en las pruebas.
 */
interface AutenticadorDeHuella {
    /** Hay sensor y el cliente registro al menos una huella en el telefono. */
    fun disponible(): Boolean

    /** Abre el dialogo del sistema y espera a que el cliente toque el sensor. */
    suspend fun autenticar(): ResultadoHuella
}

enum class ResultadoHuella {
    EXITO,

    /** El cliente cerro el dialogo o eligio entrar con contrasena. */
    CANCELADO,

    /** El sensor fallo o quedo bloqueado por demasiados intentos. */
    FALLO,
}
