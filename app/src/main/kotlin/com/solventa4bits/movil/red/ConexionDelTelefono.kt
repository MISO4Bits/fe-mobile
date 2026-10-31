package com.solventa4bits.movil.red

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/**
 * Pregunta al sistema si hay una red con salida a internet. Depende del
 * dispositivo, asi que no entra en las pruebas unitarias ni en Kover.
 */
class ConexionDelTelefono(contexto: Context) : Conexion {

    private val redes = contexto.getSystemService(ConnectivityManager::class.java)

    override fun hay(): Boolean {
        val capacidades = redes.getNetworkCapabilities(redes.activeNetwork) ?: return false
        return capacidades.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
