package com.solventa4bits.movil.cotizacion.ui

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// Punto como separador de miles, sin depender del idioma del telefono.
private val simbolos = DecimalFormatSymbols(Locale.ROOT).apply { groupingSeparator = '.' }

/** Un valor en pesos colombianos como se muestra al cliente: $380.000.000. */
fun enPesos(valor: Double): String = "$" + DecimalFormat("#,###", simbolos).format(valor)

private val zonaDeColombia = ZoneId.of("America/Bogota")
private val formatoDeFecha = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es-CO"))

/**
 * La fecha de una consulta como se muestra al cliente: 9 de octubre de 2026.
 * Null si el BFF no mando una fecha con el formato del contrato.
 */
fun fechaLegible(fechaIso: String): String? =
    runCatching { OffsetDateTime.parse(fechaIso).atZoneSameInstant(zonaDeColombia).format(formatoDeFecha) }
        .getOrNull()
