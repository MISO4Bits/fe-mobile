package com.solventa4bits.movil.identidad.datos

import com.solventa4bits.movil.identidad.dominio.CaraDeCedula
import com.solventa4bits.movil.identidad.dominio.EstadoGuardado
import com.solventa4bits.movil.identidad.dominio.MotivoDeRechazo
import com.solventa4bits.movil.identidad.dominio.ResultadoEstado
import com.solventa4bits.movil.identidad.dominio.ResultadoSubida
import com.solventa4bits.movil.identidad.dominio.ServicioDeEvidencias
import com.solventa4bits.movil.identidad.dominio.Veredicto
import com.solventa4bits.movil.sesion.datos.ConSesion
import com.solventa4bits.movil.sesion.datos.LlamadaConSesion
import com.solventa4bits.movil.traza.Traza
import kotlinx.serialization.SerializationException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

/**
 * Sube las fotos de la cedula al BFF y consulta lo que ya quedo guardado. Como
 * los demas adaptadores, nunca deja salir una excepcion: deja traza y devuelve
 * una respuesta controlada.
 */
class AdaptadorEvidencias(
    private val api: EvidenciasApi,
    private val sesion: ConSesion,
    private val traza: Traza,
) : ServicioDeEvidencias {

    override suspend fun subir(cara: CaraDeCedula, imagen: ByteArray): ResultadoSubida =
        protegido("subir la foto", ResultadoSubida.Fallo) {
            val llamada = sesion.llamar { autorizacion ->
                api.subir(
                    autorizacion,
                    cara.tipo.toRequestBody(TEXTO),
                    MultipartBody.Part.createFormData("imagen", "${cara.tipo}.jpg", imagen.toRequestBody(JPEG)),
                )
            }
            when (llamada) {
                is LlamadaConSesion.Respondio -> {
                    val cuerpo = llamada.respuesta.body()
                    if (llamada.respuesta.isSuccessful && cuerpo != null) {
                        ResultadoSubida.Exito(cuerpo.aVeredicto())
                    } else {
                        traza.error(ORIGEN, "El BFF respondio ${llamada.respuesta.code()} al subir la foto", null)
                        ResultadoSubida.Fallo
                    }
                }
                LlamadaConSesion.SesionVencida -> ResultadoSubida.SesionVencida
                LlamadaConSesion.SinRefresco -> ResultadoSubida.Fallo
            }
        }

    override suspend fun consultar(): ResultadoEstado =
        protegido("consultar las fotos guardadas", ResultadoEstado.Fallo) {
            when (val llamada = sesion.llamar { autorizacion -> api.estado(autorizacion) }) {
                is LlamadaConSesion.Respondio -> {
                    val cuerpo = llamada.respuesta.body()
                    if (llamada.respuesta.isSuccessful && cuerpo != null) {
                        ResultadoEstado.Exito(cuerpo.aCaras())
                    } else {
                        traza.error(ORIGEN, "El BFF respondio ${llamada.respuesta.code()} al consultar las fotos", null)
                        ResultadoEstado.Fallo
                    }
                }
                LlamadaConSesion.SesionVencida -> ResultadoEstado.SesionVencida
                LlamadaConSesion.SinRefresco -> ResultadoEstado.Fallo
            }
        }

    /** Sin red o con una respuesta fuera de contrato, deja traza y devuelve [fallo]. */
    private suspend fun <T> protegido(accion: String, fallo: T, bloque: suspend () -> T): T =
        try {
            bloque()
        } catch (e: IOException) {
            traza.error(ORIGEN, "No se pudo llegar al BFF al $accion", e)
            fallo
        } catch (e: SerializationException) {
            traza.error(ORIGEN, "La respuesta del BFF no cumple el contrato al $accion", e)
            fallo
        }

    // Un estado o un motivo que la app no conoce no la rompe: el contrato pide tolerarlos.
    private fun EvidenciaSubidaDto.aVeredicto(): Veredicto = when (estado) {
        "ACEPTADA" -> Veredicto.Aceptada
        "REINTENTAR" -> Veredicto.Reintentar(
            motivo = MotivoDeRechazo.entries.find { it.name == motivo },
            intentosRestantes = intentosRestantes,
        )
        else -> Veredicto.RevisionManual
    }

    // El GET trae tambien la selfie: aqui solo importan las dos caras.
    private fun List<EstadoEvidenciaDto>.aCaras(): Map<CaraDeCedula, EstadoGuardado> =
        CaraDeCedula.entries.associateWith { cara ->
            val guardado = find { it.tipo == cara.tipo }?.estado
            EstadoGuardado.entries.find { it.name == guardado } ?: EstadoGuardado.PENDIENTE
        }

    private companion object {
        const val ORIGEN = "AdaptadorEvidencias"
        val TEXTO = "text/plain".toMediaType()
        val JPEG = "image/jpeg".toMediaType()
    }
}

/** Como se llama cada cara en el contrato. */
internal val CaraDeCedula.tipo: String
    get() = when (this) {
        CaraDeCedula.FRENTE -> "cedula_frente"
        CaraDeCedula.REVERSO -> "cedula_reverso"
    }
