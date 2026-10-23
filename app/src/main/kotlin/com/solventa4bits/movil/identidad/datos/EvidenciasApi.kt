package com.solventa4bits.movil.identidad.datos

import com.solventa4bits.movil.red.crearRetrofit
import kotlinx.serialization.Serializable
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import java.util.concurrent.TimeUnit

/** Fotos de identidad del cliente. El contrato vive en bff-mobile, openapi/openapi.yaml. */
interface EvidenciasApi {
    /** Una foto por llamada: 201 si se guardo, 200 si toca tomar otra. */
    @Multipart
    @POST("v1/cuenta/evidencias-identidad")
    suspend fun subir(
        @Header("Authorization") autorizacion: String,
        @Part("tipo") tipo: RequestBody,
        @Part imagen: MultipartBody.Part,
    ): Response<EvidenciaSubidaDto>

    @GET("v1/cuenta/evidencias-identidad")
    suspend fun estado(@Header("Authorization") autorizacion: String): Response<List<EstadoEvidenciaDto>>
}

/** Los nombres de los campos son los del contrato. */
@Serializable
data class EvidenciaSubidaDto(
    val tipo: String,
    val estado: String,
    val motivo: String? = null,
    val intentosRestantes: Int,
)

@Serializable
data class EstadoEvidenciaDto(
    val tipo: String,
    val estado: String,
    val intentosRestantes: Int,
)

// El servidor analiza la foto antes de responder: puede tardar mas que una consulta normal.
private const val ESPERA_DE_ANALISIS_S = 30L

private val clienteParaFotos = OkHttpClient.Builder()
    .readTimeout(ESPERA_DE_ANALISIS_S, TimeUnit.SECONDS)
    .writeTimeout(ESPERA_DE_ANALISIS_S, TimeUnit.SECONDS)
    .build()

fun crearEvidenciasApi(urlBase: String, cliente: OkHttpClient = clienteParaFotos): EvidenciasApi =
    crearRetrofit(urlBase, cliente).create(EvidenciasApi::class.java)
