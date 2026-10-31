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
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/** BITS-229: tocar el aviso de poliza emitida abre "Tu seguro" de esa poliza. */
@OptIn(ExperimentalCoroutinesApi::class)
class SeguroDelAvisoTest {

    private fun solicitud(estado: EstadoDeSolicitud) =
        Solicitud("poliza-1", estado, "2026-10-22T14:41:00Z", null, emptyList(), emptyList())

    private class SolicitudesFalsas(private val consulta: ResultadoConsulta) : ServicioDeSolicitudes {
        val consultadas = mutableListOf<String>()

        override suspend fun condiciones() = ResultadoCondiciones.Fallo

        override suspend fun enviar(datos: DatosDeLaSolicitud) = ResultadoEnvio.Fallo

        override suspend fun consultar(id: String): ResultadoConsulta {
            consultadas += id
            return consulta
        }

        override suspend fun listar() = ResultadoLista.Fallo

        override suspend fun documento(id: String) = ResultadoDocumento.Fallo
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
    @DisplayName("trae la poliza por su id y la muestra si esta emitida")
    fun emitida() {
        val emitida = solicitud(EstadoDeSolicitud.EMITIDA)
        val solicitudes = SolicitudesFalsas(ResultadoConsulta.Exito(emitida))
        val seguro = SeguroViewModel(solicitudes)

        seguro.abrir("poliza-1")

        assertEquals(listOf("poliza-1"), solicitudes.consultadas)
        assertEquals(emitida, seguro.estado.seguro)
        assertFalse(seguro.estado.cargando)
        assertFalse(seguro.estado.noDisponible)
    }

    @Test
    @DisplayName("mientras la trae muestra que esta cargando")
    fun cargando() {
        // Sin un dispatcher que corra la corrutina de inmediato, abrir() queda cargando.
        Dispatchers.setMain(kotlinx.coroutines.test.StandardTestDispatcher())
        val seguro = SeguroViewModel(SolicitudesFalsas(ResultadoConsulta.Fallo))

        seguro.abrir("poliza-1")

        assertTrue(seguro.estado.cargando)
        assertNull(seguro.estado.seguro)
    }

    @Test
    @DisplayName("una poliza que no esta emitida no se muestra: el cliente vuelve al Inicio")
    fun noEmitida() {
        for (estado in listOf(EstadoDeSolicitud.PENDIENTE, EstadoDeSolicitud.RECHAZADA, EstadoDeSolicitud.VENCIDA)) {
            val seguro = SeguroViewModel(SolicitudesFalsas(ResultadoConsulta.Exito(solicitud(estado))))

            seguro.abrir("poliza-1")

            assertNull(seguro.estado.seguro)
            assertTrue(seguro.estado.noDisponible)
        }
    }

    @Test
    @DisplayName("si no existe o falla la consulta queda como no disponible")
    fun noExisteOFalla() {
        for (resultado in listOf(ResultadoConsulta.NoExiste, ResultadoConsulta.Fallo)) {
            val seguro = SeguroViewModel(SolicitudesFalsas(resultado)).apply { abrir("poliza-1") }

            assertTrue(seguro.estado.noDisponible)
            assertFalse(seguro.estado.cargando)
        }
    }

    @Test
    @DisplayName("si la sesion vencio lo informa para volver al inicio de sesion")
    fun sesionVencida() {
        val seguro = SeguroViewModel(SolicitudesFalsas(ResultadoConsulta.SesionVencida)).apply { abrir("poliza-1") }

        assertTrue(seguro.estado.sesionVencida)
    }
}
