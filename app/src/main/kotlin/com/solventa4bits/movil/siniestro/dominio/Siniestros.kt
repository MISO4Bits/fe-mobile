package com.solventa4bits.movil.siniestro.dominio

import com.solventa4bits.movil.solicitud.dominio.Solicitud
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId

/**
 * Cada cobertura tiene su camino (BITS-111, AC-1): Vida reporta incapacidad y Tu casa, incendio o
 * terremoto. El sismo lo crea el pago automatico: se ve en la lista, pero no se reporta.
 */
enum class TipoDeSiniestro {
    INCAPACIDAD_TOTAL,
    INCENDIO_TERREMOTO,
    SISMO_PARAMETRICO,
    ;

    /** Los de Tu casa; los demas son de Vida. */
    val deTuCasa: Boolean
        get() = this != INCAPACIDAD_TOTAL
}

/** Lo decide Solventa: la app solo lo muestra. */
enum class EstadoDeSiniestro {
    EN_ESTUDIO,
    FALTA_INFORMACION,
    PAGADO,
    RECHAZADO,
    ;

    /** Mientras esta en curso no se puede reportar otro de la misma cobertura (AC-2A). */
    val enCurso: Boolean
        get() = this == EN_ESTUDIO || this == FALTA_INFORMACION
}

enum class ClaseDeArchivo { FOTO, DOCUMENTO }

/**
 * Una foto del dano o una pagina escaneada, ya preparada (JPEG). La ubicacion viaja aparte
 * porque el preparador borra los metadatos de la foto; es null si el cliente nego el permiso.
 */
class ArchivoDelReporte(
    val clase: ClaseDeArchivo,
    val jpeg: ByteArray,
    /** Cuando se tomo, en ISO 8601. */
    val capturadoEn: String,
    val latitud: Double? = null,
    val longitud: Double? = null,
)

data class ReporteDeSiniestro(
    val polizaId: String,
    val tipo: TipoDeSiniestro,
    val fechaOcurrencia: LocalDate,
    /** Obligatoria en Tu casa. */
    val narracion: String?,
    val archivos: List<ArchivoDelReporte>,
)

sealed interface ResultadoReporte {
    /** Quedo En estudio con su numero de radicado (AC-7). */
    data class Radicado(val id: String, val numero: String) : ResultadoReporte

    /** La fecha no cae en la vigencia de la poliza (AC-2). */
    data object FueraDeVigencia : ResultadoReporte

    /** Esa cobertura ya tiene un siniestro En estudio o en Falta informacion (AC-2A). */
    data object SiniestroEnCurso : ResultadoReporte

    /** Incendio o terremoto en una poliza sin Tu casa. */
    data object TipoNoCubierto : ResultadoReporte

    data object SesionVencida : ResultadoReporte

    /** No llego al servidor o el servidor fallo. El cliente reintenta con la misma llave. */
    data object Fallo : ResultadoReporte
}

/**
 * Un siniestro de la lista. [tipo] y [estado] son null si el servidor manda uno que la app no
 * conoce. Las fechas, en ISO 8601.
 */
data class SiniestroResumen(
    val id: String,
    val numero: String,
    val polizaId: String,
    val tipo: TipoDeSiniestro?,
    val estado: EstadoDeSiniestro?,
    /** El pago automatico por sismo: nadie lo reporto. */
    val automatico: Boolean = false,
    val reportadoEn: String = "",
    val actualizadoEn: String = "",
)

sealed interface ResultadoSiniestros {
    /** Del mas reciente al mas antiguo. */
    data class Exito(val siniestros: List<SiniestroResumen>) : ResultadoSiniestros

    data object SesionVencida : ResultadoSiniestros

    data object Fallo : ResultadoSiniestros
}

/** En que va un siniestro. Lo propio de cada estado (pago, rechazo, lo que falta) es de BITS-383 y BITS-384. */
data class DetalleDeSiniestro(
    val resumen: SiniestroResumen,
    /** El dia en que ocurrio, como "2026-11-02". */
    val fechaOcurrencia: String,
    /** Hasta cuando tiene Solventa para responder. Solo En estudio. */
    val fechaLimite: String?,
    val documentos: Int,
    val fotos: Int,
)

sealed interface ResultadoDetalle {
    data class Exito(val detalle: DetalleDeSiniestro) : ResultadoDetalle

    data object NoExiste : ResultadoDetalle

    data object SesionVencida : ResultadoDetalle

    data object Fallo : ResultadoDetalle
}

interface ServicioDeSiniestros {
    /**
     * Envia el reporte con su evidencia. [llave] se crea una vez por reporte y se repite en cada
     * reintento: asi, si se cae la conexion, el servidor crea un solo siniestro (AC-7).
     */
    suspend fun reportar(reporte: ReporteDeSiniestro, llave: String): ResultadoReporte

    suspend fun listar(): ResultadoSiniestros

    suspend fun consultar(id: String): ResultadoDetalle
}

/** El siniestro en curso de esa cobertura en esa poliza, si lo hay (AC-2A). */
fun List<SiniestroResumen>.enCurso(polizaId: String, tipo: TipoDeSiniestro): SiniestroResumen? =
    find { it.polizaId == polizaId && it.tipo == tipo && it.estado?.enCurso == true }

/** Por que no se puede reportar algo que paso ese dia (AC-2). */
enum class ProblemaDeFecha { FUTURA, ANTES_DE_LA_POLIZA, DESPUES_DE_LA_POLIZA }

private val zonaDeColombia = ZoneId.of("America/Bogota")

/**
 * Los dias en que la poliza cubre, en Colombia: desde la emision hasta el fin de la vigencia. Es la
 * regla de svc-core: se compara con la emision y no con el inicio de la vigencia, que la renovacion
 * corre. Null si la poliza no trae esas fechas.
 */
fun Solicitud.diasCubiertos(zona: ZoneId = zonaDeColombia): ClosedRange<LocalDate>? {
    val desde = emitidaEn?.let { diaEn(it, zona) }
    val hasta = vigencia?.hasta?.let { diaEn(it, zona) }
    return if (desde != null && hasta != null) desde..hasta else null
}

/** Null si se puede reportar algo que paso en [fecha]; si no, por que. */
fun Solicitud.problemaConLaFecha(fecha: LocalDate, hoy: LocalDate, zona: ZoneId = zonaDeColombia): ProblemaDeFecha? {
    val dias = diasCubiertos(zona)
    return when {
        fecha.isAfter(hoy) -> ProblemaDeFecha.FUTURA
        dias == null || fecha.isBefore(dias.start) -> ProblemaDeFecha.ANTES_DE_LA_POLIZA
        fecha.isAfter(dias.endInclusive) -> ProblemaDeFecha.DESPUES_DE_LA_POLIZA
        else -> null
    }
}

private fun diaEn(fechaIso: String, zona: ZoneId): LocalDate? =
    runCatching { OffsetDateTime.parse(fechaIso).atZoneSameInstant(zona).toLocalDate() }.getOrNull()
