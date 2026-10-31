package com.solventa4bits.movil.red

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class GuardiaDeRedTest {

    private var conRed = true
    private val red = GuardiaDeRed { conRed }
    private var hechas = 0
    private val accion: () -> Unit = { hechas++ }

    @Test
    @DisplayName("con red hace la accion de una vez")
    fun conRed() {
        red.conRed(accion)

        assertEquals(1, hechas)
        assertNull(red.pendiente)
    }

    @Test
    @DisplayName("BITS-217, AC-5: sin red la deja pendiente en lugar de fallar en silencio")
    fun sinRed() {
        conRed = false

        red.conRed(accion)

        assertEquals(0, hechas)
        assertNotNull(red.pendiente)
    }

    @Test
    @DisplayName("reintentar sin red no hace nada; con red, sigue con la accion")
    fun reintentar() {
        conRed = false
        red.conRed(accion)

        red.reintentar()
        assertEquals(0, hechas)

        conRed = true
        red.reintentar()
        assertEquals(1, hechas)
        assertNull(red.pendiente)
    }

    @Test
    @DisplayName("volver al Inicio descarta la accion pendiente")
    fun descartar() {
        conRed = false
        red.conRed(accion)

        red.descartar()
        conRed = true
        red.reintentar()

        assertEquals(0, hechas)
        assertNull(red.pendiente)
    }
}
