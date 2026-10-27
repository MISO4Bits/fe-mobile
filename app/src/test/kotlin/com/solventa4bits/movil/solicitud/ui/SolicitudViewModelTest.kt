package com.solventa4bits.movil.solicitud.ui

import com.solventa4bits.movil.cotizacion.dominio.CodigoCobertura
import com.solventa4bits.movil.cotizacion.dominio.Cotizacion
import com.solventa4bits.movil.cotizacion.dominio.DatosDelCredito
import com.solventa4bits.movil.identidad.dominio.CaraDeCedula
import com.solventa4bits.movil.identidad.dominio.EstadoGuardado
import com.solventa4bits.movil.identidad.dominio.ResultadoEstado
import com.solventa4bits.movil.identidad.dominio.ResultadoSubida
import com.solventa4bits.movil.identidad.dominio.ServicioDeEvidencias
import com.solventa4bits.movil.solicitud.dominio.CondicionesDelSeguro
import com.solventa4bits.movil.solicitud.dominio.DatosDeLaSolicitud
import com.solventa4bits.movil.solicitud.dominio.EstadoDeSolicitud
import com.solventa4bits.movil.solicitud.dominio.ResultadoCondiciones
import com.solventa4bits.movil.solicitud.dominio.ResultadoEnvio
import com.solventa4bits.movil.solicitud.dominio.ServicioDeSolicitudes
import com.solventa4bits.movil.solicitud.dominio.SolicitudEnviada
import kotlinx.coroutines.CompletableDeferred
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
class SolicitudViewModelTest {

    private val cotizacion = Cotizacion(
        vigente = true,
        datosCredito = DatosDelCredito("Bancolombia", 180_000_000, 98_000_000, 96),
        datosDeclarados = false,
        primaMensual = 11_412.0,
        primaBaseMensual = 13_426.0,
        sumaAsegurada = 98_000_000.0,
        coberturaMeses = 96,
        personalizada = true,
        factores = emptyList(),
        creadaEn = "2026-10-06T14:35:00Z",
    )
    private val condiciones = CondicionesDelSeguro("V2", "Condiciones de tu seguro", "<p>Texto</p>", null)
    private val enviada = SolicitudEnviada("poliza-1", EstadoDeSolicitud.EMITIDA, emptyList())

    private class EvidenciasFalsas : ServicioDeEvidencias {
        var estado: ResultadoEstado = ResultadoEstado.Exito(
            mapOf(
                CaraDeCedula.FRENTE to EstadoGuardado.ACEPTADA,
                CaraDeCedula.REVERSO to EstadoGuardado.REVISION_MANUAL,
            ),
            selfie = EstadoGuardado.ACEPTADA,
        )
        var consultas = 0

        override suspend fun subir(cara: CaraDeCedula, imagen: ByteArray) = ResultadoSubida.Fallo

        override suspend fun subirSelfie(imagen: ByteArray) = ResultadoSubida.Fallo

        override suspend fun consultar(): ResultadoEstado {
            consultas++
            return estado
        }
    }

    /** Doble de prueba: responde lo que tenga en ese momento y guarda lo que se le envio. */
    private inner class SolicitudesFalsas : ServicioDeSolicitudes {
        var condiciones: ResultadoCondiciones = ResultadoCondiciones.Exito(this@SolicitudViewModelTest.condiciones)
        var envio: ResultadoEnvio = ResultadoEnvio.Enviada(enviada)

        /** Si no es null, el envio espera a que se complete: sirve para ver "enviando". */
        var enEspera: CompletableDeferred<Unit>? = null
        val enviados = mutableListOf<DatosDeLaSolicitud>()

        override suspend fun condiciones() = condiciones

        override suspend fun enviar(datos: DatosDeLaSolicitud): ResultadoEnvio {
            enviados += datos
            enEspera?.await()
            return envio
        }
    }

    private val evidencias = EvidenciasFalsas()
    private val solicitudes = SolicitudesFalsas()

    @BeforeEach
    fun preparar() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterEach
    fun restaurar() {
        Dispatchers.resetMain()
    }

    private fun abierto(conTuCasa: Boolean = false) =
        SolicitudViewModel(evidencias, solicitudes).apply { abrir(cotizacion, conTuCasa) }

    private fun aceptado(conTuCasa: Boolean = false) = abierto(conTuCasa).apply { cambiarAceptacion(true) }

    @Test
    @DisplayName("AC-1: al abrir muestra el estado de frente, reverso y selfie, y trae las condiciones")
    fun muestraLosRequisitos() {
        val estado = abierto().estado

        assertFalse(estado.cargando)
        assertEquals(
            mapOf(
                Requisito.FRENTE to EstadoGuardado.ACEPTADA,
                Requisito.REVERSO to EstadoGuardado.REVISION_MANUAL,
                Requisito.SELFIE to EstadoGuardado.ACEPTADA,
            ),
            estado.requisitos,
        )
        assertEquals(condiciones, estado.condiciones)
        assertEquals("Bancolombia", estado.entidad)
    }

