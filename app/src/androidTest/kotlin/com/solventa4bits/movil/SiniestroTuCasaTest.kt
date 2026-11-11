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

/** Reportar un incendio o terremoto desde "Mi seguro" (BITS-111, camino de Tu casa; M04 a M08). */
@RunWith(AndroidJUnit4::class)
class SiniestroTuCasaTest {

    private val almacen = almacenDelDispositivo()

    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val bff = BffFalso()

    @get:Rule
    val sesionLimpia = SesionLimpia(almacen)

    @Test
    fun reportaUnIncendioConFotosLoQuePasoYLaCuenta() {
        abrirMiSeguro()

        composeRule.onNodeWithText("Reportar incendio o terremoto").performScrollTo().performClick()
        composeRule.esperarTexto("Fotos del daño")
        composeRule.onNodeWithText("Continuar").assertIsNotEnabled()

        // AC-5 (M04A): sin el permiso de ubicacion se explica para que es, y se puede seguir sin el.
        composeRule.onNodeWithText("Tomar una foto").performClick()
        composeRule.esperarTexto("¿Nos dejas usar tu ubicación?")
        composeRule.onNodeWithText("Seguir sin ubicación").performClick()
        composeRule.usarFotoDePrueba()
        composeRule.esperarTexto("Foto 1")
        composeRule.onNodeWithText("Sin ubicación", substring = true).assertIsDisplayed()
        // Ya decidio: la segunda foto no vuelve a preguntar.
        composeRule.onNodeWithText("Tomar otra foto").performClick()
        composeRule.usarFotoDePrueba()
        composeRule.esperarTexto("Foto 2")
        composeRule.onNodeWithText("Continuar").performClick()

        // AC-2 y lo que paso, obligatorio en Tu casa.
        composeRule.esperarTexto("Cuéntanos qué pasó")
        composeRule.onNodeWithText("Continuar").assertIsNotEnabled()
        composeRule.onNodeWithText("Qué pasó").performTextInput("Se prendió la estufa de la cocina.")
        composeRule.onNodeWithText("Continuar").performClick()

        // BITS-116, AC-2 y AC-3: a quien se le paga y el banco de la lista.
        composeRule.esperarTexto("¿Cómo quieres recibir tu pago?")
        composeRule.onNodeWithText("Transferencia a tu cuenta").assertIsDisplayed()
        composeRule.onNodeWithText("Banco").performClick()
        composeRule.onNodeWithText("Banco de Bogotá").performClick()
        composeRule.onNodeWithText("Ahorros").performClick()
        composeRule.onNodeWithText("Número de cuenta").performTextInput("48210012345")
        composeRule.onNodeWithText("Continuar").assertIsEnabled().performClick()

        // AC-6: lo que se va a enviar.
        composeRule.esperarTexto("Revisa y envía")
        composeRule.onNodeWithText("Incendio o terremoto").assertIsDisplayed()
        composeRule.onNodeWithText("Transferencia, cuenta terminada en 2345").assertIsDisplayed()
        composeRule.onNodeWithText("Se prendió la estufa de la cocina.").assertIsDisplayed()
        composeRule.onNodeWithText("Foto 1, sin ubicación").assertIsDisplayed()
        composeRule.onNodeWithText("Foto 2, sin ubicación").assertIsDisplayed()
        radicar()
        composeRule.onNodeWithText("Enviar").performScrollTo().performClick()

        // AC-7
        composeRule.esperarTexto("Tu número de radicado es SIN-2026-00042")
    }

    @Test
    fun sinTuCasaNoSeOfreceReportarIncendio() {
        abrirMiSeguro(conTuCasa = false)

        composeRule.onNodeWithText("Reportar incapacidad total").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Reportar incendio o terremoto").assertDoesNotExist()
    }

    @Test
    fun conUnoDeTuCasaEnCursoSoloSeOfreceVida() {
        abrirMiSeguro(siniestros = "[${siniestroDeTuCasa("FALTA_INFORMACION")}]")

        // AC-2A: Vida y Tu casa son independientes.
        composeRule.esperarTexto("Radicado SIN-2026-00031")
        composeRule.onNodeWithText("Falta información").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Reportar incendio o terremoto").assertDoesNotExist()
        composeRule.onNodeWithText("Reportar incapacidad total").performScrollTo().assertIsDisplayed()
    }

    private fun abrirMiSeguro(conTuCasa: Boolean = true, siniestros: String = "[]") {
        bff.responder(200, RespuestasBff.sesionValida())
        composeRule.abrirApp(bff, almacen)
        bff.responderSiempreEn(
            RUTA_POLIZAS,
            200,
            "[${polizaEnviada("EMITIDA", conTuCasa = conTuCasa)}]",
            metodo = "GET",
        )
        bff.responderSiempreEn(RUTA_SINIESTROS, 200, siniestros, metodo = "GET")
        bff.responderSiempreEn("/v1/creditos-hipotecarios", 200, RespuestasBff.creditosDe(Personas.DANIEL))
        composeRule.iniciarSesion()
        composeRule.esperarTexto("Tus seguros")
        composeRule.onNodeWithText("SOL-VH-2026-K7M2Q9").performScrollTo().performClick()
        composeRule.esperarTexto("VIDA PARA TU CRÉDITO")
    }

    private fun radicar() {
        bff.responderSiempreEn(
            RUTA_SINIESTROS,
            201,
            """{"id": "$ID_DE_SINIESTRO", "numero": "SIN-2026-00042", "estado": "EN_ESTUDIO"}""",
            metodo = "POST",
        )
        bff.responderSiempreEn("$RUTA_SINIESTROS/$ID_DE_SINIESTRO", 404, metodo = "GET")
    }

    private companion object {
        const val RUTA_SINIESTROS = "/v1/cuenta/siniestros"
        const val ID_DE_SINIESTRO = "5a1c9d2e-8b3f-4c6a-9d70-1e2f3a4b5c6d"

        fun siniestroDeTuCasa(estado: String) = """{"id": "$ID_DE_SINIESTRO", "numero": "SIN-2026-00031",
            "polizaId": "$ID_DE_POLIZA", "tipo": "INCENDIO_TERREMOTO", "automatico": false,
            "estado": "$estado", "reportadoEn": "2026-10-20T14:12:00Z", "actualizadoEn": "2026-10-21T14:12:00Z"}"""
    }
}
