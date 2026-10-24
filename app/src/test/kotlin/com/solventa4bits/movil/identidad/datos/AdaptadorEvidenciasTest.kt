package com.solventa4bits.movil.identidad.datos

import com.solventa4bits.movil.identidad.dominio.CaraDeCedula
import com.solventa4bits.movil.identidad.dominio.EstadoGuardado
import com.solventa4bits.movil.identidad.dominio.MotivoDeRechazo
import com.solventa4bits.movil.identidad.dominio.ResultadoEstado
import com.solventa4bits.movil.identidad.dominio.ResultadoSubida
import com.solventa4bits.movil.identidad.dominio.Veredicto
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

/** El adaptador de evidencias de identidad contra un BFF falso (MockWebServer). */
class AdaptadorEvidenciasTest {

    private lateinit var servidor: MockWebServer
    private lateinit var traza: TrazaFalsa
    private val almacen = AlmacenEnMemoria(Sesion("jwt-de-acceso", "token-de-refresco", 3600))
    private val foto = byteArrayOf(1, 2, 3)

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

    private fun adaptador(): AdaptadorEvidencias {
        val sesiones = object : ServicioDeSesion {
            override suspend fun iniciarSesion(correo: String, contrasena: String) = ResultadoInicioSesion.Fallo
            override suspend fun refrescar(tokenDeRefresco: String) = ResultadoInicioSesion.CredencialesInvalidas
        }
        return AdaptadorEvidencias(
            crearEvidenciasApi(servidor.url("/mobile/").toString()),
            ConSesion(almacen, sesiones),
            traza,
        )
    }

    private fun responder(codigo: Int, cuerpo: String = "") {
        servidor.enqueue(MockResponse().setResponseCode(codigo).setBody(cuerpo))
    }

    private fun subida(estado: String, motivo: String? = null, intentos: Int = 3) =
        """{"evidenciaId": null, "tipo": "cedula_reverso", "estado": "$estado",
           "motivo": ${motivo?.let { "\"$it\"" } ?: "null"}, "intentosRestantes": $intentos}"""

    @Test
    @DisplayName("sube la foto por multipart con su tipo, como JPEG y con el token de la sesion")
    fun subeLaFoto() = runTest {
        responder(201, subida("ACEPTADA"))

        adaptador().subir(CaraDeCedula.REVERSO, foto)

        val peticion = servidor.takeRequest()
        val cuerpo = peticion.body.readUtf8()
        assertEquals("POST", peticion.method)
        assertEquals("/mobile/v1/cuenta/evidencias-identidad", peticion.path)
        assertEquals("Bearer jwt-de-acceso", peticion.getHeader("Authorization"))
        assertTrue(peticion.getHeader("Content-Type")!!.startsWith("multipart/form-data"))
        assertTrue(cuerpo.contains("name=\"tipo\""))
        assertTrue(cuerpo.contains("cedula_reverso"))
        assertTrue(cuerpo.contains("name=\"imagen\"; filename=\"cedula_reverso.jpg\""))
        assertTrue(cuerpo.contains("Content-Type: image/jpeg"))
    }

    @Test
    @DisplayName("201 con ACEPTADA es una foto que sirve")
    fun aceptada() = runTest {
        responder(201, subida("ACEPTADA"))

        assertEquals(ResultadoSubida.Exito(Veredicto.Aceptada), adaptador().subir(CaraDeCedula.FRENTE, foto))
    }

    @Test
    @DisplayName("200 con REINTENTAR trae el motivo y los intentos que quedan")
    fun reintentar() = runTest {
        responder(200, subida("REINTENTAR", motivo = "BORROSA", intentos = 2))

        assertEquals(
            ResultadoSubida.Exito(Veredicto.Reintentar(MotivoDeRechazo.BORROSA, 2)),
            adaptador().subir(CaraDeCedula.REVERSO, foto),
        )
    }

