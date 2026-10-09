package com.solventa4bits.movil.fixtures

import com.solventa4bits.movil.sesion.dominio.AlmacenDeSesion
import org.junit.rules.ExternalResource

/**
 * Borra la sesion guardada antes y despues de cada prueba. Va como regla y no
 * al final del cuerpo de la prueba para que limpie aunque la prueba falle:
 * ninguna prueba hereda la sesion de otra (regla de independencia, BITS-280).
 */
class SesionLimpia(private val almacen: AlmacenDeSesion) : ExternalResource() {

    override fun before() {
        almacen.borrar()
    }

    override fun after() {
        almacen.borrar()
    }
}
