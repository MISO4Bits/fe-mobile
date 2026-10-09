package com.solventa4bits.movil.sesion.datos

import com.solventa4bits.movil.sesion.dominio.Sesion
import com.solventa4bits.movil.traza.TrazaFalsa
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class AlmacenDeSesionSeguroTest {

    private val boveda = BovedaFalsa()
    private val traza = TrazaFalsa()
    private val almacen = AlmacenDeSesionSeguro(boveda, traza)
    private val sesion = Sesion("jwt-de-acceso", "token-de-refresco", 3600)

    @Test
    @DisplayName("si no se ha guardado nada no hay sesion")
    fun sinSesion() {
        assertNull(almacen.leer())
    }

    @Test
    @DisplayName("devuelve la misma sesion que se guardo")
    fun guardaYLee() {
        almacen.guardar(sesion)

        assertEquals(sesion, almacen.leer())
    }

    @Test
    @DisplayName("todo lo que guarda pasa por la boveda")
    fun usaLaBoveda() {
        almacen.guardar(sesion)

        assertTrue(boveda.valores.values.single().contains("jwt-de-acceso"))
    }

    @Test
    @DisplayName("al borrar no queda ningun dato de sesion")
    fun borra() {
        almacen.guardar(sesion)

        almacen.borrar()

        assertNull(almacen.leer())
        assertTrue(boveda.valores.isEmpty())
    }

    @Test
    @DisplayName("si lo guardado esta danado lo descarta, deja traza y no hay sesion")
    fun sesionDanada() {
        almacen.guardar(sesion)
        boveda.valores.replaceAll { _, _ -> "esto no es una sesion" }

        assertNull(almacen.leer())
        assertTrue(boveda.valores.isEmpty())
        assertEquals(1, traza.errores.size)
    }
}

/** Doble de prueba: una boveda en memoria, sin cifrado. */
private class BovedaFalsa : Boveda {
    val valores = mutableMapOf<String, String>()

    override fun guardar(clave: String, valor: String) {
        valores[clave] = valor
    }

    override fun leer(clave: String): String? = valores[clave]

    override fun borrar(clave: String) {
        valores.remove(clave)
    }
}
