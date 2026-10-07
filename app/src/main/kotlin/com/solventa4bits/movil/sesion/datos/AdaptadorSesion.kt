package com.solventa4bits.movil.sesion.datos

import com.solventa4bits.movil.sesion.dominio.ResultadoInicioSesion
import com.solventa4bits.movil.sesion.dominio.ServicioDeSesion
import com.solventa4bits.movil.sesion.dominio.Sesion
import com.solventa4bits.movil.traza.Traza
import java.io.IOException
import java.net.HttpURLConnection.HTTP_UNAUTHORIZED

/**
 * Habla con el BFF y traduce lo que pase a un [ResultadoInicioSesion].
 *
 * De aqui no sale ninguna excepcion de red: se captura, se deja traza y se
 * devuelve una respuesta controlada.
 */
class AdaptadorSesion(
    private val api: SesionApi,
    private val traza: Traza,
) : ServicioDeSesion {

    override suspend fun iniciarSesion(correo: String, contrasena: String): ResultadoInicioSesion =
        try {
            val respuesta = api.iniciarSesion(CredencialesDto(email = correo, password = contrasena))
            val cuerpo = respuesta.body()
            when {
                respuesta.isSuccessful && cuerpo != null -> ResultadoInicioSesion.Exito(cuerpo.aSesion())
                respuesta.code() == HTTP_UNAUTHORIZED -> ResultadoInicioSesion.CredencialesInvalidas
                else -> {
                    traza.error(ORIGEN, "El BFF respondio ${respuesta.code()} al iniciar sesion", null)
                    ResultadoInicioSesion.Fallo
                }
            }
        } catch (e: IOException) {
            traza.error(ORIGEN, "No se pudo llegar al BFF al iniciar sesion", e)
            ResultadoInicioSesion.Fallo
        }

    private fun SesionDto.aSesion() = Sesion(
        tokenDeAcceso = accessToken,
        tokenDeRefresco = refreshToken,
        segundosDeVigencia = expiresIn,
    )

    private companion object {
        const val ORIGEN = "AdaptadorSesion"
    }
}
