package com.solventa4bits.movil.cotizacion.datos

import com.solventa4bits.movil.cotizacion.dominio.CreditoHipotecario
import com.solventa4bits.movil.cotizacion.dominio.CreditosHipotecarios
import com.solventa4bits.movil.cotizacion.dominio.EntidadFinanciera
import com.solventa4bits.movil.cotizacion.dominio.EstadoCreditos
import com.solventa4bits.movil.cotizacion.dominio.ResultadoCreditos
import com.solventa4bits.movil.cotizacion.dominio.ServicioDeCreditos
import com.solventa4bits.movil.sesion.dominio.AlmacenDeSesion
import com.solventa4bits.movil.traza.Traza
import java.io.IOException
import java.net.HttpURLConnection.HTTP_UNAUTHORIZED

/**
 * Consulta las hipotecas del cliente en el BFF, con el token de acceso de la
 * sesion guardada. Como el adaptador de sesion, nunca deja salir una
 * excepcion de red: deja traza y devuelve una respuesta controlada.
 */
class AdaptadorCreditos(
    private val api: CreditosApi,
    private val almacen: AlmacenDeSesion,
    private val traza: Traza,
) : ServicioDeCreditos {

    override suspend fun consultar(): ResultadoCreditos {
        val sesion = almacen.leer() ?: return ResultadoCreditos.SesionVencida
        return try {
            val respuesta = api.consultar("Bearer ${sesion.tokenDeAcceso}")
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
        }
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
    )

    private companion object {
        const val ORIGEN = "AdaptadorCreditos"
    }
}
