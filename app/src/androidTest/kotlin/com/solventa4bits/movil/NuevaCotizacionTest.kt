package com.solventa4bits.movil

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.solventa4bits.movil.fixtures.BffFalso
import com.solventa4bits.movil.fixtures.Personas
import com.solventa4bits.movil.fixtures.RespuestasBff
import com.solventa4bits.movil.fixtures.SesionLimpia
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Nueva cotizacion de extremo a extremo (BITS-219): las dos corridas que pide
 * la integracion con Open Finance, la exitosa y la degradada.
 */
@RunWith(AndroidJUnit4::class)
class NuevaCotizacionTest {

    private val almacen = almacenDelDispositivo()

    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val bff = BffFalso()

    @get:Rule
    val sesionLimpia = SesionLimpia(almacen)

    @Test
    fun siElBancoReportaLaHipotecaMuestraSusDatos() {
        bff.responder(200, RespuestasBff.sesionValida())
        bff.responder(200, RespuestasBff.creditosDe(Personas.DANIEL))
        composeRule.abrirApp(bff, almacen)
        composeRule.iniciarSesion()

        composeRule.onNodeWithText("Cotizar mi seguro").performClick()
        elegirBanco("Bancolombia")

        composeRule.esperarTexto("$180.000.000")
        composeRule.onNodeWithText("$98.000.000").assertIsDisplayed()
        composeRule.onNodeWithText("96 meses").assertIsDisplayed()
        composeRule.onNodeWithText("Ver mi cotización").assertIsEnabled()
    }

    @Test
    fun siNoHayDatosDelBancoElClienteLosEscribe() {
        bff.responder(200, RespuestasBff.sesionValida())
        bff.responder(200, RespuestasBff.creditosDe(Personas.NICOLAS))
        composeRule.abrirApp(bff, almacen)
        composeRule.iniciarSesion()

        composeRule.onNodeWithText("Cotizar mi seguro").performClick()
        elegirBanco("Davivienda")

        composeRule.esperarTexto("Los datos los declaras tú")
        composeRule.onNodeWithText("Ver mi cotización").assertIsNotEnabled()

        escribirEn("Monto total del crédito", "200000000")
        escribirEn("¿Cuánto debes hoy?", "150000000")
        escribirEn("Meses que te faltan", "120")

        composeRule.onNodeWithText("Ver mi cotización").assertIsEnabled()
    }

    private fun elegirBanco(nombre: String) {
        composeRule.esperarTexto("Banco o entidad")
        composeRule.onNodeWithText("Banco o entidad").performClick()
        composeRule.esperarTexto(nombre)
        composeRule.onNodeWithText(nombre).performClick()
    }

    private fun escribirEn(campo: String, texto: String) {
        composeRule.onNodeWithText(campo).performScrollTo().performTextInput(texto)
    }
}
