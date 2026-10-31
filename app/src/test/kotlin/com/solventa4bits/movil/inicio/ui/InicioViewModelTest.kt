package com.solventa4bits.movil.inicio.ui

import com.solventa4bits.movil.cotizacion.dominio.Cotizacion
import com.solventa4bits.movil.cotizacion.dominio.DatosDelCredito
import com.solventa4bits.movil.cotizacion.dominio.ResultadoCotizacion
import com.solventa4bits.movil.cotizacion.dominio.ResultadoDeclinacion
import com.solventa4bits.movil.cotizacion.dominio.ServicioDeCotizaciones
import com.solventa4bits.movil.seguro.dominio.AlmacenDeSeguros
import com.solventa4bits.movil.seguro.dominio.SegurosGuardados
import com.solventa4bits.movil.solicitud.dominio.DatosDeLaSolicitud
import com.solventa4bits.movil.solicitud.dominio.EstadoDeSolicitud
import com.solventa4bits.movil.solicitud.dominio.ResultadoCondiciones
import com.solventa4bits.movil.solicitud.dominio.ResultadoConsulta
import com.solventa4bits.movil.solicitud.dominio.ResultadoDocumento
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
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.Instant

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

    private val guardados = SegurosEnMemoria()
    private val ahora = Instant.parse("2026-10-19T14:41:00Z")

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
            override suspend fun documento(id: String) = ResultadoDocumento.Fallo
        }
        return InicioViewModel(cotizaciones, solicitudes, guardados) { ahora }.apply { consultar() }
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
    @DisplayName("BITS-104, AC-3: las polizas emitidas van en Mis seguros y no como solicitud")
    fun segurosEmitidos() {
        val lista = ResultadoLista.Exito(
            listOf(
                solicitud("emitida", EstadoDeSolicitud.EMITIDA),
                solicitud("rechazada", EstadoDeSolicitud.RECHAZADA),
                solicitud("otra", EstadoDeSolicitud.EMITIDA),
            ),
        )

        val estado = inicioQueRecibe(ResultadoCotizacion.SinCotizacion, lista).estado

        assertEquals(listOf("emitida", "otra"), estado.seguros.map { it.id })
        assertNull(estado.solicitudReciente)
    }

    @Test
    @DisplayName("una cotizacion que ya va en una solicitud pendiente o emitida no se muestra")
    fun cotizacionYaUsada() {
        val usada = vigente.copy(creadaEn = "2026-10-27T14:00:00Z")
        fun conUna(estado: EstadoDeSolicitud) = inicioQueRecibe(
            ResultadoCotizacion.Exito(usada),
            ResultadoLista.Exito(listOf(solicitud("s", estado).copy(cotizacionCreadaEn = usada.creadaEn))),
        ).estado.cotizacionEnCurso

        assertNull(conUna(EstadoDeSolicitud.EMITIDA))
        assertNull(conUna(EstadoDeSolicitud.PENDIENTE))
        assertEquals(usada, conUna(EstadoDeSolicitud.RECHAZADA))
    }

    @Test
    @DisplayName("si la sesion vencio al listar las solicitudes tambien lo informa")
    fun sesionVencidaAlListar() {
        val inicio = inicioQueRecibe(ResultadoCotizacion.SinCotizacion, ResultadoLista.SesionVencida)

        assertTrue(inicio.estado.sesionVencida)
    }

    @Test
    @DisplayName("BITS-217, AC-1 y AC-4: guarda las polizas emitidas que llegan y cuando llegaron")
    fun guardaLasEmitidas() {
        val lista = ResultadoLista.Exito(
            listOf(
                solicitud("emitida", EstadoDeSolicitud.EMITIDA),
                solicitud("pendiente", EstadoDeSolicitud.PENDIENTE),
            ),
        )

        val estado = inicioQueRecibe(ResultadoCotizacion.SinCotizacion, lista).estado

        assertEquals(listOf("emitida"), guardados.guardado?.seguros?.map { it.id })
        assertEquals("2026-10-19T14:41:00Z", guardados.guardado?.actualizadosEn)
        assertFalse(estado.sinConexion)
        assertNull(estado.actualizadosEn)
    }

    @Test
    @DisplayName("BITS-217, AC-2: si nada llega, no hay conexion y se ven las polizas guardadas")
    fun sinConexionMuestraLoGuardado() {
        guardados.guardado = SegurosGuardados(listOf(solicitud("emitida", EstadoDeSolicitud.EMITIDA)), ANTES)

        val estado = inicioQueRecibe(ResultadoCotizacion.Fallo, ResultadoLista.Fallo).estado

        assertTrue(estado.sinConexion)
        assertEquals(listOf("emitida"), estado.seguros.map { it.id })
        assertEquals(ANTES, estado.actualizadosEn)
    }

    @Test
    @DisplayName("si solo falla la lista, se ven las polizas guardadas pero no es falta de conexion")
    fun soloFallaLaLista() {
        guardados.guardado = SegurosGuardados(listOf(solicitud("emitida", EstadoDeSolicitud.EMITIDA)), ANTES)

        val estado = inicioQueRecibe(ResultadoCotizacion.SinCotizacion, ResultadoLista.Fallo).estado

        assertFalse(estado.sinConexion)
        assertEquals(listOf("emitida"), estado.seguros.map { it.id })
    }

    @Test
    @DisplayName("sin conexion y sin nada guardado, no hay seguros ni fecha")
    fun sinConexionNiGuardados() {
        val estado = inicioQueRecibe(ResultadoCotizacion.Fallo, ResultadoLista.Fallo).estado

        assertTrue(estado.sinConexion)
        assertTrue(estado.seguros.isEmpty())
        assertNull(estado.actualizadosEn)
    }

    @Test
    @DisplayName("BITS-217, AC-6: al cerrar sesion borra las polizas guardadas")
    fun olvidarSeguros() {
        val inicio = inicioQueRecibe(
            ResultadoCotizacion.SinCotizacion,
            ResultadoLista.Exito(listOf(solicitud("emitida", EstadoDeSolicitud.EMITIDA))),
        )

        inicio.olvidarSeguros()

        assertNull(guardados.guardado)
        assertTrue(inicio.estado.seguros.isEmpty())
    }

    private class SegurosEnMemoria : AlmacenDeSeguros {
        var guardado: SegurosGuardados? = null

        override suspend fun guardar(seguros: List<Solicitud>, en: String) {
            guardado = SegurosGuardados(seguros, en)
        }

        override suspend fun leer() = guardado

        override suspend fun borrar() {
            guardado = null
        }
    }

    private companion object {
        const val ANTES = "2026-10-18T10:00:00Z"
    }
}
