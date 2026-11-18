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
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Aceptar o rechazar la renovacion con el aumento por IPC (BITS-237; M00R, M10, M10A, M10B y M11). */
@RunWith(AndroidJUnit4::class)
class RenovacionDelSeguroTest {

    private val almacen = almacenDelDispositivo()

    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val bff = BffFalso()

    @get:Rule
    val sesionLimpia = SesionLimpia(almacen)

    @Test
    fun noRenuevaYPuedeCambiarDeOpinion() {
        entrar(conRenovacion("EMITIDA", "SIN_RESPUESTA"))

        // AC-2: la etiqueta en Mis seguros y el modal al abrir el detalle.
        composeRule.onNodeWithText("Por renovar").assertIsDisplayed()
        composeRule.onNodeWithText("SOL-VH-2026-K7M2Q9").performScrollTo().performClick()
        composeRule.esperarTexto("Tu póliza se renueva el 17 dic 2099")
        composeRule.onNodeWithText("$81.397 al mes").assertIsDisplayed()

        // AC-4: no renovar pide confirmar.
        composeRule.onNodeWithText("No renovar").performClick()
        composeRule.esperarTexto("¿Seguro que no quieres renovar?")
        bff.responderSiempreEn("$RUTA_POLIZAS/$ID_DE_POLIZA/renovacion", 204, metodo = "POST")
        bff.responderSiempreEn(
            "$RUTA_POLIZAS/$ID_DE_POLIZA",
            200,
            conRenovacion("EMITIDA", "RECHAZADA"),
            metodo = "GET",
        )
        composeRule.onNodeWithText("No renovar").performClick()

        // AC-5: no se renueva, cuando termina y el enlace para cambiar de opinion.
        composeRule.esperarTexto("Quiero renovarla")
        composeRule.onNodeWithText("Seguro N.º SOL-VH-2026-K7M2Q9 · No se renueva").assertIsDisplayed()
        composeRule.onNodeWithText("17 dic 2099").performScrollTo().assertIsDisplayed()
        assertTrue(bff.recibidas.contains("POST $RUTA_POLIZAS/$ID_DE_POLIZA/renovacion"))
    }

    @Test
    fun desactivadaOfreceCotizarDeNuevo() {
        entrar(conRenovacion("DESACTIVADA", "RECHAZADA", avisoBanco = """{"registradoEn": "2099-11-17T15:00:00Z"}"""))

        // AC-1 de BITS-307 y AC-6: en Mis seguros y en su detalle, desactivada.
        composeRule.onNodeWithText("Desactivada").assertIsDisplayed()
        composeRule.onNodeWithText("SOL-VH-2026-K7M2Q9").performScrollTo().performClick()
        composeRule.esperarTexto("Seguro N.º SOL-VH-2026-K7M2Q9 · Desactivada")
        composeRule.onNodeWithText("Estuvo vigente").assertIsDisplayed()
        composeRule.onNodeWithText("Bancolombia, el 17 nov 2099").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Cotizar un seguro nuevo").performClick()
        composeRule.esperarTexto("Banco o entidad")
    }

    private fun entrar(poliza: String) {
        bff.responder(200, RespuestasBff.sesionValida())
        composeRule.abrirApp(bff, almacen)
        bff.responderSiempreEn(RUTA_POLIZAS, 200, "[$poliza]", metodo = "GET")
        composeRule.iniciarSesion()
        composeRule.esperarTexto("Tus seguros")
    }

    private fun conRenovacion(estado: String, renovacion: String, avisoBanco: String = "null") =
        polizaEnviada("EMITIDA", conTuCasa = true)
            .replace("\"estado\": \"EMITIDA\"", "\"estado\": \"$estado\"")
            .removeSuffix("}") +
            """, "renovacion": {"estado": "$renovacion", "primaActual": 77374, "primaNueva": 81397,
                "ipc": 0.052, "fechaRenovacion": "2099-12-17T15:00:00Z", "decidirHasta": "2099-11-17T15:00:00Z",
                "avisoBanco": $avisoBanco}}"""
}
