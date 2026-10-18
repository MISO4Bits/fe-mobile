package com.solventa4bits.movil.cotizacion.ui

import com.solventa4bits.movil.cotizacion.dominio.Cotizacion
import com.solventa4bits.movil.cotizacion.dominio.DatosDelCredito
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class VarianteDeCotizacionTest {

    @ParameterizedTest(name = "declarados={0}, personalizada={1} -> {2}")
    @CsvSource(
        "false, true,  PREAPROBADA",
        "true,  true,  DECLARADA_CON_PERFIL",
        "true,  false, DECLARADA_SIN_PERFIL",
        "false, false, VERIFICADA_SIN_PERFIL",
    )
    fun variantePorOrigenYPerfil(declarados: Boolean, personalizada: Boolean, esperada: VarianteDeCotizacion) {
        val cotizacion = Cotizacion(
            vigente = true,
            datosCredito = DatosDelCredito("Bancolombia", 250_000_000, 238_500_000, 180),
            datosDeclarados = declarados,
            primaMensual = 39_734.0,
            primaBaseMensual = 46_746.0,
            sumaAsegurada = 238_500_000.0,
            coberturaMeses = 180,
            personalizada = personalizada,
            factores = emptyList(),
        )

        assertEquals(esperada, cotizacion.variante())
    }

    @Test
    @DisplayName("sin autorizacion financiera la cotizacion a tarifa estandar lo dice, y no promete un perfil")
    fun sinAutorizacion() {
        val estandar = Cotizacion(
            vigente = true,
            datosCredito = DatosDelCredito("Bancolombia", 250_000_000, 238_500_000, 180),
            datosDeclarados = true,
            primaMensual = 46_746.0,
            primaBaseMensual = 46_746.0,
            sumaAsegurada = 238_500_000.0,
            coberturaMeses = 180,
            personalizada = false,
            factores = emptyList(),
        )

        assertEquals(VarianteDeCotizacion.SIN_AUTORIZACION, estandar.variante(sinAutorizacion = true))
        assertEquals(VarianteDeCotizacion.DECLARADA_SIN_PERFIL, estandar.variante(sinAutorizacion = false))
        assertEquals(
            VarianteDeCotizacion.DECLARADA_CON_PERFIL,
            estandar.copy(personalizada = true).variante(sinAutorizacion = true),
        )
    }
}
