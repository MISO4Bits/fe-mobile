package com.solventa4bits.movil.siniestro.ui

import com.solventa4bits.movil.siniestro.dominio.Asistencia
import com.solventa4bits.movil.siniestro.dominio.CatalogoDeSiniestros
import com.solventa4bits.movil.siniestro.dominio.DetalleDeSiniestro
import com.solventa4bits.movil.siniestro.dominio.EstadoDeSiniestro
import com.solventa4bits.movil.siniestro.dominio.Localidad
import com.solventa4bits.movil.siniestro.dominio.OrigenDeAyuda
import com.solventa4bits.movil.siniestro.dominio.Prestador
import com.solventa4bits.movil.siniestro.dominio.PuntoDeAyuda
import com.solventa4bits.movil.siniestro.dominio.ResultadoAyuda
import com.solventa4bits.movil.siniestro.dominio.ResultadoCatalogo
import com.solventa4bits.movil.siniestro.dominio.SiniestroResumen
import com.solventa4bits.movil.siniestro.dominio.TipoDeAyuda
import com.solventa4bits.movil.siniestro.dominio.TipoDeSiniestro
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
class AyudaViewModelTest {

    private val plomeria = TipoDeAyuda("plomeria", "Plomería", 1)
    private val chapinero = Localidad("chapinero", "Chapinero", "Bogotá", 4.649, -74.063)
    private val cerca = Prestador("cerca", "Cerca", listOf("plomeria"), "chapinero", "Calle 1", 4.650, -74.064, 20)
    private val lejos = Prestador("lejos", "Lejos", listOf("plomeria"), "kennedy", "Calle 2", 4.628, -74.155, 50)
    private val catalogo = CatalogoDeSiniestros(
        tiposAyuda = listOf(plomeria),
        localidades = listOf(chapinero),
        prestadores = listOf(lejos, cerca),
    )
    private val fotos = Ubicacion(4.65, -74.06)
    private val asistencia = Asistencia(
        "a1", "ASI-1", "plomeria", "Cerca", 20, OrigenDeAyuda.FOTOS, null, null, "2026-11-12T14:30:00Z",
    )

    private val servicio = SiniestrosFalsos().apply { alPedirCatalogo = ResultadoCatalogo.Exito(catalogo) }
    private var donde: Ubicacion? = null
    private var llaves = 0

    @BeforeEach
    fun preparar() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterEach
    fun restaurar() {
        Dispatchers.resetMain()
    }

    private fun ayuda() = AyudaViewModel(servicio, { donde }) { "llave-${++llaves}" }

    private fun siniestro(ubicacionDeLasFotos: Ubicacion? = fotos) = DetalleDeSiniestro(
        SiniestroResumen("s1", "SIN-1", "p1", TipoDeSiniestro.INCENDIO_TERREMOTO, EstadoDeSiniestro.EN_ESTUDIO),
        "2026-11-02",
        null,
        0,
        1,
        ubicacionDeLasFotos = ubicacionDeLasFotos,
    )

    @Test
    @DisplayName("BITS-239, AC-3 y AC-5: va a donde se tomaron las fotos; los prestadores, del mas cercano")
    fun dondeLasFotos() {
        val vm = ayuda()

        vm.empezar(siniestro())
        vm.elegirTipo(plomeria)

        assertEquals(PuntoDeAyuda.DondeTomoLasFotos(fotos), vm.estado.punto)
        assertEquals(listOf(cerca, lejos), vm.estado.prestadores.map { it.prestador })
    }

    @Test
    @DisplayName("el catalogo se trae una sola vez y, si falla, se reintenta")
    fun catalogoUnaVez() {
        servicio.alPedirCatalogo = ResultadoCatalogo.Fallo
        val vm = ayuda()
        vm.empezar(siniestro())
        assertTrue(vm.estado.fallo)

        servicio.alPedirCatalogo = ResultadoCatalogo.Exito(catalogo)
        vm.cargar()
        vm.empezar(siniestro())

        assertEquals(listOf(plomeria), vm.estado.catalogo.tiposEnOrden)
        assertEquals(2, servicio.catalogosPedidos)
    }

