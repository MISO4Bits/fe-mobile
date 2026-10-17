package com.solventa4bits.movil.cotizacion.ui

import com.solventa4bits.movil.cotizacion.dominio.Cotizacion
import com.solventa4bits.movil.cotizacion.dominio.DatosDelCredito
import com.solventa4bits.movil.cotizacion.dominio.EfectoEnElPrecio
import com.solventa4bits.movil.cotizacion.dominio.FactorDelPrecio
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class PorQueEstePrecioTest {

    private val historial = FactorDelPrecio("Historial de pagos excelente", EfectoEnElPrecio.BAJA, 0.1)
    private val endeudamiento = FactorDelPrecio("Endeudamiento bajo", EfectoEnElPrecio.BAJA, 0.2)
    private val sinPeso = FactorDelPrecio("Perfil parcial", EfectoEnElPrecio.SUBE, null)

    private fun cotizacionCon(vararg factores: FactorDelPrecio) = Cotizacion(
        vigente = true,
        datosCredito = DatosDelCredito("Bancolombia", 250_000_000, 238_500_000, 180),
        datosDeclarados = true,
        primaMensual = 39_734.0,
        primaBaseMensual = 46_746.0,
        sumaAsegurada = 238_500_000.0,
        coberturaMeses = 180,
        personalizada = true,
        factores = factores.toList(),
    )

    @Test
    @DisplayName("ordena los factores de mayor a menor peso, con los que no traen peso al final")
    fun ordenaPorPeso() {
        val cotizacion = cotizacionCon(sinPeso, historial, endeudamiento)

        assertEquals(listOf(endeudamiento, historial, sinPeso), cotizacion.factoresPorPeso())
    }

    @Test
    @DisplayName("senala como lo que mas pesa al factor de mayor peso")
    fun loQueMasPesa() {
        assertEquals(endeudamiento, cotizacionCon(historial, endeudamiento).factorQueMasPesa())
    }

    @Test
    @DisplayName("con un solo factor, o sin pesos, no senala ninguno")
    fun sinComparacion() {
        assertNull(cotizacionCon(historial).factorQueMasPesa())
        assertNull(cotizacionCon(sinPeso, sinPeso.copy(descripcion = "Otro")).factorQueMasPesa())
    }
}
