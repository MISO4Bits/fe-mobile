package com.solventa4bits.movil

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnySibling
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.solventa4bits.movil.fixtures.BffFalso
import com.solventa4bits.movil.fixtures.Personas
import com.solventa4bits.movil.fixtures.RespuestasBff
import com.solventa4bits.movil.fixtures.SesionLimpia
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Declinar las condiciones de la cotizacion de extremo a extremo (BITS-337, CP-M). */
@RunWith(AndroidJUnit4::class)
class DeclinarCotizacionTest {

    private val almacen = almacenDelDispositivo()

    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val bff = BffFalso()

    @get:Rule
    val sesionLimpia = SesionLimpia(almacen)

    @Test
    fun alVolverDelModalLaCotizacionQuedaSinCambios() {
        llegarALoQuePagas()

        // AC-1: el modal explica que declinar elimina la cotizacion.
        composeRule.onNodeWithText(DECLINAR).performClick()
        composeRule.esperarTexto(TITULO_DEL_MODAL)
        composeRule.onNodeWithText("Al declinarlas rechazas esta cotización", substring = true).assertIsDisplayed()

        // AC-2: "Volver" cierra el modal sin llamar al servidor.
        composeRule.onNodeWithText("Volver").performClick()
        composeRule.esperarQueDesaparezca(TITULO_DEL_MODAL)
        composeRule.onNodeWithText("Esto es lo que pagas").assertIsDisplayed()
        assertFalse(bff.recibidas.contains(DELETE_COTIZACION))
    }

    @Test
    fun alDeclinarSeEliminaLaCotizacionYVuelveAlInicio() {
        bff.responderSiempreEn(RUTA_COTIZACION, 204, metodo = "DELETE")
        llegarALoQuePagas()

        declinarDesdeElModal()

        // AC-3: el Inicio ya no tiene cotizacion y deja cotizar de nuevo.
        composeRule.esperarTexto("Cotizar mi seguro")
        composeRule.onAllNodesWithText("Tienes una cotización en curso").assertCountEquals(0)
        assertTrue(bff.recibidas.contains(DELETE_COTIZACION))
    }

    @Test
    fun siFallaElBorradoLoDiceYElModalSigueAbierto() {
        bff.responderSiempreEn(RUTA_COTIZACION, 503, metodo = "DELETE")
        llegarALoQuePagas()

        declinarDesdeElModal()

        composeRule.esperarTexto("No pudimos declinar tu cotización. Inténtalo de nuevo.")
        composeRule.onNodeWithText(TITULO_DEL_MODAL).assertIsDisplayed()
    }

    private fun llegarALoQuePagas() {
        bff.responder(200, RespuestasBff.sesionValida())
        bff.responder(200, RespuestasBff.creditosDe(Personas.DANIEL))
        bff.responder(201, RespuestasBff.cotizacion())
        composeRule.abrirApp(bff, almacen)
        composeRule.iniciarSesion()

        composeRule.onNodeWithText("Cotizar mi seguro").performClick()
        composeRule.elegirBanco("Bancolombia")
        composeRule.esperarTexto("$180.000.000")
        composeRule.onNodeWithText("Ver mi cotización").performClick()
        composeRule.continuarDesdeCoberturas()
        composeRule.esperarTexto("Esto es lo que pagas")
    }

    // Con el modal abierto hay dos "Declinar estas condiciones": el de la
    // pantalla y el del modal, que es el que esta junto a "Volver".
    private fun declinarDesdeElModal() {
        composeRule.onNodeWithText(DECLINAR).performClick()
        composeRule.esperarTexto(TITULO_DEL_MODAL)
        composeRule.onNode(hasText(DECLINAR) and hasAnySibling(hasText("Volver"))).performClick()
    }

    private companion object {
        const val DECLINAR = "Declinar estas condiciones"
        const val TITULO_DEL_MODAL = "¿Quieres declinar estas condiciones?"
        const val RUTA_COTIZACION = "/v1/cotizaciones/actual"
        const val DELETE_COTIZACION = "DELETE $RUTA_COTIZACION"
    }
}
