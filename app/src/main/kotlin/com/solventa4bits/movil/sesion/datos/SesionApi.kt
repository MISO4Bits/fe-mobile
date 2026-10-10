package com.solventa4bits.movil.sesion.datos

import com.solventa4bits.movil.red.crearRetrofit
import kotlinx.serialization.Serializable
import okhttp3.OkHttpClient
import retrofit2.Response
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

fun crearSesionApi(urlBase: String, cliente: OkHttpClient = OkHttpClient()): SesionApi =
    crearRetrofit(urlBase, cliente).create(SesionApi::class.java)