    @Test
    @DisplayName("BITS-239, AC-4: estando en la casa, va a donde esta el telefono")
    fun dondeElTelefono() {
        donde = Ubicacion(4.6, -74.1)
        var ubicada = false
        val vm = ayuda()
        vm.empezar(siniestro(ubicacionDeLasFotos = null))
        assertNull(vm.estado.punto)

        vm.usarUbicacionDelTelefono { ubicada = true }

        assertTrue(ubicada)
        assertEquals(PuntoDeAyuda.DondeEstaElTelefono(Ubicacion(4.6, -74.1)), vm.estado.punto)
    }

    @Test
    @DisplayName("BITS-239, AC-4: sin la ubicacion del telefono, se escribe la direccion y la localidad")
    fun direccionEscrita() {
        var ubicada = false
        val vm = ayuda()
        vm.empezar(siniestro(ubicacionDeLasFotos = null))

        vm.usarUbicacionDelTelefono { ubicada = true }
        assertFalse(ubicada)
        assertTrue(vm.estado.sinUbicacion)
        vm.cambiarDireccion(DireccionEscrita("Calle 63 # 11-40 "))
        vm.usarDireccion()
        assertNull(vm.estado.punto)
        vm.cambiarDireccion(vm.estado.escrita.copy(localidad = chapinero))
        vm.usarDireccion()

        assertEquals(PuntoDeAyuda.EnUnaDireccion("Calle 63 # 11-40", chapinero), vm.estado.punto)
    }

    @Test
    @DisplayName("BITS-239, AC-6 y AC-7: pide la ayuda y el siniestro queda con ella")
    fun pide() {
        servicio.alPedirAyuda = ResultadoAyuda.Pedida(asistencia)
        var pedida = false
        val vm = ayuda()
        vm.empezar(siniestro())
        vm.elegirTipo(plomeria)

        vm.pedir(cerca) { pedida = true }

        assertTrue(pedida)
        val (id, solicitud, llave) = servicio.ayudas.single()
        assertEquals("s1", id)
        assertEquals(cerca, solicitud.prestador)
        assertEquals("llave-1", llave)
        assertEquals(listOf(asistencia), vm.estado.siniestroConLaAyuda?.asistencias)
    }

    @Test
    @DisplayName("BITS-239, AC-6: si falla, reintenta con la misma llave; otro prestador lleva otra")
    fun mismaLlave() {
        val vm = ayuda()
        vm.empezar(siniestro())
        vm.elegirTipo(plomeria)

        vm.pedir(cerca) {}
        assertTrue(vm.estado.falloAlPedir)
        vm.pedir(cerca) {}
        vm.pedir(lejos) {}

        assertEquals(listOf("llave-1", "llave-1", "llave-2"), servicio.ayudas.map { it.third })
    }

    @Test
    @DisplayName("BITS-239: si el siniestro ya no admite ayuda lo dice; sin tipo no se pide")
    fun noSePuede() {
        servicio.alPedirAyuda = ResultadoAyuda.NoSePuede
        val vm = ayuda()
        vm.empezar(siniestro())
        vm.pedir(cerca) {}
        assertTrue(servicio.ayudas.isEmpty())

        vm.elegirTipo(plomeria)
        vm.pedir(cerca) {}

        assertTrue(vm.estado.noSePuede)
        servicio.alPedirAyuda = ResultadoAyuda.SesionVencida
        vm.pedir(cerca) {}
        assertTrue(vm.estado.sesionVencida)
    }

    @Test
    @DisplayName("una sesion vencida al traer el catalogo se avisa")
    fun sesionVencidaEnElCatalogo() {
        servicio.alPedirCatalogo = ResultadoCatalogo.SesionVencida
        val vm = ayuda()

        vm.empezar(siniestro())

        assertTrue(vm.estado.sesionVencida)
        assertTrue(vm.estado.prestadores.isEmpty())
    }
}
