package com.solventa4bits.movil.cotizacion.dominio

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/** Los valores son el ejemplo de BITS-96: credito de $320.000.000. */
class CoberturasTest {

    private val vida = PrecioDeCobertura(CodigoCobertura.VIDA, 320_000_000.0, 49_100.0, 0.0, 49_100.0)
    private val hogar = PrecioDeCobertura(CodigoCobertura.HOGAR, 240_000_000.0, 23_760.0, 4_514.0, 28_274.0)

    private fun cotizacionCon(coberturas: List<PrecioDeCobertura>) = Cotizacion(
        vigente = true,
        datosCredito = DatosDelCredito("Bancolombia", 320_000_000, 320_000_000, 240),
        datosDeclarados = true,
        primaMensual = 49_100.0,
        primaBaseMensual = 49_100.0,
        sumaAsegurada = 320_000_000.0,
        coberturaMeses = 240,
        personalizada = false,
        factores = emptyList(),
        coberturas = coberturas,
    )

    @Test
    @DisplayName("con Tu casa suma la vida y la casa: subtotal sin IVA, el IVA de la casa y el total")
    fun conTuCasa() {
        assertEquals(
            PlanElegido(vida = 49_100.0, tuCasa = 23_760.0, subtotal = 72_860.0, iva = 4_514.0, total = 77_374.0),
            cotizacionCon(listOf(vida, hogar)).plan(conTuCasa = true),
        )
    }

    @Test
    @DisplayName("solo vida: el total es la vida y el IVA es cero")
    fun soloVida() {
        assertEquals(
            PlanElegido(vida = 49_100.0, tuCasa = null, subtotal = 49_100.0, iva = 0.0, total = 49_100.0),
            cotizacionCon(listOf(vida, hogar)).plan(conTuCasa = false),
        )
    }

    @Test
    @DisplayName("sin desglose del backend la vida es la prima mensual y no hay Tu casa que ofrecer")
    fun sinDesglose() {
        val cotizacion = cotizacionCon(emptyList())

        assertNull(cotizacion.tuCasa)
        assertEquals(49_100.0, cotizacion.plan(conTuCasa = true).total)
    }
}
