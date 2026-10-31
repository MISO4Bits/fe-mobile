package com.solventa4bits.movil.avisos.datos

import com.solventa4bits.movil.red.crearRetrofit
import kotlinx.serialization.Serializable
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.Header
import retrofit2.http.PUT
import retrofit2.http.Path

/** Dispositivos para avisos push. El contrato vive en bff-mobile, openapi/openapi.yaml. */
interface DispositivosApi {
    /** 200 con el id del dispositivo; el mismo token devuelve el mismo id. */
    @PUT("v1/cuenta/dispositivos")
    suspend fun registrar(
        @Header("Authorization") autorizacion: String,
        @Body dispositivo: RegistrarDispositivoDto,
    ): Response<DispositivoDto>

    /** 204, tambien si el dispositivo ya no existia. */
    @DELETE("v1/cuenta/dispositivos/{dispositivoId}")
    suspend fun darDeBaja(
        @Header("Authorization") autorizacion: String,
        @Path("dispositivoId") dispositivoId: String,
    ): Response<Unit>
}

/** Los nombres de los campos son los del contrato. */
@Serializable
data class RegistrarDispositivoDto(val token: String, val plataforma: String, val versionApp: String)

@Serializable
data class DispositivoDto(val dispositivoId: String)

fun crearDispositivosApi(urlBase: String, cliente: OkHttpClient = OkHttpClient()): DispositivosApi =
    crearRetrofit(urlBase, cliente).create(DispositivosApi::class.java)
