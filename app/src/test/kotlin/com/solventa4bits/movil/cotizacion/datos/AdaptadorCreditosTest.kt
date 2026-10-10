package com.solventa4bits.movil.cotizacion.datos

import com.solventa4bits.movil.cotizacion.dominio.CreditoHipotecario
import com.solventa4bits.movil.cotizacion.dominio.EntidadFinanciera
import com.solventa4bits.movil.cotizacion.dominio.EstadoCreditos
import com.solventa4bits.movil.cotizacion.dominio.ResultadoCreditos
import com.solventa4bits.movil.fixtures.Personas
import com.solventa4bits.movil.fixtures.RespuestasBff
import com.solventa4bits.movil.sesion.dominio.AlmacenEnMemoria
import com.solventa4bits.movil.sesion.dominio.ResultadoInicioSesion
import com.solventa4bits.movil.sesion.dominio.ServicioDeSesion
import com.solventa4bits.movil.sesion.dominio.Sesion
import com.solventa4bits.movil.traza.TrazaFalsa
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
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

    private val sesiones = SesionesFalsas()

    private fun adaptador() =
        AdaptadorCreditos(crearCreditosApi(servidor.url("/mobile/").toString()), almacen, sesiones, traza)

    /** Doble de prueba del refresco: responde [refresco] y cuenta las veces que lo llaman. */
    private class SesionesFalsas : ServicioDeSesion {
        var refresco: ResultadoInicioSesion = ResultadoInicioSesion.CredencialesInvalidas
        var refrescos = 0

        override suspend fun iniciarSesion(correo: String, contrasena: String) = ResultadoInicioSesion.Fallo

        override suspend fun refrescar(tokenDeRefresco: String): ResultadoInicioSesion {
            refrescos++
            return refresco
        }
    }

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
    @DisplayName("si el token de acceso vencio refresca la sesion y repite la consulta sin que se note")
    fun refrescaYReintenta() = runTest {
        val nueva = Sesion("jwt-nuevo", "refresco-nuevo", 3600)
        sesiones.refresco = ResultadoInicioSesion.Exito(nueva)
        servidor.enqueue(MockResponse().setResponseCode(401))
        servidor.enqueue(MockResponse().setResponseCode(200).setBody(RespuestasBff.creditosDe(Personas.DANIEL)))

        val resultado = adaptador().consultar()

        assertInstanceOf(ResultadoCreditos.Exito::class.java, resultado)
        assertEquals(nueva, almacen.leer())
        assertEquals("Bearer jwt-de-acceso", servidor.takeRequest().getHeader("Authorization"))
        assertEquals("Bearer jwt-nuevo", servidor.takeRequest().getHeader("Authorization"))
    }

    @Test
    @DisplayName("si el token de refresco tambien vencio informa que la sesion vencio")
    fun refrescoVencido() = runTest {
        sesiones.refresco = ResultadoInicioSesion.CredencialesInvalidas
        servidor.enqueue(MockResponse().setResponseCode(401))

        assertEquals(ResultadoCreditos.SesionVencida, adaptador().consultar())
        assertEquals(1, servidor.requestCount)
    }

    @Test
    @DisplayName("si no se puede refrescar por falta de red responde con fallo y conserva la sesion")
    fun refrescoSinRed() = runTest {
        sesiones.refresco = ResultadoInicioSesion.Fallo
        servidor.enqueue(MockResponse().setResponseCode(401))

        assertEquals(ResultadoCreditos.Fallo, adaptador().consultar())
        assertEquals(Sesion("jwt-de-acceso", "token-de-refresco", 3600), almacen.leer())
    }

    @Test
    @DisplayName("si el token nuevo tambien es rechazado no insiste: un solo reintento")
    fun unSoloReintento() = runTest {
        sesiones.refresco = ResultadoInicioSesion.Exito(Sesion("jwt-nuevo", "refresco-nuevo", 3600))
        servidor.enqueue(MockResponse().setResponseCode(401))
        servidor.enqueue(MockResponse().setResponseCode(401))

        assertEquals(ResultadoCreditos.SesionVencida, adaptador().consultar())
        assertEquals(2, servidor.requestCount)
        assertEquals(1, sesiones.refrescos)
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
    @DisplayName("si la respuesta no cumple el contrato responde controlado y deja traza")
    fun respuestaFueraDeContrato() = runTest {
        servidor.enqueue(MockResponse().setResponseCode(200).setBody("{\"inesperado\": true}"))

        assertEquals(ResultadoCreditos.Fallo, adaptador().consultar())
        assertInstanceOf(SerializationException::class.java, traza.errores.single().causa)
    }

    @Test
    @DisplayName("si no hay red captura la excepcion, responde controlado y deja traza")
    fun sinRed() = runTest {
        servidor.shutdown()

        assertEquals(ResultadoCreditos.Fallo, adaptador().consultar())
        assertInstanceOf(IOException::class.java, traza.errores.single().causa)
    }
}
