package com.solventa4bits.movil.avisos.datos

import com.solventa4bits.movil.avisos.dominio.ResultadoRegistro
import com.solventa4bits.movil.avisos.dominio.ServicioDeDispositivos
import com.solventa4bits.movil.sesion.datos.ConSesion
import com.solventa4bits.movil.sesion.datos.LlamadaConSesion
import com.solventa4bits.movil.traza.Traza
import kotlinx.serialization.SerializationException
import java.io.IOException

/**
 * Registra y da de baja el telefono en el BFF. Como los demas adaptadores, nunca deja salir una
 * excepcion: deja traza y devuelve una respuesta controlada. Nunca escribe el token en la traza.
 */
class AdaptadorDispositivos(
    private val api: DispositivosApi,
    private val sesion: ConSesion,
    private val traza: Traza,
) : ServicioDeDispositivos {

    override suspend fun registrar(token: String, versionApp: String): ResultadoRegistro =
        try {
            val cuerpo = RegistrarDispositivoDto(token, PLATAFORMA, versionApp)
            when (val resultado = sesion.llamar { autorizacion -> api.registrar(autorizacion, cuerpo) }) {
                is LlamadaConSesion.Respondio -> {
                    val dispositivo = resultado.respuesta.body()
                    if (resultado.respuesta.isSuccessful && dispositivo != null) {
                        ResultadoRegistro.Registrado(dispositivo.dispositivoId)
                    } else {
                        traza.error(
                            ORIGEN,
                            "El BFF respondio ${resultado.respuesta.code()} al registrar el telefono",
                            null,
                        )
                        ResultadoRegistro.Fallo
                    }
                }
                LlamadaConSesion.SesionVencida -> ResultadoRegistro.SesionVencida
                LlamadaConSesion.SinRefresco -> ResultadoRegistro.Fallo
            }
        } catch (e: IOException) {
            traza.error(ORIGEN, "No se pudo llegar al BFF al registrar el telefono", e)
            ResultadoRegistro.Fallo
        } catch (e: SerializationException) {
            traza.error(ORIGEN, "La respuesta del BFF no cumple el contrato al registrar el telefono", e)
            ResultadoRegistro.Fallo
        }

    override suspend fun darDeBaja(dispositivoId: String): Boolean =
        try {
            when (val resultado = sesion.llamar { autorizacion -> api.darDeBaja(autorizacion, dispositivoId) }) {
                is LlamadaConSesion.Respondio -> resultado.respuesta.isSuccessful.also { exito ->
                    if (!exito) {
                        traza.error(
                            ORIGEN,
                            "El BFF respondio ${resultado.respuesta.code()} al dar de baja el telefono",
                            null,
                        )
                    }
                }
                LlamadaConSesion.SesionVencida, LlamadaConSesion.SinRefresco -> false
            }
        } catch (e: IOException) {
            traza.error(ORIGEN, "No se pudo llegar al BFF al dar de baja el telefono", e)
            false
        }

    private companion object {
        const val ORIGEN = "AdaptadorDispositivos"
        const val PLATAFORMA = "android"
    }
}