    @Test
    @DisplayName("si no llegan las condiciones lo informa, y reintentar vuelve a consultar")
    fun falloAlCargar() {
        solicitudes.condiciones = ResultadoCondiciones.Fallo
        val viewModel = abierto()
        assertTrue(viewModel.estado.falloAlCargar)

        solicitudes.condiciones = ResultadoCondiciones.Exito(condiciones)
        viewModel.reintentar()

        assertFalse(viewModel.estado.falloAlCargar)
        assertEquals(condiciones, viewModel.estado.condiciones)
        assertEquals(2, evidencias.consultas)
    }

    @Test
    @DisplayName("si no llega el estado de las fotos tambien lo informa")
    fun falloAlCargarLasFotos() {
        evidencias.estado = ResultadoEstado.Fallo

        assertTrue(abierto().estado.falloAlCargar)
    }

    @Test
    @DisplayName("AC-2: sin aceptar las condiciones, enviar sigue inactivo")
    fun sinAceptar() {
        val viewModel = abierto()

        assertFalse(viewModel.estado.puedeEnviar)
        viewModel.enviar()
        assertTrue(solicitudes.enviados.isEmpty())

        viewModel.cambiarAceptacion(true)
        assertTrue(viewModel.estado.puedeEnviar)
    }

    @Test
    @DisplayName("sin las condiciones cargadas no se puede enviar, aunque la casilla este marcada")
    fun sinCondiciones() {
        solicitudes.condiciones = ResultadoCondiciones.Fallo

        assertFalse(abierto().apply { cambiarAceptacion(true) }.estado.puedeEnviar)
    }

    @Test
    @DisplayName("la hoja de condiciones se abre, se cierra sin aceptar y acepta marcando la casilla")
    fun hojaDeCondiciones() {
        val viewModel = abierto()

        viewModel.verCondiciones()
        assertTrue(viewModel.estado.viendoCondiciones)
        viewModel.cerrarCondiciones()
        assertFalse(viewModel.estado.viendoCondiciones)
        assertFalse(viewModel.estado.aceptadas)

        viewModel.verCondiciones()
        viewModel.aceptarDesdeCondiciones()

        assertTrue(viewModel.estado.aceptadas)
        assertFalse(viewModel.estado.viendoCondiciones)
    }

    @Test
    @DisplayName("AC-2: envia solo VIDA, la cotizacion que vio y la version aceptada")
    fun enviaSoloVida() {
        val viewModel = aceptado()

        viewModel.enviar()

        assertEquals(
            DatosDeLaSolicitud(setOf(CodigoCobertura.VIDA), "2026-10-06T14:35:00Z", "V2"),
            solicitudes.enviados.single(),
        )
        assertEquals(enviada, viewModel.estado.enviada)
    }

    @Test
    @DisplayName("con Tu casa envia tambien HOGAR")
    fun enviaConTuCasa() {
        aceptado(conTuCasa = true).enviar()

        assertEquals(setOf(CodigoCobertura.VIDA, CodigoCobertura.HOGAR), solicitudes.enviados.single().coberturas)
    }

    @Test
    @DisplayName("mientras se envia, tocar otra vez no manda una segunda solicitud")
    fun noEnviaDosVeces() {
        solicitudes.enEspera = CompletableDeferred()
        val viewModel = aceptado()

        viewModel.enviar()
        assertTrue(viewModel.estado.enviando)
        viewModel.enviar()

        assertEquals(1, solicitudes.enviados.size)
    }

    @Test
    @DisplayName("CP-E: si falla el envio lo dice, conserva la casilla marcada y deja enviar otra vez")
    fun falloAlEnviar() {
        solicitudes.envio = ResultadoEnvio.Fallo
        val viewModel = aceptado()

        viewModel.enviar()
        assertEquals(ErrorAlEnviar.FALLO, viewModel.estado.errorAlEnviar)
        assertTrue(viewModel.estado.aceptadas)
        assertTrue(viewModel.estado.puedeEnviar)

        solicitudes.envio = ResultadoEnvio.Enviada(enviada)
        viewModel.enviar()

        assertNull(viewModel.estado.errorAlEnviar)
        assertEquals(enviada, viewModel.estado.enviada)
    }

    @Test
    @DisplayName("si la cotizacion vencio o cambio pide cotizar de nuevo")
    fun cotizarDeNuevo() {
        solicitudes.envio = ResultadoEnvio.CotizarDeNuevo

        val viewModel = aceptado().apply { enviar() }

        assertEquals(ErrorAlEnviar.COTIZAR_DE_NUEVO, viewModel.estado.errorAlEnviar)
        assertNull(viewModel.estado.enviada)
    }

    @Test
    @DisplayName("si la sesion vencio al abrir o al enviar lo informa para volver al inicio de sesion")
    fun sesionVencida() {
        evidencias.estado = ResultadoEstado.SesionVencida
        assertTrue(abierto().estado.sesionVencida)

        evidencias.estado = ResultadoEstado.Exito(emptyMap(), EstadoGuardado.ACEPTADA)
        solicitudes.envio = ResultadoEnvio.SesionVencida
        val viewModel = aceptado().apply { enviar() }

        assertTrue(viewModel.estado.sesionVencida)
    }
}
