package com.solventa4bits.movil.sesion.dominio

/**
 * Doble de prueba: responde lo que se le programe sin abrir ningun dialogo y
 * cuenta cuantas veces se le pidio autenticar.
 */
class AutenticadorFalso(
    private val disponible: Boolean = true,
    var respuesta: ResultadoHuella = ResultadoHuella.EXITO,
) : AutenticadorDeHuella {

    var veces = 0
        private set

    override fun disponible(): Boolean = disponible

    override suspend fun autenticar(): ResultadoHuella {
        veces++
        return respuesta
    }
}
