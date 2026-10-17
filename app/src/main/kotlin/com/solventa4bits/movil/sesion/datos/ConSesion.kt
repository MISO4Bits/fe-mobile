package com.solventa4bits.movil.sesion.datos

import com.solventa4bits.movil.sesion.dominio.AlmacenDeSesion
import com.solventa4bits.movil.sesion.dominio.ResultadoInicioSesion
import com.solventa4bits.movil.sesion.dominio.ServicioDeSesion
import com.solventa4bits.movil.sesion.dominio.Sesion
import retrofit2.Response
import java.net.HttpURLConnection.HTTP_UNAUTHORIZED

/** Lo que pasa al llamar al BFF con la sesion guardada. */
sealed interface LlamadaConSesion<out T> {
    data class Respondio<T>(val respuesta: Response<T>) : LlamadaConSesion<T>

    /** No hay sesion, o el BFF la rechazo aun despues de refrescarla. */
    data object SesionVencida : LlamadaConSesion<Nothing>

    /** El token vencio y no se pudo refrescar porque no hubo red. */
    data object SinRefresco : LlamadaConSesion<Nothing>
}

/**
 * Hace las llamadas que necesitan el token de acceso. Si el BFF lo rechaza
 * porque vencio, refresca la sesion y repite la llamada una sola vez, sin que
 * el cliente lo note. La usan todos los adaptadores de endpoints protegidos.
 */
class ConSesion(
    private val almacen: AlmacenDeSesion,
    private val sesiones: ServicioDeSesion,
) {

    suspend fun <T> llamar(llamada: suspend (autorizacion: String) -> Response<T>): LlamadaConSesion<T> {
        val sesion = almacen.leer() ?: return LlamadaConSesion.SesionVencida
        val primera = llamada("Bearer ${sesion.tokenDeAcceso}")
        return if (primera.code() == HTTP_UNAUTHORIZED) {
            refrescarYRepetir(sesion, llamada)
        } else {
            LlamadaConSesion.Respondio(primera)
        }
    }

    // Un solo reintento: si el token nuevo tambien es rechazado, la sesion se
    // da por vencida y no se insiste.
    private suspend fun <T> refrescarYRepetir(
        vencida: Sesion,
        llamada: suspend (autorizacion: String) -> Response<T>,
    ): LlamadaConSesion<T> =
        when (val refresco = sesiones.refrescar(vencida.tokenDeRefresco)) {
            is ResultadoInicioSesion.Exito -> {
                almacen.guardar(refresco.sesion)
                val segunda = llamada("Bearer ${refresco.sesion.tokenDeAcceso}")
                if (segunda.code() == HTTP_UNAUTHORIZED) {
                    LlamadaConSesion.SesionVencida
                } else {
                    LlamadaConSesion.Respondio(segunda)
                }
            }
            ResultadoInicioSesion.CredencialesInvalidas -> LlamadaConSesion.SesionVencida
            ResultadoInicioSesion.Fallo -> LlamadaConSesion.SinRefresco
        }
}
