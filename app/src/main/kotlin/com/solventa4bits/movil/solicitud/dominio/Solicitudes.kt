package com.solventa4bits.movil.solicitud.dominio

import com.solventa4bits.movil.cotizacion.dominio.CodigoCobertura

/** Las condiciones del seguro que el cliente lee y acepta antes de enviar su solicitud. */
data class CondicionesDelSeguro(
    /** La version publicada (V1, V2, ...). Viaja de vuelta al enviar la solicitud. */
    val version: String,
    val titulo: String,
    /** El texto con formato, en HTML sencillo (titulos, parrafos y listas). */
    val contenido: String,
    val notaPie: String?,
)

sealed interface ResultadoCondiciones {
    data class Exito(val condiciones: CondicionesDelSeguro) : ResultadoCondiciones

    data object Fallo : ResultadoCondiciones
}

/** Lo que el cliente elige y acepta al enviar la solicitud de cambio de seguro. */
data class DatosDeLaSolicitud(
    val coberturas: Set<CodigoCobertura>,
    /** Cuando se creo la cotizacion que el cliente vio: asi paga el precio que vio. */
    val cotizacionCreadaEn: String,
    val versionDeCondiciones: String,
)

enum class EstadoDeSolicitud { EMITIDA, PENDIENTE }

/** La solicitud ya registrada. [pendientes] son los mensajes de lo que falta, listos para mostrar. */
data class SolicitudEnviada(
    val polizaId: String,
    val estado: EstadoDeSolicitud,
    val pendientes: List<String>,
)

sealed interface ResultadoEnvio {
    data class Enviada(val solicitud: SolicitudEnviada) : ResultadoEnvio

    /** La cotizacion vencio, cambio o ya no admite solicitud: el cliente cotiza de nuevo. */
    data object CotizarDeNuevo : ResultadoEnvio

    data object SesionVencida : ResultadoEnvio

    /** No llego al servidor o el servidor fallo. No se reintenta solo: lo decide el cliente. */
    data object Fallo : ResultadoEnvio
}

interface ServicioDeSolicitudes {
    suspend fun condiciones(): ResultadoCondiciones

    suspend fun enviar(datos: DatosDeLaSolicitud): ResultadoEnvio
}
