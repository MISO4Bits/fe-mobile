package com.solventa4bits.movil.red

/** Si el telefono tiene red. En las pruebas se reemplaza por una falsa. */
fun interface Conexion {
    fun hay(): Boolean
}
