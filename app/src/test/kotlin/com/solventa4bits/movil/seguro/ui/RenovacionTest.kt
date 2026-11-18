package com.solventa4bits.movil.seguro.ui

import com.solventa4bits.movil.solicitud.dominio.DatosDeLaSolicitud
import com.solventa4bits.movil.solicitud.dominio.EstadoDeRenovacion
import com.solventa4bits.movil.solicitud.dominio.EstadoDeSolicitud
import com.solventa4bits.movil.solicitud.dominio.Renovacion
import com.solventa4bits.movil.solicitud.dominio.ResultadoCondiciones
import com.solventa4bits.movil.solicitud.dominio.ResultadoConsulta
import com.solventa4bits.movil.solicitud.dominio.ResultadoDocumento
import com.solventa4bits.movil.solicitud.dominio.ResultadoEnvio
import com.solventa4bits.movil.solicitud.dominio.ResultadoLista
import com.solventa4bits.movil.solicitud.dominio.ResultadoRenovacion
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

/** BITS-237: la propuesta de renovacion en el detalle del seguro. */
@OptIn(ExperimentalCoroutinesApi::class)
class RenovacionTest {

    private val propuesta = Renovacion(
        estado = EstadoDeRenovacion.PROPUESTA,
        primaActual = 77_374.0,
        primaNueva = 81_397.0,
        ipc = 0.052,
        fechaRenovacion = "2026-12-17T15:00:00Z",
        decidirHasta = "2026-11-17T15:00:00Z",
        avisoAlBancoEn = null,
    )
    private val antesDelLimite = Instant.parse("2026-11-10T10:00:00Z")
    private val despuesDelLimite = Instant.parse("2026-11-20T10:00:00Z")

    private fun poliza(renovacion: Renovacion? = propuesta, estado: EstadoDeSolicitud = EstadoDeSolicitud.EMITIDA) =
        Solicitud("poliza-1", estado, "2025-12-17T14:41:00Z", null, emptyList(), emptyList(), renovacion = renovacion)

    private val solicitudes = SolicitudesFalsas()

    private fun seguro(ahora: Instant = antesDelLimite) = SeguroViewModel(solicitudes) { ahora }

    @BeforeEach
    fun preparar() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterEach
    fun restaurar() {
        Dispatchers.resetMain()
    }

    @Test
    @DisplayName("la etiqueta: vigente, por renovar, no se renueva o desactivada")
    fun etiquetas() {
        assertEquals(EtiquetaDelSeguro.VIGENTE, etiquetaDe(poliza(renovacion = null)))
        assertEquals(EtiquetaDelSeguro.POR_RENOVAR, etiquetaDe(poliza()))
        assertEquals(
            EtiquetaDelSeguro.NO_SE_RENUEVA,
            etiquetaDe(poliza(propuesta.copy(estado = EstadoDeRenovacion.RECHAZADA))),
        )
        val aceptada = poliza(propuesta.copy(estado = EstadoDeRenovacion.ACEPTADA))
        assertEquals(EtiquetaDelSeguro.VIGENTE, etiquetaDe(aceptada))
        assertEquals(EtiquetaDelSeguro.DESACTIVADA, etiquetaDe(poliza(estado = EstadoDeSolicitud.DESACTIVADA)))
    }

    @Test
    @DisplayName("AC-2: con la propuesta sin responder, el modal se abre cada vez que se abre el detalle")
    fun abreLaPropuesta() {
        val vm = seguro()

        vm.mostrar(poliza())
        vm.cerrarRenovacion()
        assertNull(vm.estado.renovando)
        vm.mostrar(poliza())

        assertEquals(PasoDeRenovacion.PROPUESTA, vm.estado.renovando)
        assertTrue(vm.estado.puedeDecidir)
    }

    @Test
    @DisplayName("AC-2: pasado el dia limite, o sin conexion, no se abre ni se puede decidir")
    fun sinPropuesta() {
        val tarde = seguro(despuesDelLimite).apply { mostrar(poliza()) }
        val sinConexion = seguro().apply { mostrar(poliza(), guardadoEn = "2026-11-10T09:00:00Z") }

        assertNull(tarde.estado.renovando)
        assertFalse(tarde.estado.puedeDecidir)
        assertNull(sinConexion.estado.renovando)
    }

    @Test
    @DisplayName("AC-4 y AC-5: no renovar pide confirmar; volver regresa a la propuesta")
    fun confirmaNoRenovar() {
        val vm = seguro().apply { mostrar(poliza()) }

        vm.pedirNoRenovar()
        assertEquals(PasoDeRenovacion.NO_RENOVAR, vm.estado.renovando)
        vm.verPropuesta()

        assertEquals(PasoDeRenovacion.PROPUESTA, vm.estado.renovando)
    }

    @Test
    @DisplayName("AC-5: al no renovar, se trae la poliza de nuevo y se cierra el modal")
    fun noRenueva() {
        val rechazada = poliza(propuesta.copy(estado = EstadoDeRenovacion.RECHAZADA))
        solicitudes.consulta = ResultadoConsulta.Exito(rechazada)
        val vm = seguro().apply { mostrar(poliza()) }
        vm.pedirNoRenovar()

        vm.responder(aceptar = false)

        assertEquals(listOf("poliza-1" to false), solicitudes.respuestas)
        assertEquals(rechazada, vm.estado.seguro)
        assertNull(vm.estado.renovando)
        assertTrue(vm.estado.puedeDecidir)
    }

    @Test
    @DisplayName("si ya no se podia decidir, muestra lo que tiene el servidor; si no llega, la que tenia")
    fun yaNoSePuede() {
        solicitudes.respuesta = ResultadoRenovacion.YaNoSePuede
        val vm = seguro().apply { mostrar(poliza()) }

        vm.responder(aceptar = true)

        assertEquals(poliza(), vm.estado.seguro)
        assertNull(vm.estado.renovando)
        assertFalse(vm.estado.respondiendo)
    }

    @Test
    @DisplayName("si falla lo dice en el modal; si la sesion vencio lo informa")
    fun fallas() {
        solicitudes.respuesta = ResultadoRenovacion.Fallo
        val vm = seguro().apply { mostrar(poliza()) }

        vm.responder(aceptar = true)
        assertTrue(vm.estado.falloAlResponder)
        assertEquals(PasoDeRenovacion.PROPUESTA, vm.estado.renovando)
        vm.pedirNoRenovar()
        assertFalse(vm.estado.falloAlResponder)

        solicitudes.respuesta = ResultadoRenovacion.SesionVencida
        vm.responder(aceptar = false)
        assertTrue(vm.estado.sesionVencida)
    }

    private class SolicitudesFalsas : ServicioDeSolicitudes {
        var respuesta: ResultadoRenovacion = ResultadoRenovacion.Respondida
        var consulta: ResultadoConsulta = ResultadoConsulta.Fallo
        val respuestas = mutableListOf<Pair<String, Boolean>>()

        override suspend fun condiciones() = ResultadoCondiciones.Fallo

        override suspend fun enviar(datos: DatosDeLaSolicitud) = ResultadoEnvio.Fallo

        override suspend fun consultar(id: String) = consulta

        override suspend fun listar() = ResultadoLista.Fallo

        override suspend fun documento(id: String) = ResultadoDocumento.Fallo

        override suspend fun responderRenovacion(id: String, aceptar: Boolean): ResultadoRenovacion {
            respuestas += id to aceptar
            return respuesta
        }
    }
}
