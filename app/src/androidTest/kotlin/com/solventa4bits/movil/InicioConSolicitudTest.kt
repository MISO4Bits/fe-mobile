package com.solventa4bits.movil

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.solventa4bits.movil.fixtures.BffFalso
import com.solventa4bits.movil.fixtures.Personas
import com.solventa4bits.movil.fixtures.RespuestasBff
import com.solventa4bits.movil.fixtures.SesionLimpia
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** El Inicio con una solicitud en revision (Figma 02C) y el aviso antes de cotizar de nuevo (02D). */
@RunWith(AndroidJUnit4::class)
class InicioConSolicitudTest {

    private val almacen = almacenDelDispositivo()

    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val bff = BffFalso()

    @get:Rule
    val sesionLimpia = SesionLimpia(almacen)

    @Test
    fun laSolicitudEnRevisionReemplazaALaCotizacion() {
        llegarAlInicio()

        composeRule.onNodeWithText("En revisión").assertIsDisplayed()
        composeRule.onNodeWithText("$39.734 al mes.").assertIsDisplayed()
        composeRule.onNodeWithText("Revisa el enlace en tu correo").assertIsDisplayed()
        composeRule.onNodeWithText("Ver mi solicitud").assertIsDisplayed()
        composeRule.onNodeWithText("Tu cotización está lista").assertDoesNotExist()
    }

    @Test
    fun antesDeCotizarDeNuevoAvisaYVolverLoCierra() {
        llegarAlInicio()

        composeRule.onNodeWithText("Nueva cotización").performScrollTo().performClick()
        composeRule.esperarTexto("¿Quieres hacer una nueva cotización?")
        composeRule.onNodeWithText("Volver").performClick()

        composeRule.esperarQueDesaparezca("¿Quieres hacer una nueva cotización?")
        composeRule.onNodeWithText("Tu solicitud está en revisión").assertIsDisplayed()
    }

    @Test
    fun alCotizarDeNuevoEmpiezaLaCotizacion() {
        llegarAlInicio()
        bff.responderSiempreEn("/v1/creditos-hipotecarios", 200, RespuestasBff.creditosDe(Personas.DANIEL))

        composeRule.onNodeWithText("Nueva cotización").performScrollTo().performClick()
        composeRule.esperarTexto("¿Quieres hacer una nueva cotización?")
        composeRule.onNodeWithText("Cotizar de nuevo").performClick()

        composeRule.elegirBanco("Bancolombia")
        composeRule.esperarTexto("$180.000.000")
    }

    private fun llegarAlInicio() {
        bff.responder(200, RespuestasBff.sesionValida())
        composeRule.abrirApp(bff, almacen)
        bff.responderSiempreEn("/v1/cotizaciones/actual", 200, RespuestasBff.cotizacion())
        val pendiente = polizaEnviada(
            "PENDIENTE",
            pendientes = """[{"codigo": "CORREO_SIN_CONFIRMAR", "tipo": "USUARIO",
                "mensaje": "Revisa el enlace en tu correo"}]""",
        )
        bff.responderSiempreEn(RUTA_POLIZAS, 200, "[$pendiente]", metodo = "GET")
        composeRule.iniciarSesion()
        composeRule.esperarTexto("Tu solicitud está en revisión")
    }
}
