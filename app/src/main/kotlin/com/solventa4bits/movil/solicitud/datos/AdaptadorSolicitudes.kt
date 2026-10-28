package com.solventa4bits.movil.solicitud.datos

import com.solventa4bits.movil.sesion.datos.ConSesion
import com.solventa4bits.movil.sesion.datos.LlamadaConSesion
import com.solventa4bits.movil.solicitud.dominio.CodigoDePendiente
import com.solventa4bits.movil.solicitud.dominio.CondicionesDelSeguro
import com.solventa4bits.movil.solicitud.dominio.DatosDeLaSolicitud
import com.solventa4bits.movil.solicitud.dominio.EstadoDeSolicitud
import com.solventa4bits.movil.solicitud.dominio.Pendiente
import com.solventa4bits.movil.solicitud.dominio.ResultadoCondiciones
import com.solventa4bits.movil.solicitud.dominio.ResultadoConsulta
import com.solventa4bits.movil.solicitud.dominio.ResultadoEnvio
import com.solventa4bits.movil.solicitud.dominio.ResultadoLista
import com.solventa4bits.movil.solicitud.dominio.ServicioDeSolicitudes
import com.solventa4bits.movil.solicitud.dominio.Solicitud
import com.solventa4bits.movil.traza.Traza
import kotlinx.serialization.SerializationException
import retrofit2.Response
import java.io.IOException
import java.net.HttpURLConnection.HTTP_CONFLICT
import java.net.HttpURLConnection.HTTP_NOT_FOUND

/**
 * Trae las condiciones del seguro y envia, consulta y lista las solicitudes
 * de poliza en el BFF. Como los demas adaptadores, nunca deja salir una
 * excepcion: deja traza y devuelve una respuesta controlada.
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

    override suspend fun enviar(datos: DatosDeLaSolicitud): ResultadoEnvio {
        val solicitud = SolicitarPolizaDto(
            // VIDA siempre va primero: es la obligatoria.
            coberturas = datos.coberturas.sortedBy { it.ordinal }.map { it.name },
            cotizacionCreadaEn = datos.cotizacionCreadaEn,
            documentosAceptados = listOf(DocumentoAceptadoDto(TIPO_POLIZA, datos.versionDeCondiciones)),
        )
        return pedir(
            accion = "enviar la solicitud",
            fallo = ResultadoEnvio.Fallo,
            sesionVencida = ResultadoEnvio.SesionVencida,
            llamada = { autorizacion -> api.solicitar(autorizacion, solicitud) },
        ) { respuesta ->
            val cuerpo = respuesta.body()
            when {
                respuesta.isSuccessful && cuerpo != null -> ResultadoEnvio.Enviada(cuerpo.aDominio())
                // Sin cotizacion, vencida, distinta de la que vio o con una solicitud
                // ya rechazada: en todos los casos el contrato pide cotizar de nuevo.
                respuesta.code() == HTTP_CONFLICT -> ResultadoEnvio.CotizarDeNuevo
                else -> null
            }
        }
    }

    override suspend fun consultar(id: String): ResultadoConsulta = pedir(
        accion = "consultar la solicitud",
        fallo = ResultadoConsulta.Fallo,
        sesionVencida = ResultadoConsulta.SesionVencida,
        llamada = { autorizacion -> api.poliza(autorizacion, id) },
    ) { respuesta ->
        val cuerpo = respuesta.body()
        when {
            respuesta.isSuccessful && cuerpo != null -> ResultadoConsulta.Exito(cuerpo.aDominio())
            respuesta.code() == HTTP_NOT_FOUND -> ResultadoConsulta.NoExiste
            else -> null
        }
    }

    override suspend fun listar(): ResultadoLista = pedir(
        accion = "listar las solicitudes",
        fallo = ResultadoLista.Fallo,
        sesionVencida = ResultadoLista.SesionVencida,
        llamada = { autorizacion -> api.polizas(autorizacion) },
    ) { respuesta ->
        respuesta.body()?.takeIf { respuesta.isSuccessful }?.let { polizas ->
            ResultadoLista.Exito(polizas.map { it.aDominio() })
        }
    }

    /**
     * Llama con la sesion y traduce la respuesta con [traducir]. Si [traducir]
     * no la reconoce (null), deja traza con el codigo y devuelve [fallo].
     */
    private suspend fun <T, R> pedir(
        accion: String,
        fallo: R,
        sesionVencida: R,
        llamada: suspend (autorizacion: String) -> Response<T>,
        traducir: (Response<T>) -> R?,
    ): R = protegido(accion, fallo) {
        when (val resultado = sesion.llamar(llamada)) {
            is LlamadaConSesion.Respondio -> traducir(resultado.respuesta) ?: run {
                traza.error(ORIGEN, "El BFF respondio ${resultado.respuesta.code()} al $accion", null)
                fallo
            }
            LlamadaConSesion.SesionVencida -> sesionVencida
            LlamadaConSesion.SinRefresco -> fallo
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

    // Un estado o un codigo que la app no conoce no la rompe: el estado se trata
    // como pendiente (aun no hay poliza) y el pendiente queda sin codigo.
    private fun PolizaDto.aDominio() = Solicitud(
        id = polizaId,
        estado = EstadoDeSolicitud.entries.find { it.name == estado } ?: EstadoDeSolicitud.PENDIENTE,
        creadaEn = creadaEn,
        emitidaEn = emitidaEn,
        pendientes = pendientes.map { pendiente ->
            Pendiente(CodigoDePendiente.entries.find { it.name == pendiente.codigo }, pendiente.mensaje)
        },
        motivosDeRechazo = rechazo?.motivos.orEmpty().map { it.mensaje },
    )

    private companion object {
        const val ORIGEN = "AdaptadorSolicitudes"
        const val TIPO_POLIZA = "poliza"
        const val MERCADO = "CO"
    }
}
