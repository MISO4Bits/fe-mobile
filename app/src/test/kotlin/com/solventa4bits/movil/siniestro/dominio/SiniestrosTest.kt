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
        assertNull(poliza.problemaConLaFecha(hoy, hoy))
        assertNull(poliza.problemaConLaFecha(LocalDate.of(2026, 10, 25), hoy))
    }

    @Test
    @DisplayName("BITS-111, AC-2: el dia de la emision cuenta en hora de Colombia")
    fun diaDeLaEmision() {
        assertNull(poliza.problemaConLaFecha(LocalDate.of(2026, 10, 19), hoy))
        assertEquals(
            ProblemaDeFecha.ANTES_DE_LA_POLIZA,
            poliza.problemaConLaFecha(LocalDate.of(2026, 10, 18), hoy),
        )
    }

    @Test
    @DisplayName("BITS-111, AC-2: no se reporta algo que paso en el futuro")
    fun futura() {
        assertEquals(ProblemaDeFecha.FUTURA, poliza.problemaConLaFecha(hoy.plusDays(1), hoy))
    }

    @Test
    @DisplayName("BITS-111, AC-2: no se reporta despues del fin de la vigencia")
    fun despuesDeLaVigencia() {
        val luegoDeVencer = LocalDate.of(2027, 12, 1)

        assertNull(poliza.problemaConLaFecha(LocalDate.of(2027, 10, 19), luegoDeVencer))
        assertEquals(
            ProblemaDeFecha.DESPUES_DE_LA_POLIZA,
            poliza.problemaConLaFecha(LocalDate.of(2027, 10, 20), luegoDeVencer),
        )
    }

    @Test
    @DisplayName("BITS-111, AC-2: los dias cubiertos van de la emision al fin de la vigencia, en Colombia")
    fun diasCubiertos() {
        assertEquals(LocalDate.of(2026, 10, 19)..LocalDate.of(2027, 10, 19), poliza.diasCubiertos())
    }

    @Test
    @DisplayName("una poliza sin emision o sin vigencia no admite reportes")
    fun sinFechas() {
        assertNull(poliza.copy(emitidaEn = null).diasCubiertos())
        assertEquals(ProblemaDeFecha.ANTES_DE_LA_POLIZA, poliza.copy(vigencia = null).problemaConLaFecha(hoy, hoy))
    }

    @Test
    @DisplayName("BITS-111, AC-1: el sismo automatico y el incendio son de Tu casa; la incapacidad, de Vida")
    fun cobertura() {
        assertFalse(TipoDeSiniestro.INCAPACIDAD_TOTAL.deTuCasa)
        assertTrue(TipoDeSiniestro.INCENDIO_TERREMOTO.deTuCasa)
        assertTrue(TipoDeSiniestro.SISMO_PARAMETRICO.deTuCasa)
    }

    @Test
    @DisplayName("la cuenta del pago necesita banco, tipo y un numero de 4 a 20 digitos")
    fun destinoDePago() {
        val cuenta = DestinoDePago.desde(" Bancolombia ", TipoDeCuenta.AHORROS, "48210012345")

        assertEquals(DestinoDePago.Transferencia("Bancolombia", TipoDeCuenta.AHORROS, "48210012345"), cuenta)
        assertEquals("2345", cuenta?.terminadaEn)
        assertNull(DestinoDePago.desde("", TipoDeCuenta.AHORROS, "48210012345"))
        assertNull(DestinoDePago.desde("Bancolombia", null, "48210012345"))
        assertNull(DestinoDePago.desde("Bancolombia", TipoDeCuenta.CORRIENTE, "123"))
        assertNull(DestinoDePago.desde("Bancolombia", TipoDeCuenta.CORRIENTE, "1".repeat(21)))
        assertNull(DestinoDePago.desde("Bancolombia", TipoDeCuenta.CORRIENTE, "12a4"))
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
