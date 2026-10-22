package com.solventa4bits.movil

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
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
 * Cotizacion de extremo a extremo (BITS-95, CP-M): cotizar y ver lo que se
 * paga, con perfil y sin perfil, y retomar desde el Home la cotizacion en curso.
 */
@RunWith(AndroidJUnit4::class)
class CotizacionTest {

    private val almacen = almacenDelDispositivo()

    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val bff = BffFalso()

    @get:Rule
    val sesionLimpia = SesionLimpia(almacen)

    @Test
    fun conPerfilMuestraElPrecioYPorQueEsePrecio() {
        cotizarConLosDatosDelBanco(RespuestasBff.cotizacion(conPerfil = true))

        composeRule.esperarTexto("Esto es lo que pagas")
        composeRule.onNodeWithText("$39.734").assertIsDisplayed()

        composeRule.onNodeWithText("¿Por qué este precio?").performClick()
        composeRule.esperarTexto("Historial de pagos excelente")
        composeRule.onNodeWithText("Pagas $39.734 al mes. La tarifa estándar para tu crédito sería $46.746.")
            .assertIsDisplayed()
        // El de mayor peso va primero y se senala.
        composeRule.onNodeWithText("Baja tu precio. Es lo que más pesa.").assertIsDisplayed()
    }

    @Test
    fun conPerfilNeutroExplicaQueElHistorialNoCambiaElPrecio() {
        cotizarConLosDatosDelBanco(RespuestasBff.cotizacion(perfilNeutro = true))

        composeRule.esperarTexto("Esto es lo que pagas")
        composeRule.onNodeWithText("¿Por qué este precio?").performClick()

        composeRule.esperarTexto("Tu historial no cambia tu precio")
        composeRule.onNodeWithText("Pagas $46.746 al mes. Es la tarifa estándar para tu crédito.").assertIsDisplayed()
    }

    @Test
    fun sinPerfilMuestraLaTarifaEstandarSinHablarDelPerfil() {
        cotizarConLosDatosDelBanco(RespuestasBff.cotizacion(conPerfil = false))

        composeRule.esperarTexto("Esto es lo que pagas")
        composeRule.onNodeWithText("$46.746").assertIsDisplayed()
        composeRule.onNodeWithText("Esta cotización es estimada").assertIsDisplayed()
        composeRule.onAllNodesWithText("¿Por qué este precio?").assertCountEquals(0)
        // BITS-333, AC-1: con datos declarados ya no se pide buscar el credito.
        composeRule.onNodeWithText("Aceptar y continuar").assertIsDisplayed()
        composeRule.onAllNodesWithText("Buscar mi crédito").assertCountEquals(0)
    }

    @Test
    fun sinAutorizacionFinancieraDiceQueUsaLaTarifaEstandar() {
        bff.responder(200, RespuestasBff.sesionValida())
        bff.responder(200, RespuestasBff.creditosDe(Personas.DANIEL, sinConsentimiento = true))
        bff.responder(201, RespuestasBff.cotizacion(conPerfil = false))
        composeRule.abrirApp(bff, almacen)
        composeRule.iniciarSesion()

        composeRule.onNodeWithText("Cotizar mi seguro").performClick()
        composeRule.elegirBanco("Davivienda")
        escribirEn("Monto total del crédito", "250000000")
        escribirEn("¿Cuánto debes hoy?", "238500000")
        escribirEn("Meses que te faltan", "180")
        composeRule.onNodeWithText("Ver mi cotización").performClick()
        composeRule.continuarDesdeCoberturas()

        composeRule.esperarTexto("Esta cotización usa la tarifa estándar")
        // BITS-333, AC-1: sin autorizacion tambien se acepta y continua.
        composeRule.onNodeWithText("Aceptar y continuar").assertIsDisplayed()
        composeRule.onAllNodesWithText("Estamos analizando tu perfil").assertCountEquals(0)
    }

    @Test
    fun conUnaCotizacionEnCursoElHomeLaMuestraYSeRetoma() {
        bff.responder(200, RespuestasBff.sesionValida())
        composeRule.abrirApp(bff, almacen)
        bff.responderSiempreEn("/v1/cotizaciones/actual", 200, RespuestasBff.cotizacion())
        composeRule.iniciarSesion()

        composeRule.esperarTexto("Tienes una cotización en curso")
        composeRule.onNodeWithText("Ver mi cotización").performClick()
        composeRule.continuarDesdeCoberturas()

        composeRule.esperarTexto("Esto es lo que pagas")
        composeRule.onNodeWithText("$39.734").assertIsDisplayed()
    }

    private fun escribirEn(campo: String, texto: String) {
        composeRule.onNodeWithText(campo).performScrollTo().performTextInput(texto)
    }

    private fun cotizarConLosDatosDelBanco(cotizacion: String) {
        bff.responder(200, RespuestasBff.sesionValida())
        bff.responder(200, RespuestasBff.creditosDe(Personas.DANIEL))
        bff.responder(201, cotizacion)
        composeRule.abrirApp(bff, almacen)
        composeRule.iniciarSesion()

        composeRule.onNodeWithText("Cotizar mi seguro").performClick()
        composeRule.elegirBanco("Bancolombia")
        composeRule.esperarTexto("$180.000.000")
        composeRule.onNodeWithText("Ver mi cotización").performClick()
        composeRule.continuarDesdeCoberturas()
    }
}
