package com.solventa4bits.movil

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.solventa4bits.movil.fixtures.BffFalso
import com.solventa4bits.movil.fixtures.SesionLimpia
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Revisar los requisitos y enviar la solicitud de extremo a extremo
 * (BITS-278, CP-M y CP-E). Lo que viaja en el envio lo cubren las pruebas
 * unitarias del adaptador.
 */
@RunWith(AndroidJUnit4::class)
class SolicitudTest {

    private val almacen = almacenDelDispositivo()

    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val bff = BffFalso()

    @get:Rule
    val sesionLimpia = SesionLimpia(almacen)

    @Test
    fun muestraLosRequisitosYSinAceptarNoDejaEnviar() {
        composeRule.llegarATuSolicitud(bff, almacen)

        // AC-1: el estado de cada requisito.
        composeRule.onNodeWithText("Frente de tu cédula").assertIsDisplayed()
        composeRule.onNodeWithText("Reverso de tu cédula").assertIsDisplayed()
        composeRule.onNodeWithText("Tu selfie").assertIsDisplayed()
        composeRule.onAllNodesWithText("Se ve bien").assertCountEquals(3)
        composeRule.onNodeWithText("Bancolombia", substring = true).assertIsDisplayed()
        // AC-2: mientras no las acepte, el boton sigue inactivo.
        composeRule.onNodeWithText(ENVIAR).assertIsNotEnabled()
    }

    @Test
    fun aceptarDesdeLasCondicionesMarcaLaCasillaYDejaEnviar() {
        composeRule.llegarATuSolicitud(bff, almacen)

        aceptarDesdeLaHoja()

        composeRule.onNode(isToggleable()).assertIsOn()
        composeRule.onNodeWithText(ENVIAR).assertIsEnabled()
    }

    @Test
    fun alEnviarQuedaRegistradaYMuestraSuResultado() {
        bff.responderSiempreEn(RUTA_POLIZAS, 201, polizaEnviada(), metodo = "POST")
        composeRule.llegarATuSolicitud(bff, almacen)
        aceptarDesdeLaHoja()

        composeRule.onNodeWithText(ENVIAR).performClick()

        composeRule.esperarTexto("Revisamos tu solicitud")
        assertTrue(bff.recibidas.contains("POST $RUTA_POLIZAS"))
    }

    @Test
    fun siFallaElEnvioLoDiceYNoSePierdeLoMarcado() {
        bff.responderSiempreEn(RUTA_POLIZAS, 503, metodo = "POST")
        composeRule.llegarATuSolicitud(bff, almacen)
        aceptarDesdeLaHoja()

        composeRule.onNodeWithText(ENVIAR).performClick()

        // CP-E: se puede enviar otra vez sin volver a aceptar.
        composeRule.esperarTexto("No pudimos enviar tu solicitud. Inténtalo de nuevo.")
        composeRule.onNode(isToggleable()).assertIsOn()
        composeRule.onNodeWithText(ENVIAR).assertIsEnabled()
    }

    @Test
    fun siLaCotizacionVencioPideCotizarDeNuevo() {
        bff.responderSiempreEn(
            RUTA_POLIZAS,
            409,
            """{"title": "Conflicto", "status": 409, "codigo": "COTIZACION_VENCIDA"}""",
            metodo = "POST",
        )
        composeRule.llegarATuSolicitud(bff, almacen)
        aceptarDesdeLaHoja()

        composeRule.onNodeWithText(ENVIAR).performClick()

        composeRule.esperarTexto("Tu cotización cambió o venció. Cotiza de nuevo para continuar.")
        composeRule.onNodeWithText("Ir al inicio").performClick()
        composeRule.esperarTexto("Cotizar mi seguro")
    }

    @Test
    fun siNoLleganLasCondicionesLoDiceYDejaReintentar() {
        composeRule.llegarATuSolicitud(bff, almacen, condiciones = 503 to "")

        composeRule.esperarTexto("No pudimos cargar tu solicitud. Inténtalo de nuevo.")
        composeRule.onNodeWithText("Reintentar").assertIsDisplayed()
        composeRule.onNodeWithText(ENVIAR).assertIsNotEnabled()
    }

    private fun aceptarDesdeLaHoja() {
        composeRule.onNodeWithText("Leer las condiciones").performClick()
        composeRule.esperarTexto("Condiciones de tu seguro")
        composeRule.onNodeWithText("Qué estás aceptando", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Volver").performClick()
        composeRule.esperarQueDesaparezca("Acepto estas condiciones")
        composeRule.aceptarLasCondiciones()
    }

    private companion object {
        const val ENVIAR = "Enviar solicitud"
    }
}
