package com.solventa4bits.movil

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.solventa4bits.movil.fixtures.BffFalso
import com.solventa4bits.movil.fixtures.RespuestasBff
import com.solventa4bits.movil.fixtures.SesionLimpia
import com.solventa4bits.movil.seguro.dominio.AlmacenDeSeguros
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** La poliza guardada en el telefono para verla sin conexion (BITS-217). */
@RunWith(AndroidJUnit4::class)
class SinConexionTest {

    private val almacen = almacenDelDispositivo()
    private lateinit var guardados: AlmacenDeSeguros

    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val bff = BffFalso()

    @get:Rule
    val sesionLimpia = SesionLimpia(almacen)

    @Test
    fun conConexionLaGuardaYAlCerrarSesionLaBorra() {
        llegarAlInicioConUnaPoliza()

        // AC-1: la poliza que se vio quedo en el telefono.
        esperarGuardado { it == "SOL-VH-2026-K7M2Q9" }

        composeRule.onNodeWithContentDescription("Menú").performClick()
        composeRule.onNodeWithText("Cerrar sesión").performClick()
        composeRule.esperarTexto("¿Quieres cerrar sesión?")
        composeRule.onNodeWithText("Cerrar sesión").performClick()

        // AC-6
        composeRule.esperarTexto("Iniciar sesión")
        esperarGuardado { it == null }
    }

    @Test
    fun sinConexionSeVeLaPolizaGuardadaYSuDetalle() {
        llegarAlInicioConUnaPoliza()
        esperarGuardado { it == "SOL-VH-2026-K7M2Q9" }

        sinConexion()
        composeRule.onNodeWithText("SOL-VH-2026-K7M2Q9").performScrollTo().performClick()
        composeRule.esperarTexto("Detalle de tu seguro")
        composeRule.onNodeWithText("Inicio").performClick()

        // AC-2: lo guardado, con la fecha y el aviso, sin pantalla de error.
        composeRule.esperarTexto(AVISO)
        composeRule.onNodeWithText("Mi póliza").assertIsDisplayed()
        composeRule.onNodeWithText("ACTUALIZADA EL", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("SOL-VH-2026-K7M2Q9").performClick()
        composeRule.esperarTexto("Detalle de tu seguro")
        composeRule.onNodeWithText(AVISO).assertIsDisplayed()
        composeRule.onNodeWithText("$39.734").assertIsDisplayed()

        composeRule.onNodeWithText("Ver documento").performClick()
        composeRule.esperarTexto("Para ver el documento necesitas conexión.")
    }

    @Test
    fun alVolverLaConexionSeActualizaSinPedirlo() {
        bff.responder(200, RespuestasBff.sesionValida())
        guardados = composeRule.abrirApp(bff, almacen)
        sinConexion()

        // Sin nada guardado y sin conexion: no hay pantalla de error.
        composeRule.iniciarSesion(hasta = "Todavía no tienes seguros guardados en el teléfono.")
        composeRule.onNodeWithText(AVISO).assertIsDisplayed()

        // AC-4: con conexion otra vez, se actualiza lo guardado.
        bff.responderSiempreEn("/v1/cotizaciones/actual", 404)
        bff.responderSiempreEn(RUTA_POLIZAS, 200, "[${polizaEnviada("EMITIDA")}]", metodo = "GET")
        composeRule.onNodeWithText("Reintentar la conexión").performClick()
        composeRule.esperarTexto("Tus seguros")
        esperarGuardado { it == "SOL-VH-2026-K7M2Q9" }
    }

    /** El BFF deja de responder, como si el telefono no tuviera red. */
    private fun sinConexion() {
        bff.responderSiempreEn("/v1/cotizaciones/actual", 503)
        bff.responderSiempreEn(RUTA_POLIZAS, 503, metodo = "GET")
    }

    private fun llegarAlInicioConUnaPoliza() {
        bff.responder(200, RespuestasBff.sesionValida())
        guardados = composeRule.abrirApp(bff, almacen)
        bff.responderSiempreEn(RUTA_POLIZAS, 200, "[${polizaEnviada("EMITIDA")}]", metodo = "GET")
        composeRule.iniciarSesion()
        composeRule.esperarTexto("Tus seguros")
    }

    /** Room guarda en segundo plano: espera a que el numero guardado cumpla [condicion]. */
    private fun esperarGuardado(condicion: (String?) -> Boolean) {
        composeRule.waitUntil(ESPERA_MS) {
            condicion(runBlocking { guardados.leer() }?.seguros?.singleOrNull()?.numero)
        }
    }

    private companion object {
        const val ESPERA_MS = 5_000L
        const val AVISO = "Sin conexión. Ves lo que guardamos en el teléfono."
    }
}
