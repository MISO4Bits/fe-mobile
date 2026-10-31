package com.solventa4bits.movil.seguro.datos

import com.solventa4bits.movil.cotizacion.dominio.CodigoCobertura
import com.solventa4bits.movil.seguro.dominio.SegurosGuardados
import com.solventa4bits.movil.solicitud.dominio.CreditoAsegurado
import com.solventa4bits.movil.solicitud.dominio.EstadoDeSolicitud
import com.solventa4bits.movil.solicitud.dominio.Solicitud
import com.solventa4bits.movil.solicitud.dominio.Vigencia
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class AlmacenDeSegurosRoomTest {

    private val dao = PolizasEnMemoria()
    private val almacen = AlmacenDeSegurosRoom(dao)

    private val emitida = Solicitud(
        id = "poliza-1",
        estado = EstadoDeSolicitud.EMITIDA,
        creadaEn = "2026-10-19T14:00:00Z",
        emitidaEn = "2026-10-19T14:41:00Z",
        pendientes = emptyList(),
        motivosDeRechazo = emptyList(),
        numero = "SOL-VH-2026-K7M2Q9",
        vigencia = Vigencia("2026-10-19T14:41:00Z", "2027-10-19T14:41:00Z"),
        primaMensual = 39_734.0,
        credito = CreditoAsegurado("Bancolombia", 238_500_000.0, 180),
        primasPorCobertura = mapOf(CodigoCobertura.VIDA to 30_000.0, CodigoCobertura.HOGAR to 9_734.0),
    )

    @Test
    @DisplayName("si no se ha guardado nada no hay polizas")
    fun sinNada() = runTest {
        assertNull(almacen.leer())
    }

    @Test
    @DisplayName("BITS-217, AC-1: devuelve los datos de la poliza y cuando se guardaron")
    fun guardaYLee() = runTest {
        almacen.guardar(listOf(emitida), GUARDADA_EN)

        assertEquals(SegurosGuardados(listOf(emitida), GUARDADA_EN), almacen.leer())
    }

    @Test
    @DisplayName("una poliza sin vigencia, credito ni primas se guarda y se lee igual")
    fun sinDatosOpcionales() = runTest {
        val minima = emitida.copy(vigencia = null, credito = null, primasPorCobertura = emptyMap())

        almacen.guardar(listOf(minima), GUARDADA_EN)

        assertEquals(listOf(minima), almacen.leer()?.seguros)
    }

    @Test
    @DisplayName("BITS-217, AC-4: guardar de nuevo reemplaza lo anterior")
    fun reemplaza() = runTest {
        almacen.guardar(listOf(emitida, emitida.copy(id = "poliza-2")), "2026-10-18T10:00:00Z")

        almacen.guardar(listOf(emitida), GUARDADA_EN)

        assertEquals(listOf("poliza-1"), almacen.leer()?.seguros?.map { it.id })
        assertEquals(GUARDADA_EN, almacen.leer()?.actualizadosEn)
    }

    @Test
    @DisplayName("BITS-217, AC-6: al borrar no queda ninguna poliza")
    fun borra() = runTest {
        almacen.guardar(listOf(emitida), GUARDADA_EN)

        almacen.borrar()

        assertNull(almacen.leer())
        assertTrue(dao.filas.isEmpty())
    }

    /** Doble de prueba: la tabla en memoria. Room real se prueba en PolizasDaoTest. */
    private class PolizasEnMemoria : PolizasDao() {
        val filas = mutableListOf<PolizaGuardada>()

        override suspend fun todas() = filas.toList()

        override suspend fun insertar(polizas: List<PolizaGuardada>) {
            filas += polizas
        }

        override suspend fun borrarTodas() {
            filas.clear()
        }
    }

    private companion object {
        const val GUARDADA_EN = "2026-10-19T14:41:00Z"
    }
}
