package com.solventa4bits.movil.sesion.dominio

/** Lo que entrega el IAM cuando el inicio de sesion sale bien. */
data class Sesion(
    val tokenDeAcceso: String,
    val tokenDeRefresco: String,
    val segundosDeVigencia: Long,
)
