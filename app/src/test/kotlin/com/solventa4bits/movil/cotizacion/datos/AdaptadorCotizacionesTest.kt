package com.solventa4bits.movil.cotizacion.datos

import com.solventa4bits.movil.cotizacion.dominio.CodigoCobertura
import com.solventa4bits.movil.cotizacion.dominio.Cotizacion
import com.solventa4bits.movil.cotizacion.dominio.DatosDelCredito
import com.solventa4bits.movil.cotizacion.dominio.EfectoEnElPrecio
import com.solventa4bits.movil.cotizacion.dominio.FactorDelPrecio
import com.solventa4bits.movil.cotizacion.dominio.PrecioDeCobertura
import com.solventa4bits.movil.cotizacion.dominio.ResultadoCotizacion
import com.solventa4bits.movil.fixtures.RespuestasBff
import com.solventa4bits.movil.sesion.datos.ConSesion
import com.solventa4bits.movil.sesion.dominio.AlmacenEnMemoria
import com.solventa4bits.movil.sesion.dominio.ResultadoInicioSesion
import com.solventa4bits.movil.sesion.dominio.ServicioDeSesion
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

/** El adaptador de cotizaciones contra un BFF falso (MockWebServer). */
class AdaptadorCotizacionesTest {

    private lateinit var servidor: MockWebServer
    private lateinit var traza: TrazaFalsa
    private val almacen = AlmacenEnMemoria(Sesion("jwt-de-acceso", "token-de-refresco", 3600))
    private var refresco: ResultadoInicioSesion = ResultadoInicioSesion.CredencialesInvalidas

