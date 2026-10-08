package com.solventa4bits.movil.sesion.datos

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

/** Operaciones de sesion del BFF. El contrato vive en bff-mobile, openapi/openapi.yaml. */
interface SesionApi {
    @POST("v1/sesiones")
    suspend fun iniciarSesion(@Body credenciales: CredencialesDto): Response<SesionDto>

    @POST("v1/sesiones/refresco")
    suspend fun refrescar(@Body refresco: RefrescoDto): Response<SesionDto>
}

/** Los nombres de los campos son los del contrato: van en ingles. */
@Serializable
data class CredencialesDto(
    val email: String,
    val password: String,
)

@Serializable
data class RefrescoDto(val refreshToken: String)

@Serializable
data class SesionDto(
    val accessToken: String,
    val tokenType: String,
    val expiresIn: Long,
    val refreshToken: String,
)

// Si el BFF agrega un campo nuevo, la app no debe romperse por eso.
private val formatoJson = Json { ignoreUnknownKeys = true }

/** Arma el cliente de Retrofit. La URL base debe terminar en "/". */
fun crearSesionApi(urlBase: String, cliente: OkHttpClient = OkHttpClient()): SesionApi =
    Retrofit.Builder()
        .baseUrl(urlBase)
        .client(cliente)
        .addConverterFactory(formatoJson.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(SesionApi::class.java)
