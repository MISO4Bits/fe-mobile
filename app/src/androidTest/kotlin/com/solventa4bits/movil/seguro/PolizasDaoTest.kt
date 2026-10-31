package com.solventa4bits.movil.seguro

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.solventa4bits.movil.seguro.datos.BaseDeDatosSolventa
import com.solventa4bits.movil.seguro.datos.PolizaGuardada
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** La tabla "polizas" sobre Room de verdad, en memoria (BITS-217). */
@RunWith(AndroidJUnit4::class)
class PolizasDaoTest {

    private val base = Room.inMemoryDatabaseBuilder(
        InstrumentationRegistry.getInstrumentation().targetContext,
        BaseDeDatosSolventa::class.java,
    ).build()
    private val dao = base.polizas()

    @After
    fun cerrar() {
        base.close()
    }

    @Test
    fun reemplazarDejaSoloLasNuevasConLaMasRecientePrimero() = runBlocking {
        dao.reemplazar(listOf(fila("vieja", "2026-01-01T10:00:00Z")))

        dao.reemplazar(listOf(fila("antes", "2026-09-01T10:00:00Z"), fila("despues", "2026-10-01T10:00:00Z")))

        assertEquals(listOf("despues", "antes"), dao.todas().map { it.id })
    }

    @Test
    fun borrarTodasDejaLaTablaVacia() = runBlocking {
        dao.reemplazar(listOf(fila("poliza-1", "2026-10-01T10:00:00Z")))

        dao.borrarTodas()

        assertTrue(dao.todas().isEmpty())
    }

    private fun fila(id: String, emitidaEn: String) = PolizaGuardada(
        id = id,
        numero = "SOL-$id",
        creadaEn = emitidaEn,
        emitidaEn = emitidaEn,
        vigenciaDesde = null,
        vigenciaHasta = null,
        primaMensual = null,
        banco = null,
        saldo = null,
        plazoMeses = null,
        primaVida = null,
        primaHogar = null,
        guardadaEn = "2026-10-19T14:41:00Z",
    )
}
