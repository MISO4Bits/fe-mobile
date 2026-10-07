package com.solventa4bits.movil.sesion.datos

import com.solventa4bits.movil.sesion.dominio.ResultadoInicioSesion
import com.solventa4bits.movil.sesion.dominio.Sesion
import com.solventa4bits.movil.traza.TrazaFalsa
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.IOException

/**
 * El adaptador contra un BFF falso (MockWebServer): un servidor HTTP real que
 * corre dentro de la prueba y responde lo que se le encola.
 */
class AdaptadorSesionTest {

    private lateinit var servidor: MockWebServer
    private lateinit var traza: TrazaFalsa
    private lateinit var adaptador: AdaptadorSesion

    @BeforeEach
    fun preparar() {
        servidor = MockWebServer()
        servidor.start()
        traza = TrazaFalsa()
        adaptador = AdaptadorSesion(crearSesionApi(servidor.url("/web/").toString()), traza)
    }

    @AfterEach
    fun apagar() {
        servidor.shutdown()
    }

    @Test
    @DisplayName("con credenciales correctas devuelve la sesion que entrega el BFF")
    fun credencialesCorrectas() = runTest {
        servidor.enqueue(MockResponse().setResponseCode(200).setBody(SESION_VALIDA))

        val resultado = adaptador.iniciarSesion(CORREO, CONTRASENA)

        assertEquals(
            ResultadoInicioSesion.Exito(Sesion("jwt-de-acceso", "token-de-refresco", 3600)),
            resultado,
        )
        assertTrue(traza.errores.isEmpty())
    }

    @Test
    @DisplayName("llama a POST /v1/sesiones con los campos del contrato")
    fun respetaElContrato() = runTest {
        servidor.enqueue(MockResponse().setResponseCode(200).setBody(SESION_VALIDA))

        adaptador.iniciarSesion(CORREO, CONTRASENA)

        val peticion = servidor.takeRequest()
        assertEquals("POST", peticion.method)
        assertEquals("/web/v1/sesiones", peticion.path)
        assertEquals("""{"email":"$CORREO","password":"$CONTRASENA"}""", peticion.body.readUtf8())
    }

    @Test
    @DisplayName("si el BFF responde 401 informa credenciales invalidas")
    fun credencialesInvalidas() = runTest {
        servidor.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setHeader("Content-Type", "application/problem+json")
                .setBody("""{"title":"No autorizado","status":401}"""),
        )

        val resultado = adaptador.iniciarSesion(CORREO, CONTRASENA)

        assertEquals(ResultadoInicioSesion.CredencialesInvalidas, resultado)
    }

    @Test
    @DisplayName("si no hay red captura la excepcion, responde controlado y deja traza")
    fun sinConexion() = runTest {
        servidor.shutdown()

        val resultado = adaptador.iniciarSesion(CORREO, CONTRASENA)

        assertEquals(ResultadoInicioSesion.Fallo, resultado)
        assertEquals(1, traza.errores.size)
        assertInstanceOf(IOException::class.java, traza.errores.single().causa)
    }

    private companion object {
        const val CORREO = "ana@correo.com"
        const val CONTRASENA = "Clave-Segura-123"
        const val SESION_VALIDA = """
            {
              "accessToken": "jwt-de-acceso",
              "tokenType": "Bearer",
              "expiresIn": 3600,
              "refreshToken": "token-de-refresco"
            }
        """
    }
}
