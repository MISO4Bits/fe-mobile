package com.solventa4bits.movil.avisos.dominio

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class NumeroDeAvisoTest {

    @Test
    @DisplayName("BITS-372, AC-1: dos avisos seguidos tienen numeros distintos y no se pisan")
    fun avisosDistintos() {
        assertNotEquals(
            numeroDeAviso("0:1731340800000%abc", 1_000L),
            numeroDeAviso("0:1731340860000%def", 1_000L),
        )
    }

    @Test
    @DisplayName("el mismo aviso da siempre el mismo numero")
    fun mismoAviso() {
        assertEquals(numeroDeAviso("0:1731340800000%abc", 1_000L), numeroDeAviso("0:1731340800000%abc", 9_000L))
    }

    @Test
    @DisplayName("sin id del mensaje, avisos que llegan en momentos distintos tienen numeros distintos")
    fun sinIdDelMensaje() {
        assertNotEquals(numeroDeAviso(null, 1_731_340_800_000L), numeroDeAviso(null, 1_731_340_800_001L))
    }
}
