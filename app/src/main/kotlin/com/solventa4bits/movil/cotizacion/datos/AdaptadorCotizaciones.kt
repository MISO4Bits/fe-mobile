package com.solventa4bits.movil.cotizacion.datos

import com.solventa4bits.movil.cotizacion.dominio.CodigoCobertura
import com.solventa4bits.movil.cotizacion.dominio.Cotizacion
import com.solventa4bits.movil.cotizacion.dominio.DatosDelCredito
import com.solventa4bits.movil.cotizacion.dominio.EfectoEnElPrecio
import com.solventa4bits.movil.cotizacion.dominio.FactorDelPrecio
import com.solventa4bits.movil.cotizacion.dominio.PrecioDeCobertura
import com.solventa4bits.movil.cotizacion.dominio.ResultadoCotizacion
import com.solventa4bits.movil.cotizacion.dominio.ResultadoDeclinacion
import com.solventa4bits.movil.cotizacion.dominio.ServicioDeCotizaciones
import com.solventa4bits.movil.red.formatoJson
import com.solventa4bits.movil.sesion.datos.ConSesion
import com.solventa4bits.movil.sesion.datos.LlamadaConSesion
import com.solventa4bits.movil.traza.Traza
import kotlinx.serialization.SerializationException
import retrofit2.Response
import java.io.IOException
import java.net.HttpURLConnection.HTTP_BAD_REQUEST
import java.net.HttpURLConnection.HTTP_NOT_FOUND

/**
 * Cotiza, consulta y declina la cotizacion activa en el BFF. Como los demas
 * adaptadores, nunca deja salir una excepcion: deja traza y devuelve una
 * respuesta controlada.
 */
class AdaptadorCotizaciones(
    private val api: CotizacionesApi,
    private val sesion: ConSesion,
    private val traza: Traza,
) : ServicioDeCotizaciones {

    override suspend fun cotizar(datos: DatosDelCredito): ResultadoCotizacion =
        pedir("cotizar") { autorizacion ->
            api.cotizar(autorizacion, SolicitudCotizacionDto(datos.aDto()))
        }

    override suspend fun consultarActual(): ResultadoCotizacion =
        pedir("consultar la cotizacion activa") { autorizacion -> api.actual(autorizacion) }

    // 404 tambien cuenta como declinada: en los dos casos el cliente queda sin cotizacion.
    override suspend fun declinarActual(): ResultadoDeclinacion =
        protegido("declinar la cotizacion activa", ResultadoDeclinacion.Fallo) {
            when (val resultado = sesion.llamar { autorizacion -> api.declinarActual(autorizacion) }) {
                is LlamadaConSesion.Respondio -> {
                    val codigo = resultado.respuesta.code()
                    if (resultado.respuesta.isSuccessful || codigo == HTTP_NOT_FOUND) {
                        ResultadoDeclinacion.Declinada
                    } else {
                        traza.error(ORIGEN, "El BFF respondio $codigo al declinar la cotizacion activa", null)
                        ResultadoDeclinacion.Fallo
                    }
                }
                LlamadaConSesion.SesionVencida -> ResultadoDeclinacion.SesionVencida
                LlamadaConSesion.SinRefresco -> ResultadoDeclinacion.Fallo
            }
        }

    private suspend fun pedir(
        accion: String,
        llamada: suspend (autorizacion: String) -> Response<CotizacionDto>,
    ): ResultadoCotizacion =
        protegido(accion, ResultadoCotizacion.Fallo) {
            when (val resultado = sesion.llamar(llamada)) {
                is LlamadaConSesion.Respondio -> traducir(accion, resultado.respuesta)
                LlamadaConSesion.SesionVencida -> ResultadoCotizacion.SesionVencida
                LlamadaConSesion.SinRefresco -> ResultadoCotizacion.Fallo
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

    private fun traducir(accion: String, respuesta: Response<CotizacionDto>): ResultadoCotizacion {
        val cuerpo = respuesta.body()
        return when {
            respuesta.isSuccessful && cuerpo != null -> ResultadoCotizacion.Exito(cuerpo.aDominio())
            respuesta.code() == HTTP_NOT_FOUND -> ResultadoCotizacion.SinCotizacion
            respuesta.code() == HTTP_BAD_REQUEST -> ResultadoCotizacion.DatosRechazados(mensajeDe(respuesta))
            else -> {
                traza.error(ORIGEN, "El BFF respondio ${respuesta.code()} al $accion", null)
                ResultadoCotizacion.Fallo
            }
        }
    }

    // El mensaje del campo que fallo, por ejemplo el del limite de los 75 anos.
    private fun mensajeDe(respuesta: Response<CotizacionDto>): String? {
        val problema = respuesta.errorBody()?.string()
            ?.let { runCatching { formatoJson.decodeFromString<ProblemaDto>(it) }.getOrNull() }
        return problema?.errores?.firstOrNull()?.mensaje ?: problema?.detail
    }

    private fun DatosDelCredito.aDto() = DatosCreditoEnviadoDto(
        valorCredito = valorCredito,
        plazoRestanteMeses = plazoRestanteMeses,
        entidadAcreedora = entidadAcreedora,
        saldoInsoluto = saldoInsoluto,
    )

    private fun CotizacionDto.aDominio() = Cotizacion(
        vigente = estado == "VIGENTE",
        datosCredito = DatosDelCredito(
            entidadAcreedora = datosCredito.entidadAcreedora,
            valorCredito = datosCredito.valorCredito.toLong(),
            saldoInsoluto = datosCredito.saldoInsoluto.toLong(),
            plazoRestanteMeses = datosCredito.plazoRestanteMeses,
        ),
        datosDeclarados = origenDatos == "DECLARADO",
        primaMensual = oferta.primaMensual,
        primaBaseMensual = oferta.primaBaseMensual,
        sumaAsegurada = oferta.sumaAsegurada,
        coberturaMeses = oferta.coberturaMeses,
        personalizada = oferta.personalizado,
        factores = perfilRiesgo?.factores.orEmpty().map {
            FactorDelPrecio(
                descripcion = it.descripcion,
                efecto = if (it.efecto == "POSITIVO") EfectoEnElPrecio.BAJA else EfectoEnElPrecio.SUBE,
                peso = it.pesoRelativo,
            )
        },
        // Una cobertura que la app aun no conoce se ignora.
        coberturas = oferta.desglose?.coberturas.orEmpty().mapNotNull { cobertura ->
            CodigoCobertura.entries.find { it.name == cobertura.codigo }?.let { codigo ->
                PrecioDeCobertura(
                    codigo = codigo,
                    sumaAsegurada = cobertura.sumaAsegurada,
                    primaNeta = cobertura.primaNeta,
                    iva = cobertura.iva,
                    primaTotal = cobertura.primaTotal,
                )
            }
        },
        creadaEn = creadaEn,
    )

    private companion object {
        const val ORIGEN = "AdaptadorCotizaciones"
    }
}
