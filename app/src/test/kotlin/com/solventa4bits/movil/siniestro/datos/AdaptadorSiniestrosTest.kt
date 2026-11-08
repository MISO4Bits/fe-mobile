package com.solventa4bits.movil.siniestro.datos

import com.solventa4bits.movil.sesion.datos.ConSesion
import com.solventa4bits.movil.sesion.dominio.AlmacenEnMemoria
import com.solventa4bits.movil.sesion.dominio.ResultadoInicioSesion
import com.solventa4bits.movil.sesion.dominio.ServicioDeSesion
import com.solventa4bits.movil.sesion.dominio.Sesion
import com.solventa4bits.movil.siniestro.dominio.ArchivoDelReporte
import com.solventa4bits.movil.siniestro.dominio.ClaseDeArchivo
import com.solventa4bits.movil.siniestro.dominio.DestinoDePago
import com.solventa4bits.movil.siniestro.dominio.EstadoDeSiniestro
import com.solventa4bits.movil.siniestro.dominio.ReporteDeSiniestro
import com.solventa4bits.movil.siniestro.dominio.ResultadoDetalle
import com.solventa4bits.movil.siniestro.dominio.ResultadoReporte
import com.solventa4bits.movil.siniestro.dominio.ResultadoSiniestros
import com.solventa4bits.movil.siniestro.dominio.SiniestroResumen
import com.solventa4bits.movil.siniestro.dominio.TipoDeCuenta
import com.solventa4bits.movil.siniestro.dominio.TipoDeSiniestro
import com.solventa4bits.movil.traza.TrazaFalsa
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.IOException
import java.time.LocalDate

/** El adaptador de siniestros contra un BFF falso (MockWebServer). */
class AdaptadorSiniestrosTest {

    private lateinit var servidor: MockWebServer
    private lateinit var traza: TrazaFalsa
    private val almacen = AlmacenEnMemoria(Sesion("jwt-de-acceso", "token-de-refresco", 3600))

    private val reporte = ReporteDeSiniestro(
        polizaId = "poliza-1",
        tipo = TipoDeSiniestro.INCENDIO_TERREMOTO,
        fechaOcurrencia = LocalDate.of(2026, 11, 10),
        narracion = "Se incendio la cocina",
        archivos = listOf(
            ArchivoDelReporte(ClaseDeArchivo.FOTO, byteArrayOf(1, 2, 3), "2026-11-10T15:02:00Z", 4.65, -74.06),
            ArchivoDelReporte(ClaseDeArchivo.DOCUMENTO, byteArrayOf(4, 5), "2026-11-10T15:05:00Z"),
        ),
    )

    @BeforeEach
    fun preparar() {
        servidor = MockWebServer()
        servidor.start()
        traza = TrazaFalsa()
    }

    @AfterEach
    fun apagar() {
        servidor.shutdown()
    }

    private fun adaptador(): AdaptadorSiniestros {
        val sesiones = object : ServicioDeSesion {
            override suspend fun iniciarSesion(correo: String, contrasena: String) = ResultadoInicioSesion.Fallo
            override suspend fun refrescar(tokenDeRefresco: String) = ResultadoInicioSesion.CredencialesInvalidas
        }
        return AdaptadorSiniestros(
            crearSiniestrosApi(servidor.url("/mobile/").toString()),
            ConSesion(almacen, sesiones),
            traza,
        )
    }

    private fun responder(codigo: Int, cuerpo: String = "") {
        servidor.enqueue(MockResponse().setResponseCode(codigo).setBody(cuerpo))
    }

    private fun problema(codigo: String) = """{"title": "Regla de negocio", "codigo": "$codigo"}"""

