package com.solventa4bits.movil.avisos.datos

import com.google.firebase.messaging.FirebaseMessaging
import com.solventa4bits.movil.avisos.dominio.FuenteDeToken
import com.solventa4bits.movil.traza.Traza
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * El token que Firebase le da a esta instalacion. Depende de Firebase y del telefono, asi que no
 * entra en Kover. Nunca se escribe el token en la traza.
 */
class FuenteDeTokenFirebase(private val traza: Traza) : FuenteDeToken {

    override suspend fun obtener(): String? = suspendCancellableCoroutine { continuacion ->
        FirebaseMessaging.getInstance().token.addOnCompleteListener { tarea ->
            if (tarea.isSuccessful) {
                continuacion.resume(tarea.result)
            } else {
                traza.error(ORIGEN, "Firebase no entrego el token de este telefono", tarea.exception)
                continuacion.resume(null)
            }
        }
    }

    private companion object {
        const val ORIGEN = "FuenteDeTokenFirebase"
    }
}
