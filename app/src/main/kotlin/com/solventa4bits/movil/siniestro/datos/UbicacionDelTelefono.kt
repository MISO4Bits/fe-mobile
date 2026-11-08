package com.solventa4bits.movil.siniestro.datos

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.solventa4bits.movil.siniestro.dominio.FuenteDeUbicacion
import com.solventa4bits.movil.siniestro.dominio.Ubicacion
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Donde esta el telefono en el momento de la foto, con los servicios de ubicacion de Google. Sin
 * permiso, o si el GPS no responde a tiempo, la foto queda sin ubicacion (BITS-111, AC-5).
 * Depende del telefono: no entra en Kover.
 */
class UbicacionDelTelefono(private val contexto: Context) : FuenteDeUbicacion {

    private val cliente = LocationServices.getFusedLocationProviderClient(contexto)

    // El permiso se revisa justo antes de pedirla: el lint no ve esa revision dentro de conPermiso().
    @SuppressLint("MissingPermission")
    override suspend fun actual(): Ubicacion? {
        if (!conPermiso()) return null
        return withTimeoutOrNull(ESPERA_MS) {
            suspendCancellableCoroutine { continuacion ->
                val cancelacion = CancellationTokenSource()
                continuacion.invokeOnCancellation { cancelacion.cancel() }
                cliente.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancelacion.token)
                    .addOnCompleteListener { tarea ->
                        val lugar = if (tarea.isSuccessful) tarea.result else null
                        continuacion.resume(lugar?.let { Ubicacion(it.latitude, it.longitude) })
                    }
            }
        }
    }

    // Con la aproximada tambien sirve: el cliente puede dar solo esa desde Android 12.
    private fun conPermiso() = PERMISOS.any {
        ContextCompat.checkSelfPermission(contexto, it) == PackageManager.PERMISSION_GRANTED
    }

    private companion object {
        const val ESPERA_MS = 5_000L
        val PERMISOS = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    }
}
