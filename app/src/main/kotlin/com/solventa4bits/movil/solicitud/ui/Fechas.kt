package com.solventa4bits.movil.solicitud.ui

import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val zonaDeColombia = ZoneId.of("America/Bogota")
private val formatoDelDia = DateTimeFormatter.ofPattern("d 'de' MMMM", Locale.forLanguageTag("es-CO"))
private const val HORAS_DE_MEDIO_DIA = 12

/**
 * Una fecha de la solicitud como en el Figma 08: "Hoy, 9:41 a. m.", "Ayer,
 * 9:41 a. m." o "27 de octubre, 9:41 a. m.", en la hora de Colombia. Null si
 * el servidor no mando una fecha con el formato del contrato. [ahora] viene de
 * afuera para que las pruebas no dependan del dia en que corren.
 */
fun fechaDeSolicitud(fechaIso: String, ahora: Instant = Instant.now(), zona: ZoneId = zonaDeColombia): String? {
    val fecha = runCatching { OffsetDateTime.parse(fechaIso).atZoneSameInstant(zona) }.getOrNull() ?: return null
    val hoy = ahora.atZone(zona).toLocalDate()
    val dia = when (fecha.toLocalDate()) {
        hoy -> "Hoy"
        hoy.minusDays(1) -> "Ayer"
        else -> fecha.format(formatoDelDia)
    }
    val hora = (fecha.hour + HORAS_DE_MEDIO_DIA - 1) % HORAS_DE_MEDIO_DIA + 1
    val minutos = fecha.minute.toString().padStart(2, '0')
    val sufijo = if (fecha.hour < HORAS_DE_MEDIO_DIA) "a. m." else "p. m."
    return "$dia, $hora:$minutos $sufijo"
}

/** El dia en que empieza el seguro, como en el Figma 09: "22 de octubre", en Colombia. Null si no tiene el formato. */
fun fechaDelDia(fechaIso: String, zona: ZoneId = zonaDeColombia): String? =
    runCatching { OffsetDateTime.parse(fechaIso).atZoneSameInstant(zona).format(formatoDelDia) }.getOrNull()

// Sin punto, como en el Figma 11; el formato del sistema le pone "oct.".
private val mesesCortos = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")

/** Una fecha de la vigencia como en el Figma 11: "22 oct 2026", en Colombia. Null si no tiene el formato. */
fun fechaCorta(fechaIso: String, zona: ZoneId = zonaDeColombia): String? {
    val fecha = runCatching { OffsetDateTime.parse(fechaIso).atZoneSameInstant(zona) }.getOrNull() ?: return null
    return "${fecha.dayOfMonth} ${mesesCortos[fecha.monthValue - 1]} ${fecha.year}"
}
