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

    /** Fue emitida y cumplio el año sin renovarse (BITS-237, AC-6). */
    DESACTIVADA,
}

/** Lo que el cliente hizo con la propuesta de renovacion (BITS-237). */
enum class EstadoDeRenovacion { PROPUESTA, ACEPTADA, RECHAZADA }

/** La renovacion con el aumento por IPC. Llega desde 45 dias antes del aniversario. Fechas en ISO 8601. */
data class Renovacion(
    val estado: EstadoDeRenovacion,
    val primaActual: Double,
    /** La actual mas el IPC, sobre toda la prima. */
    val primaNueva: Double,
    /** En fraccion: 0.052 es 5,2 %. */
    val ipc: Double,
    /** El aniversario: el dia que se renueva o queda desactivada. */
    val fechaRenovacion: String,
    /** Hasta cuando puede decidir: el aniversario menos 30 dias. */
    val decidirHasta: String,
    /** Cuando se le aviso al banco que la poliza termina. Null si aun no. */
    val avisoAlBancoEn: String?,
)

/** Por que la solicitud sigue pendiente. */
enum class CodigoDePendiente { CORREO_SIN_CONFIRMAR, EVIDENCIA_FALTANTE, EVIDENCIA_EN_REVISION, CREDITO_NO_VERIFICADO }

/** Lo que falta para emitir. [codigo] es null si el servidor mando uno que la app no conoce. */
data class Pendiente(val codigo: CodigoDePendiente?, val mensaje: String)

/** El credito que cubre el seguro. */
data class CreditoAsegurado(val banco: String, val saldo: Double, val plazoMeses: Int)

/** Desde y hasta cuando cubre el seguro, en ISO 8601. */
data class Vigencia(val desde: String, val hasta: String)

/**
 * La solicitud de cambio de seguro, tal como la tiene el servidor. Emitida, es
 * la poliza del cliente (BITS-104). Fechas en ISO 8601.
 */
data class Solicitud(
    val id: String,
    val estado: EstadoDeSolicitud,
    val creadaEn: String,
    /** Solo cuando esta emitida. */
    val emitidaEn: String?,
    val pendientes: List<Pendiente>,
    /** Los mensajes de por que no se aprobo, listos para mostrar. Solo cuando esta rechazada. */
    val motivosDeRechazo: List<String>,
    /** Solo cuando esta emitida. */
    val numero: String? = null,
    /** Solo cuando esta emitida. */
    val vigencia: Vigencia? = null,
    /** Cuando se creo la cotizacion con la que se envio. */
    val cotizacionCreadaEn: String? = null,
    val primaMensual: Double? = null,
    val credito: CreditoAsegurado? = null,
    /** Lo que vale cada cobertura al mes, con IVA (BITS-102). */
    val primasPorCobertura: Map<CodigoCobertura, Double> = emptyMap(),
    /** Null antes de los 45 dias del aniversario. */
    val renovacion: Renovacion? = null,
) {
    /** Si ademas de la vida tomo "Tu casa". */
    val conTuCasa: Boolean
        get() = CodigoCobertura.HOGAR in primasPorCobertura
}

sealed interface ResultadoDocumento {
    /** El enlace al PDF es temporal: se abre de una vez. */
    data class Listo(val url: String) : ResultadoDocumento

    /** El servidor lo esta generando: se pide de nuevo en un momento. */
    data object Generando : ResultadoDocumento

    data object SesionVencida : ResultadoDocumento

    data object Fallo : ResultadoDocumento
}

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

sealed interface ResultadoRenovacion {
    /** El servidor guardo la decision: la poliza se consulta de nuevo para verla. */
    data object Respondida : ResultadoRenovacion

    /** No hay propuesta o ya paso el dia limite para decidir (409). */
    data object YaNoSePuede : ResultadoRenovacion

    data object SesionVencida : ResultadoRenovacion

    data object Fallo : ResultadoRenovacion
}

interface ServicioDeSolicitudes {
    suspend fun condiciones(): ResultadoCondiciones

    suspend fun enviar(datos: DatosDeLaSolicitud): ResultadoEnvio

    /** El estado actual: si estaba pendiente, el servidor la revisa de nuevo antes de responder. */
    suspend fun consultar(id: String): ResultadoConsulta

    suspend fun listar(): ResultadoLista

    /** El documento de una poliza emitida. */
    suspend fun documento(id: String): ResultadoDocumento

    /** Acepta o rechaza la propuesta de renovacion de una poliza (BITS-237). */
    suspend fun responderRenovacion(id: String, aceptar: Boolean): ResultadoRenovacion
}
