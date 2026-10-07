package com.solventa4bits.movil.sesion.dominio

/** Donde la app guarda la sesion entre un uso y otro. */
interface AlmacenDeSesion {
    fun guardar(sesion: Sesion)

    /** La sesion guardada, o null si no hay ninguna. */
    fun leer(): Sesion?

    fun borrar()
}
