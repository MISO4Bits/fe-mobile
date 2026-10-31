package com.solventa4bits.movil.avisos.datos

import com.solventa4bits.movil.avisos.dominio.ResultadoRegistro
import com.solventa4bits.movil.sesion.datos.ConSesion
import com.solventa4bits.movil.sesion.dominio.AlmacenEnMemoria
import com.solventa4bits.movil.sesion.dominio.ResultadoInicioSesion
import com.solventa4bits.movil.sesion.dominio.ServicioDeSesion
import com.solventa4bits.movil.sesion.dominio.Sesion
import com.solventa4bits.movil.traza.TrazaFalsa
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/** El adaptador de dispositivos contra un BFF falso (MockWebServer). */
class AdaptadorDispositivosTest {

    private lateinit var servidor: MockWebServer
    private lateinit var traza: TrazaFalsa
    private val almacen = AlmacenEnMemoria(Sesion("jwt-de-acceso", "token-de-refresco", 3600))

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

    private var refresco: ResultadoInicioSesion = ResultadoInicioSesion.CredencialesInvalidas

    private fun adaptador(): AdaptadorDispositivos {
        val sesiones = object : ServicioDeSesion {
            override suspend fun iniciarSesion(correo: String, contrasena: String) = ResultadoInicioSesion.Fallo

            override suspend fun refrescar(tokenDeRefresco: String) = refresco
        }
        return AdaptadorDispositivos(
            crearDispositivosApi(servidor.url("/mobile/").toString()),
            ConSesion(almacen, sesiones),
            traza,
        )
    }

    private fun responder(codigo: Int, cuerpo: String = "") {
        servidor.enqueue(MockResponse().setResponseCode(codigo).setBody(cuerpo))
    }

    @Test
    @DisplayName("registra con PUT /v1/cuenta/dispositivos: token, plataforma android y version, con la sesion")
    fun registra() = runTest {
        responder(200, """{"dispositivoId":"d-1"}""")

        val resultado = adaptador().registrar("fcm-token", "0.1.0")

        val peticion = servidor.takeRequest()
        assertEquals("PUT", peticion.method)
        assertEquals("/mobile/v1/cuenta/dispositivos", peticion.path)
        assertEquals("Bearer jwt-de-acceso", peticion.getHeader("Authorization"))
        assertEquals(
            RegistrarDispositivoDto("fcm-token", "android", "0.1.0"),
            Json.decodeFromString<RegistrarDispositivoDto>(peticion.body.readUtf8()),
        )
        assertEquals(ResultadoRegistro.Registrado("d-1"), resultado)
    }

    @Test
    @DisplayName("un campo nuevo en la respuesta no la rompe")
    fun campoNuevo() = runTest {
        responder(200, """{"dispositivoId":"d-1","extra":true}""")

        assertEquals(ResultadoRegistro.Registrado("d-1"), adaptador().registrar("t", "1"))
    }

    @Test
    @DisplayName("un error del BFF es Fallo y queda en la traza sin el token")
    fun errorDelBff() = runTest {
        responder(503)

        assertEquals(ResultadoRegistro.Fallo, adaptador().registrar("fcm-token-secreto", "1"))

        assertEquals(1, traza.errores.size)
        assertFalse(traza.errores.single().mensaje.contains("fcm-token-secreto"))
    }

    @Test
    @DisplayName("una respuesta fuera de contrato es Fallo")
    fun fueraDeContrato() = runTest {
        responder(200, """{"otraCosa":1}""")

        assertEquals(ResultadoRegistro.Fallo, adaptador().registrar("t", "1"))
        assertEquals(1, traza.errores.size)
    }

    @Test
    @DisplayName("sin red es Fallo y no lanza")
    fun sinRed() = runTest {
        servidor.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))

        assertEquals(ResultadoRegistro.Fallo, adaptador().registrar("t", "1"))
        assertEquals(1, traza.errores.size)
    }

    @Test
    @DisplayName("sin sesion guardada informa que la sesion vencio")
    fun sinSesion() = runTest {
        almacen.borrar()

        assertEquals(ResultadoRegistro.SesionVencida, adaptador().registrar("t", "1"))
        assertEquals(0, servidor.requestCount)
    }

    @Test
    @DisplayName("da de baja con DELETE /v1/cuenta/dispositivos/{id}")
    fun daDeBaja() = runTest {
        responder(204)

        assertTrue(adaptador().darDeBaja("d-1"))

        val peticion = servidor.takeRequest()
        assertEquals("DELETE", peticion.method)
        assertEquals("/mobile/v1/cuenta/dispositivos/d-1", peticion.path)
        assertEquals("Bearer jwt-de-acceso", peticion.getHeader("Authorization"))
    }

    @Test
    @DisplayName("una baja que falla, sin red o sin sesion devuelve false y no lanza")
    fun bajaFallida() = runTest {
        responder(503)
        assertFalse(adaptador().darDeBaja("d-1"))

        servidor.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))
        assertFalse(adaptador().darDeBaja("d-1"))

        almacen.borrar()
        assertFalse(adaptador().darDeBaja("d-1"))
    }

    @Test
    @DisplayName("un 204 sin cuerpo al registrar no trae el id: es Fallo")
    fun sinCuerpo() = runTest {
        responder(204)

        assertEquals(ResultadoRegistro.Fallo, adaptador().registrar("t", "1"))
        assertEquals(1, traza.errores.size)
    }

    @Test
    @DisplayName("si el token vencio y no se pudo refrescar por falta de red, no se registra ni se da de baja")
    fun sinRefresco() = runTest {
        refresco = ResultadoInicioSesion.Fallo
        responder(401)
        responder(401)

        assertEquals(ResultadoRegistro.Fallo, adaptador().registrar("t", "1"))
        assertFalse(adaptador().darDeBaja("d-1"))
    }

    @Test
    @DisplayName("si el token vencio y el refresco sirve, repite la llamada con el token nuevo")
    fun conRefresco() = runTest {
        refresco = ResultadoInicioSesion.Exito(Sesion("jwt-nuevo", "refresco-nuevo", 3600))
        responder(401)
        responder(200, """{"dispositivoId":"d-1"}""")

        assertEquals(ResultadoRegistro.Registrado("d-1"), adaptador().registrar("t", "1"))

        servidor.takeRequest()
        assertEquals("Bearer jwt-nuevo", servidor.takeRequest().getHeader("Authorization"))
    }
}
