package com.solventa4bits.movil.solicitud.datos

import com.solventa4bits.movil.red.crearRetrofit
import kotlinx.serialization.Serializable
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/** Condiciones del seguro y solicitud de la poliza. El contrato vive en bff-mobile, openapi/openapi.yaml. */
interface SolicitudesApi {
    /** Publico: no lleva token. */
    @GET("v1/documentos-legales")
    suspend fun documentosLegales(
        @Query("tipo") tipo: String,
        @Query("mercado") mercado: String,
    ): Response<List<DocumentoLegalDto>>

    /** 201 si se creo, 200 si ya existia una solicitud de esa cotizacion. */
    @POST("v1/cuenta/polizas")
    suspend fun solicitar(
        @Header("Authorization") autorizacion: String,
        @Body solicitud: SolicitarPolizaDto,
    ): Response<PolizaDto>

    /** Si esta pendiente, el servidor la revisa de nuevo antes de responder. */
    @GET("v1/cuenta/polizas/{polizaId}")
    suspend fun poliza(
        @Header("Authorization") autorizacion: String,
        @Path("polizaId") polizaId: String,
    ): Response<PolizaDto>

    /** De la mas reciente a la mas antigua. */
    @GET("v1/cuenta/polizas")
    suspend fun polizas(@Header("Authorization") autorizacion: String): Response<List<PolizaDto>>

    /** 200 con un enlace temporal; 202 mientras se genera. */
    @GET("v1/cuenta/polizas/{polizaId}/documento")
    suspend fun documento(
        @Header("Authorization") autorizacion: String,
        @Path("polizaId") polizaId: String,
    ): Response<DocumentoPolizaDto>
}

/** Los nombres de los campos son los del contrato. */
@Serializable
data class DocumentoLegalDto(
    val tipo: String,
    val version: String,
    val titulo: String,
    val contenido: String,
    val notaPie: String? = null,
)

@Serializable
data class SolicitarPolizaDto(
    val coberturas: List<String>,
    val cotizacionCreadaEn: String,
    val documentosAceptados: List<DocumentoAceptadoDto>,
)

@Serializable
data class DocumentoAceptadoDto(val tipo: String, val version: String)

@Serializable
data class PolizaDto(
    val polizaId: String,
    val estado: String,
    val creadaEn: String,
    val emitidaEn: String? = null,
    val pendientes: List<PendienteDto> = emptyList(),
    val rechazo: RechazoDto? = null,
    val numero: String? = null,
    val vigencia: VigenciaDto? = null,
    val cotizacionCreadaEn: String? = null,
    val primaTotalMensual: Double? = null,
    val datosCredito: CreditoPolizaDto? = null,
    val coberturas: List<CoberturaPolizaDto> = emptyList(),
)

@Serializable
data class VigenciaDto(val desde: String, val hasta: String)

@Serializable
data class CreditoPolizaDto(val entidadAcreedora: String, val saldoInsoluto: Double, val plazoRestanteMeses: Int)

@Serializable
data class CoberturaPolizaDto(val codigo: String)

@Serializable
data class DocumentoPolizaDto(val estado: String, val url: String? = null)

@Serializable
data class PendienteDto(val codigo: String, val mensaje: String)

@Serializable
data class RechazoDto(val motivos: List<MotivoDto>)

@Serializable
data class MotivoDto(val mensaje: String)

fun crearSolicitudesApi(urlBase: String, cliente: OkHttpClient = OkHttpClient()): SolicitudesApi =
    crearRetrofit(urlBase, cliente).create(SolicitudesApi::class.java)
