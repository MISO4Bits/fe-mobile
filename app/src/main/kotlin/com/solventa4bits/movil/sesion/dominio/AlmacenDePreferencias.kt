package com.solventa4bits.movil.sesion.dominio

/** Lo que el cliente decidio sobre entrar con la huella en este telefono. */
enum class PreferenciaDeHuella {
    /** Todavia no se le ha preguntado, o se le volvera a preguntar. */
    SIN_DECIDIR,
    ACTIVADA,

    /** Dijo "ahora no": no se le vuelve a preguntar hasta que cierre sesion. */
    RECHAZADA,
}

/** Donde la app guarda las decisiones del cliente que no son secretas. */
interface AlmacenDePreferencias {
    fun huella(): PreferenciaDeHuella

    fun guardarHuella(preferencia: PreferenciaDeHuella)

    /** El id que el servidor le dio a este telefono para los avisos push, o null si no esta registrado (BITS-229). */
    fun dispositivoId(): String?

    fun guardarDispositivoId(id: String?)

    /** Ya se le pidio al cliente el permiso de notificaciones: se pide una sola vez. */
    fun permisoDeAvisosPedido(): Boolean

    fun marcarPermisoDeAvisosPedido()
}
