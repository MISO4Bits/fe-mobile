package com.solventa4bits.movil.solicitud.datos

import com.solventa4bits.movil.sesion.datos.ConSesion
import com.solventa4bits.movil.sesion.datos.LlamadaConSesion
import com.solventa4bits.movil.solicitud.dominio.CondicionesDelSeguro
import com.solventa4bits.movil.solicitud.dominio.DatosDeLaSolicitud
import com.solventa4bits.movil.solicitud.dominio.EstadoDeSolicitud
import com.solventa4bits.movil.solicitud.dominio.ResultadoCondiciones
import com.solventa4bits.movil.solicitud.dominio.ResultadoEnvio
import com.solventa4bits.movil.solicitud.dominio.ServicioDeSolicitudes
import com.solventa4bits.movil.solicitud.dominio.SolicitudEnviada
import com.solventa4bits.movil.traza.Traza
import kotlinx.serialization.SerializationException
import java.io.IOException
import java.net.HttpURLConnection.HTTP_CONFLICT

/**
 * Trae las condiciones del seguro y envia la solicitud de la poliza al BFF.
 * Como los demas adaptadores, nunca deja salir una excepcion: deja traza y
 * devuelve una respuesta controlada.
 */
class AdaptadorSolicitudes(
    private val api: SolicitudesApi,
    private val sesion: ConSesion,
    private val traza: Traza,
) : ServicioDeSolicitudes {

    override suspend fun condiciones(): ResultadoCondiciones =
        protegido("traer las condiciones del seguro", ResultadoCondiciones.Fallo) {
            val respuesta = api.documentosLegales(tipo = TIPO_POLIZA, mercado = MERCADO)
            val documento = respuesta.body()?.firstOrNull { it.tipo == TIPO_POLIZA }
            if (respuesta.isSuccessful && documento != null) {
                ResultadoCondiciones.Exito(
                    CondicionesDelSeguro(documento.version, documento.titulo, documento.contenido, documento.notaPie),
                )
            } else {
                traza.error(ORIGEN, "El BFF respondio ${respuesta.code()} sin las condiciones del seguro", null)
                ResultadoCondiciones.Fallo
            }
        }

    override suspend fun enviar(datos: DatosDeLaSolicitud): ResultadoEnvio =
        protegido("enviar la solicitud", ResultadoEnvio.Fallo) {
            val solicitud = SolicitarPolizaDto(
                // VIDA siempre va primero: es la obligatoria.
                coberturas = datos.coberturas.sortedBy { it.ordinal }.map { it.name },
                cotizacionCreadaEn = datos.cotizacionCreadaEn,
                documentosAceptados = listOf(DocumentoAceptadoDto(TIPO_POLIZA, datos.versionDeCondiciones)),
            )
            when (val llamada = sesion.llamar { autorizacion -> api.solicitar(autorizacion, solicitud) }) {
                is LlamadaConSesion.Respondio -> {
                    val cuerpo = llamada.respuesta.body()
                    val codigo = llamada.respuesta.code()
                    when {
                        llamada.respuesta.isSuccessful && cuerpo != null -> ResultadoEnvio.Enviada(cuerpo.aDominio())
                        // Sin cotizacion, vencida, distinta de la que vio o con una solicitud
                        // ya rechazada: en todos los casos el contrato pide cotizar de nuevo.
                        codigo == HTTP_CONFLICT -> ResultadoEnvio.CotizarDeNuevo
                        else -> {
                            traza.error(ORIGEN, "El BFF respondio $codigo al enviar la solicitud", null)
                            ResultadoEnvio.Fallo
                        }
                    }
                }
                LlamadaConSesion.SesionVencida -> ResultadoEnvio.SesionVencida
                LlamadaConSesion.SinRefresco -> ResultadoEnvio.Fallo
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

    // Un estado que la app no conoce se trata como pendiente: aun no hay poliza.
    private fun PolizaDto.aDominio() = SolicitudEnviada(
        polizaId = polizaId,
        estado = if (estado == "EMITIDA") EstadoDeSolicitud.EMITIDA else EstadoDeSolicitud.PENDIENTE,
        pendientes = pendientes.map { it.mensaje },
    )

    private companion object {
        const val ORIGEN = "AdaptadorSolicitudes"
        const val TIPO_POLIZA = "poliza"
        const val MERCADO = "CO"
    }
}
