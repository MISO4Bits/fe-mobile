package com.solventa4bits.movil.fixtures

import com.solventa4bits.movil.fotoGenerada
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import org.junit.rules.ExternalResource
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/**
 * Una cuenta nueva en dev por prueba, para que ninguna herede lo que dejo otra.
 * El movil no tiene registro: la cuenta se crea por el BFF web, y las fotos de
 * identidad se siembran por el BFF movil. Son tres fotos grises por tipo, que
 * el servidor no acepta: la tercera queda en revision manual, que basta para
 * enviar la solicitud sin subir la foto de una persona real.
 */
class CuentaEnDev(private val urlBase: String) : ExternalResource() {

    private val http = OkHttpClient.Builder().readTimeout(LECTURA_S, TimeUnit.SECONDS).build()
    private val json = "application/json".toMediaType()

    // La cedula es inventada: Open Finance la atiende con la persona por defecto,
    // con su hipoteca en BBVA Colombia.
    private val documento = "3" + (1..9).joinToString("") { Random.nextInt(10).toString() }

    val correo = "danielr.ulloa+e2e$documento@gmail.com"
    val contrasena = "E2e-" + UUID.randomUUID().toString().take(12)

    /** URL del BFF movil, con la "/" final que exige Retrofit. */
    val urlBffMovil: String
        get() = "${urlBase}mobile/"

    override fun before() {
        registrar()
        val token = JSONObject(enviar(sesion())).getString("accessToken")
        for (tipo in listOf("cedula_frente", "cedula_reverso", "selfie")) {
            // Mientras pida otra foto se reintenta; la selfie puede quedar en revision a la primera.
            var estado = REINTENTAR
            var intentos = 0
            while (estado == REINTENTAR && intentos++ < INTENTOS) {
                estado = JSONObject(enviar(foto(tipo, token))).getString("estado")
            }
        }
    }

    private fun registrar() {
        val cuerpo = JSONObject()
            .put("email", correo)
            .put("password", contrasena)
            .put("tipoDocumento", "CC")
            .put("numeroDocumento", documento)
            .put("primerNombre", "Prueba")
            .put("primerApellido", "Movil")
            .put("fechaNacimiento", "1990-05-10")
            .put("telefono", "+57300" + documento.takeLast(7))
            .put("politicaVersion", "v1")
            .put("aceptaTerminos", true)
            .put("autorizaTratamientoDatos", true)
            .put("politicaVersionTratamientoDatos", "v1")
            .put("autorizaDatosFinancieros", true)
            .put("politicaVersionDatosFinancieros", "v1")
        enviar(
            Request.Builder().url("${urlBase}web/v1/registro")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .post(cuerpo.toString().toRequestBody(json)).build(),
        )
    }

    private fun sesion(): Request {
        val cuerpo = JSONObject().put("email", correo).put("password", contrasena)
        return Request.Builder().url("${urlBffMovil}v1/sesiones").post(cuerpo.toString().toRequestBody(json)).build()
    }

    private fun foto(tipo: String, token: String): Request {
        val imagen = fotoGenerada().toRequestBody("image/jpeg".toMediaType())
        val cuerpo = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("tipo", tipo)
            .addFormDataPart("imagen", "$tipo.jpg", imagen)
            .build()
        return Request.Builder().url("${urlBffMovil}v1/cuenta/evidencias-identidad")
            .header("Authorization", "Bearer $token").post(cuerpo).build()
    }

    private fun enviar(peticion: Request): String =
        http.newCall(peticion).execute().use { respuesta ->
            val cuerpo = respuesta.body?.string().orEmpty()
            check(respuesta.isSuccessful) {
                "${peticion.method} ${peticion.url.encodedPath}: ${respuesta.code} $cuerpo"
            }
            cuerpo
        }

    private companion object {
        const val INTENTOS = 3
        const val REINTENTAR = "REINTENTAR"
        const val LECTURA_S = 30L
    }
}
