package com.solventa4bits.movil.seguro.datos

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * La base de datos de la app en el telefono. Queda en la carpeta privada de
 * la app y sin cifrar; los respaldos estan apagados en el manifest.
 */
@Database(entities = [PolizaGuardada::class], version = 1, exportSchema = false)
abstract class BaseDeDatosSolventa : RoomDatabase() {

    abstract fun polizas(): PolizasDao

    companion object {
        fun crear(contexto: Context): BaseDeDatosSolventa =
            Room.databaseBuilder(contexto, BaseDeDatosSolventa::class.java, "solventa").build()
    }
}
