package com.solventa4bits.movil.sesion.dominio

/** Doble de prueba: guarda la sesion en una variable. */
class AlmacenEnMemoria(private var guardada: Sesion? = null) : AlmacenDeSesion {

    override fun guardar(sesion: Sesion) {
        guardada = sesion
    }

    override fun leer(): Sesion? = guardada

    override fun borrar() {
        guardada = null
    }
}
