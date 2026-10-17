package com.solventa4bits.movil.inicio.ui

import com.solventa4bits.movil.cotizacion.dominio.Cotizacion
import com.solventa4bits.movil.cotizacion.dominio.DatosDelCredito
import com.solventa4bits.movil.cotizacion.dominio.ResultadoCotizacion
import com.solventa4bits.movil.cotizacion.dominio.ServicioDeCotizaciones
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InicioViewModelTest {

    private val vigente = Cotizacion(
        vigente = true,
        datosCredito = DatosDelCredito("Bancolombia", 280_000_000, 231_000_000, 190),
        datosDeclarados = true,
        primaMensual = 21_945.0,
        primaBaseMensual = 21_945.0,
        sumaAsegurada = 231_000_000.0,
        coberturaMeses = 190,
        personalizada = false,
        factores = emptyList(),
    )

    @BeforeEach
    fun preparar() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterEach
    fun restaurar() {
        Dispatchers.resetMain()
    }

    private fun inicioQueRecibe(resultado: ResultadoCotizacion): InicioViewModel {
        val cotizaciones = object : ServicioDeCotizaciones {
            override suspend fun cotizar(datos: DatosDelCredito) = ResultadoCotizacion.Fallo
            override suspend fun consultarActual() = resultado
        }
        return InicioViewModel(cotizaciones).apply { consultar() }
    }

    @Test
    @DisplayName("con una cotizacion vigente el Home la muestra para retomarla")
    fun cotizacionEnCurso() {
        assertEquals(vigente, inicioQueRecibe(ResultadoCotizacion.Exito(vigente)).estado.cotizacionEnCurso)
    }

    @Test
    @DisplayName("una cotizacion vencida se trata como si no hubiera")
    fun cotizacionVencida() {
        val vencida = vigente.copy(vigente = false)

        assertNull(inicioQueRecibe(ResultadoCotizacion.Exito(vencida)).estado.cotizacionEnCurso)
    }

    @Test
    @DisplayName("sin cotizacion, o si la consulta falla, el Home ofrece cotizar")
    fun sinCotizacion() {
        assertNull(inicioQueRecibe(ResultadoCotizacion.SinCotizacion).estado.cotizacionEnCurso)
        assertNull(inicioQueRecibe(ResultadoCotizacion.Fallo).estado.cotizacionEnCurso)
    }

    @Test
    @DisplayName("si la sesion vencio lo informa para volver al inicio de sesion")
    fun sesionVencida() {
        assertTrue(inicioQueRecibe(ResultadoCotizacion.SesionVencida).estado.sesionVencida)
    }
}
