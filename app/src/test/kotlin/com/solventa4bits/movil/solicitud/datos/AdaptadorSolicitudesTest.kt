package com.solventa4bits.movil.solicitud.datos

import com.solventa4bits.movil.cotizacion.dominio.CodigoCobertura
import com.solventa4bits.movil.sesion.datos.ConSesion
import com.solventa4bits.movil.sesion.dominio.AlmacenEnMemoria
import com.solventa4bits.movil.sesion.dominio.ResultadoInicioSesion
import com.solventa4bits.movil.sesion.dominio.ServicioDeSesion
import com.solventa4bits.movil.sesion.dominio.Sesion
import com.solventa4bits.movil.solicitud.dominio.CondicionesDelSeguro
import com.solventa4bits.movil.solicitud.dominio.CreditoAsegurado
import com.solventa4bits.movil.solicitud.dominio.DatosDeLaSolicitud
import com.solventa4bits.movil.solicitud.dominio.ResultadoDocumento
import com.solventa4bits.movil.solicitud.dominio.Vigencia
import com.solventa4bits.movil.solicitud.dominio.CodigoDePendiente
import com.solventa4bits.movil.solicitud.dominio.EstadoDeSolicitud
import com.solventa4bits.movil.solicitud.dominio.Pendiente
import com.solventa4bits.movil.solicitud.dominio.ResultadoConsulta
import com.solventa4bits.movil.solicitud.dominio.ResultadoLista
import com.solventa4bits.movil.solicitud.dominio.ResultadoCondiciones
import com.solventa4bits.movil.solicitud.dominio.ResultadoEnvio
import com.solventa4bits.movil.solicitud.dominio.Solicitud
import com.solventa4bits.movil.traza.TrazaFalsa
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.IOException

/** El adaptador de la solicitud contra un BFF falso (MockWebServer). */
class AdaptadorSolicitudesTest {

    private lateinit var servidor: MockWebServer
    private lateinit var traza: TrazaFalsa
    private val almacen = AlmacenEnMemoria(Sesion("jwt-de-acceso", "token-de-refresco", 3600))

