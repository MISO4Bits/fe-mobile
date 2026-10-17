package com.solventa4bits.movil.cotizacion.ui

import com.solventa4bits.movil.cotizacion.dominio.Cotizacion
import com.solventa4bits.movil.cotizacion.dominio.FactorDelPrecio

/** Los factores del precio de mayor a menor peso. Los que no traen peso van al final. */
fun Cotizacion.factoresPorPeso(): List<FactorDelPrecio> =
    factores.sortedByDescending { it.peso ?: Double.NEGATIVE_INFINITY }

/**
 * El factor que mas influye en el precio, para senalarlo como "lo que mas
 * pesa". Null si hay uno solo o si el perfil no trae pesos: no hay con que
 * compararlo.
 */
fun Cotizacion.factorQueMasPesa(): FactorDelPrecio? =
    if (factores.size < 2) null else factores.filter { it.peso != null }.maxByOrNull { it.peso ?: 0.0 }
