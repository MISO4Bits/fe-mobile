package com.solventa4bits.movil.seguro.datos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction

@Dao
abstract class PolizasDao {

    @Query("SELECT * FROM polizas ORDER BY emitidaEn DESC")
    abstract suspend fun todas(): List<PolizaGuardada>

    @Insert
    abstract suspend fun insertar(polizas: List<PolizaGuardada>)

    @Query("DELETE FROM polizas")
    abstract suspend fun borrarTodas()

    /** Cambia todo lo guardado por [polizas] de una vez: nunca queda una tabla a medias. */
    @Transaction
    open suspend fun reemplazar(polizas: List<PolizaGuardada>) {
        borrarTodas()
        insertar(polizas)
    }
}
