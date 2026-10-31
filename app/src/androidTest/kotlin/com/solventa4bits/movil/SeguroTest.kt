package com.solventa4bits.movil

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.solventa4bits.movil.fixtures.BffFalso
import com.solventa4bits.movil.fixtures.RespuestasBff
import com.solventa4bits.movil.fixtures.SesionLimpia
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** "Mis seguros" en el Inicio (Figma 10) y el detalle del seguro con su documento (Figma 11), BITS-104. */
@RunWith(AndroidJUnit4::class)
class SeguroTest {

    private val almacen = almacenDelDispositivo()
    private val enlacesAbiertos = mutableListOf<String>()

    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val bff = BffFalso()

    @get:Rule
    val sesionLimpia = SesionLimpia(almacen)

    @Test
    fun conUnaPolizaEmitidaElInicioLaMuestraYNoLaCotizacion() {
        llegarAlInicio()

        composeRule.onNodeWithText("Vigente").assertIsDisplayed()
        composeRule.onNodeWithText("Vida para tu crédito").assertIsDisplayed()
        composeRule.onNodeWithText("SOL-VH-2026-K7M2Q9").assertIsDisplayed()
        composeRule.onNodeWithText("Bancolombia").assertIsDisplayed()
        // AC-3: la cotizacion con la que se emitio ya no se ofrece.
        composeRule.onNodeWithText("Tu cotización está lista").assertDoesNotExist()
    }

    @Test
    fun elDetalleMuestraLasCondicionesYElCredito() {
        abrirElDetalle()

        composeRule.onNodeWithText("Seguro N.º SOL-VH-2026-K7M2Q9 · Vigente").assertIsDisplayed()
        composeRule.onNodeWithText("$39.734").assertIsDisplayed()
        composeRule.onNodeWithText("28 oct 2026 al 28 oct 2027").assertIsDisplayed()
        composeRule.onNodeWithText("Bancolombia").assertIsDisplayed()
        composeRule.onNodeWithText("$238.500.000").assertIsDisplayed()
        composeRule.onNodeWithText("180 meses").assertIsDisplayed()

        composeRule.onNodeWithText("Inicio").performClick()
        composeRule.esperarTexto("Tus seguros")
    }

    @Test
    fun conElDocumentoListoAbreSuEnlace() {
        abrirElDetalle()
        bff.responderSiempreEn(RUTA_DOCUMENTO, 200, """{"estado": "LISTO", "url": "$URL_DEL_PDF"}""")

        composeRule.onNodeWithText("Ver documento").performClick()

        composeRule.waitUntil(ESPERA_MS) { enlacesAbiertos.isNotEmpty() }
        assertEquals(listOf(URL_DEL_PDF), enlacesAbiertos)
    }

    @Test
    fun siElDocumentoSeEstaGenerandoPideEsperar() {
        abrirElDetalle()
        bff.responderSiempreEn(RUTA_DOCUMENTO, 202, """{"estado": "GENERANDO", "url": null}""")

        composeRule.onNodeWithText("Ver documento").performClick()

        composeRule.esperarTexto("Estamos preparando tu documento. Inténtalo en un momento.")
        assertEquals(emptyList<String>(), enlacesAbiertos)
    }

    @Test
    fun siElDocumentoFallaLoDice() {
        abrirElDetalle()
        bff.responderSiempreEn(RUTA_DOCUMENTO, 503)

        composeRule.onNodeWithText("Ver documento").performClick()

        composeRule.esperarTexto("No pudimos abrir tu documento. Inténtalo de nuevo.")
    }

    private fun llegarAlInicio() {
        bff.responder(200, RespuestasBff.sesionValida())
        composeRule.abrirApp(bff, almacen, abrirEnlace = { enlacesAbiertos += it })
        bff.responderSiempreEn("/v1/cotizaciones/actual", 200, RespuestasBff.cotizacion())
        bff.responderSiempreEn(RUTA_POLIZAS, 200, "[${polizaEnviada("EMITIDA")}]", metodo = "GET")
        composeRule.iniciarSesion()
        composeRule.esperarTexto("Tus seguros")
    }

    private fun abrirElDetalle() {
        llegarAlInicio()
        composeRule.onNodeWithText("SOL-VH-2026-K7M2Q9").performScrollTo().performClick()
        composeRule.esperarTexto("Detalle de tu seguro")
    }

    private companion object {
        const val RUTA_DOCUMENTO = "$RUTA_POLIZAS/$ID_DE_POLIZA/documento"
        const val URL_DEL_PDF = "https://docs.solventa.test/poliza.pdf"
        const val ESPERA_MS = 5_000L
    }
}
