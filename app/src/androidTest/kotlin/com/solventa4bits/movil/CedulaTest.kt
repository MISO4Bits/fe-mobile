package com.solventa4bits.movil

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.solventa4bits.movil.fixtures.BffFalso
import com.solventa4bits.movil.fixtures.SesionLimpia
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Cargar la cedula por las dos caras de extremo a extremo (BITS-275, CP-M).
 * Todo es real menos la camara: [camaraDePrueba] entrega una foto generada.
 * La camara real (marco, permiso, galeria) se prueba a mano.
 */
@RunWith(AndroidJUnit4::class)
class CedulaTest {

    private val almacen = almacenDelDispositivo()

    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val bff = BffFalso()

    @get:Rule
    val sesionLimpia = SesionLimpia(almacen)

    @Test
    fun aceptarLasCondicionesLlevaATuCedulaSinFotos() {
        composeRule.llegarATuCedula(bff, almacen)

        // AC-1: un espacio para cada cara; AC-4: sin las dos, no se continua.
        composeRule.onAllNodesWithText("Sin foto todavía").assertCountEquals(2)
        composeRule.onNodeWithText("Continuar").assertIsNotEnabled()
    }

    @Test
    fun alRetomarMuestraLoGuardadoYDejaContinuar() {
        composeRule.llegarATuCedula(bff, almacen, estadoGuardado(frente = "ACEPTADA", reverso = "REVISION_MANUAL"))

        composeRule.esperarTexto("Se ve bien")
        composeRule.onNodeWithText("La revisará una persona. Puedes seguir.").assertIsDisplayed()
        // AC-4: aceptada y en revision manual bastan para seguir.
        composeRule.onNodeWithText("Continuar").assertIsEnabled()
    }

    @Test
    fun unaFotoQueSirveSeVeBien() {
        bff.responderSiempreEn(RUTA_EVIDENCIAS, 201, subida("ACEPTADA"), metodo = "POST")
        composeRule.llegarATuCedula(bff, almacen)

        tomarFotoDelFrente()

        // AC-2: primero "revisando" y despues el resultado.
        composeRule.esperarTexto("Se ve bien")
    }

    @Test
    fun unaFotoQueNoSirvePideOtraConElMotivo() {
        bff.responderSiempreEn(
            RUTA_EVIDENCIAS,
            200,
            subida("REINTENTAR", motivo = "BORROSA", intentos = 2),
            metodo = "POST",
        )
        composeRule.llegarATuCedula(bff, almacen)

        tomarFotoDelFrente()

        composeRule.esperarTexto("Mejor otra foto: está borrosa. Te quedan 2 intentos.")
        composeRule.onNodeWithText("Tomar otra vez").assertIsDisplayed()
    }

    @Test
    fun siFallaLaRedLoDiceYDejaIntentarDeNuevo() {
        bff.responderSiempreEn(RUTA_EVIDENCIAS, 503, metodo = "POST")
        composeRule.llegarATuCedula(bff, almacen)

        tomarFotoDelFrente()

        composeRule.esperarTexto("No pudimos enviar la foto. Inténtalo de nuevo.")
        composeRule.onNodeWithText("Tomar otra vez").assertIsDisplayed()
    }

    // El primer recuadro es el del frente.
    private fun tomarFotoDelFrente() {
        composeRule.onAllNodesWithText("Toca para tomar la foto")[0].performClick()
        composeRule.usarFotoDePrueba()
    }
}
