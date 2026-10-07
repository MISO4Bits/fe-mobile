package com.solventa4bits.movil.traza

/** Doble de prueba: guarda lo que se registro para poder comprobarlo. */
class TrazaFalsa : Traza {
    data class Entrada(val origen: String, val mensaje: String, val causa: Throwable?)

    val errores = mutableListOf<Entrada>()

    override fun error(origen: String, mensaje: String, causa: Throwable?) {
        errores += Entrada(origen, mensaje, causa)
    }
}
