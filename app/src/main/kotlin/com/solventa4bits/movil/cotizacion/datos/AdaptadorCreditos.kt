package com.solventa4bits.movil.cotizacion.datos

import com.solventa4bits.movil.cotizacion.dominio.CreditoHipotecario
import com.solventa4bits.movil.cotizacion.dominio.CreditosHipotecarios
import com.solventa4bits.movil.cotizacion.dominio.EntidadFinanciera
import com.solventa4bits.movil.cotizacion.dominio.EstadoCreditos
import com.solventa4bits.movil.cotizacion.dominio.ResultadoCreditos
import com.solventa4bits.movil.cotizacion.dominio.ServicioDeCreditos
import com.solventa4bits.movil.sesion.dominio.AlmacenDeSesion
import com.solventa4bits.movil.sesion.dominio.ResultadoInicioSesion
import com.solventa4bits.movil.sesion.dominio.ServicioDeSesion
import com.solventa4bits.movil.sesion.dominio.Sesion
import com.solventa4bits.movil.traza.Traza
import kotlinx.serialization.SerializationException
import java.io.IOException
import java.net.HttpURLConnection.HTTP_UNAUTHORIZED

/**
 * Consulta las hipotecas del cliente en el BFF, con el token de acceso de la
 * sesion guardada. Como el adaptador de sesion, nunca deja salir una
 * excepcion de red: deja traza y devuelve una respuesta controlada.
 *
 * Si el BFF rechaza el token porque vencio, refresca la sesion y repite la
 * consulta una sola vez, sin que el cliente lo note.
 */
class AdaptadorCreditos(
    private val api: CreditosApi,
    private val almacen: AlmacenDeSesion,
    private val sesiones: ServicioDeSesion,
    private val traza: Traza,
) : ServicioDeCreditos {

    override suspend fun consultar(): ResultadoCreditos {
        val sesion = almacen.leer() ?: return ResultadoCreditos.SesionVencida
        val primerIntento = consultarCon(sesion.tokenDeAcceso)
        return if (primerIntento == ResultadoCreditos.SesionVencida) refrescarYReintentar(sesion) else primerIntento
    }

    // Un solo reintento: si el token nuevo tambien es rechazado, la sesion
    // se da por vencida y no se insiste.
    private suspend fun refrescarYReintentar(vencida: Sesion): ResultadoCreditos =
        when (val refresco = sesiones.refrescar(vencida.tokenDeRefresco)) {
            is ResultadoInicioSesion.Exito -> {
                almacen.guardar(refresco.sesion)
                consultarCon(refresco.sesion.tokenDeAcceso)
            }
            ResultadoInicioSesion.CredencialesInvalidas -> ResultadoCreditos.SesionVencida
            ResultadoInicioSesion.Fallo -> ResultadoCreditos.Fallo
        }

    private suspend fun consultarCon(tokenDeAcceso: String): ResultadoCreditos =
        try {
            val respuesta = api.consultar("Bearer $tokenDeAcceso")
            val cuerpo = respuesta.body()
            when {
                respuesta.isSuccessful && cuerpo != null -> ResultadoCreditos.Exito(cuerpo.aDominio())
                respuesta.code() == HTTP_UNAUTHORIZED -> ResultadoCreditos.SesionVencida
                else -> {
                    traza.error(ORIGEN, "El BFF respondio ${respuesta.code()} al consultar las hipotecas", null)
                    ResultadoCreditos.Fallo
                }
            }
        } catch (e: IOException) {
            traza.error(ORIGEN, "No se pudo llegar al BFF al consultar las hipotecas", e)
            ResultadoCreditos.Fallo
        } catch (e: SerializationException) {
            traza.error(ORIGEN, "La respuesta del BFF no cumple el contrato de hipotecas", e)
            ResultadoCreditos.Fallo
        }

    private fun CreditosDto.aDominio() = CreditosHipotecarios(
        // Un estado que la app aun no conoce se trata como "no disponible":
        // el cliente puede reintentar o escribir sus datos.
        estado = EstadoCreditos.entries.find { it.name == estado } ?: EstadoCreditos.NO_DISPONIBLE,
        creditos = creditos.map {
            CreditoHipotecario(
                entidadId = it.entidadId,
                entidadNombre = it.entidadNombre,
                valorCredito = it.valorCredito,
                saldoInsoluto = it.saldoInsoluto,
                plazoRestanteMeses = it.plazoRestanteMeses,
                cuotaMensual = it.cuotaMensual,
            )
        },
        entidades = entidades.map { EntidadFinanciera(id = it.id, nombre = it.nombre) },
        fechaConsulta = fechaConsulta,
    )

    private companion object {
        const val ORIGEN = "AdaptadorCreditos"
    }
}
