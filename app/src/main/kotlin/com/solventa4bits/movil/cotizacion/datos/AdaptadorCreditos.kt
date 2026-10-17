package com.solventa4bits.movil.cotizacion.datos

import com.solventa4bits.movil.cotizacion.dominio.CreditoHipotecario
import com.solventa4bits.movil.cotizacion.dominio.CreditosHipotecarios
import com.solventa4bits.movil.cotizacion.dominio.EntidadFinanciera
import com.solventa4bits.movil.cotizacion.dominio.EstadoCreditos
import com.solventa4bits.movil.cotizacion.dominio.ResultadoCreditos
import com.solventa4bits.movil.cotizacion.dominio.ServicioDeCreditos
import com.solventa4bits.movil.sesion.datos.ConSesion
import com.solventa4bits.movil.sesion.datos.LlamadaConSesion
import com.solventa4bits.movil.traza.Traza
import kotlinx.serialization.SerializationException
import retrofit2.Response
import java.io.IOException

/**
 * Consulta las hipotecas del cliente en el BFF, con la sesion guardada (el
 * refresco del token lo resuelve [ConSesion]). Nunca deja salir una
 * excepcion: deja traza y devuelve una respuesta controlada.
 */
class AdaptadorCreditos(
    private val api: CreditosApi,
    private val sesion: ConSesion,
    private val traza: Traza,
) : ServicioDeCreditos {

    override suspend fun consultar(): ResultadoCreditos =
        try {
            when (val resultado = sesion.llamar { autorizacion -> api.consultar(autorizacion) }) {
                is LlamadaConSesion.Respondio -> traducir(resultado.respuesta)
                LlamadaConSesion.SesionVencida -> ResultadoCreditos.SesionVencida
                LlamadaConSesion.SinRefresco -> ResultadoCreditos.Fallo
            }
        } catch (e: IOException) {
            traza.error(ORIGEN, "No se pudo llegar al BFF al consultar las hipotecas", e)
            ResultadoCreditos.Fallo
        } catch (e: SerializationException) {
            traza.error(ORIGEN, "La respuesta del BFF no cumple el contrato de hipotecas", e)
            ResultadoCreditos.Fallo
        }

    private fun traducir(respuesta: Response<CreditosDto>): ResultadoCreditos {
        val cuerpo = respuesta.body()
        return if (respuesta.isSuccessful && cuerpo != null) {
            ResultadoCreditos.Exito(cuerpo.aDominio())
        } else {
            traza.error(ORIGEN, "El BFF respondio ${respuesta.code()} al consultar las hipotecas", null)
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
        fechaConsulta = fechaConsulta,
    )

    private companion object {
        const val ORIGEN = "AdaptadorCreditos"
    }
}
