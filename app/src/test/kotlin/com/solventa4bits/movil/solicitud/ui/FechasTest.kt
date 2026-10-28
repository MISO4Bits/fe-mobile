package com.solventa4bits.movil.solicitud.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.Instant

class FechasTest {

    // 28 de octubre de 2026, 3:00 p. m. en Colombia (UTC-5).
    private val ahora = Instant.parse("2026-10-28T20:00:00Z")

    @Test
    @DisplayName("una fecha de hoy dice Hoy, en la hora de Colombia")
    fun hoy() {
        assertEquals("Hoy, 9:41 a. m.", fechaDeSolicitud("2026-10-28T14:41:00Z", ahora))
    }

    @Test
    @DisplayName("una fecha de ayer dice Ayer")
    fun ayer() {
        assertEquals("Ayer, 9:41 a. m.", fechaDeSolicitud("2026-10-27T14:41:00Z", ahora))
    }

    @Test
    @DisplayName("una fecha mas vieja dice el dia y el mes")
    fun otraFecha() {
        assertEquals("25 de octubre, 9:41 a. m.", fechaDeSolicitud("2026-10-25T14:41:00Z", ahora))
    }

    @Test
    @DisplayName("la tarde, el medio dia y la medianoche llevan su a. m. o p. m.")
    fun mananaYTarde() {
        assertEquals("Hoy, 2:05 p. m.", fechaDeSolicitud("2026-10-28T19:05:00Z", ahora))
        assertEquals("Hoy, 12:00 p. m.", fechaDeSolicitud("2026-10-28T17:00:00Z", ahora))
        assertEquals("Hoy, 12:30 a. m.", fechaDeSolicitud("2026-10-28T05:30:00Z", ahora))
    }

    @Test
    @DisplayName("el dia se cuenta en Colombia: las 2 a. m. UTC de hoy todavia son ayer")
    fun cambioDeDiaPorLaZona() {
        assertEquals("Ayer, 9:00 p. m.", fechaDeSolicitud("2026-10-28T02:00:00Z", ahora))
    }

    @Test
    @DisplayName("una fecha con otro formato no se muestra")
    fun fechaInvalida() {
        assertNull(fechaDeSolicitud("ayer por la tarde", ahora))
    }
}
