package com.solventa4bits.movil.cotizacion.datos

import com.solventa4bits.movil.red.crearRetrofit
import kotlinx.serialization.Serializable
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

/** Cotizacion activa del cliente. El contrato vive en bff-mobile, openapi/openapi.yaml. */
interface CotizacionesApi {
    @POST("v1/cotizaciones")
    suspend fun cotizar(
        @Header("Authorization") autorizacion: String,
        @Body solicitud: SolicitudCotizacionDto,
    ): Response<CotizacionDto>

    @GET("v1/cotizaciones/actual")
    suspend fun actual(@Header("Authorization") autorizacion: String): Response<CotizacionDto>
}

/** Los nombres de los campos son los del contrato. */
@Serializable
data class SolicitudCotizacionDto(val datosCredito: DatosCreditoEnviadoDto)

// Los pesos viajan como enteros: un Double saldria como 2.5E8.
@Serializable
data class DatosCreditoEnviadoDto(
    val valorCredito: Long,
    val plazoRestanteMeses: Int,
    val entidadAcreedora: String,
    val saldoInsoluto: Long,
)

@Serializable
data class CotizacionDto(
    val estado: String,
    val datosCredito: DatosCreditoRecibidoDto,
    val origenDatos: String,
    val oferta: OfertaDto,
    val perfilRiesgo: PerfilRiesgoDto? = null,
)

@Serializable
data class DatosCreditoRecibidoDto(
    val valorCredito: Double,
    val plazoRestanteMeses: Int,
    val entidadAcreedora: String,
    val saldoInsoluto: Double,
)

@Serializable
data class OfertaDto(
    val primaMensual: Double,
    val primaBaseMensual: Double,
    val sumaAsegurada: Double,
    val coberturaMeses: Int,
    val personalizado: Boolean,
)

@Serializable
data class PerfilRiesgoDto(val factores: List<FactorRiesgoDto>)

@Serializable
data class FactorRiesgoDto(
    val descripcion: String,
    val efecto: String,
    val pesoRelativo: Double? = null,
)

/** Cuerpo de un 400 (RFC 9457): el detalle general y los errores por campo. */
@Serializable
data class ProblemaDto(
    val detail: String? = null,
    val errores: List<ErrorDeCampoDto> = emptyList(),
)

@Serializable
data class ErrorDeCampoDto(val mensaje: String)

fun crearCotizacionesApi(urlBase: String, cliente: OkHttpClient = OkHttpClient()): CotizacionesApi =
    crearRetrofit(urlBase, cliente).create(CotizacionesApi::class.java)
