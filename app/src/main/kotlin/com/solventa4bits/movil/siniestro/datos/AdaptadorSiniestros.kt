package com.solventa4bits.movil.siniestro.datos

import com.solventa4bits.movil.red.formatoJson
import com.solventa4bits.movil.sesion.datos.ConSesion
import com.solventa4bits.movil.sesion.datos.LlamadaConSesion
import com.solventa4bits.movil.siniestro.dominio.ArchivoDelReporte
import com.solventa4bits.movil.siniestro.dominio.ReporteDeSiniestro
import com.solventa4bits.movil.siniestro.dominio.ResultadoCatalogo
import com.solventa4bits.movil.siniestro.dominio.ResultadoDetalle
import com.solventa4bits.movil.siniestro.dominio.ResultadoEntrega
import com.solventa4bits.movil.siniestro.dominio.ResultadoReporte
import com.solventa4bits.movil.siniestro.dominio.ResultadoSiniestros
import com.solventa4bits.movil.siniestro.dominio.ServicioDeSiniestros
import com.solventa4bits.movil.traza.Traza
import kotlinx.serialization.SerializationException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import java.io.IOException
import java.net.HttpURLConnection.HTTP_NOT_FOUND

/**
 * Reporta los siniestros al BFF y trae los que ya tiene el cliente. Como los demas adaptadores,
 * nunca deja salir una excepcion: deja traza y devuelve una respuesta controlada.
 */
