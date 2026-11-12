package com.solventa4bits.movil.siniestro.dominio

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class OficinasTest {

    private val chapinero =
        Oficina("b1", "Bancolombia", "Oficina Chapinero", "Calle 57 # 9-20", "Bogotá", 4.6486, -74.0628)
    private val salitre =
        Oficina("b2", "Bancolombia", "Oficina Salitre", "Av. Calle 26 # 69-76", "Bogotá", 4.6584, -74.1103)
    private val chia = Oficina("b3", "Bancolombia", "Oficina Chía", "Calle 12 # 10-50", "Chía", 4.8619, -74.0336)
    private val bbva = Oficina("v1", "BBVA", "Oficina Lourdes", "Carrera 13 # 63-39", "Bogotá", 4.6503, -74.0620)
    private val oficinas = listOf(salitre, chia, bbva, chapinero)

    // Cerca del parque de Lourdes, en Chapinero.
    private val cliente = Ubicacion(4.6497, -74.0631)

    @Test
    @DisplayName("la distancia en linea recta, en km")
    fun distancia() {
        assertEquals(0.13, chapinero.kmHasta(cliente), 0.05)
        assertEquals(5.2, salitre.kmHasta(cliente), 0.3)
    }

    @Test
    @DisplayName("BITS-116, AC-4: con ubicacion, las del banco, de la mas cercana a la mas lejana")
    fun conUbicacion() {
        val lista = oficinas.paraElegir("Bancolombia", cliente, ciudad = null)

        assertEquals(listOf(chapinero, salitre, chia), lista.map { it.oficina })
        assertEquals(0.13, lista.first().km!!, 0.05)
    }

    @Test
    @DisplayName("BITS-116, AC-4: sin ubicacion, las del banco en la ciudad elegida, por nombre y sin distancia")
    fun sinUbicacion() {
        val lista = oficinas.paraElegir("Bancolombia", ubicacion = null, ciudad = "Bogotá")

        assertEquals(listOf(chapinero, salitre), lista.map { it.oficina })
        assertNull(lista.first().km)
        assertEquals(emptyList<OficinaCercana>(), oficinas.paraElegir("Bancolombia", null, null))
    }

    @Test
    @DisplayName("las ciudades donde el banco tiene oficinas, sin repetir y en orden")
    fun ciudades() {
        assertEquals(listOf("Bogotá", "Chía"), oficinas.ciudadesDe("Bancolombia"))
        assertEquals(listOf("Bogotá"), oficinas.ciudadesDe("BBVA"))
    }
}
