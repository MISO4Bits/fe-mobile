package com.solventa4bits.movil.sesion.datos

import com.solventa4bits.movil.sesion.dominio.ResultadoInicioSesion
import com.solventa4bits.movil.sesion.dominio.ServicioDeSesion
import com.solventa4bits.movil.sesion.dominio.Sesion
import com.solventa4bits.movil.traza.Traza
import kotlinx.serialization.SerializationException
import retrofit2.Response
import java.io.IOException
import java.net.HttpURLConnection.HTTP_UNAUTHORIZED

/**
 * Habla con el BFF y traduce lo que pase a un [ResultadoInicioSesion].
 *
 * De aqui no sale ninguna excepcion, ni de red ni de una respuesta que no
 * cumpla el contrato: se captura, se deja traza y se devuelve una respuesta
 * controlada.
 */
class AdaptadorSesion(
    private val api: SesionApi,
    private val traza: Traza,
) : ServicioDeSesion {

    override suspend fun iniciarSesion(correo: String, contrasena: String): ResultadoInicioSesion =
        pedirSesion("iniciar sesion") {
            api.iniciarSesion(CredencialesDto(email = correo, password = contrasena))
        }

    override suspend fun refrescar(tokenDeRefresco: String): ResultadoInicioSesion =
        pedirSesion("refrescar la sesion") {
            api.refrescar(RefrescoDto(refreshToken = tokenDeRefresco))
        }

    // Las dos operaciones responden igual: una sesion, un 401 o un fallo.
    private suspend fun pedirSesion(
        accion: String,
        llamada: suspend () -> Response<SesionDto>,
    ): ResultadoInicioSesion =
        try {
            val respuesta = llamada()
            val cuerpo = respuesta.body()
            when {
                respuesta.isSuccessful && cuerpo != null -> ResultadoInicioSesion.Exito(cuerpo.aSesion())
                respuesta.code() == HTTP_UNAUTHORIZED -> ResultadoInicioSesion.CredencialesInvalidas
                else -> {
                    traza.error(ORIGEN, "El BFF respondio ${respuesta.code()} al $accion", null)
                    ResultadoInicioSesion.Fallo
                }
            }
        } catch (e: IOException) {
            traza.error(ORIGEN, "No se pudo llegar al BFF al $accion", e)
            ResultadoInicioSesion.Fallo
        } catch (e: SerializationException) {
            traza.error(ORIGEN, "La respuesta del BFF no cumple el contrato al $accion", e)
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
