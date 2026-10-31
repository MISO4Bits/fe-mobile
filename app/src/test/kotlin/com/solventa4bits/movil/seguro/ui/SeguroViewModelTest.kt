package com.solventa4bits.movil.seguro.ui

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
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SeguroViewModelTest {

    private val emitida =
        Solicitud("poliza-1", EstadoDeSolicitud.EMITIDA, "2026-10-22T14:41:00Z", null, emptyList(), emptyList())

    @BeforeEach
    fun preparar() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterEach
    fun restaurar() {
        Dispatchers.resetMain()
    }

    private class SolicitudesFalsas(private val documento: ResultadoDocumento) : ServicioDeSolicitudes {
        val pedidos = mutableListOf<String>()

        override suspend fun condiciones() = ResultadoCondiciones.Fallo
        override suspend fun enviar(datos: DatosDeLaSolicitud) = ResultadoEnvio.Fallo
        override suspend fun consultar(id: String) = ResultadoConsulta.Fallo
        override suspend fun listar() = ResultadoLista.Fallo
        override suspend fun documento(id: String): ResultadoDocumento {
            pedidos += id
            return documento
        }
    }

    private fun seguroQueResponde(documento: ResultadoDocumento) =
        SeguroViewModel(SolicitudesFalsas(documento)).apply { mostrar(emitida) }

    @Test
    @DisplayName("muestra la poliza que llego de la lista, sin pedir nada")
    fun muestraLaPoliza() {
        val solicitudes = SolicitudesFalsas(ResultadoDocumento.Fallo)
        val seguro = SeguroViewModel(solicitudes).apply { mostrar(emitida) }

        assertEquals(emitida, seguro.estado.seguro)
        assertTrue(solicitudes.pedidos.isEmpty())
    }

    @Test
    @DisplayName("con el documento listo deja su enlace para abrirlo una sola vez")
    fun documentoListo() {
        val seguro = seguroQueResponde(ResultadoDocumento.Listo("https://docs.example/poliza.pdf"))

        seguro.verDocumento()
        assertEquals("https://docs.example/poliza.pdf", seguro.estado.documentoPorAbrir)
        assertEquals(AvisoDeDocumento.NINGUNO, seguro.estado.aviso)

        seguro.documentoAbierto()
        assertNull(seguro.estado.documentoPorAbrir)
    }

    @Test
    @DisplayName("si se esta generando o falla, lo avisa sin abrir nada")
    fun documentoQueNoEsta() {
        val generando = seguroQueResponde(ResultadoDocumento.Generando).apply { verDocumento() }
        assertEquals(AvisoDeDocumento.GENERANDO, generando.estado.aviso)
        assertNull(generando.estado.documentoPorAbrir)

        val fallo = seguroQueResponde(ResultadoDocumento.Fallo).apply { verDocumento() }
        assertEquals(AvisoDeDocumento.FALLO, fallo.estado.aviso)
    }

    @Test
    @DisplayName("si la sesion vencio lo informa para volver al inicio de sesion")
    fun sesionVencida() {
        val seguro = seguroQueResponde(ResultadoDocumento.SesionVencida).apply { verDocumento() }

        assertTrue(seguro.estado.sesionVencida)
    }

    @Test
    @DisplayName("sin una poliza no pide el documento")
    fun sinPoliza() {
        val solicitudes = SolicitudesFalsas(ResultadoDocumento.Fallo)

        SeguroViewModel(solicitudes).verDocumento()

        assertTrue(solicitudes.pedidos.isEmpty())
    }

    @Test
    @DisplayName("BITS-217: sin conexion muestra lo guardado y el documento pide conexion sin llamar")
    fun sinConexion() {
        val solicitudes = SolicitudesFalsas(ResultadoDocumento.Fallo)
        val seguro = SeguroViewModel(solicitudes).apply { mostrar(emitida, guardadoEn = "2026-10-19T14:41:00Z") }

        seguro.verDocumento()

        assertEquals("2026-10-19T14:41:00Z", seguro.estado.guardadoEn)
        assertEquals(AvisoDeDocumento.SIN_CONEXION, seguro.estado.aviso)
        assertTrue(solicitudes.pedidos.isEmpty())
    }
}
