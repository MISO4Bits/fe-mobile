package com.solventa4bits.movil.cotizacion.ui

import com.solventa4bits.movil.cotizacion.dominio.CreditoHipotecario
import com.solventa4bits.movil.cotizacion.dominio.CreditosHipotecarios
import com.solventa4bits.movil.cotizacion.dominio.EntidadFinanciera
import com.solventa4bits.movil.cotizacion.dominio.EstadoCreditos
import com.solventa4bits.movil.cotizacion.dominio.ResultadoCreditos
import com.solventa4bits.movil.cotizacion.dominio.ServicioDeCreditos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NuevaCotizacionViewModelTest {

    private val bancolombia = EntidadFinanciera("bancolombia", "Bancolombia")
    private val davivienda = EntidadFinanciera("davivienda", "Davivienda")
    private val hipoteca = CreditoHipotecario(
        entidadId = "bancolombia",
        entidadNombre = "BANCOLOMBIA S.A.",
        valorCredito = 180_000_000.0,
        saldoInsoluto = 98_000_000.0,
        plazoRestanteMeses = 96,
        cuotaMensual = 1_650_000.0,
    )
    private val conHipoteca = ResultadoCreditos.Exito(
        CreditosHipotecarios(EstadoCreditos.DISPONIBLE, listOf(hipoteca), listOf(bancolombia, davivienda)),
    )

    /** Doble de prueba: responde lo que tenga en [resultado] en ese momento. */
    private class ServicioFalso(var resultado: ResultadoCreditos) : ServicioDeCreditos {
        override suspend fun consultar() = resultado
    }

    @BeforeEach
    fun preparar() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterEach
    fun restaurar() {
        Dispatchers.resetMain()
    }

    @Test
    @DisplayName("antes de consultar muestra que esta cargando")
    fun empiezaCargando() {
        val viewModel = NuevaCotizacionViewModel(ServicioFalso(conHipoteca))

        assertTrue(viewModel.estado.cargando)
    }

    @Test
    @DisplayName("al consultar deja listos los bancos y aun no hay banco elegido")
    fun consultaLosBancos() {
        val viewModel = NuevaCotizacionViewModel(ServicioFalso(conHipoteca))

        viewModel.consultar()

        assertFalse(viewModel.estado.cargando)
        assertEquals(listOf(bancolombia, davivienda), viewModel.estado.entidades)
        assertNull(viewModel.estado.entidadElegida)
        assertNull(viewModel.estado.creditoElegido)
    }

    @Test
    @DisplayName("al elegir el banco de la hipoteca muestra sus datos")
    fun eligeElBancoDeLaHipoteca() {
        val viewModel = NuevaCotizacionViewModel(ServicioFalso(conHipoteca))
        viewModel.consultar()

        viewModel.elegirEntidad(bancolombia)

        assertEquals(hipoteca, viewModel.estado.creditoElegido)
    }

    @Test
    @DisplayName("al elegir un banco donde no tiene hipoteca no hay datos que mostrar")
    fun eligeOtroBanco() {
        val viewModel = NuevaCotizacionViewModel(ServicioFalso(conHipoteca))
        viewModel.consultar()

        viewModel.elegirEntidad(davivienda)

        assertEquals(davivienda, viewModel.estado.entidadElegida)
        assertNull(viewModel.estado.creditoElegido)
    }

    @Test
    @DisplayName("si la consulta falla lo informa y al reintentar se recupera")
    fun falloYReintento() {
        val servicio = ServicioFalso(ResultadoCreditos.Fallo)
        val viewModel = NuevaCotizacionViewModel(servicio)
        viewModel.consultar()
        assertTrue(viewModel.estado.fallo)

        servicio.resultado = conHipoteca
        viewModel.consultar()

        assertFalse(viewModel.estado.fallo)
        assertEquals(listOf(bancolombia, davivienda), viewModel.estado.entidades)
    }

    @Test
    @DisplayName("si la sesion vencio lo informa para volver al inicio de sesion")
    fun sesionVencida() {
        val viewModel = NuevaCotizacionViewModel(ServicioFalso(ResultadoCreditos.SesionVencida))

        viewModel.consultar()

        assertTrue(viewModel.estado.sesionVencida)
    }
}
