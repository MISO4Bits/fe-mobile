package com.solventa4bits.movil.red

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

// Si el BFF agrega un campo nuevo, la app no debe romperse por eso.
private val formatoJson = Json { ignoreUnknownKeys = true }

/** Arma el cliente de Retrofit contra el BFF. La URL base debe terminar en "/". */
fun crearRetrofit(urlBase: String, cliente: OkHttpClient = OkHttpClient()): Retrofit =
    Retrofit.Builder()
        .baseUrl(urlBase)
        .client(cliente)
        .addConverterFactory(formatoJson.asConverterFactory("application/json".toMediaType()))
        .build()
