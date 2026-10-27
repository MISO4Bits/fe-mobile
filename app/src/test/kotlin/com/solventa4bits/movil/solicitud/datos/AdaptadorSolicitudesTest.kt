package com.solventa4bits.movil.solicitud.datos

import com.solventa4bits.movil.cotizacion.dominio.CodigoCobertura
import com.solventa4bits.movil.sesion.datos.ConSesion
import com.solventa4bits.movil.sesion.dominio.AlmacenEnMemoria
import com.solventa4bits.movil.sesion.dominio.ResultadoInicioSesion
import com.solventa4bits.movil.sesion.dominio.ServicioDeSesion
import com.solventa4bits.movil.sesion.dominio.Sesion
import com.solventa4bits.movil.solicitud.dominio.CondicionesDelSeguro
import com.solventa4bits.movil.solicitud.dominio.DatosDeLaSolicitud
import com.solventa4bits.movil.solicitud.dominio.EstadoDeSolicitud
import com.solventa4bits.movil.solicitud.dominio.ResultadoCondiciones
import com.solventa4bits.movil.solicitud.dominio.ResultadoEnvio
import com.solventa4bits.movil.solicitud.dominio.SolicitudEnviada
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

    private fun poliza(estado: String, pendientes: String = "[]") = """{
        "polizaId": "7c0f2a52-3b6f-4a0e-9e7e-2f6c9d1a8b11", "numero": null, "estado": "$estado",
        "producto": "VIDA_HIPOTECARIO", "pendientes": $pendientes
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
        responder(201, poliza("EMITIDA"))

        assertEquals(
            ResultadoEnvio.Enviada(
                SolicitudEnviada("7c0f2a52-3b6f-4a0e-9e7e-2f6c9d1a8b11", EstadoDeSolicitud.EMITIDA, emptyList()),
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
            listOf("Los datos de tu crédito deben ser verificados por un agente."),
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
}
