package com.solventa4bits.movil.sesion.datos

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.solventa4bits.movil.R
import com.solventa4bits.movil.sesion.dominio.AutenticadorDeHuella
import com.solventa4bits.movil.sesion.dominio.ResultadoHuella
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Autenticador sobre el dialogo biometrico de Android.
 *
 * Solo acepta sensores fuertes (BIOMETRIC_STRONG): huella o rostro con
 * profundidad, no la cara en 2D. El sistema compara contra lo que el cliente
 * registro en Ajustes y a la app solo le llega el resultado. BiometricPrompt
 * exige una FragmentActivity.
 *
 * Depende del dispositivo y el dialogo del sistema no se automatiza, asi que
 * no entra en Kover: se prueba a mano en el emulador o en un telefono.
 */
class AutenticadorBiometrico(private val activity: FragmentActivity) : AutenticadorDeHuella {

    override fun disponible(): Boolean =
        BiometricManager.from(activity).canAuthenticate(BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS

    override suspend fun autenticar(): ResultadoHuella = suspendCancellableCoroutine { continuacion ->
        val respuesta = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                if (continuacion.isActive) continuacion.resume(ResultadoHuella.EXITO)
            }

            // Un intento que el sensor no reconoce no termina el dialogo: el
            // sistema deja reintentar. Solo se responde cuando el dialogo cierra.
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                val resultado = when (errorCode) {
                    BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                    BiometricPrompt.ERROR_USER_CANCELED,
                    BiometricPrompt.ERROR_CANCELED,
                    -> ResultadoHuella.CANCELADO
                    else -> ResultadoHuella.FALLO
                }
                if (continuacion.isActive) continuacion.resume(resultado)
            }
        }
        val dialogo = BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), respuesta)
        val descripcion = BiometricPrompt.PromptInfo.Builder()
            .setTitle(activity.getString(R.string.huella_dialogo_titulo))
            // Solo cierra el dialogo: la pantalla de abajo ofrece la contrasena.
            .setNegativeButtonText(activity.getString(R.string.barra_cancelar))
            .setAllowedAuthenticators(BIOMETRIC_STRONG)
            .build()
        dialogo.authenticate(descripcion)
        continuacion.invokeOnCancellation { dialogo.cancelAuthentication() }
    }
}
