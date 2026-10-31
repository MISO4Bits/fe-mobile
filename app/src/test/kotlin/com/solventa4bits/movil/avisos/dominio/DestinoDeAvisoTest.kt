package com.solventa4bits.movil.avisos.dominio

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/** El aviso llega de fuera de la app: solo se acepta un destino conocido con un id que tenga forma de id. */
class DestinoDeAvisoTest {

    @Test
    @DisplayName("el aviso de poliza emitida lleva a su seguro")
    fun seguro() {
        val id = "0c9a3a52-6c0e-4d8e-9a7e-1c5d2f9b7a10"

        assertEquals(DestinoDeAviso(id), DestinoDeAviso.desde("seguro", id))
    }

    @Test
    @DisplayName("un destino desconocido o ausente se ignora")
    fun destinoDesconocido() {
        assertNull(DestinoDeAviso.desde("siniestros", "poliza-1"))
        assertNull(DestinoDeAviso.desde("SEGURO", "poliza-1"))
        assertNull(DestinoDeAviso.desde(null, "poliza-1"))
    }

    @Test
    @DisplayName("un id ausente, vacio o con forma rara se ignora")
    fun idInvalido() {
        assertNull(DestinoDeAviso.desde("seguro", null))
        assertNull(DestinoDeAviso.desde("seguro", ""))
        assertNull(DestinoDeAviso.desde("seguro", "../otra-ruta"))
        assertNull(DestinoDeAviso.desde("seguro", "con espacio"))
        assertNull(DestinoDeAviso.desde("seguro", "a".repeat(65)))
    }
}
