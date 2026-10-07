package com.solventa4bits.movil.sesion.datos

/**
 * Guarda textos cifrados en el dispositivo. Quien la usa no sabe como se
 * cifra: eso permite reemplazarla por un doble en las pruebas unitarias.
 */
interface Boveda {
    fun guardar(clave: String, valor: String)

    /** El valor en claro, o null si no existe o no se pudo descifrar. */
    fun leer(clave: String): String?

    fun borrar(clave: String)
}
