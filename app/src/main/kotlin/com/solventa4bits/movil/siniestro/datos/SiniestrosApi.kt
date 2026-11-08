package com.solventa4bits.movil.siniestro.datos

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

/** Siniestros del cliente. El contrato vive en bff-mobile, openapi/openapi.yaml. */
interface SiniestrosApi {
    /** 201 con el radicado. La parte `datos` describe los `archivos`, en el mismo orden. */
    @Multipart
    @POST("v1/cuenta/siniestros")
    suspend fun reportar(
        @Header("Authorization") autorizacion: String,
        @Header("Idempotency-Key") llave: String,
        @Part("datos") datos: RequestBody,
        @Part archivos: List<MultipartBody.Part>,
    ): Response<SiniestroReportadoDto>

    @GET("v1/cuenta/siniestros")
    suspend fun listar(@Header("Authorization") autorizacion: String): Response<List<SiniestroResumenDto>>
}

/** Los nombres de los campos son los del contrato. */
@Serializable
data class ReportarSiniestroDatosDto(
    val polizaId: String,
    val tipo: String,
    val fechaOcurrencia: String,
    val narracion: String? = null,
    val archivos: List<ArchivoReporteDto>,
)

@Serializable
data class ArchivoReporteDto(
    val clase: String,
    val capturadoEn: String,
    val latitud: Double?,
    val longitud: Double?,
)

@Serializable
data class SiniestroReportadoDto(val id: String, val numero: String, val estado: String)

@Serializable
data class SiniestroResumenDto(
    val id: String,
    val numero: String,
    val polizaId: String,
    val tipo: String,
    val estado: String,
)

/** Cuerpo de un 400 o un 409: [codigo] dice cual regla no se cumplio. */
@Serializable
data class ProblemaSiniestroDto(val codigo: String? = null)

// Hasta 10 fotos en una sola llamada: con una red lenta tarda mas que una consulta normal.
private const val ESPERA_DE_ENVIO_S = 60L

private val clienteParaReportes = OkHttpClient.Builder()
    .readTimeout(ESPERA_DE_ENVIO_S, TimeUnit.SECONDS)
    .writeTimeout(ESPERA_DE_ENVIO_S, TimeUnit.SECONDS)
    .build()

fun crearSiniestrosApi(urlBase: String, cliente: OkHttpClient = clienteParaReportes): SiniestrosApi =
    crearRetrofit(urlBase, cliente).create(SiniestrosApi::class.java)
