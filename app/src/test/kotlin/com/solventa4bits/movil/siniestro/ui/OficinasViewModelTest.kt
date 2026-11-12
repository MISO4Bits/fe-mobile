package com.solventa4bits.movil.siniestro.ui

import com.solventa4bits.movil.siniestro.dominio.CatalogoDeSiniestros
import com.solventa4bits.movil.siniestro.dominio.Oficina
import com.solventa4bits.movil.siniestro.dominio.ResultadoCatalogo
import com.solventa4bits.movil.siniestro.dominio.Ubicacion
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
class OficinasViewModelTest {

    private val chapinero =
        Oficina("b1", "Bancolombia", "Oficina Chapinero", "Calle 57 # 9-20", "Bogotá", 4.6486, -74.0628)
    private val salitre =
        Oficina("b2", "Bancolombia", "Oficina Salitre", "Av. Calle 26 # 69-76", "Bogotá", 4.6584, -74.1103)
    private val servicio = SiniestrosFalsos()
    private var donde: Ubicacion? = Ubicacion(4.6497, -74.0631)

    @BeforeEach
    fun preparar() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterEach
    fun restaurar() {
        Dispatchers.resetMain()
    }

    private fun oficinas() = OficinasViewModel(servicio) { donde }

    @Test
    @DisplayName("BITS-116, AC-4: con la ubicacion, propone la mas cercana")
    fun conUbicacion() {
        servicio.alPedirCatalogo = ResultadoCatalogo.Exito(CatalogoDeSiniestros(oficinas = listOf(salitre, chapinero)))
        val vm = oficinas()

        vm.cargar()

        assertFalse(vm.estado.cargando)
        assertEquals(chapinero, vm.estado.paraElegir("Bancolombia").first().oficina)
    }

    @Test
    @DisplayName("BITS-116, AC-4: sin la ubicacion, se elige la ciudad")
    fun sinUbicacion() {
        donde = null
        servicio.alPedirCatalogo = ResultadoCatalogo.Exito(CatalogoDeSiniestros(oficinas = listOf(salitre, chapinero)))
        val vm = oficinas()
        vm.cargar()
        assertNull(vm.estado.ubicacion)
        assertTrue(vm.estado.paraElegir("Bancolombia").isEmpty())

        vm.elegirCiudad("Bogotá")

        assertEquals(listOf(chapinero, salitre), vm.estado.paraElegir("Bancolombia").map { it.oficina })
    }

    @Test
    @DisplayName("las oficinas se traen una vez; la ubicacion, cada vez que se abre")
    fun soloUnaVez() {
        servicio.alPedirCatalogo = ResultadoCatalogo.Exito(CatalogoDeSiniestros(oficinas = listOf(chapinero)))
        val vm = oficinas()
        donde = null
        vm.cargar()
        donde = Ubicacion(4.6497, -74.0631)
        vm.cargar()

        assertEquals(1, servicio.catalogosPedidos)
        assertEquals(donde, vm.estado.ubicacion)
    }

    @Test
    @DisplayName("si falla lo dice; con la sesion vencida lo marca")
    fun fallos() {
        val vm = oficinas()
        vm.cargar()
        assertTrue(vm.estado.fallo)

        servicio.alPedirCatalogo = ResultadoCatalogo.SesionVencida
        vm.cargar()
        assertTrue(vm.estado.sesionVencida)
        assertFalse(vm.estado.fallo)
    }
}
