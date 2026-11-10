package com.solventa4bits.movil.siniestro.ui

import com.solventa4bits.movil.siniestro.dominio.ClaseDeArchivo
import com.solventa4bits.movil.siniestro.dominio.DestinoDePago
import com.solventa4bits.movil.siniestro.dominio.DetalleDeSiniestro
import com.solventa4bits.movil.siniestro.dominio.EstadoDeSiniestro
import com.solventa4bits.movil.siniestro.dominio.EvidenciaPedida
import com.solventa4bits.movil.siniestro.dominio.ProblemaDeFecha
import com.solventa4bits.movil.siniestro.dominio.ResultadoEntrega
import com.solventa4bits.movil.siniestro.dominio.ResultadoReporte
import com.solventa4bits.movil.siniestro.dominio.TipoDeCuenta
import com.solventa4bits.movil.siniestro.dominio.SiniestroResumen
import com.solventa4bits.movil.siniestro.dominio.TipoDeSiniestro
import com.solventa4bits.movil.siniestro.dominio.Ubicacion
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

    // --- Tu casa.

    private val enLaCasa = Ubicacion(4.65, -74.06)
    private val cuenta = DatosDePago("Bancolombia", TipoDeCuenta.AHORROS, "48210012345")

    private fun deTuCasa(ubicacion: Ubicacion? = enLaCasa) = ReporteViewModel(
        servicio = servicio,
        preparar = { byteArrayOf(9) },
        ubicacion = { ubicacion },
        ahora = { ahora },
        llaveNueva = { "llave-${++llaves}" },
    ).apply { empezar(poliza, TipoDeSiniestro.INCENDIO_TERREMOTO) }

    @Test
    @DisplayName("BITS-111, AC-4: con el permiso, cada foto del dano lleva donde y cuando se tomo")
    fun fotoConUbicacion() {
        val reporte = deTuCasa()
        reporte.decidirUbicacion(true)

        reporte.agregarPagina(Escaneo.FOTO_DEL_DANO, foto)

        val tomada = reporte.estado.fotos.single()
        assertEquals(ClaseDeArchivo.FOTO, tomada.clase)
        assertEquals(4.65, tomada.latitud)
        assertEquals(-74.06, tomada.longitud)
        assertEquals("2026-11-08T14:12:00Z", tomada.capturadoEn)
    }

    @Test
    @DisplayName("BITS-111, AC-5: si el cliente siguio sin ubicacion, la foto no la lleva y se puede seguir")
    fun fotoSinUbicacion() {
        val reporte = deTuCasa()
        reporte.decidirUbicacion(false)

        reporte.agregarPagina(Escaneo.FOTO_DEL_DANO, foto)

        assertNull(reporte.estado.fotos.single().latitud)
        assertTrue(reporte.estado.fotosListas)
    }

    @Test
    @DisplayName("BITS-111, AC-5: si el telefono no sabe donde esta, la foto queda sin ubicacion")
    fun gpsSinRespuesta() {
        val reporte = deTuCasa(ubicacion = null)
        reporte.decidirUbicacion(true)

        reporte.agregarPagina(Escaneo.FOTO_DEL_DANO, foto)

        assertNull(reporte.estado.fotos.single().latitud)
    }

    @Test
    @DisplayName("los documentos de Tu casa no llevan ubicacion")
    fun documentoSinUbicacion() {
        val reporte = deTuCasa()
        reporte.decidirUbicacion(true)

        reporte.agregarPagina(Escaneo.DOCUMENTO, foto)

        assertEquals(ClaseDeArchivo.DOCUMENTO, reporte.estado.documentos.single().clase)
        assertNull(reporte.estado.documentos.single().latitud)
    }

    @Test
    @DisplayName("BITS-111, AC-6: se puede quitar una sola foto")
    fun quitarUnaFoto() {
        val reporte = deTuCasa()
        reporte.agregarPagina(Escaneo.FOTO_DEL_DANO, foto)
        reporte.agregarPagina(Escaneo.FOTO_DEL_DANO, byteArrayOf(7))

        reporte.quitarUno(Escaneo.FOTO_DEL_DANO, 0)

        assertEquals(1, reporte.estado.fotos.size)
    }

    @Test
    @DisplayName("BITS-111, AC-4: sin foto, sin lo que paso o sin la cuenta no se envia")
    fun loObligatorioDeTuCasa() {
        val reporte = deTuCasa()
        assertFalse(reporte.estado.fotosListas)

        reporte.agregarPagina(Escaneo.FOTO_DEL_DANO, foto)
        reporte.cambiarNarracion("   ")
        reporte.cambiarPago(cuenta)
        assertFalse(reporte.estado.relatoListo)
        assertFalse(reporte.estado.listoParaEnviar)

        reporte.cambiarNarracion("Se incendio la cocina")
        reporte.cambiarPago(cuenta.copy(numeroCuenta = "12"))
        assertTrue(reporte.estado.relatoListo)
        assertFalse(reporte.estado.listoParaEnviar)

        reporte.cambiarPago(cuenta)
        assertTrue(reporte.estado.listoParaEnviar)
    }

    @Test
    @DisplayName("lo que paso se corta en 500 caracteres, como en el Figma")
    fun narracionLarga() {
        val reporte = deTuCasa()

        reporte.cambiarNarracion("a".repeat(600))

        assertEquals(500, reporte.estado.narracion.length)
    }

    @Test
    @DisplayName("BITS-111, AC-7: envia incendio o terremoto con fotos, documentos, lo que paso y la cuenta")
    fun enviaTuCasa() {
        servicio.alReportar = ResultadoReporte.Radicado("s2", "SIN-2026-00042")
        val reporte = deTuCasa()
        reporte.agregarPagina(Escaneo.FOTO_DEL_DANO, foto)
        reporte.agregarPagina(Escaneo.DOCUMENTO, foto)
        reporte.cambiarNarracion("  Se incendio la cocina ")
        reporte.cambiarPago(cuenta)

        reporte.enviar()

        val enviado = servicio.reportes.single().first
        assertEquals(TipoDeSiniestro.INCENDIO_TERREMOTO, enviado.tipo)
        assertEquals("Se incendio la cocina", enviado.narracion)
        assertEquals(listOf(ClaseDeArchivo.FOTO, ClaseDeArchivo.DOCUMENTO), enviado.archivos.map { it.clase })
        assertEquals(DestinoDePago("Bancolombia", TipoDeCuenta.AHORROS, "48210012345"), enviado.destinoPago)
        assertEquals("SIN-2026-00042", reporte.estado.radicado?.numero)
    }

    @Test
    @DisplayName("en Vida no viajan lo que paso ni la cuenta: se le paga al banco")
    fun vidaSinDestino() {
        servicio.alReportar = ResultadoReporte.Radicado("s1", "SIN-1")
        val reporte = reporte()
        reporte.agregarPagina(Escaneo.DICTAMEN, foto)
        reporte.cambiarNarracion("algo")
        reporte.cambiarPago(cuenta)

        reporte.enviar()

        val enviado = servicio.reportes.single().first
        assertNull(enviado.narracion)
        assertNull(enviado.destinoPago)
    }

    // --- Lo que falta (BITS-118, AC-4).

    private fun enFaltaInformacion(tipo: TipoDeSiniestro) = DetalleDeSiniestro(
        resumen = SiniestroResumen("s3", "SIN-2026-00031", "poliza-1", tipo, EstadoDeSiniestro.FALTA_INFORMACION),
        fechaOcurrencia = "2026-10-19",
        fechaLimite = null,
        documentos = 0,
        fotos = 1,
        evidenciaPedida = EvidenciaPedida("El informe de los bomberos", "2026-10-28T15:00:00Z"),
    )

    private fun loQueFalta(tipo: TipoDeSiniestro) = ReporteViewModel(
        servicio = servicio,
        preparar = { byteArrayOf(9) },
        ubicacion = { enLaCasa },
        ahora = { ahora },
        llaveNueva = { "llave-${++llaves}" },
    ).apply {
        empezar(poliza)
        empezarEntrega(enFaltaInformacion(tipo))
    }

    @Test
    @DisplayName("BITS-118, AC-4: en Tu casa envia fotos y documentos al siniestro y queda recibido")
    fun entregaTuCasa() {
        val recibida = enFaltaInformacion(TipoDeSiniestro.INCENDIO_TERREMOTO)
        servicio.alEntregar = ResultadoEntrega.Recibida(recibida)
        val reporte = loQueFalta(TipoDeSiniestro.INCENDIO_TERREMOTO)
        assertEquals("El informe de los bomberos", reporte.estado.entrega?.pedido)
        assertFalse(reporte.estado.listoParaEnviar)

        reporte.decidirUbicacion(true)
        reporte.agregarPagina(Escaneo.DOCUMENTO, foto)
        assertTrue(reporte.estado.listoParaEnviar)
        reporte.agregarPagina(Escaneo.FOTO_DEL_DANO, foto)
        reporte.enviar()

        val (id, archivos, llave) = servicio.entregas.single()
        assertEquals("s3", id)
        assertEquals(listOf(ClaseDeArchivo.FOTO, ClaseDeArchivo.DOCUMENTO), archivos.map { it.clase })
        assertEquals(4.65, archivos.first().latitud)
        assertEquals("llave-2", llave)
        assertEquals(recibida, reporte.estado.recibida)
        assertTrue(servicio.reportes.isEmpty())
    }

    @Test
    @DisplayName("BITS-118, AC-4: en Vida envia el dictamen")
    fun entregaVida() {
        servicio.alEntregar = ResultadoEntrega.Recibida(enFaltaInformacion(TipoDeSiniestro.INCAPACIDAD_TOTAL))
        val reporte = loQueFalta(TipoDeSiniestro.INCAPACIDAD_TOTAL)
        assertFalse(reporte.estado.listoParaEnviar)

        reporte.agregarPagina(Escaneo.DICTAMEN, foto)
        reporte.agregarPagina(Escaneo.DICTAMEN, foto)
        reporte.enviar()

        assertEquals(2, servicio.entregas.single().second.size)
    }

    @Test
    @DisplayName("BITS-118, AC-4: si falla, el reintento lleva la misma llave")
    fun reintentoDeLaEntrega() {
        val reporte = loQueFalta(TipoDeSiniestro.INCENDIO_TERREMOTO)
        reporte.agregarPagina(Escaneo.FOTO_DEL_DANO, foto)

        reporte.enviar()
        assertEquals(ProblemaAlEnviar.FALLO, reporte.estado.problemaAlEnviar)
        servicio.alEntregar = ResultadoEntrega.Recibida(enFaltaInformacion(TipoDeSiniestro.INCENDIO_TERREMOTO))
        reporte.enviar()

        assertEquals(listOf("llave-2", "llave-2"), servicio.entregas.map { it.third })
        assertNull(reporte.estado.problemaAlEnviar)
    }

    @Test
    @DisplayName("si ya no se pide, o la sesion vencio, lo marca")
    fun yaNoSePide() {
        val reporte = loQueFalta(TipoDeSiniestro.INCENDIO_TERREMOTO)
        reporte.agregarPagina(Escaneo.FOTO_DEL_DANO, foto)

        servicio.alEntregar = ResultadoEntrega.YaNoSePide
        reporte.enviar()
        assertTrue(reporte.estado.yaNoSePide)

        servicio.alEntregar = ResultadoEntrega.SesionVencida
        reporte.enviar()
        assertTrue(reporte.estado.sesionVencida)
    }
}