    private val datos = DatosDelCredito(
        entidadAcreedora = "Bancolombia",
        valorCredito = 250_000_000,
        saldoInsoluto = 238_500_000,
        plazoRestanteMeses = 180,
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

    private fun adaptador(): AdaptadorCotizaciones {
        val sesiones = object : ServicioDeSesion {
            override suspend fun iniciarSesion(correo: String, contrasena: String) = ResultadoInicioSesion.Fallo
            override suspend fun refrescar(tokenDeRefresco: String) = refresco
        }
        return AdaptadorCotizaciones(
            crearCotizacionesApi(servidor.url("/mobile/").toString()),
            ConSesion(almacen, sesiones),
            traza,
        )
    }

    private fun responder(codigo: Int, cuerpo: String = "") {
        servidor.enqueue(MockResponse().setResponseCode(codigo).setBody(cuerpo))
    }

    @Test
    @DisplayName("cotiza con POST /v1/cotizaciones, solo con los datos del credito y el token de la sesion")
    fun enviaLosDatosDelCredito() = runTest {
        responder(201, RespuestasBff.cotizacion())

        adaptador().cotizar(datos)

        val peticion = servidor.takeRequest()
        assertEquals("POST", peticion.method)
        assertEquals("/mobile/v1/cotizaciones", peticion.path)
        assertEquals("Bearer jwt-de-acceso", peticion.getHeader("Authorization"))
        assertEquals(
            """{"datosCredito":{"valorCredito":250000000,"plazoRestanteMeses":180,""" +
                """"entidadAcreedora":"Bancolombia","saldoInsoluto":238500000}}""",
            peticion.body.readUtf8(),
        )
    }

    @Test
    @DisplayName("devuelve la cotizacion personalizada con sus factores, tal como la calcula el backend")
    fun cotizacionPersonalizada() = runTest {
        responder(201, RespuestasBff.cotizacion())

        val resultado = adaptador().cotizar(datos)

        assertEquals(
            Cotizacion(
                vigente = true,
                datosCredito = datos,
                datosDeclarados = true,
                primaMensual = 39_734.0,
                primaBaseMensual = 46_746.0,
                sumaAsegurada = 238_500_000.0,
                coberturaMeses = 180,
                personalizada = true,
                factores = listOf(
                    FactorDelPrecio("Historial de pagos excelente", EfectoEnElPrecio.BAJA, 0.1),
                    FactorDelPrecio("Endeudamiento bajo", EfectoEnElPrecio.BAJA, 0.2),
                ),
                coberturas = listOf(
                    PrecioDeCobertura(CodigoCobertura.VIDA, 238_500_000.0, 39_734.0, 0.0, 39_734.0),
                    PrecioDeCobertura(CodigoCobertura.HOGAR, 240_000_000.0, 23_760.0, 4_514.0, 28_274.0),
                ),
            ),
            assertInstanceOf(ResultadoCotizacion.Exito::class.java, resultado).cotizacion,
        )
    }

    @Test
    @DisplayName("ignora una cobertura que la app aun no conoce y lee una oferta sin desglose")
    fun coberturasDesconocidas() = runTest {
        responder(201, RespuestasBff.cotizacion().replace("\"HOGAR\"", "\"ROBO\""))

        val cotizacion = assertInstanceOf(ResultadoCotizacion.Exito::class.java, adaptador().cotizar(datos)).cotizacion

        assertEquals(listOf(CodigoCobertura.VIDA), cotizacion.coberturas.map { it.codigo })
    }

    @Test
    @DisplayName("sin perfil la cotizacion llega con tarifa estandar y sin factores")
    fun cotizacionSinPerfil() = runTest {
        responder(201, RespuestasBff.cotizacion(conPerfil = false))

        val cotizacion = assertInstanceOf(ResultadoCotizacion.Exito::class.java, adaptador().cotizar(datos)).cotizacion

        assertEquals(false, cotizacion.personalizada)
        assertTrue(cotizacion.factores.isEmpty())
    }

    @Test
    @DisplayName("si el backend rechaza los datos devuelve el mensaje del campo")
    fun datosRechazados() = runTest {
        responder(
            400,
            """{"title":"Solicitud inválida","status":400,""" +
                """"errores":[{"campo":"plazoRestanteMeses","mensaje":"El crédito terminaría a los 80 años"}]}""",
        )

        assertEquals(
            ResultadoCotizacion.DatosRechazados("El crédito terminaría a los 80 años"),
            adaptador().cotizar(datos),
        )
    }

    @Test
    @DisplayName("consulta la cotizacion activa con GET /v1/cotizaciones/actual")
    fun cotizacionActiva() = runTest {
        responder(200, RespuestasBff.cotizacion())

        val resultado = adaptador().consultarActual()

        assertInstanceOf(ResultadoCotizacion.Exito::class.java, resultado)
        assertEquals("/mobile/v1/cotizaciones/actual", servidor.takeRequest().path)
    }

    @Test
    @DisplayName("si el cliente no tiene cotizacion activa lo informa")
    fun sinCotizacionActiva() = runTest {
        responder(404)

        assertEquals(ResultadoCotizacion.SinCotizacion, adaptador().consultarActual())
    }

    @Test
    @DisplayName("si el token vencio refresca la sesion y repite la cotizacion")
    fun refrescaYRepite() = runTest {
        refresco = ResultadoInicioSesion.Exito(Sesion("jwt-nuevo", "refresco-nuevo", 3600))
        responder(401)
        responder(201, RespuestasBff.cotizacion())

        assertInstanceOf(ResultadoCotizacion.Exito::class.java, adaptador().cotizar(datos))
        servidor.takeRequest()
        assertEquals("Bearer jwt-nuevo", servidor.takeRequest().getHeader("Authorization"))
    }

    @Test
    @DisplayName("si el BFF falla con 503 responde controlado y deja traza con el codigo")
    fun fallaDelServidor() = runTest {
        responder(503)

        assertEquals(ResultadoCotizacion.Fallo, adaptador().cotizar(datos))
        assertTrue(traza.errores.single().mensaje.contains("503"))
    }

    @Test
    @DisplayName("si no hay red captura la excepcion, responde controlado y deja traza")
    fun sinRed() = runTest {
        servidor.shutdown()

        assertEquals(ResultadoCotizacion.Fallo, adaptador().consultarActual())
        assertInstanceOf(IOException::class.java, traza.errores.single().causa)
    }
}
