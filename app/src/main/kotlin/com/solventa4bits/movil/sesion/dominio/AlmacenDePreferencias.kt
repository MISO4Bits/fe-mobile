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
}
