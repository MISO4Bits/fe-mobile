package com.solventa4bits.movil.cotizacion.ui

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

// Punto como separador de miles, sin depender del idioma del telefono.
private val simbolos = DecimalFormatSymbols(Locale.ROOT).apply { groupingSeparator = '.' }

/** Un valor en pesos colombianos como se muestra al cliente: $380.000.000. */
fun enPesos(valor: Double): String = "$" + DecimalFormat("#,###", simbolos).format(valor)
