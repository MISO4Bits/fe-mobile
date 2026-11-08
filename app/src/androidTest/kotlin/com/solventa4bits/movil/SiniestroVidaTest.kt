package com.solventa4bits.movil

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
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

/** Reportar una incapacidad total desde "Mi seguro" (BITS-111, camino de Vida; M01 a M09V). */
@RunWith(AndroidJUnit4::class)
class SiniestroVidaTest {

    private val almacen = almacenDelDispositivo()

    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val bff = BffFalso()

    @get:Rule
    val sesionLimpia = SesionLimpia(almacen)

    @Test
    fun reportaUnaIncapacidadTotalYVeSuRadicado() {
        abrirMiSeguro()

        // AC-1: la fecha empieza en hoy y sin el dictamen no se sigue (AC-3).
        composeRule.onNodeWithText("Reportar incapacidad total").performScrollTo().performClick()
        composeRule.esperarTexto("Tu dictamen")
        composeRule.onNodeWithText("Es hoy. Cámbiala si ocurrió otro día.").assertIsDisplayed()
        composeRule.onNodeWithText("Continuar").assertIsNotEnabled()

        // Cada foto de la camara es una pagina del dictamen.
        composeRule.onNodeWithText("Escanear con la cámara").performClick()
        composeRule.usarFotoDePrueba()
        composeRule.esperarTexto("Dictamen, 1 página")
        composeRule.onNodeWithText("Agregar otra página").performClick()
        composeRule.usarFotoDePrueba()
        composeRule.esperarTexto("Dictamen, 2 páginas")
        composeRule.onNodeWithText("Continuar").performClick()

        // AC-6: lo que se va a enviar, con el pago al banco.
        composeRule.esperarTexto("Revisa y envía")
        composeRule.onNodeWithText("Incapacidad total").assertIsDisplayed()
        composeRule.onNodeWithText("A tu banco, abono al saldo de tu crédito").assertIsDisplayed()
        composeRule.onNodeWithText("Dictamen, 2 páginas").assertIsDisplayed()
        radicar()
        composeRule.onNodeWithText("Enviar").performClick()

        // AC-7: queda En estudio con su numero de radicado.
        composeRule.esperarTexto("Tu número de radicado es SIN-2026-00058")
        esperarTextoQueContenga("Te respondemos a más tardar el 8 dic 2026.")
        composeRule.onNodeWithText("En estudio").assertIsDisplayed()

        composeRule.onNodeWithText("Ver mi siniestro").performClick()
        composeRule.esperarTexto("Estamos revisando tu siniestro SIN-2026-00058.")
        composeRule.onNodeWithText("Documentos, 2 páginas").assertIsDisplayed()
        composeRule.onNodeWithText("Abono al saldo de tu crédito").assertIsDisplayed()
    }

    @Test
    fun siSeCaeLaConexionAlEnviarLoDiceYAlReintentarQuedaRadicado() {
        abrirMiSeguro()
        llegarARevisaYEnvia()
        bff.responderSiempreEn(RUTA_SINIESTROS, 503, metodo = "POST")

        composeRule.onNodeWithText("Enviar").performClick()

        composeRule.esperarTexto("No pudimos enviar tu reporte. Revisa tu conexión e inténtalo otra vez.")
        radicar()
        composeRule.onNodeWithText("Enviar").performClick()
        composeRule.esperarTexto("Tu número de radicado es SIN-2026-00058")
        assertEquals(2, bff.recibidas.count { it == "POST $RUTA_SINIESTROS" })
    }

    @Test
    fun conUnoEnEstudioNoSeOfreceReportarOtroYSeVeSuEstado() {
        abrirMiSeguro(siniestros = "[${siniestro("EN_ESTUDIO")}]")

        // AC-2A: la entrada de Vida no aparece mientras haya uno en curso.
        composeRule.esperarTexto("Radicado SIN-2026-00058")
        composeRule.onNodeWithText("Reportar incapacidad total").assertDoesNotExist()
        composeRule.onNodeWithText("En estudio").performScrollTo().assertIsDisplayed()

        bff.responderSiempreEn("$RUTA_SINIESTROS/$ID_DE_SINIESTRO", 200, detalle(), metodo = "GET")
        composeRule.onNodeWithText("Radicado SIN-2026-00058").performScrollTo().performClick()
        composeRule.esperarTexto("Estamos revisando tu siniestro SIN-2026-00058.")
    }

