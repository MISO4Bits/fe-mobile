package com.solventa4bits.movil.solicitud.ui

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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ResultadoViewModelTest {

    private val pendiente =
        Solicitud("poliza-1", EstadoDeSolicitud.PENDIENTE, "2026-10-27T14:41:00Z", null, emptyList(), emptyList())
    private val emitida = pendiente.copy(estado = EstadoDeSolicitud.EMITIDA, emitidaEn = "2026-10-28T16:20:00Z")

    /** Doble de prueba: responde lo que tenga en ese momento y cuenta las consultas. */
    private class SolicitudesFalsas : ServicioDeSolicitudes {
        var consulta: ResultadoConsulta = ResultadoConsulta.Fallo
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

    private val solicitudes = SolicitudesFalsas()

    @BeforeEach
    fun preparar() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterEach
    fun restaurar() {
        Dispatchers.resetMain()
    }

    @Test
    @DisplayName("justo despues de enviarla muestra lo que respondio el envio, sin volver a llamar al servidor")
    fun mostrar() {
        val viewModel = ResultadoViewModel(solicitudes).apply { mostrar(pendiente) }

        assertEquals(pendiente, viewModel.estado.solicitud)
        assertTrue(solicitudes.consultadas.isEmpty())
    }

    @Test
    @DisplayName("AC-3: desde el Inicio la consulta de nuevo y, si ya se cumplio lo ultimo, llega emitida")
    fun abrir() {
        solicitudes.consulta = ResultadoConsulta.Exito(emitida)

        val viewModel = ResultadoViewModel(solicitudes).apply { abrir("poliza-1") }

        assertEquals(listOf("poliza-1"), solicitudes.consultadas)
        assertEquals(emitida, viewModel.estado.solicitud)
        assertFalse(viewModel.estado.cargando)
    }

    @Test
    @DisplayName("si la consulta falla lo informa, y reintentar vuelve a consultar la misma")
    fun falloYReintento() {
        val viewModel = ResultadoViewModel(solicitudes).apply { abrir("poliza-1") }
        assertTrue(viewModel.estado.fallo)

        solicitudes.consulta = ResultadoConsulta.Exito(pendiente)
        viewModel.reintentar()

        assertFalse(viewModel.estado.fallo)
        assertEquals(pendiente, viewModel.estado.solicitud)
        assertEquals(listOf("poliza-1", "poliza-1"), solicitudes.consultadas)
    }

    @Test
    @DisplayName("si el servidor ya no la tiene, lo informa para volver al Inicio")
    fun noExiste() {
        solicitudes.consulta = ResultadoConsulta.NoExiste

        assertTrue(ResultadoViewModel(solicitudes).apply { abrir("poliza-1") }.estado.noExiste)
    }

    @Test
    @DisplayName("si la sesion vencio lo informa para volver al inicio de sesion")
    fun sesionVencida() {
        solicitudes.consulta = ResultadoConsulta.SesionVencida

        assertTrue(ResultadoViewModel(solicitudes).apply { abrir("poliza-1") }.estado.sesionVencida)
    }
}
