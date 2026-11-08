package com.solventa4bits.movil.siniestro.dominio

import com.solventa4bits.movil.solicitud.dominio.Solicitud
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId

/** Cada cobertura tiene su camino (BITS-111, AC-1): Vida reporta incapacidad y Tu casa, incendio o terremoto. */
enum class TipoDeSiniestro { INCAPACIDAD_TOTAL, INCENDIO_TERREMOTO }

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
 * Una foto del dano o un documento escaneado, ya preparado (JPEG). La ubicacion viaja aparte
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

/** Un siniestro de la lista. [tipo] es null en los que la app no reporta, como el pago automatico por sismo. */
data class SiniestroResumen(
    val id: String,
    val numero: String,
    val polizaId: String,
    val tipo: TipoDeSiniestro?,
    val estado: EstadoDeSiniestro?,
)

sealed interface ResultadoSiniestros {
    /** Del mas reciente al mas antiguo. */
    data class Exito(val siniestros: List<SiniestroResumen>) : ResultadoSiniestros

    data object SesionVencida : ResultadoSiniestros

    data object Fallo : ResultadoSiniestros
}

interface ServicioDeSiniestros {
    /**
     * Envia el reporte con su evidencia. [llave] se crea una vez por reporte y se repite en cada
     * reintento: asi, si se cae la conexion, el servidor crea un solo siniestro (AC-7).
     */
    suspend fun reportar(reporte: ReporteDeSiniestro, llave: String): ResultadoReporte

    suspend fun listar(): ResultadoSiniestros
}

/** El siniestro en curso de esa cobertura en esa poliza, si lo hay (AC-2A). */
fun List<SiniestroResumen>.enCurso(polizaId: String, tipo: TipoDeSiniestro): SiniestroResumen? =
    find { it.polizaId == polizaId && it.tipo == tipo && it.estado?.enCurso == true }

private val zonaDeColombia = ZoneId.of("America/Bogota")

/**
 * Si se puede reportar algo que paso en [fecha] (AC-2): no en el futuro, ni antes de que se emitiera
 * la poliza, ni despues del fin de su vigencia. Es la misma regla de svc-core, con las fechas en
 * Colombia; se compara con la emision y no con el inicio de la vigencia, que la renovacion corre.
 */
fun Solicitud.admiteSiniestroEl(fecha: LocalDate, hoy: LocalDate, zona: ZoneId = zonaDeColombia): Boolean {
    val desde = emitidaEn?.let { diaEn(it, zona) }
    val hasta = vigencia?.hasta?.let { diaEn(it, zona) }
    if (desde == null || hasta == null) return false
    return !fecha.isAfter(hoy) && !fecha.isBefore(desde) && !fecha.isAfter(hasta)
}

private fun diaEn(fechaIso: String, zona: ZoneId): LocalDate? =
    runCatching { OffsetDateTime.parse(fechaIso).atZoneSameInstant(zona).toLocalDate() }.getOrNull()
