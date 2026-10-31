package com.solventa4bits.movil.avisos.dominio

import com.solventa4bits.movil.sesion.dominio.AlmacenDePreferencias

/** El permiso de notificaciones del telefono. */
interface PermisoDeNotificaciones {
    /** Las notificaciones de la app estan habilitadas. */
    fun concedido(): Boolean

    /** Desde Android 13 el permiso se pide en ejecucion; antes lo concede la instalacion. */
    fun sePuedePedir(): Boolean
}

/** Lo que la app hace con los avisos al abrir y cerrar la sesion (BITS-229). */
interface ControlDeAvisos {
    /**
     * Hay sesion iniciada (recien o restaurada). Si el permiso ya esta, registra el telefono.
     * Devuelve `true` si la pantalla debe pedir el permiso ahora.
     */
    suspend fun alEntrar(): Boolean

    /** El cliente acepto el permiso que se le pidio. */
    suspend fun alConcederPermiso()

    /** Va a cerrar sesion: el telefono deja de recibir los avisos de este cliente. */
    suspend fun alSalir()
}

/** Sin avisos: las pruebas de pantalla y cualquier armado que no los necesite. */
object SinAvisos : ControlDeAvisos {
    override suspend fun alEntrar() = false

    override suspend fun alConcederPermiso() = Unit

    override suspend fun alSalir() = Unit
}

class AvisosDelCliente(
    private val registro: RegistroDeDispositivo,
    private val preferencias: AlmacenDePreferencias,
    private val permiso: PermisoDeNotificaciones,
    private val haySesion: () -> Boolean,
) : ControlDeAvisos {

    override suspend fun alEntrar(): Boolean {
        if (permiso.concedido()) {
            registro.registrar()
            return false
        }
        // Se pide una sola vez (AC-1). Si lo nego, la app sigue igual y no se registra nada.
        val debePedir = permiso.sePuedePedir() && !preferencias.permisoDeAvisosPedido()
        if (debePedir) preferencias.marcarPermisoDeAvisosPedido()
        return debePedir
    }

    override suspend fun alConcederPermiso() {
        if (haySesion()) registro.registrar()
    }

    override suspend fun alSalir() {
        registro.darDeBaja()
    }

    /** Firebase entrego un token nuevo (lo rota de vez en cuando). Solo cuenta con sesion y permiso. */
    suspend fun alCambiarElToken(token: String) {
        if (haySesion() && permiso.concedido()) registro.registrarToken(token)
    }
}
