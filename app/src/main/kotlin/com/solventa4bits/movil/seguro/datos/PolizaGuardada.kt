package com.solventa4bits.movil.seguro.datos

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Una fila de la tabla "polizas": lo que se ve de la poliza sin conexion (BITS-217, AC-1). */
@Entity(tableName = "polizas")
data class PolizaGuardada(
    @PrimaryKey val id: String,
    val numero: String?,
    val creadaEn: String,
    val emitidaEn: String?,
    val vigenciaDesde: String?,
    val vigenciaHasta: String?,
    val primaMensual: Double?,
    val banco: String?,
    val saldo: Double?,
    val plazoMeses: Int?,
    val primaVida: Double?,
    val primaHogar: Double?,
    /** Cuando se guardo, en ISO 8601. */
    val guardadaEn: String,
)