    @Test
    @DisplayName("envia por multipart los datos en JSON y cada archivo como JPEG, con la llave y el token")
    fun enviaElReporte() = runTest {
        responder(201, """{"id": "sin-1", "numero": "SIN-2026-7K3M9Q", "estado": "EN_ESTUDIO"}""")

        adaptador().reportar(reporte, "llave-1")

        val peticion = servidor.takeRequest()
        val cuerpo = peticion.body.readUtf8()
        assertEquals("POST", peticion.method)
        assertEquals("/mobile/v1/cuenta/siniestros", peticion.path)
        assertEquals("Bearer jwt-de-acceso", peticion.getHeader("Authorization"))
        assertEquals("llave-1", peticion.getHeader("Idempotency-Key"))
        assertTrue(peticion.getHeader("Content-Type")!!.startsWith("multipart/form-data"))
        assertTrue(cuerpo.contains("name=\"datos\""))
        assertTrue(cuerpo.contains("Content-Type: application/json"))
        assertTrue(cuerpo.contains("\"polizaId\":\"poliza-1\""))
        assertTrue(cuerpo.contains("\"tipo\":\"INCENDIO_TERREMOTO\""))
        assertTrue(cuerpo.contains("\"fechaOcurrencia\":\"2026-11-10\""))
        assertTrue(cuerpo.contains("\"narracion\":\"Se incendio la cocina\""))
        assertTrue(cuerpo.contains("name=\"archivos\"; filename=\"archivo-1.jpg\""))
        assertTrue(cuerpo.contains("name=\"archivos\"; filename=\"archivo-2.jpg\""))
        assertTrue(cuerpo.contains("Content-Type: image/jpeg"))
    }

    @Test
    @DisplayName("BITS-111, AC-5: la ubicacion de cada foto viaja en datos, y en null si no la hay")
    fun ubicacionEnLosDatos() = runTest {
        responder(201, """{"id": "sin-1", "numero": "SIN-1", "estado": "EN_ESTUDIO"}""")

        adaptador().reportar(reporte, "llave-1")

        val cuerpo = servidor.takeRequest().body.readUtf8()
        assertTrue(
            cuerpo.contains(
                """{"clase":"FOTO","capturadoEn":"2026-11-10T15:02:00Z","latitud":4.65,"longitud":-74.06}""",
            ),
        )
        assertTrue(
            cuerpo.contains(
                """{"clase":"DOCUMENTO","capturadoEn":"2026-11-10T15:05:00Z","latitud":null,"longitud":null}""",
            ),
        )
    }

    @Test
    @DisplayName("Tu casa: la cuenta viaja como transferencia")
    fun destinoComoTransferencia() = runTest {
        responder(201, """{"id": "sin-1", "numero": "SIN-1", "estado": "EN_ESTUDIO"}""")
        val conCuenta = reporte.copy(destinoPago = DestinoDePago("Bancolombia", TipoDeCuenta.AHORROS, "48210012345"))

        adaptador().reportar(conCuenta, "llave-1")

        val cuerpo = servidor.takeRequest().body.readUtf8()
        assertTrue(
            cuerpo.contains(
                """"destinoPago":{"tipo":"TRANSFERENCIA","banco":"Bancolombia","tipoCuenta":"AHORROS",""" +
                    """"numeroCuenta":"48210012345"}""",
            ),
        )
    }

    @Test
    @DisplayName("Vida: sin cuenta, el destino no viaja")
    fun vidaSinDestino() = runTest {
        responder(201, """{"id": "sin-1", "numero": "SIN-1", "estado": "EN_ESTUDIO"}""")

        adaptador().reportar(reporte.copy(tipo = TipoDeSiniestro.INCAPACIDAD_TOTAL, narracion = null), "llave-1")

        val cuerpo = servidor.takeRequest().body.readUtf8()
        assertFalse(cuerpo.contains("destinoPago"))
        assertFalse(cuerpo.contains("narracion"))
    }

    @Test
    @DisplayName("BITS-111, AC-7: 201 trae el numero de radicado")
    fun radicado() = runTest {
        responder(201, """{"id": "sin-1", "numero": "SIN-2026-7K3M9Q", "estado": "EN_ESTUDIO"}""")

        assertEquals(ResultadoReporte.Radicado("sin-1", "SIN-2026-7K3M9Q"), adaptador().reportar(reporte, "llave-1"))
    }

