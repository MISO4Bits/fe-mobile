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
    fun siElServidorNoRespondeSeVeLaPolizaGuardada() {
        llegarAlInicioConUnaPoliza()
        esperarGuardado { it == "SOL-VH-2026-K7M2Q9" }

        bff.responderSiempreEn("/v1/cotizaciones/actual", 503)
        bff.responderSiempreEn(RUTA_POLIZAS, 503, metodo = "GET")
        composeRule.onNodeWithText("SOL-VH-2026-K7M2Q9").performScrollTo().performClick()
        composeRule.esperarTexto("Detalle de tu seguro")
        composeRule.onNodeWithText("Inicio").performClick()

        composeRule.esperarTexto("Tus seguros")
        composeRule.onNodeWithText("SOL-VH-2026-K7M2Q9").performScrollTo().assertIsDisplayed()
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
    }
}
