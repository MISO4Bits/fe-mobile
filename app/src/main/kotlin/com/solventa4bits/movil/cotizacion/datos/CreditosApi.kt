package com.solventa4bits.movil.cotizacion.datos

import com.solventa4bits.movil.red.crearRetrofit
import kotlinx.serialization.Serializable
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header

/** Hipotecas del cliente. El contrato vive en bff-mobile, openapi/openapi.yaml. */
interface CreditosApi {
    @GET("v1/creditos-hipotecarios")
    suspend fun consultar(@Header("Authorization") autorizacion: String): Response<CreditosDto>
}

/** Los nombres de los campos son los del contrato. */
@Serializable
data class CreditosDto(
    val estado: String,
    val creditos: List<CreditoDto>,
    val entidades: List<EntidadDto>,
)

@Serializable
data class CreditoDto(
    val entidadId: String? = null,
    val entidadNombre: String,
    val valorCredito: Double,
    val saldoInsoluto: Double,
    val plazoRestanteMeses: Int,
    val cuotaMensual: Double,
)

@Serializable
data class EntidadDto(
    val id: String,
    val nombre: String,
)

fun crearCreditosApi(urlBase: String, cliente: OkHttpClient = OkHttpClient()): CreditosApi =
    crearRetrofit(urlBase, cliente).create(CreditosApi::class.java)