    @Test
    @DisplayName("BITS-111, AC-2: 409 FUERA_DE_VIGENCIA es la fecha fuera de la vigencia")
    fun fueraDeVigencia() = runTest {
        responder(409, problema("FUERA_DE_VIGENCIA"))

        assertEquals(ResultadoReporte.FueraDeVigencia, adaptador().reportar(reporte, "llave-1"))
    }

    @Test
    @DisplayName("BITS-111, AC-2A: 409 SINIESTRO_EN_CURSO es que esa cobertura ya tiene uno en curso")
    fun enCurso() = runTest {
        responder(409, problema("SINIESTRO_EN_CURSO"))

        assertEquals(ResultadoReporte.SiniestroEnCurso, adaptador().reportar(reporte, "llave-1"))
    }

    @Test
    @DisplayName("400 TIPO_NO_CUBIERTO es una poliza sin Tu casa")
    fun tipoNoCubierto() = runTest {
        responder(400, problema("TIPO_NO_CUBIERTO"))

        assertEquals(ResultadoReporte.TipoNoCubierto, adaptador().reportar(reporte, "llave-1"))
    }

    @Test
    @DisplayName("un 400 sin codigo conocido es un fallo, con traza")
    fun reporteInvalido() = runTest {
        responder(400, """{"title": "Solicitud invalida", "errores": [{"campo": "narracion"}]}""")

        assertEquals(ResultadoReporte.Fallo, adaptador().reportar(reporte, "llave-1"))
        assertTrue(traza.errores.single().mensaje.contains("400"))
    }

    @Test
    @DisplayName("si el BFF falla al reportar responde controlado y deja traza con el codigo")
    fun falloAlReportar() = runTest {
        responder(503)

        assertEquals(ResultadoReporte.Fallo, adaptador().reportar(reporte, "llave-1"))
        assertTrue(traza.errores.single().mensaje.contains("503"))
    }

    @Test
    @DisplayName("si no hay red al reportar responde controlado y deja traza")
    fun sinRedAlReportar() = runTest {
        servidor.shutdown()

        assertEquals(ResultadoReporte.Fallo, adaptador().reportar(reporte, "llave-1"))
        assertInstanceOf(IOException::class.java, traza.errores.single().causa)
    }

    @Test
    @DisplayName("si la sesion no se puede refrescar al reportar, la da por vencida")
    fun sesionVencidaAlReportar() = runTest {
        responder(401)

        assertEquals(ResultadoReporte.SesionVencida, adaptador().reportar(reporte, "llave-1"))
    }

    @Test
    @DisplayName("lista los siniestros, con el automatico; un tipo o estado que la app no conoce queda en null")
    fun lista() = runTest {
        responder(
            200,
            """[
              {"id": "s1", "numero": "SIN-1", "polizaId": "poliza-1", "tipo": "INCAPACIDAD_TOTAL",
               "automatico": false, "estado": "EN_ESTUDIO", "reportadoEn": "2026-11-10T15:02:00Z",
               "actualizadoEn": "2026-11-10T15:02:00Z"},
              {"id": "s2", "numero": "SIN-2", "polizaId": "poliza-1", "tipo": "SISMO_PARAMETRICO",
               "automatico": true, "estado": "PAGADO", "reportadoEn": "2026-11-01T15:02:00Z",
               "actualizadoEn": "2026-11-02T15:02:00Z"},
              {"id": "s3", "numero": "SIN-3", "polizaId": "poliza-1", "tipo": "TIPO_NUEVO",
               "automatico": false, "estado": "ESTADO_NUEVO", "reportadoEn": "2026-10-01T15:02:00Z",
               "actualizadoEn": "2026-10-01T15:02:00Z"}
            ]""",
        )

        val resultado = adaptador().listar() as ResultadoSiniestros.Exito

        assertEquals(
            SiniestroResumen(
                id = "s2",
                numero = "SIN-2",
                polizaId = "poliza-1",
                tipo = TipoDeSiniestro.SISMO_PARAMETRICO,
                estado = EstadoDeSiniestro.PAGADO,
                automatico = true,
                reportadoEn = "2026-11-01T15:02:00Z",
                actualizadoEn = "2026-11-02T15:02:00Z",
            ),
            resultado.siniestros[1],
        )
        assertEquals(TipoDeSiniestro.INCAPACIDAD_TOTAL, resultado.siniestros[0].tipo)
        assertEquals(EstadoDeSiniestro.EN_ESTUDIO, resultado.siniestros[0].estado)
        assertNull(resultado.siniestros[2].tipo)
        assertNull(resultado.siniestros[2].estado)
        val peticion = servidor.takeRequest()
        assertEquals("GET", peticion.method)
        assertEquals("/mobile/v1/cuenta/siniestros", peticion.path)
        assertEquals("Bearer jwt-de-acceso", peticion.getHeader("Authorization"))
    }

