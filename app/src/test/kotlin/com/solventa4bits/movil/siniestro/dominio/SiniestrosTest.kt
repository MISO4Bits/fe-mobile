package com.solventa4bits.movil.siniestro.dominio

import com.solventa4bits.movil.solicitud.dominio.EstadoDeSolicitud
import com.solventa4bits.movil.solicitud.dominio.Solicitud
import com.solventa4bits.movil.solicitud.dominio.Vigencia
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SiniestrosTest {

    // Emitida el 19 oct a las 9:41 p. m. en Colombia (20 oct en UTC); vence el 19 oct de 2027.
    private val poliza = Solicitud(
        id = "poliza-1",
        estado = EstadoDeSolicitud.EMITIDA,
        creadaEn = "2026-10-19T14:00:00Z",
        emitidaEn = "2026-10-20T02:41:00Z",
        pendientes = emptyList(),
        motivosDeRechazo = emptyList(),
        vigencia = Vigencia("2026-10-20T02:41:00Z", "2027-10-20T02:41:00Z"),
    )
    private val hoy = LocalDate.of(2026, 11, 8)

    @Test
    @DisplayName("BITS-111, AC-2: hoy y cualquier dia desde la emision se pueden reportar")
    fun dentroDeLaVigencia() {
        assertTrue(poliza.admiteSiniestroEl(hoy, hoy))
        assertTrue(poliza.admiteSiniestroEl(LocalDate.of(2026, 10, 25), hoy))
    }

    @Test
    @DisplayName("BITS-111, AC-2: el dia de la emision cuenta en hora de Colombia")
    fun diaDeLaEmision() {
        assertTrue(poliza.admiteSiniestroEl(LocalDate.of(2026, 10, 19), hoy))
        assertFalse(poliza.admiteSiniestroEl(LocalDate.of(2026, 10, 18), hoy))
    }

    @Test
    @DisplayName("BITS-111, AC-2: no se reporta algo que paso en el futuro")
    fun futura() {
        assertFalse(poliza.admiteSiniestroEl(hoy.plusDays(1), hoy))
    }

    @Test
    @DisplayName("BITS-111, AC-2: no se reporta despues del fin de la vigencia")
    fun despuesDeLaVigencia() {
        val luegoDeVencer = LocalDate.of(2027, 12, 1)

        assertTrue(poliza.admiteSiniestroEl(LocalDate.of(2027, 10, 19), luegoDeVencer))
        assertFalse(poliza.admiteSiniestroEl(LocalDate.of(2027, 10, 20), luegoDeVencer))
    }

    @Test
    @DisplayName("una poliza sin emision o sin vigencia no admite reportes")
    fun sinFechas() {
        assertFalse(poliza.copy(emitidaEn = null).admiteSiniestroEl(hoy, hoy))
        assertFalse(poliza.copy(vigencia = null).admiteSiniestroEl(hoy, hoy))
    }

    @Test
    @DisplayName("BITS-111, AC-2A: En estudio y Falta informacion estan en curso; pagado y rechazado no")
    fun estadosEnCurso() {
        assertTrue(EstadoDeSiniestro.EN_ESTUDIO.enCurso)
        assertTrue(EstadoDeSiniestro.FALTA_INFORMACION.enCurso)
        assertFalse(EstadoDeSiniestro.PAGADO.enCurso)
        assertFalse(EstadoDeSiniestro.RECHAZADO.enCurso)
    }

    @Test
    @DisplayName("BITS-111, AC-2A: encuentra el siniestro en curso de esa poliza y esa cobertura")
    fun siniestroEnCurso() {
        val vida =
            SiniestroResumen("s1", "SIN-1", "poliza-1", TipoDeSiniestro.INCAPACIDAD_TOTAL, EstadoDeSiniestro.EN_ESTUDIO)
        val siniestros = listOf(
            SiniestroResumen("s0", "SIN-0", "poliza-1", TipoDeSiniestro.INCENDIO_TERREMOTO, EstadoDeSiniestro.PAGADO),
            vida,
            SiniestroResumen("s2", "SIN-2", "otra", TipoDeSiniestro.INCENDIO_TERREMOTO, EstadoDeSiniestro.EN_ESTUDIO),
            SiniestroResumen("s3", "SIN-3", "poliza-1", null, EstadoDeSiniestro.EN_ESTUDIO),
        )

        assertEquals(vida, siniestros.enCurso("poliza-1", TipoDeSiniestro.INCAPACIDAD_TOTAL))
        // Vida y Tu casa son independientes.
        assertNull(siniestros.enCurso("poliza-1", TipoDeSiniestro.INCENDIO_TERREMOTO))
    }
}
