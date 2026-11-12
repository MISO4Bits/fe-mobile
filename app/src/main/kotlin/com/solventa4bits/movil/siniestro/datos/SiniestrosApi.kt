package com.solventa4bits.movil.siniestro.datos

import com.solventa4bits.movil.red.crearRetrofit
import kotlinx.serialization.Serializable
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
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

    /** 404 si no existe o es de otro cliente. */
    @GET("v1/cuenta/siniestros/{siniestroId}")
    suspend fun consultar(
        @Header("Authorization") autorizacion: String,
        @Path("siniestroId") id: String,
    ): Response<SiniestroDto>

    /** Lo que Solventa pidio: 201 con el detalle, ya En estudio; 409 si ya no esta en Falta informacion. */
    @Multipart
    @POST("v1/cuenta/siniestros/{siniestroId}/evidencias")
    suspend fun entregar(
        @Header("Authorization") autorizacion: String,
        @Header("Idempotency-Key") llave: String,
        @Path("siniestroId") id: String,
        @Part("datos") datos: RequestBody,
        @Part archivos: List<MultipartBody.Part>,
    ): Response<SiniestroDto>

    /** El catalogo de siniestros. La app solo usa las oficinas; los prestadores son de BITS-371. */
    @GET("v1/catalogos/siniestros")
    suspend fun catalogo(@Header("Authorization") autorizacion: String): Response<CatalogoSiniestrosDto>

    /** Ayuda en sitio: 201 con la solicitud; 409 si el siniestro no la admite. */
    @POST("v1/cuenta/siniestros/{siniestroId}/asistencias")
    suspend fun pedirAyuda(
        @Header("Authorization") autorizacion: String,
        @Header("Idempotency-Key") llave: String,
        @Path("siniestroId") id: String,
        @Body solicitud: PedirAyudaDto,
    ): Response<AsistenciaDto>
}

@Serializable
data class CatalogoSiniestrosDto(
    val tiposAyuda: List<TipoAyudaDto> = emptyList(),
    val localidades: List<LocalidadDto> = emptyList(),
    val prestadores: List<PrestadorDto> = emptyList(),
    val oficinas: List<OficinaDto> = emptyList(),
)

@Serializable
data class TipoAyudaDto(val id: String, val nombre: String, val orden: Int)

@Serializable
data class LocalidadDto(
    val id: String,
    val nombre: String,
    val ciudad: String,
    val latitud: Double,
    val longitud: Double,
)

@Serializable
data class PrestadorDto(
    val id: String,
    val nombre: String,
    val tipos: List<String> = emptyList(),
    val localidad: String,
    val direccion: String,
    val latitud: Double,
    val longitud: Double,
    val tiempoLlegadaMin: Int,
)

/** Lo que se guarda del prestador con la solicitud, para mostrarlo sin consultar el catalogo. */
@Serializable
data class PrestadorAsistenciaDto(
    val id: String,
    val nombre: String,
    val direccion: String,
    val tiempoLlegadaMin: Int,
)

/** FOTOS y TELEFONO llevan latitud y longitud; DIRECCION, la direccion y el id de la localidad. */
@Serializable
data class PedirAyudaDto(
    val tipo: String,
    val prestador: PrestadorAsistenciaDto,
    val origen: String,
    val latitud: Double? = null,
    val longitud: Double? = null,
    val direccion: String? = null,
    val localidad: String? = null,
)

@Serializable
data class AsistenciaDto(
    val id: String,
    val numero: String,
    val tipo: String,
    val prestador: PrestadorAsistenciaDto,
    val tiempoLlegadaMin: Int,
    val origen: String,
    val direccion: String? = null,
    val localidad: String? = null,
    val solicitadaEn: String,
)

@Serializable
data class OficinaDto(
    val id: String,
    val banco: String,
    val nombre: String,
    val direccion: String,
    val ciudad: String,
    val latitud: Double,
    val longitud: Double,
)

/** La parte `datos` de una entrega: una entrada por archivo, en el mismo orden. */
@Serializable
data class EntregarEvidenciasDatosDto(val archivos: List<ArchivoReporteDto>)

/** Los nombres de los campos son los del contrato. */
@Serializable
data class ReportarSiniestroDatosDto(
    val polizaId: String,
    val tipo: String,
    val fechaOcurrencia: String,
    val narracion: String? = null,
    val archivos: List<ArchivoReporteDto>,
    val destinoPago: DestinoPagoDto? = null,
)

/** Transferencia (banco, tipo y numero) o cheque (la oficina); [tipo] sin valor por defecto para que siempre viaje. */
@Serializable
data class DestinoPagoDto(
    val tipo: String,
    val banco: String? = null,
    val tipoCuenta: String? = null,
    val numeroCuenta: String? = null,
    val oficina: OficinaDelChequeDto? = null,
)

/** Lo que se guarda de la oficina con el siniestro, para mostrarla sin consultar el catalogo. */
@Serializable
data class OficinaDelChequeDto(val id: String, val banco: String, val nombre: String, val direccion: String)

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
    val automatico: Boolean = false,
    val reportadoEn: String,
    val actualizadoEn: String,
)

/** El detalle trae mas campos (la ultima entrega, la narracion); la app solo usa estos. */
@Serializable
data class SiniestroDto(
    val id: String,
    val numero: String,
    val polizaId: String,
    val tipo: String,
    val estado: String,
    val automatico: Boolean = false,
    val reportadoEn: String,
    val actualizadoEn: String,
    // El pago automatico por sismo no lo reporto nadie: se tolera sin fecha de ocurrencia.
    val fechaOcurrencia: String = "",
    val fechaLimite: String? = null,
    val evidenciaPedida: EvidenciaPedidaDto? = null,
    val motivoRechazo: String? = null,
    val pago: PagoDto? = null,
    val evento: EventoSismoDto? = null,
    val archivos: List<ArchivoSiniestroDto> = emptyList(),
    val asistencias: List<AsistenciaDto> = emptyList(),
)

@Serializable
data class ArchivoSiniestroDto(val clase: String, val latitud: Double? = null, val longitud: Double? = null)

@Serializable
data class EvidenciaPedidaDto(val detalle: String, val pedidaEn: String)

@Serializable
data class PagoDto(val pagadoEn: String, val lineas: List<LineaPagoDto> = emptyList())

@Serializable
data class LineaPagoDto(val beneficiario: String, val valor: Double, val destino: String)

@Serializable
data class EventoSismoDto(val fecha: String, val municipio: String, val magnitud: Double)

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