    @Test
    @DisplayName("un motivo que la app no conoce no la rompe: queda sin motivo")
    fun motivoDesconocido() = runTest {
        responder(200, subida("REINTENTAR", motivo = "MOTIVO_NUEVO", intentos = 1))

        assertEquals(
            ResultadoSubida.Exito(Veredicto.Reintentar(null, 1)),
            adaptador().subir(CaraDeCedula.REVERSO, foto),
        )
    }

    @Test
    @DisplayName("201 con REVISION_MANUAL deja seguir")
    fun revisionManual() = runTest {
        responder(201, subida("REVISION_MANUAL", intentos = 0))

        assertEquals(ResultadoSubida.Exito(Veredicto.RevisionManual), adaptador().subir(CaraDeCedula.REVERSO, foto))
    }

    @Test
    @DisplayName("si la sesion no se puede refrescar al subir, la da por vencida")
    fun sesionVencidaAlSubir() = runTest {
        responder(401)

        assertEquals(ResultadoSubida.SesionVencida, adaptador().subir(CaraDeCedula.FRENTE, foto))
    }

    @Test
    @DisplayName("si el BFF falla al subir responde controlado y deja traza con el codigo")
    fun falloAlSubir() = runTest {
        responder(503)

        assertEquals(ResultadoSubida.Fallo, adaptador().subir(CaraDeCedula.FRENTE, foto))
        assertTrue(traza.errores.single().mensaje.contains("503"))
    }

    @Test
    @DisplayName("si no hay red al subir no reintenta: responde controlado y deja traza")
    fun sinRedAlSubir() = runTest {
        servidor.shutdown()

        assertEquals(ResultadoSubida.Fallo, adaptador().subir(CaraDeCedula.FRENTE, foto))
        assertInstanceOf(IOException::class.java, traza.errores.single().causa)
    }

    @Test
    @DisplayName("consulta lo guardado de cada cara e ignora la selfie")
    fun consulta() = runTest {
        responder(
            200,
            """[
              {"tipo": "cedula_frente", "estado": "ACEPTADA", "intentosRestantes": 3},
              {"tipo": "cedula_reverso", "estado": "REVISION_MANUAL", "intentosRestantes": 0},
              {"tipo": "selfie", "estado": "PENDIENTE", "intentosRestantes": 3}
            ]""",
        )

        val resultado = adaptador().consultar()

        assertEquals(
            ResultadoEstado.Exito(
                mapOf(
                    CaraDeCedula.FRENTE to EstadoGuardado.ACEPTADA,
                    CaraDeCedula.REVERSO to EstadoGuardado.REVISION_MANUAL,
                ),
            ),
            resultado,
        )
        val peticion = servidor.takeRequest()
        assertEquals("GET", peticion.method)
        assertEquals("/mobile/v1/cuenta/evidencias-identidad", peticion.path)
    }

    @Test
    @DisplayName("una cara que no viene en la consulta queda pendiente")
    fun caraQueNoViene() = runTest {
        responder(200, "[]")

        assertEquals(
            ResultadoEstado.Exito(
                mapOf(
                    CaraDeCedula.FRENTE to EstadoGuardado.PENDIENTE,
                    CaraDeCedula.REVERSO to EstadoGuardado.PENDIENTE,
                ),
            ),
            adaptador().consultar(),
        )
    }

    @Test
    @DisplayName("si el BFF falla al consultar responde controlado y deja traza")
    fun falloAlConsultar() = runTest {
        responder(503)

        assertEquals(ResultadoEstado.Fallo, adaptador().consultar())
        assertTrue(traza.errores.single().mensaje.contains("503"))
    }

    @Test
    @DisplayName("si la sesion no se puede refrescar al consultar, la da por vencida")
    fun sesionVencidaAlConsultar() = runTest {
        responder(401)

        assertEquals(ResultadoEstado.SesionVencida, adaptador().consultar())
    }

    @Test
    @DisplayName("una respuesta fuera de contrato al consultar responde controlado")
    fun fueraDeContrato() = runTest {
        responder(200, """{"no": "es una lista"}""")

        assertEquals(ResultadoEstado.Fallo, adaptador().consultar())
    }
}
