package com.solventa4bits.movil.traza

/**
 * Donde la app deja constancia de un error para poder diagnosticarlo.
 *
 * Es una interfaz y no una llamada directa al Log de Android por dos razones:
 * el Log no existe en las pruebas unitarias, y asi una prueba puede comprobar
 * que el error si quedo registrado.
 *
 * Nunca se registran contrasenas ni tokens.
 */
fun interface Traza {
    fun error(origen: String, mensaje: String, causa: Throwable?)
}
