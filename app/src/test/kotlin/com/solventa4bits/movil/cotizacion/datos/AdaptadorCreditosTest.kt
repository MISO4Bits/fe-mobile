package com.solventa4bits.movil.cotizacion.datos

import com.solventa4bits.movil.cotizacion.dominio.CreditoHipotecario
import com.solventa4bits.movil.cotizacion.dominio.EntidadFinanciera
import com.solventa4bits.movil.cotizacion.dominio.EstadoCreditos
import com.solventa4bits.movil.cotizacion.dominio.ResultadoCreditos
import com.solventa4bits.movil.fixtures.Personas
import com.solventa4bits.movil.fixtures.RespuestasBff
import com.solventa4bits.movil.sesion.dominio.AlmacenEnMemoria
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

/** El adaptador de hipotecas contra un BFF falso (MockWebServer). */
class AdaptadorCreditosTest {

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

    private fun adaptador() =
        AdaptadorCreditos(crearCreditosApi(servidor.url("/mobile/").toString()), almacen, traza)

    @Test
    @DisplayName("devuelve la hipoteca del cliente y los bancos que entrega el BFF")
    fun hipotecaDisponible() = runTest {
        servidor.enqueue(MockResponse().setResponseCode(200).setBody(RespuestasBff.creditosDe(Personas.DANIEL)))

        val resultado = adaptador().consultar()

        val creditos = assertInstanceOf(ResultadoCreditos.Exito::class.java, resultado).creditos
        assertEquals(EstadoCreditos.DISPONIBLE, creditos.estado)
        assertEquals(
            listOf(
                CreditoHipotecario(
                    entidadId = "bancolombia",
                    entidadNombre = "BANCOLOMBIA S.A.",
                    valorCredito = 180_000_000.0,
                    saldoInsoluto = 98_000_000.0,
                    plazoRestanteMeses = 96,
                    cuotaMensual = 1_650_000.0,
                ),
            ),
            creditos.creditos,
        )
        assertTrue(creditos.entidades.contains(EntidadFinanciera("bancolombia", "Bancolombia")))
    }

    @Test
    @DisplayName("llama a GET /v1/creditos-hipotecarios con el token de acceso de la sesion")
    fun enviaElToken() = runTest {
        servidor.enqueue(MockResponse().setResponseCode(200).setBody(RespuestasBff.creditosDe(Personas.DANIEL)))

        adaptador().consultar()

        val peticion = servidor.takeRequest()
        assertEquals("GET", peticion.method)
        assertEquals("/mobile/v1/creditos-hipotecarios", peticion.path)
        assertEquals("Bearer jwt-de-acceso", peticion.getHeader("Authorization"))
    }

    @Test
    @DisplayName("si el cliente no tiene hipotecas lo informa con la lista de bancos")
    fun sinHipotecas() = runTest {
        servidor.enqueue(MockResponse().setResponseCode(200).setBody(RespuestasBff.creditosDe(Personas.NICOLAS)))

        val resultado = adaptador().consultar()

        val creditos = assertInstanceOf(ResultadoCreditos.Exito::class.java, resultado).creditos
        assertEquals(EstadoCreditos.SIN_HIPOTECAS, creditos.estado)
        assertTrue(creditos.creditos.isEmpty())
        assertTrue(creditos.entidades.isNotEmpty())
    }

    @Test
    @DisplayName("si el BFF rechaza el token con 401 informa que la sesion vencio")
    fun tokenRechazado() = runTest {
        servidor.enqueue(MockResponse().setResponseCode(401))

        assertEquals(ResultadoCreditos.SesionVencida, adaptador().consultar())
    }

    @Test
    @DisplayName("si no hay sesion guardada informa que vencio sin llamar al BFF")
    fun sinSesion() = runTest {
        almacen.borrar()

        assertEquals(ResultadoCreditos.SesionVencida, adaptador().consultar())
        assertEquals(0, servidor.requestCount)
    }

    @Test
    @DisplayName("si el BFF falla con 503 responde controlado y deja traza con el codigo")
    fun fallaDelServidor() = runTest {
        servidor.enqueue(MockResponse().setResponseCode(503))

        assertEquals(ResultadoCreditos.Fallo, adaptador().consultar())
        assertTrue(traza.errores.single().mensaje.contains("503"))
    }

    @Test
    @DisplayName("si no hay red captura la excepcion, responde controlado y deja traza")
    fun sinRed() = runTest {
        servidor.shutdown()

        assertEquals(ResultadoCreditos.Fallo, adaptador().consultar())
        assertInstanceOf(IOException::class.java, traza.errores.single().causa)
    }
}
