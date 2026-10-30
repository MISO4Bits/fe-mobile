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

/** Lo decide el servidor: la app solo lo muestra (BITS-97). */
enum class EstadoDeSolicitud {
    EMITIDA,

    /** Falta algo: lo dicen sus [Solicitud.pendientes]. */
    PENDIENTE,

    /** La rechazo un agente: lo dicen sus [Solicitud.motivosDeRechazo]. */
    RECHAZADA,

    /** Seguia pendiente cuando vencio la cotizacion: el cliente cotiza de nuevo. */
    VENCIDA,
}

/** Por que la solicitud sigue pendiente. */
enum class CodigoDePendiente { CORREO_SIN_CONFIRMAR, EVIDENCIA_FALTANTE, EVIDENCIA_EN_REVISION, CREDITO_NO_VERIFICADO }

/** Lo que falta para emitir. [codigo] es null si el servidor mando uno que la app no conoce. */
data class Pendiente(val codigo: CodigoDePendiente?, val mensaje: String)

/** La solicitud de cambio de seguro, tal como la tiene el servidor. Fechas en ISO 8601. */
data class Solicitud(
    val id: String,
    val estado: EstadoDeSolicitud,
    val creadaEn: String,
    /** Solo cuando esta emitida. */
    val emitidaEn: String?,
    val pendientes: List<Pendiente>,
    /** Los mensajes de por que no se aprobo, listos para mostrar. Solo cuando esta rechazada. */
    val motivosDeRechazo: List<String>,
)

sealed interface ResultadoEnvio {
    data class Enviada(val solicitud: Solicitud) : ResultadoEnvio

    /** La cotizacion vencio, cambio o ya no admite solicitud: el cliente cotiza de nuevo. */
    data object CotizarDeNuevo : ResultadoEnvio

    data object SesionVencida : ResultadoEnvio

    /** No llego al servidor o el servidor fallo. No se reintenta solo: lo decide el cliente. */
    data object Fallo : ResultadoEnvio
}

sealed interface ResultadoConsulta {
    data class Exito(val solicitud: Solicitud) : ResultadoConsulta

    data object NoExiste : ResultadoConsulta

    data object SesionVencida : ResultadoConsulta

    data object Fallo : ResultadoConsulta
}

sealed interface ResultadoLista {
    /** De la mas reciente a la mas antigua. */
    data class Exito(val solicitudes: List<Solicitud>) : ResultadoLista

    data object SesionVencida : ResultadoLista

    data object Fallo : ResultadoLista
}

interface ServicioDeSolicitudes {
    suspend fun condiciones(): ResultadoCondiciones

    suspend fun enviar(datos: DatosDeLaSolicitud): ResultadoEnvio

    /** El estado actual: si estaba pendiente, el servidor la revisa de nuevo antes de responder. */
    suspend fun consultar(id: String): ResultadoConsulta

    suspend fun listar(): ResultadoLista
}