    private val soloVida = DatosDeLaSolicitud(
        coberturas = setOf(CodigoCobertura.VIDA),
        cotizacionCreadaEn = "2026-10-06T14:35:00Z",
        versionDeCondiciones = "V1",
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

    private fun adaptador(): AdaptadorSolicitudes {
        val sesiones = object : ServicioDeSesion {
            override suspend fun iniciarSesion(correo: String, contrasena: String) = ResultadoInicioSesion.Fallo
            override suspend fun refrescar(tokenDeRefresco: String) = ResultadoInicioSesion.CredencialesInvalidas
        }
        return AdaptadorSolicitudes(
            crearSolicitudesApi(servidor.url("/mobile/").toString()),
            ConSesion(almacen, sesiones),
            traza,
        )
    }

    private fun responder(codigo: Int, cuerpo: String = "") {
        servidor.enqueue(MockResponse().setResponseCode(codigo).setBody(cuerpo))
    }

    private val condiciones = """[
        {"tipo": "poliza", "version": "V2", "titulo": "Condiciones de tu seguro",
         "baseLegal": "Ley 1328 de 2009", "contenido": "<h3>1 · Qué estás aceptando</h3><p>Texto</p>",
         "notaPie": "Aceptar estas condiciones no contrata todavía."}
    ]"""

    private fun poliza(
        estado: String,
        pendientes: String = "[]",
        emitidaEn: String? = null,
        rechazo: String = "null",
        id: String = ID,
    ) = """{
        "polizaId": "$id", "numero": null, "estado": "$estado",
        "producto": "VIDA_HIPOTECARIO", "creadaEn": "2026-10-27T14:41:00Z",
        "emitidaEn": ${emitidaEn?.let { "\"$it\"" } ?: "null"},
        "pendientes": $pendientes, "rechazo": $rechazo
    }"""

    @Test
    @DisplayName("trae las condiciones de la poliza sin token y para el mercado colombiano")
    fun traeLasCondiciones() = runTest {
        responder(200, condiciones)

        val resultado = adaptador().condiciones()

        assertEquals(
            ResultadoCondiciones.Exito(
                CondicionesDelSeguro(
                    version = "V2",
                    titulo = "Condiciones de tu seguro",
                    contenido = "<h3>1 · Qué estás aceptando</h3><p>Texto</p>",
                    notaPie = "Aceptar estas condiciones no contrata todavía.",
                ),
            ),
            resultado,
        )
        val peticion = servidor.takeRequest()
        assertEquals("GET", peticion.method)
        assertEquals("/mobile/v1/documentos-legales?tipo=poliza&mercado=CO", peticion.path)
        assertNull(peticion.getHeader("Authorization"))
    }

    @Test
    @DisplayName("sin el documento de la poliza en la respuesta, las condiciones fallan con traza")
    fun sinCondiciones() = runTest {
        responder(200, "[]")

        assertEquals(ResultadoCondiciones.Fallo, adaptador().condiciones())
        assertTrue(traza.errores.single().mensaje.contains("200"))
    }

    @Test
    @DisplayName("si no hay red al traer las condiciones responde controlado y deja traza")
    fun condicionesSinRed() = runTest {
        servidor.shutdown()

        assertEquals(ResultadoCondiciones.Fallo, adaptador().condiciones())
        assertInstanceOf(IOException::class.java, traza.errores.single().causa)
    }

    @Test
    @DisplayName("envia las coberturas, la cotizacion que vio y la version aceptada, con el token")
    fun enviaLaSolicitud() = runTest {
        responder(201, poliza("EMITIDA"))

        adaptador().enviar(soloVida)

        val peticion = servidor.takeRequest()
        assertEquals("POST", peticion.method)
        assertEquals("/mobile/v1/cuenta/polizas", peticion.path)
        assertEquals("Bearer jwt-de-acceso", peticion.getHeader("Authorization"))
        assertEquals(
            """{"coberturas":["VIDA"],"cotizacionCreadaEn":"2026-10-06T14:35:00Z",""" +
                """"documentosAceptados":[{"tipo":"poliza","version":"V1"}]}""",
            peticion.body.readUtf8(),
        )
    }

    @Test
    @DisplayName("con Tu casa envia tambien HOGAR, despues de VIDA")
    fun conTuCasa() = runTest {
        responder(201, poliza("EMITIDA"))

        adaptador().enviar(soloVida.copy(coberturas = setOf(CodigoCobertura.HOGAR, CodigoCobertura.VIDA)))

        assertTrue(servidor.takeRequest().body.readUtf8().contains(""""coberturas":["VIDA","HOGAR"]"""))
    }

    @Test
    @DisplayName("una solicitud emitida no tiene pendientes")
    fun emitida() = runTest {
        responder(201, poliza("EMITIDA", emitidaEn = "2026-10-27T14:41:05Z"))

        assertEquals(
            ResultadoEnvio.Enviada(
                Solicitud(
                    id = ID,
                    estado = EstadoDeSolicitud.EMITIDA,
                    creadaEn = "2026-10-27T14:41:00Z",
                    emitidaEn = "2026-10-27T14:41:05Z",
                    pendientes = emptyList(),
                    motivosDeRechazo = emptyList(),
                ),
            ),
            adaptador().enviar(soloVida),
        )
    }

    @Test
    @DisplayName("una solicitud pendiente trae los mensajes de lo que falta; un 200 tambien es envio")
    fun pendiente() = runTest {
        responder(
            200,
            poliza(
                "PENDIENTE",
                """[{"codigo": "CREDITO_NO_VERIFICADO", "tipo": "AGENTE",
                     "mensaje": "Los datos de tu crédito deben ser verificados por un agente."}]""",
            ),
        )

        val enviada = assertInstanceOf(ResultadoEnvio.Enviada::class.java, adaptador().enviar(soloVida))

        assertEquals(EstadoDeSolicitud.PENDIENTE, enviada.solicitud.estado)
        assertEquals(
            listOf(
                Pendiente(
                    CodigoDePendiente.CREDITO_NO_VERIFICADO,
                    "Los datos de tu crédito deben ser verificados por un agente.",
                ),
            ),
            enviada.solicitud.pendientes,
        )
    }

    @Test
    @DisplayName("un 409 (cotizacion vencida, cambiada o solicitud rechazada) pide cotizar de nuevo")
    fun cotizarDeNuevo() = runTest {
        responder(409, """{"title": "Conflicto", "status": 409, "codigo": "COTIZACION_VENCIDA"}""")

        assertEquals(ResultadoEnvio.CotizarDeNuevo, adaptador().enviar(soloVida))
    }

    @Test
    @DisplayName("si la sesion no se puede refrescar al enviar, la da por vencida")
    fun sesionVencida() = runTest {
        responder(401)

        assertEquals(ResultadoEnvio.SesionVencida, adaptador().enviar(soloVida))
    }

    @Test
    @DisplayName("si el BFF falla al enviar responde controlado y deja traza con el codigo")
    fun falloAlEnviar() = runTest {
        responder(503)

        assertEquals(ResultadoEnvio.Fallo, adaptador().enviar(soloVida))
        assertTrue(traza.errores.single().mensaje.contains("503"))
    }

    @Test
    @DisplayName("si no hay red al enviar no reintenta: responde controlado y deja traza")
    fun envioSinRed() = runTest {
        servidor.shutdown()

        assertEquals(ResultadoEnvio.Fallo, adaptador().enviar(soloVida))
        assertInstanceOf(IOException::class.java, traza.errores.single().causa)
    }

    // --- BITS-97: el resultado de la solicitud, al enviarla y al volver a verla.

    @Test
    @DisplayName("un pendiente con un codigo que la app no conoce queda sin codigo, con su mensaje")
    fun codigoDesconocido() = runTest {
        responder(
            201,
            poliza(
                "PENDIENTE",
                """[{"codigo": "CORREO_SIN_CONFIRMAR", "tipo": "USUARIO", "mensaje": "Revisa el enlace en tu correo"},
                    {"codigo": "ALGO_NUEVO", "tipo": "AGENTE", "mensaje": "Otra revision"}]""",
            ),
        )

        val enviada = assertInstanceOf(ResultadoEnvio.Enviada::class.java, adaptador().enviar(soloVida))

        assertEquals(
            listOf(
                Pendiente(CodigoDePendiente.CORREO_SIN_CONFIRMAR, "Revisa el enlace en tu correo"),
                Pendiente(null, "Otra revision"),
            ),
            enviada.solicitud.pendientes,
        )
    }

    @Test
    @DisplayName("un estado que la app no conoce se trata como pendiente")
    fun estadoDesconocido() = runTest {
        responder(201, poliza("ESTADO_NUEVO"))

        val enviada = assertInstanceOf(ResultadoEnvio.Enviada::class.java, adaptador().enviar(soloVida))

        assertEquals(EstadoDeSolicitud.PENDIENTE, enviada.solicitud.estado)
    }

    @Test
    @DisplayName("consulta una solicitud por su id, con el token, y lee su rechazo")
    fun consultaRechazada() = runTest {
        responder(
            200,
            poliza(
                "RECHAZADA",
                rechazo = """{"motivos": [{"codigo": "CREDITO_NO_VERIFICADO",
                    "mensaje": "Los datos de tu crédito no coinciden con los de tu banco."}],
                    "rechazadaEn": "2026-10-28T10:00:00Z"}""",
            ),
        )

        val resultado = assertInstanceOf(ResultadoConsulta.Exito::class.java, adaptador().consultar(ID))

        assertEquals(EstadoDeSolicitud.RECHAZADA, resultado.solicitud.estado)
        assertEquals(
            listOf("Los datos de tu crédito no coinciden con los de tu banco."),
            resultado.solicitud.motivosDeRechazo,
        )
        val peticion = servidor.takeRequest()
        assertEquals("GET", peticion.method)
        assertEquals("/mobile/v1/cuenta/polizas/$ID", peticion.path)
        assertEquals("Bearer jwt-de-acceso", peticion.getHeader("Authorization"))
    }

    @Test
    @DisplayName("una solicitud que no existe (404) se informa como tal")
    fun consultaInexistente() = runTest {
        responder(404)

        assertEquals(ResultadoConsulta.NoExiste, adaptador().consultar(ID))
    }

    @Test
    @DisplayName("al consultar, sesion vencida, error del servidor y sin red responden controlado")
    fun consultaConFallas() = runTest {
        responder(401)
        assertEquals(ResultadoConsulta.SesionVencida, adaptador().consultar(ID))

        responder(503)
        assertEquals(ResultadoConsulta.Fallo, adaptador().consultar(ID))
        assertTrue(traza.errores.single().mensaje.contains("503"))

        servidor.shutdown()
        assertEquals(ResultadoConsulta.Fallo, adaptador().consultar(ID))
    }

    @Test
    @DisplayName("lista las solicitudes del cliente en el orden en que llegan, con el token")
    fun lista() = runTest {
        responder(200, "[${poliza("PENDIENTE", id = "nueva")}, ${poliza("RECHAZADA", id = "vieja")}]")

        val resultado = assertInstanceOf(ResultadoLista.Exito::class.java, adaptador().listar())

        assertEquals(listOf("nueva", "vieja"), resultado.solicitudes.map { it.id })
        val peticion = servidor.takeRequest()
        assertEquals("/mobile/v1/cuenta/polizas", peticion.path)
        assertEquals("Bearer jwt-de-acceso", peticion.getHeader("Authorization"))
    }

    @Test
    @DisplayName("al listar, sesion vencida, error del servidor y sin red responden controlado")
    fun listaConFallas() = runTest {
        responder(401)
        assertEquals(ResultadoLista.SesionVencida, adaptador().listar())

        responder(503)
        assertEquals(ResultadoLista.Fallo, adaptador().listar())
        assertTrue(traza.errores.single().mensaje.contains("503"))

        servidor.shutdown()
        assertEquals(ResultadoLista.Fallo, adaptador().listar())
    }

    @Test
    @DisplayName("BITS-104, AC-1: una poliza emitida trae numero, vigencia, prima, credito y coberturas")
    fun polizaEmitida() = runTest {
        responder(
            200,
            """[{
                "polizaId": "$ID", "numero": "SOL-VH-2026-K7M2Q9", "estado": "EMITIDA",
                "producto": "VIDA_HIPOTECARIO", "creadaEn": "2026-10-22T14:41:00Z",
                "emitidaEn": "2026-10-22T15:00:00Z", "cotizacionCreadaEn": "2026-10-22T14:00:00Z",
                "vigencia": {"desde": "2026-10-22T15:00:00Z", "hasta": "2027-10-22T15:00:00Z"},
                "primaTotalMensual": 77374, "moneda": "COP",
                "datosCredito": {"valorCredito": 350000000, "plazoRestanteMeses": 180,
                    "entidadAcreedora": "Bancolombia S.A.", "saldoInsoluto": 320000000},
                "coberturas": [{"codigo": "VIDA", "obligatoria": true, "sumaAsegurada": 320000000,
                    "primaNeta": 50000, "iva": 0, "primaTotal": 50000},
                    {"codigo": "HOGAR", "obligatoria": false, "sumaAsegurada": 200000000,
                    "primaNeta": 23000, "iva": 4374, "primaTotal": 27374}],
                "pendientes": [], "documento": {"estado": "DISPONIBLE"}
            }]""",
        )

        val resultado = assertInstanceOf(ResultadoLista.Exito::class.java, adaptador().listar())

        val seguro = resultado.solicitudes.single()
        assertEquals("SOL-VH-2026-K7M2Q9", seguro.numero)
        assertEquals(Vigencia("2026-10-22T15:00:00Z", "2027-10-22T15:00:00Z"), seguro.vigencia)
        assertEquals("2026-10-22T14:00:00Z", seguro.cotizacionCreadaEn)
        assertEquals(77_374.0, seguro.primaMensual)
        assertEquals(CreditoAsegurado("Bancolombia S.A.", 320_000_000.0, 180), seguro.credito)
        assertTrue(seguro.conTuCasa)
    }

    @Test
    @DisplayName("el documento listo trae su enlace temporal, con el token")
    fun documentoListo() = runTest {
        responder(200, """{"estado": "LISTO", "url": "https://docs.example/poliza.pdf", "expiraEn": null}""")

        assertEquals(ResultadoDocumento.Listo("https://docs.example/poliza.pdf"), adaptador().documento(ID))
        val peticion = servidor.takeRequest()
        assertEquals("/mobile/v1/cuenta/polizas/$ID/documento", peticion.path)
        assertEquals("Bearer jwt-de-acceso", peticion.getHeader("Authorization"))
    }

    @Test
    @DisplayName("el documento que se esta generando, la sesion vencida y los errores responden controlado")
    fun documentoConEsperasYFallas() = runTest {
        responder(202, """{"estado": "GENERANDO", "url": null}""")
        assertEquals(ResultadoDocumento.Generando, adaptador().documento(ID))

        responder(401)
        assertEquals(ResultadoDocumento.SesionVencida, adaptador().documento(ID))

        responder(409, """{"codigo": "POLIZA_NO_EMITIDA"}""")
        assertEquals(ResultadoDocumento.Fallo, adaptador().documento(ID))
        assertTrue(traza.errores.single().mensaje.contains("409"))

        servidor.shutdown()
        assertEquals(ResultadoDocumento.Fallo, adaptador().documento(ID))
    }

    private companion object {
        const val ID = "7c0f2a52-3b6f-4a0e-9e7e-2f6c9d1a8b11"
    }
}