    @Test
    fun cuandoSePagaLaEntradaVuelve() {
        abrirMiSeguro(siniestros = "[${siniestro("PAGADO")}]")

        composeRule.esperarTexto("Radicado SIN-2026-00058")
        composeRule.onNodeWithText("Pagado").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Reportar incapacidad total").performScrollTo().assertIsDisplayed()
    }

    private fun abrirMiSeguro(siniestros: String = "[]") {
        bff.responder(200, RespuestasBff.sesionValida())
        composeRule.abrirApp(bff, almacen)
        bff.responderSiempreEn(RUTA_POLIZAS, 200, "[${polizaEnviada("EMITIDA")}]", metodo = "GET")
        bff.responderSiempreEn(RUTA_SINIESTROS, 200, siniestros, metodo = "GET")
        composeRule.iniciarSesion()
        composeRule.esperarTexto("Tus seguros")
        composeRule.onNodeWithText("SOL-VH-2026-K7M2Q9").performScrollTo().performClick()
        composeRule.esperarTexto("VIDA PARA TU CRÉDITO")
    }

    private fun llegarARevisaYEnvia() {
        composeRule.onNodeWithText("Reportar incapacidad total").performScrollTo().performClick()
        composeRule.esperarTexto("Tu dictamen")
        composeRule.onNodeWithText("Escanear con la cámara").performClick()
        composeRule.usarFotoDePrueba()
        composeRule.esperarTexto("Dictamen, 1 página")
        composeRule.onNodeWithText("Continuar").performClick()
        composeRule.esperarTexto("Revisa y envía")
    }

    /** El BFF radica el reporte y responde su detalle. */
    private fun radicar() {
        bff.responderSiempreEn(
            RUTA_SINIESTROS,
            201,
            """{"id": "$ID_DE_SINIESTRO", "numero": "SIN-2026-00058", "estado": "EN_ESTUDIO"}""",
            metodo = "POST",
        )
        bff.responderSiempreEn("$RUTA_SINIESTROS/$ID_DE_SINIESTRO", 200, detalle(), metodo = "GET")
    }

    private fun esperarTextoQueContenga(texto: String) {
        composeRule.waitUntil(ESPERA_MS) {
            composeRule.onAllNodesWithText(texto, substring = true).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private companion object {
        const val RUTA_SINIESTROS = "/v1/cuenta/siniestros"
        const val ID_DE_SINIESTRO = "3f2b8c1e-7d4a-4e5b-9a61-2c8d5f0b7e14"
        const val ESPERA_MS = 5_000L

        fun siniestro(estado: String) = """{"id": "$ID_DE_SINIESTRO", "numero": "SIN-2026-00058",
            "polizaId": "$ID_DE_POLIZA", "tipo": "INCAPACIDAD_TOTAL", "automatico": false,
            "estado": "$estado", "reportadoEn": "2026-11-02T14:12:00Z", "actualizadoEn": "2026-11-02T14:12:00Z"}"""

        fun detalle() = """{"id": "$ID_DE_SINIESTRO", "numero": "SIN-2026-00058", "polizaId": "$ID_DE_POLIZA",
            "tipo": "INCAPACIDAD_TOTAL", "automatico": false, "estado": "EN_ESTUDIO",
            "reportadoEn": "2026-11-08T14:12:00Z", "actualizadoEn": "2026-11-08T14:12:00Z",
            "fechaOcurrencia": "2026-11-08", "narracion": null, "ultimaEntregaEn": "2026-11-08T14:12:00Z",
            "fechaLimite": "2026-12-08T14:12:00Z", "evidenciaPedida": null, "motivoRechazo": null,
            "pago": null, "evento": null,
            "archivos": [
              {"id": "a1", "clase": "DOCUMENTO", "entregadoEn": "2026-11-08T14:12:00Z"},
              {"id": "a2", "clase": "DOCUMENTO", "entregadoEn": "2026-11-08T14:12:00Z"}
            ]}"""
    }
}
