package com.solventa4bits.movil.inicio.ui

import com.solventa4bits.movil.cotizacion.dominio.Cotizacion
import com.solventa4bits.movil.cotizacion.dominio.DatosDelCredito
import com.solventa4bits.movil.cotizacion.dominio.ResultadoCotizacion
import com.solventa4bits.movil.cotizacion.dominio.ResultadoDeclinacion
import com.solventa4bits.movil.cotizacion.dominio.ServicioDeCotizaciones
import com.solventa4bits.movil.solicitud.dominio.DatosDeLaSolicitud
import com.solventa4bits.movil.solicitud.dominio.EstadoDeSolicitud
import com.solventa4bits.movil.solicitud.dominio.ResultadoCondiciones
import com.solventa4bits.movil.solicitud.dominio.ResultadoConsulta
import com.solventa4bits.movil.solicitud.dominio.ResultadoEnvio
import com.solventa4bits.movil.solicitud.dominio.ResultadoLista
import com.solventa4bits.movil.solicitud.dominio.ServicioDeSolicitudes
import com.solventa4bits.movil.solicitud.dominio.Solicitud
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

    private fun solicitud(id: String, estado: EstadoDeSolicitud) =
        Solicitud(id, estado, "2026-10-27T14:41:00Z", null, emptyList(), emptyList())

    @BeforeEach
    fun preparar() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterEach
    fun restaurar() {
        Dispatchers.resetMain()
    }

    private fun inicioQueRecibe(
        resultado: ResultadoCotizacion,
        lista: ResultadoLista = ResultadoLista.Exito(emptyList()),
    ): InicioViewModel {
        val cotizaciones = object : ServicioDeCotizaciones {
            override suspend fun cotizar(datos: DatosDelCredito) = ResultadoCotizacion.Fallo
            override suspend fun consultarActual() = resultado
            override suspend fun declinarActual() = ResultadoDeclinacion.Fallo
        }
        val solicitudes = object : ServicioDeSolicitudes {
            override suspend fun condiciones() = ResultadoCondiciones.Fallo
            override suspend fun enviar(datos: DatosDeLaSolicitud) = ResultadoEnvio.Fallo
            override suspend fun consultar(id: String) = ResultadoConsulta.Fallo
            override suspend fun listar() = lista
        }
        return InicioViewModel(cotizaciones, solicitudes).apply { consultar() }
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

    @Test
    @DisplayName("BITS-97, AC-2: con solicitudes, el Home ofrece volver a ver la mas reciente")
    fun solicitudReciente() {
        val lista = ResultadoLista.Exito(
            listOf(solicitud("nueva", EstadoDeSolicitud.PENDIENTE), solicitud("vieja", EstadoDeSolicitud.RECHAZADA)),
        )

        val estado = inicioQueRecibe(ResultadoCotizacion.Exito(vigente), lista).estado

        assertEquals("nueva", estado.solicitudReciente?.id)
        assertEquals(vigente, estado.cotizacionEnCurso)
    }

    @Test
    @DisplayName("sin solicitudes, o si la mas reciente vencio, el Home no la muestra")
    fun sinSolicitudVigente() {
        assertNull(inicioQueRecibe(ResultadoCotizacion.SinCotizacion).estado.solicitudReciente)
        val vencida = ResultadoLista.Exito(listOf(solicitud("vieja", EstadoDeSolicitud.VENCIDA)))
        assertNull(inicioQueRecibe(ResultadoCotizacion.SinCotizacion, vencida).estado.solicitudReciente)
    }

    @Test
    @DisplayName("si la lista de solicitudes falla, el Home se ve igual que sin solicitudes")
    fun listaQueFalla() {
        val estado = inicioQueRecibe(ResultadoCotizacion.Exito(vigente), ResultadoLista.Fallo).estado

        assertNull(estado.solicitudReciente)
        assertEquals(vigente, estado.cotizacionEnCurso)
    }

    @Test
    @DisplayName("si la sesion vencio al listar las solicitudes tambien lo informa")
    fun sesionVencidaAlListar() {
        val inicio = inicioQueRecibe(ResultadoCotizacion.SinCotizacion, ResultadoLista.SesionVencida)

        assertTrue(inicio.estado.sesionVencida)
    }
}