class AdaptadorSiniestros(
    private val api: SiniestrosApi,
    private val sesion: ConSesion,
    private val traza: Traza,
) : ServicioDeSiniestros {

    override suspend fun reportar(reporte: ReporteDeSiniestro, llave: String): ResultadoReporte =
        protegido("reportar el siniestro", ResultadoReporte.Fallo) {
            val datos = formatoJson.encodeToString(reporte.aDto()).toRequestBody(JSON)
            val archivos = partesDe(reporte.archivos)
            when (val llamada = sesion.llamar { autorizacion -> api.reportar(autorizacion, llave, datos, archivos) }) {
                is LlamadaConSesion.Respondio -> traducir(llamada.respuesta)
                LlamadaConSesion.SesionVencida -> ResultadoReporte.SesionVencida
                LlamadaConSesion.SinRefresco -> ResultadoReporte.Fallo
            }
        }

    override suspend fun listar(): ResultadoSiniestros =
        protegido("listar los siniestros", ResultadoSiniestros.Fallo) {
            when (val llamada = sesion.llamar { autorizacion -> api.listar(autorizacion) }) {
                is LlamadaConSesion.Respondio -> {
                    val cuerpo = llamada.respuesta.body()
                    if (llamada.respuesta.isSuccessful && cuerpo != null) {
                        ResultadoSiniestros.Exito(cuerpo.map { it.aDominio() })
                    } else {
                        val codigo = llamada.respuesta.code()
                        traza.error(ORIGEN, "El BFF respondio $codigo al listar los siniestros", null)
                        ResultadoSiniestros.Fallo
                    }
                }
                LlamadaConSesion.SesionVencida -> ResultadoSiniestros.SesionVencida
                LlamadaConSesion.SinRefresco -> ResultadoSiniestros.Fallo
            }
        }

    override suspend fun consultar(id: String): ResultadoDetalle =
        protegido("consultar el siniestro", ResultadoDetalle.Fallo) {
            when (val llamada = sesion.llamar { autorizacion -> api.consultar(autorizacion, id) }) {
                is LlamadaConSesion.Respondio -> {
                    val cuerpo = llamada.respuesta.body()
                    when {
                        llamada.respuesta.isSuccessful && cuerpo != null -> ResultadoDetalle.Exito(cuerpo.aDominio())
                        llamada.respuesta.code() == HTTP_NOT_FOUND -> ResultadoDetalle.NoExiste
                        else -> {
                            val codigo = llamada.respuesta.code()
                            traza.error(ORIGEN, "El BFF respondio $codigo al consultar el siniestro", null)
                            ResultadoDetalle.Fallo
                        }
                    }
                }
                LlamadaConSesion.SesionVencida -> ResultadoDetalle.SesionVencida
                LlamadaConSesion.SinRefresco -> ResultadoDetalle.Fallo
            }
        }

    override suspend fun entregar(id: String, archivos: List<ArchivoDelReporte>, llave: String): ResultadoEntrega =
        protegido("enviar lo que falta", ResultadoEntrega.Fallo) {
            val datos = formatoJson.encodeToString(EntregarEvidenciasDatosDto(archivos.map { it.aDto() }))
                .toRequestBody(JSON)
            val partes = partesDe(archivos)
            val llamada = sesion.llamar { autorizacion -> api.entregar(autorizacion, llave, id, datos, partes) }
            when (llamada) {
                is LlamadaConSesion.Respondio -> {
                    val respuesta = llamada.respuesta
                    val cuerpo = respuesta.body()
                    when {
                        respuesta.isSuccessful && cuerpo != null -> ResultadoEntrega.Recibida(cuerpo.aDominio())
                        codigoDelProblema(respuesta) == "ESTADO_NO_PERMITE" -> ResultadoEntrega.YaNoSePide
                        else -> {
                            traza.error(ORIGEN, "El BFF respondio ${respuesta.code()} al enviar lo que falta", null)
                            ResultadoEntrega.Fallo
                        }
                    }
                }
                LlamadaConSesion.SesionVencida -> ResultadoEntrega.SesionVencida
                LlamadaConSesion.SinRefresco -> ResultadoEntrega.Fallo
            }
        }

    override suspend fun oficinas(): ResultadoCatalogo =
        protegido("traer las oficinas", ResultadoCatalogo.Fallo) {
            when (val llamada = sesion.llamar { autorizacion -> api.catalogo(autorizacion) }) {
                is LlamadaConSesion.Respondio -> {
                    val cuerpo = llamada.respuesta.body()
                    if (llamada.respuesta.isSuccessful && cuerpo != null) {
                        ResultadoCatalogo.Exito(cuerpo.oficinas.map { it.aDominio() })
                    } else {
                        val codigo = llamada.respuesta.code()
                        traza.error(ORIGEN, "El BFF respondio $codigo al traer las oficinas", null)
                        ResultadoCatalogo.Fallo
                    }
                }
                LlamadaConSesion.SesionVencida -> ResultadoCatalogo.SesionVencida
                LlamadaConSesion.SinRefresco -> ResultadoCatalogo.Fallo
            }
        }

    private fun traducir(respuesta: Response<SiniestroReportadoDto>): ResultadoReporte {
        val cuerpo = respuesta.body()
        if (respuesta.isSuccessful && cuerpo != null) return ResultadoReporte.Radicado(cuerpo.id, cuerpo.numero)
        return when (codigoDelProblema(respuesta)) {
            "FUERA_DE_VIGENCIA" -> ResultadoReporte.FueraDeVigencia
            "SINIESTRO_EN_CURSO" -> ResultadoReporte.SiniestroEnCurso
            "TIPO_NO_CUBIERTO" -> ResultadoReporte.TipoNoCubierto
            else -> {
                traza.error(ORIGEN, "El BFF respondio ${respuesta.code()} al reportar el siniestro", null)
                ResultadoReporte.Fallo
            }
        }
    }

    /** El `codigo` de un 400 o un 409, o null si no viene o no se puede leer. */
    private fun codigoDelProblema(respuesta: Response<*>): String? =
        respuesta.errorBody()?.string()
            ?.let { runCatching { formatoJson.decodeFromString<ProblemaSiniestroDto>(it) }.getOrNull() }
            ?.codigo

    // Las imagenes van en el mismo orden que su descripcion en `datos`.
    private fun partesDe(archivos: List<ArchivoDelReporte>) = archivos.mapIndexed { i, archivo ->
        MultipartBody.Part.createFormData("archivos", "archivo-${i + 1}.jpg", archivo.jpeg.toRequestBody(JPEG))
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

    private companion object {
        const val ORIGEN = "AdaptadorSiniestros"
        val JSON = "application/json".toMediaType()
        val JPEG = "image/jpeg".toMediaType()
    }
}
