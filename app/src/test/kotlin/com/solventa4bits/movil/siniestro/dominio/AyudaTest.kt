package com.solventa4bits.movil.siniestro.dominio

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class AyudaTest {

    private val chapinero = Localidad("chapinero", "Chapinero", "Bogotá", 4.649, -74.063)
    private val kennedy = Localidad("kennedy", "Kennedy", "Bogotá", 4.628, -74.155)
    private val chia = Localidad("chia", "Chía", "Chía", 4.861, -74.032)

    private fun prestador(id: String, tipos: List<String>, latitud: Double, longitud: Double) =
        Prestador(id, "Prestador $id", tipos, "chapinero", "Calle 1 # 2-3", latitud, longitud, 35)

    private val cerca = prestador("cerca", listOf("plomeria"), 4.650, -74.064)
    private val lejos = prestador("lejos", listOf("plomeria", "vidrios"), 4.628, -74.155)
    private val electricista = prestador("luz", listOf("electricidad"), 4.649, -74.063)

    private val catalogo = CatalogoDeSiniestros(
        tiposAyuda = listOf(TipoDeAyuda("vidrios", "Vidrios", 4), TipoDeAyuda("plomeria", "Plomería", 1)),
        localidades = listOf(chapinero, chia, kennedy),
        prestadores = listOf(lejos, electricista, cerca),
    )

    @Test
    @DisplayName("BITS-239, AC-2: los tipos de ayuda en el orden del catalogo")
    fun tiposEnOrden() {
        assertEquals(listOf("plomeria", "vidrios"), catalogo.tiposEnOrden.map { it.id })
    }

    @Test
    @DisplayName("BITS-239, AC-4: solo las localidades de Bogota")
    fun soloBogota() {
        assertEquals(listOf(chapinero, kennedy), catalogo.localidadesDeBogota)
    }

    @Test
    @DisplayName("BITS-239, AC-5: los prestadores del tipo, del mas cercano al mas lejano")
    fun prestadoresCerca() {
        val lista = catalogo.prestadoresCerca("plomeria", chapinero.centro)

        assertEquals(listOf(cerca, lejos), lista.map { it.prestador })
        assertTrue(lista.first().km < 1.0)
        assertTrue(catalogo.prestadoresCerca("cerrajeria", chapinero.centro).isEmpty())
    }

    @Test
    @DisplayName("BITS-239, AC-4: con una direccion escrita, la distancia se mide desde el centro de su localidad")
    fun direccionEscrita() {
        val punto = PuntoDeAyuda.EnUnaDireccion("Calle 63 # 11-40", kennedy)

        assertEquals(OrigenDeAyuda.DIRECCION, punto.origen)
        assertEquals(lejos, catalogo.prestadoresCerca("plomeria", punto.ubicacion).first().prestador)
    }

    @Test
    @DisplayName("BITS-239, AC-1: solo incendio o terremoto, En estudio o en Falta informacion")
    fun quienPuedePedir() {
        fun detalle(tipo: TipoDeSiniestro, estado: EstadoDeSiniestro) =
            DetalleDeSiniestro(SiniestroResumen("s1", "SIN-1", "p1", tipo, estado), "2026-11-02", null, 0, 1)

        assertTrue(detalle(TipoDeSiniestro.INCENDIO_TERREMOTO, EstadoDeSiniestro.EN_ESTUDIO).puedePedirAyuda)
        assertTrue(detalle(TipoDeSiniestro.INCENDIO_TERREMOTO, EstadoDeSiniestro.FALTA_INFORMACION).puedePedirAyuda)
        assertFalse(detalle(TipoDeSiniestro.INCENDIO_TERREMOTO, EstadoDeSiniestro.PAGADO).puedePedirAyuda)
        assertFalse(detalle(TipoDeSiniestro.INCAPACIDAD_TOTAL, EstadoDeSiniestro.EN_ESTUDIO).puedePedirAyuda)
        assertFalse(detalle(TipoDeSiniestro.SISMO_PARAMETRICO, EstadoDeSiniestro.EN_ESTUDIO).puedePedirAyuda)
    }
}
