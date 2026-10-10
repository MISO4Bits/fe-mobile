package com.solventa4bits.movil.cotizacion.ui

import org.junit.jupiter.api.Assertions.assertEquals
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
}
