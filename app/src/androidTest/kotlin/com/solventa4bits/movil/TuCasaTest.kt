package com.solventa4bits.movil

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.solventa4bits.movil.fixtures.BffFalso
import com.solventa4bits.movil.fixtures.Personas
import com.solventa4bits.movil.fixtures.RespuestasBff
import com.solventa4bits.movil.fixtures.SesionLimpia
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * "Tu casa" de extremo a extremo (BITS-96, CP-M). La cotizacion de prueba trae
 * vida por $39.734 y "Tu casa" por $28.274 al mes ($23.760 mas $4.514 de IVA).
 */
@RunWith(AndroidJUnit4::class)
class TuCasaTest {

    private val almacen = almacenDelDispositivo()

    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val bff = BffFalso()

    @get:Rule
    val sesionLimpia = SesionLimpia(almacen)

    @Test
    fun conTuCasaElTotalCambiaAlInstanteYLoQuePagasLaDesglosa() {
        llegarATusCoberturas()

        // AC-1: la vida va siempre y "Tu casa" muestra hasta cuanto cubre.
        composeRule.onNodeWithText("Incendio y terremoto. Cubrimos hasta $240.000.000.").assertIsDisplayed()
        composeRule.onNodeWithText("$39.734").assertIsDisplayed()

        // AC-2: al marcarla, el total cambia sin llamar al servidor.
        composeRule.onNodeWithText("Tu casa").performClick()
        composeRule.esperarTexto("$68.008")

        // AC-3: la vida, "Tu casa", el IVA y el total por separado.
        composeRule.onNodeWithText("Continuar").performClick()
        composeRule.esperarTexto("Detalle")
        composeRule.onNodeWithText("$68.008").assertIsDisplayed()
        composeRule.onNodeWithText("$23.760/mes").assertIsDisplayed()
        composeRule.onNodeWithText("$63.494/mes").assertIsDisplayed()
        composeRule.onNodeWithText("$4.514/mes").assertIsDisplayed()
        composeRule.onNodeWithText("$68.008/mes").assertIsDisplayed()
    }

    @Test
    fun soloVidaNoLlevaIva() {
        llegarATusCoberturas()

        composeRule.onNodeWithText("Continuar").performClick()

        composeRule.esperarTexto("Detalle")
        composeRule.onNodeWithText("$0/mes").assertIsDisplayed()
        composeRule.onAllNodesWithText("Tu casa").assertCountEquals(0)
    }

    private fun llegarATusCoberturas() {
        bff.responder(200, RespuestasBff.sesionValida())
        bff.responder(200, RespuestasBff.creditosDe(Personas.DANIEL))
        bff.responder(201, RespuestasBff.cotizacion())
        composeRule.abrirApp(bff, almacen)
        composeRule.iniciarSesion()

        composeRule.onNodeWithText("Cotizar mi seguro").performClick()
        composeRule.elegirBanco("Bancolombia")
        composeRule.esperarTexto("$180.000.000")
        composeRule.onNodeWithText("Ver mi cotización").performClick()
        composeRule.esperarTexto("Elige tus coberturas")
    }
}
