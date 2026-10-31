package com.solventa4bits.movil.avisos.dominio

import com.solventa4bits.movil.sesion.dominio.PreferenciasEnMemoria
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class RegistroDeDispositivoTest {

    private class ServicioFalso(var registro: ResultadoRegistro, var baja: Boolean = true) : ServicioDeDispositivos {
        val registrados = mutableListOf<Pair<String, String>>()
        val bajas = mutableListOf<String>()

        override suspend fun registrar(token: String, versionApp: String): ResultadoRegistro {
            registrados += token to versionApp
            return registro
        }

        override suspend fun darDeBaja(dispositivoId: String): Boolean {
            bajas += dispositivoId
            return baja
        }
    }

    private val preferencias = PreferenciasEnMemoria()

    private fun registro(servicio: ServicioDeDispositivos, token: String? = "token-1") =
        RegistroDeDispositivo(servicio, { token }, preferencias, versionApp = "0.1.0")

    @Test
    @DisplayName("registra el token de Firebase con la version de la app y guarda el id que devuelve el servidor")
    fun registraYGuardaElId() = runTest {
        val servicio = ServicioFalso(ResultadoRegistro.Registrado("dispositivo-1"))

        registro(servicio).registrar()

        assertEquals(listOf("token-1" to "0.1.0"), servicio.registrados)
        assertEquals("dispositivo-1", preferencias.dispositivoId())
    }

    @Test
    @DisplayName("sin token de Firebase no llama al servidor")
    fun sinToken() = runTest {
        val servicio = ServicioFalso(ResultadoRegistro.Registrado("dispositivo-1"))

        registro(servicio, token = null).registrar()

        assertEquals(emptyList<Pair<String, String>>(), servicio.registrados)
        assertNull(preferencias.dispositivoId())
    }

    @Test
    @DisplayName("si el registro falla o la sesion vencio no guarda nada: se reintenta al proximo arranque")
    fun registroFallido() = runTest {
        for (resultado in listOf(ResultadoRegistro.Fallo, ResultadoRegistro.SesionVencida)) {
            registro(ServicioFalso(resultado)).registrar()
        }

        assertNull(preferencias.dispositivoId())
    }

    @Test
    @DisplayName("un token nuevo de Firebase se registra tal cual")
    fun tokenNuevo() = runTest {
        val servicio = ServicioFalso(ResultadoRegistro.Registrado("dispositivo-2"))

        registro(servicio).registrarToken("token-nuevo")

        assertEquals(listOf("token-nuevo" to "0.1.0"), servicio.registrados)
        assertEquals("dispositivo-2", preferencias.dispositivoId())
    }

    @Test
    @DisplayName("al cerrar sesion da de baja el telefono y olvida su id")
    fun daDeBaja() = runTest {
        preferencias.guardarDispositivoId("dispositivo-1")
        val servicio = ServicioFalso(ResultadoRegistro.Fallo)

        registro(servicio).darDeBaja()

        assertEquals(listOf("dispositivo-1"), servicio.bajas)
        assertNull(preferencias.dispositivoId())
    }

    @Test
    @DisplayName("si la baja no llega al servidor el id se olvida igual: el cierre de sesion no depende de la red")
    fun bajaSinRed() = runTest {
        preferencias.guardarDispositivoId("dispositivo-1")

        registro(ServicioFalso(ResultadoRegistro.Fallo, baja = false)).darDeBaja()

        assertNull(preferencias.dispositivoId())
    }

    @Test
    @DisplayName("sin telefono registrado no hay nada que dar de baja")
    fun sinRegistro() = runTest {
        val servicio = ServicioFalso(ResultadoRegistro.Fallo)

        registro(servicio).darDeBaja()

        assertEquals(emptyList<String>(), servicio.bajas)
    }
}
