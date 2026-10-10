package com.solventa4bits.movil.cotizacion.ui

import com.solventa4bits.movil.cotizacion.dominio.EntidadFinanciera

/** Lo que el cliente puede hacer en la pantalla de nueva cotizacion. */
interface AccionesDeCotizacion {
    /** Vuelve a consultar conservando el banco elegido y lo ya escrito. */
    fun reintentar()

    fun elegirEntidad(entidad: EntidadFinanciera)

    /** Su banco no esta en la lista: escribe el nombre y los datos. */
    fun elegirOtroBanco()

    /** Prefiere escribir los datos el mismo, haya o no datos de su banco. */
    fun escribirAMano()

    fun cambiarBanco(texto: String)

    fun cambiarMonto(texto: String)

    fun cambiarSaldo(texto: String)

    fun cambiarMeses(texto: String)
}
