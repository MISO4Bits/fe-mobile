package com.solventa4bits.movil.seguro.dominio

import com.solventa4bits.movil.solicitud.dominio.Solicitud

/** Las polizas emitidas guardadas en el telefono, para verlas sin conexion (BITS-217). */
data class SegurosGuardados(
    val seguros: List<Solicitud>,
    /** Cuando se guardaron, en ISO 8601. */
    val actualizadosEn: String,
)

/** Guarda en el telefono las polizas emitidas. Al cerrar sesion se borran (AC-6). */
interface AlmacenDeSeguros {
    /** Reemplaza lo guardado por [seguros], con la fecha [en] (ISO 8601). */
    suspend fun guardar(seguros: List<Solicitud>, en: String)

    /** Lo ultimo que se guardo, o null si no hay nada. */
    suspend fun leer(): SegurosGuardados?

    suspend fun borrar()
}
