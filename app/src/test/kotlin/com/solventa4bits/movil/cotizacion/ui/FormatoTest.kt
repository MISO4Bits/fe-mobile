package com.solventa4bits.movil.cotizacion.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class FormatoTest {

    @Test
    @DisplayName("muestra los pesos con punto de miles y sin decimales")
    fun pesos() {
        assertEquals("$380.000.000", enPesos(380_000_000.0))
        assertEquals("$1.650.000", enPesos(1_650_000.0))
        assertEquals("$0", enPesos(0.0))
    }

    @Test
    @DisplayName("muestra la fecha de la consulta en espanol y con la hora de Colombia")
    fun fecha() {
        assertEquals("9 de octubre de 2026", fechaLegible("2026-10-09T12:00:00Z"))
        // Las 2 a. m. UTC del 10 todavia son las 9 p. m. del 9 en Colombia.
        assertEquals("9 de octubre de 2026", fechaLegible("2026-10-10T02:00:00Z"))
    }

    @Test
    @DisplayName("si la fecha no tiene el formato del contrato no muestra nada")
    fun fechaIlegible() {
        assertNull(fechaLegible("ayer"))
    }
}