    @Test
    @DisplayName("si el BFF falla al listar responde controlado y deja traza")
    fun falloAlListar() = runTest {
        responder(503)

        assertEquals(ResultadoSiniestros.Fallo, adaptador().listar())
        assertTrue(traza.errores.single().mensaje.contains("503"))
    }

    @Test
    @DisplayName("si la sesion no se puede refrescar al listar, la da por vencida")
    fun sesionVencidaAlListar() = runTest {
        responder(401)

        assertEquals(ResultadoSiniestros.SesionVencida, adaptador().listar())
    }

    @Test
    @DisplayName("el detalle trae el plazo, la fecha en que ocurrio y cuenta los documentos y las fotos")
    fun detalle() = runTest {
        responder(
            200,
            """{"id": "s1", "numero": "SIN-1", "polizaId": "poliza-1", "tipo": "INCAPACIDAD_TOTAL",
               "automatico": false, "estado": "EN_ESTUDIO", "reportadoEn": "2026-11-02T14:12:00Z",
               "actualizadoEn": "2026-11-02T14:12:00Z", "fechaOcurrencia": "2026-11-02",
               "narracion": null, "ultimaEntregaEn": "2026-11-02T14:12:00Z",
               "fechaLimite": "2026-12-02T14:12:00Z", "evidenciaPedida": null, "motivoRechazo": null,
               "pago": null, "evento": null,
               "archivos": [
                 {"id": "a1", "clase": "DOCUMENTO", "entregadoEn": "2026-11-02T14:12:00Z"},
                 {"id": "a2", "clase": "DOCUMENTO", "entregadoEn": "2026-11-02T14:12:00Z"},
                 {"id": "a3", "clase": "FOTO", "entregadoEn": "2026-11-02T14:12:00Z"}
               ]}""",
        )

        val detalle = (adaptador().consultar("s1") as ResultadoDetalle.Exito).detalle

        assertEquals("SIN-1", detalle.resumen.numero)
        assertEquals("2026-11-02", detalle.fechaOcurrencia)
        assertEquals("2026-12-02T14:12:00Z", detalle.fechaLimite)
        assertEquals(2, detalle.documentos)
        assertEquals(1, detalle.fotos)
        assertEquals("/mobile/v1/cuenta/siniestros/s1", servidor.takeRequest().path)
    }

    @Test
    @DisplayName("un siniestro que no existe o es de otro cliente (404) no se muestra")
    fun detalleQueNoExiste() = runTest {
        responder(404)

        assertEquals(ResultadoDetalle.NoExiste, adaptador().consultar("s1"))
    }

    @Test
    @DisplayName("si el BFF falla al consultar el detalle responde controlado y deja traza")
    fun falloAlConsultar() = runTest {
        responder(503)

        assertEquals(ResultadoDetalle.Fallo, adaptador().consultar("s1"))
        assertTrue(traza.errores.single().mensaje.contains("503"))
    }

    @Test
    @DisplayName("si la sesion no se puede refrescar al consultar el detalle, la da por vencida")
    fun sesionVencidaAlConsultar() = runTest {
        responder(401)

        assertEquals(ResultadoDetalle.SesionVencida, adaptador().consultar("s1"))
    }

    @Test
    @DisplayName("una lista fuera de contrato responde controlado")
    fun listaFueraDeContrato() = runTest {
        responder(200, """{"no": "es una lista"}""")

        assertEquals(ResultadoSiniestros.Fallo, adaptador().listar())
    }
}
