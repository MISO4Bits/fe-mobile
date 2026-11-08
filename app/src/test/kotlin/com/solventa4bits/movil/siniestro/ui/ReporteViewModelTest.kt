package com.solventa4bits.movil.siniestro.ui

import com.solventa4bits.movil.siniestro.dominio.ClaseDeArchivo
import com.solventa4bits.movil.siniestro.dominio.ProblemaDeFecha
import com.solventa4bits.movil.siniestro.dominio.ResultadoReporte
import com.solventa4bits.movil.siniestro.dominio.TipoDeSiniestro
import com.solventa4bits.movil.solicitud.dominio.EstadoDeSolicitud
import com.solventa4bits.movil.solicitud.dominio.Solicitud
import com.solventa4bits.movil.solicitud.dominio.Vigencia
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
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class ReporteViewModelTest {

    private val poliza = Solicitud(
        id = "poliza-1",
        estado = EstadoDeSolicitud.EMITIDA,
        creadaEn = "2026-10-19T14:00:00Z",
        emitidaEn = "2026-10-19T14:41:00Z",
        pendientes = emptyList(),
        motivosDeRechazo = emptyList(),
        vigencia = Vigencia("2026-10-19T14:41:00Z", "2027-10-19T14:41:00Z"),
    )

    // El 8 de noviembre a las 9:12 a. m. en Colombia.
    private val ahora = Instant.parse("2026-11-08T14:12:00Z")
    private val servicio = SiniestrosFalsos()
    private var llaves = 0
    private val foto = byteArrayOf(1, 2, 3)

    @BeforeEach
    fun preparar() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterEach
    fun restaurar() {
        Dispatchers.resetMain()
    }

    private fun reporte(preparada: ByteArray? = byteArrayOf(9)) = ReporteViewModel(
        servicio = servicio,
        preparar = { preparada },
        ahora = { ahora },
        llaveNueva = { "llave-${++llaves}" },
    ).apply { empezar(poliza) }

    @Test
    @DisplayName("BITS-111, AC-1: la fecha empieza en hoy, en Colombia")
    fun empiezaHoy() {
        val reporte = reporte()

        assertEquals(LocalDate.of(2026, 11, 8), reporte.estado.fecha)
        assertNull(reporte.estado.problemaConLaFecha)
    }

    @Test
    @DisplayName("BITS-111, AC-2: con una fecha fuera de la vigencia dice por que y no deja seguir")
    fun fechaFuera() {
        val reporte = reporte()
        reporte.agregarPagina(Escaneo.DICTAMEN, foto)

        reporte.cambiarFecha(LocalDate.of(2026, 12, 20))

        assertEquals(ProblemaDeFecha.FUTURA, reporte.estado.problemaConLaFecha)
        assertFalse(reporte.estado.puedeContinuar)
    }

    @Test
    @DisplayName("BITS-111, AC-3: sin el dictamen no se sigue; la historia clinica sola no basta")
    fun sinDictamen() {
        val reporte = reporte()
        reporte.agregarPagina(Escaneo.HISTORIA_CLINICA, foto)

        assertFalse(reporte.estado.puedeContinuar)

        reporte.agregarPagina(Escaneo.DICTAMEN, foto)

        assertTrue(reporte.estado.puedeContinuar)
    }

    @Test
    @DisplayName("cada foto es una pagina preparada, con la hora en que se tomo")
    fun paginas() {
        val reporte = reporte()

        reporte.agregarPagina(Escaneo.DICTAMEN, foto)
        reporte.agregarPagina(Escaneo.DICTAMEN, foto)

        assertEquals(2, reporte.estado.dictamen.size)
        val pagina = reporte.estado.dictamen.first()
        assertEquals(ClaseDeArchivo.DOCUMENTO, pagina.clase)
        assertEquals(listOf<Byte>(9), pagina.jpeg.toList())
        assertEquals("2026-11-08T14:12:00Z", pagina.capturadoEn)
        assertNull(pagina.latitud)
    }

    @Test
    @DisplayName("si la foto no se puede leer, lo dice y no agrega la pagina")
    fun fotoIlegible() {
        val reporte = reporte(preparada = null)

        reporte.agregarPagina(Escaneo.DICTAMEN, foto)

        assertTrue(reporte.estado.falloAlPreparar)
        assertTrue(reporte.estado.dictamen.isEmpty())
    }

    @Test
    @DisplayName("BITS-111, AC-6: quitar un documento quita todas sus paginas")
    fun quitar() {
        val reporte = reporte()
        reporte.agregarPagina(Escaneo.DICTAMEN, foto)
        reporte.agregarPagina(Escaneo.HISTORIA_CLINICA, foto)

        reporte.quitar(Escaneo.DICTAMEN)

        assertTrue(reporte.estado.dictamen.isEmpty())
        assertEquals(1, reporte.estado.historia.size)
        assertFalse(reporte.estado.puedeContinuar)
    }

    @Test
    @DisplayName("el servidor recibe hasta 10 archivos: despues no se escanea mas")
    fun maximoDeArchivos() {
        val reporte = reporte()
        repeat(10) { reporte.agregarPagina(Escaneo.DICTAMEN, foto) }

        assertFalse(reporte.estado.puedeEscanear)
    }

    @Test
    @DisplayName("BITS-111, AC-7: envia incapacidad total con el dictamen y la historia, y queda radicado")
    fun envia() {
        servicio.alReportar = ResultadoReporte.Radicado("s1", "SIN-2026-00058")
        val reporte = reporte()
        reporte.agregarPagina(Escaneo.DICTAMEN, foto)
        reporte.agregarPagina(Escaneo.HISTORIA_CLINICA, foto)

        reporte.enviar()

        val (enviado, llave) = servicio.reportes.single()
        assertEquals("poliza-1", enviado.polizaId)
        assertEquals(TipoDeSiniestro.INCAPACIDAD_TOTAL, enviado.tipo)
        assertEquals(LocalDate.of(2026, 11, 8), enviado.fechaOcurrencia)
        assertNull(enviado.narracion)
        assertEquals(2, enviado.archivos.size)
        assertEquals("llave-1", llave)
        assertEquals(ResultadoReporte.Radicado("s1", "SIN-2026-00058"), reporte.estado.radicado)
    }

    @Test
    @DisplayName("BITS-111, AC-7: si se cae la conexion, el reintento lleva la misma llave")
    fun reintentoConLaMismaLlave() {
        val reporte = reporte()
        reporte.agregarPagina(Escaneo.DICTAMEN, foto)

        reporte.enviar()
        assertEquals(ProblemaAlEnviar.FALLO, reporte.estado.problemaAlEnviar)
        servicio.alReportar = ResultadoReporte.Radicado("s1", "SIN-1")
        reporte.enviar()

        assertEquals(listOf("llave-1", "llave-1"), servicio.reportes.map { it.second })
        assertNull(reporte.estado.problemaAlEnviar)
    }

    @Test
    @DisplayName("un reporte nuevo lleva una llave nueva")
    fun reporteNuevoLlaveNueva() {
        servicio.alReportar = ResultadoReporte.Radicado("s1", "SIN-1")
        val reporte = reporte()
        reporte.agregarPagina(Escaneo.DICTAMEN, foto)
        reporte.enviar()

        reporte.empezar(poliza)
        reporte.agregarPagina(Escaneo.DICTAMEN, foto)
        reporte.enviar()

        assertEquals(listOf("llave-1", "llave-2"), servicio.reportes.map { it.second })
        assertTrue(reporte.estado.historia.isEmpty())
    }

    @Test
    @DisplayName("BITS-111, AC-2 y AC-2A: dice si el servidor rechazo la fecha o si ya hay uno en curso")
    fun rechazosDelServidor() {
        val reporte = reporte()
        reporte.agregarPagina(Escaneo.DICTAMEN, foto)

        servicio.alReportar = ResultadoReporte.FueraDeVigencia
        reporte.enviar()
        assertEquals(ProblemaAlEnviar.FUERA_DE_VIGENCIA, reporte.estado.problemaAlEnviar)

        servicio.alReportar = ResultadoReporte.SiniestroEnCurso
        reporte.enviar()
        assertEquals(ProblemaAlEnviar.EN_CURSO, reporte.estado.problemaAlEnviar)
    }

    @Test
    @DisplayName("con la sesion vencida lo marca para volver a entrar")
    fun sesionVencida() {
        servicio.alReportar = ResultadoReporte.SesionVencida
        val reporte = reporte()
        reporte.agregarPagina(Escaneo.DICTAMEN, foto)

        reporte.enviar()

        assertTrue(reporte.estado.sesionVencida)
    }

    @Test
    @DisplayName("sin el dictamen no envia nada")
    fun noEnviaSinDictamen() {
        val reporte = reporte()

        reporte.enviar()

        assertTrue(servicio.reportes.isEmpty())
    }
}
