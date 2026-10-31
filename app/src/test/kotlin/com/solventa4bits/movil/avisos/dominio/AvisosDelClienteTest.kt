package com.solventa4bits.movil.avisos.dominio

import com.solventa4bits.movil.sesion.dominio.PreferenciasEnMemoria
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/** BITS-229, AC-1: el permiso se pide una vez; si lo niega no se registra nada. */
class AvisosDelClienteTest {

    private class Permiso(var concedido: Boolean, var sePuedePedir: Boolean = true) : PermisoDeNotificaciones {
        override fun concedido() = concedido

        override fun sePuedePedir() = sePuedePedir
    }

    private class ServicioFalso : ServicioDeDispositivos {
        val tokens = mutableListOf<String>()
        val bajas = mutableListOf<String>()

        override suspend fun registrar(token: String, versionApp: String): ResultadoRegistro {
            tokens += token
            return ResultadoRegistro.Registrado("dispositivo-1")
        }

        override suspend fun darDeBaja(dispositivoId: String): Boolean {
            bajas += dispositivoId
            return true
        }
    }

    private val preferencias = PreferenciasEnMemoria()
    private val servicio = ServicioFalso()
    private var haySesion = true

    private fun avisos(permiso: Permiso) = AvisosDelCliente(
        RegistroDeDispositivo(servicio, { "token-1" }, preferencias, versionApp = "0.1.0"),
        preferencias,
        permiso,
        haySesion = { haySesion },
    )

    @Test
    @DisplayName("con el permiso ya concedido registra el telefono y no vuelve a pedirlo")
    fun conPermiso() = runTest {
        val debePedir = avisos(Permiso(concedido = true)).alEntrar()

        assertFalse(debePedir)
        assertEquals(listOf("token-1"), servicio.tokens)
    }

    @Test
    @DisplayName("sin permiso lo pide, una sola vez, y no registra nada todavia")
    fun pideElPermisoUnaVez() = runTest {
        val avisos = avisos(Permiso(concedido = false))

        assertTrue(avisos.alEntrar())
        assertFalse(avisos.alEntrar())

        assertTrue(preferencias.permisoDeAvisosPedido())
        assertEquals(emptyList<String>(), servicio.tokens)
    }

    @Test
    @DisplayName("si lo niega la app sigue igual: no registra nada y no insiste")
    fun siLoNiega() = runTest {
        val avisos = avisos(Permiso(concedido = false))
        avisos.alEntrar()

        assertFalse(avisos.alEntrar())
        assertEquals(emptyList<String>(), servicio.tokens)
        assertNull(preferencias.dispositivoId())
    }

    @Test
    @DisplayName("si el sistema no deja pedirlo (antes de Android 13) no se pide")
    fun noSePuedePedir() = runTest {
        val debePedir = avisos(Permiso(concedido = false, sePuedePedir = false)).alEntrar()

        assertFalse(debePedir)
        assertFalse(preferencias.permisoDeAvisosPedido())
    }

    @Test
    @DisplayName("al aceptar el permiso registra el telefono")
    fun alConcederElPermiso() = runTest {
        avisos(Permiso(concedido = true)).alConcederPermiso()

        assertEquals(listOf("token-1"), servicio.tokens)
        assertEquals("dispositivo-1", preferencias.dispositivoId())
    }

    @Test
    @DisplayName("aceptar el permiso despues de que la sesion se cerro no registra nada")
    fun alConcederSinSesion() = runTest {
        haySesion = false

        avisos(Permiso(concedido = true)).alConcederPermiso()

        assertEquals(emptyList<String>(), servicio.tokens)
    }

    @Test
    @DisplayName("un token nuevo de Firebase se registra solo con sesion y permiso")
    fun tokenNuevo() = runTest {
        avisos(Permiso(concedido = true)).alCambiarElToken("token-nuevo")
        avisos(Permiso(concedido = false)).alCambiarElToken("sin-permiso")
        haySesion = false
        avisos(Permiso(concedido = true)).alCambiarElToken("sin-sesion")

        assertEquals(listOf("token-nuevo"), servicio.tokens)
    }

    @Test
    @DisplayName("al salir da de baja el telefono registrado")
    fun alSalir() = runTest {
        preferencias.guardarDispositivoId("dispositivo-1")

        avisos(Permiso(concedido = true)).alSalir()

        assertEquals(listOf("dispositivo-1"), servicio.bajas)
        assertNull(preferencias.dispositivoId())
    }

    @Test
    @DisplayName("SinAvisos no hace nada ni pide nada")
    fun sinAvisos() = runTest {
        assertFalse(SinAvisos.alEntrar())
        SinAvisos.alConcederPermiso()
        SinAvisos.alSalir()
    }
}
