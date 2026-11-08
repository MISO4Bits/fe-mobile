package com.solventa4bits.movil.siniestro.datos

import com.solventa4bits.movil.red.formatoJson
import com.solventa4bits.movil.sesion.datos.ConSesion
import com.solventa4bits.movil.sesion.datos.LlamadaConSesion
import com.solventa4bits.movil.siniestro.dominio.ClaseDeArchivo
import com.solventa4bits.movil.siniestro.dominio.DetalleDeSiniestro
import com.solventa4bits.movil.siniestro.dominio.EstadoDeSiniestro
import com.solventa4bits.movil.siniestro.dominio.ReporteDeSiniestro
import com.solventa4bits.movil.siniestro.dominio.ResultadoDetalle
import com.solventa4bits.movil.siniestro.dominio.ResultadoReporte
import com.solventa4bits.movil.siniestro.dominio.ResultadoSiniestros
import com.solventa4bits.movil.siniestro.dominio.ServicioDeSiniestros
import com.solventa4bits.movil.siniestro.dominio.SiniestroResumen
import com.solventa4bits.movil.siniestro.dominio.TipoDeSiniestro
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
            val archivos = reporte.archivos.mapIndexed { i, archivo ->
                MultipartBody.Part.createFormData("archivos", "archivo-${i + 1}.jpg", archivo.jpeg.toRequestBody(JPEG))
            }
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

    private fun traducir(respuesta: Response<SiniestroReportadoDto>): ResultadoReporte {
        val cuerpo = respuesta.body()
        if (respuesta.isSuccessful && cuerpo != null) return ResultadoReporte.Radicado(cuerpo.id, cuerpo.numero)
        val codigo = respuesta.errorBody()?.string()
            ?.let { runCatching { formatoJson.decodeFromString<ProblemaSiniestroDto>(it) }.getOrNull() }
            ?.codigo
        return when (codigo) {
            "FUERA_DE_VIGENCIA" -> ResultadoReporte.FueraDeVigencia
            "SINIESTRO_EN_CURSO" -> ResultadoReporte.SiniestroEnCurso
            "TIPO_NO_CUBIERTO" -> ResultadoReporte.TipoNoCubierto
            else -> {
                traza.error(ORIGEN, "El BFF respondio ${respuesta.code()} al reportar el siniestro", null)
                ResultadoReporte.Fallo
            }
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

    private fun ReporteDeSiniestro.aDto() = ReportarSiniestroDatosDto(
        polizaId = polizaId,
        tipo = tipo.name,
        fechaOcurrencia = fechaOcurrencia.toString(),
        narracion = narracion,
        archivos = archivos.map { ArchivoReporteDto(it.clase.name, it.capturadoEn, it.latitud, it.longitud) },
        destinoPago = destinoPago?.let {
            DestinoPagoDto(TRANSFERENCIA, it.banco, it.tipoCuenta.name, it.numeroCuenta)
        },
    )

    // Un tipo o un estado que la app no conoce no la rompe: queda en null.
    private fun SiniestroDto.aDominio() = DetalleDeSiniestro(
        resumen = SiniestroResumenDto(id, numero, polizaId, tipo, estado, automatico, reportadoEn, actualizadoEn)
            .aDominio(),
        fechaOcurrencia = fechaOcurrencia,
        fechaLimite = fechaLimite,
        documentos = archivos.count { it.clase == ClaseDeArchivo.DOCUMENTO.name },
        fotos = archivos.count { it.clase == ClaseDeArchivo.FOTO.name },
    )

    private fun SiniestroResumenDto.aDominio() = SiniestroResumen(
        id = id,
        numero = numero,
        polizaId = polizaId,
        tipo = TipoDeSiniestro.entries.find { it.name == tipo },
        estado = EstadoDeSiniestro.entries.find { it.name == estado },
        automatico = automatico,
        reportadoEn = reportadoEn,
        actualizadoEn = actualizadoEn,
    )

    private companion object {
        const val ORIGEN = "AdaptadorSiniestros"
        const val TRANSFERENCIA = "TRANSFERENCIA"
        val JSON = "application/json".toMediaType()
        val JPEG = "image/jpeg".toMediaType()
    }
}
