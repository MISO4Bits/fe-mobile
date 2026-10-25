package com.solventa4bits.movil

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.solventa4bits.movil.fixtures.BffFalso
import com.solventa4bits.movil.fixtures.SesionLimpia
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Verificar la identidad con una selfie de extremo a extremo (BITS-235, AC-4 y
 * AC-5). Todo es real menos la camara: [camaraDePrueba] entrega una foto
 * generada. La camara frontal real (ovalo, permiso) se prueba a mano.
 */
@RunWith(AndroidJUnit4::class)
class SelfieTest {

    private val almacen = almacenDelDispositivo()

    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val bff = BffFalso()

    @get:Rule
    val sesionLimpia = SesionLimpia(almacen)

    @Test
    fun conLasDosCarasListasContinuarLlevaATuSelfie() {
        llegarATuSelfie()

        composeRule.onNodeWithText("Tomar la selfie").assertIsDisplayed()
        composeRule.onNodeWithText("Volver a tu cédula").assertIsDisplayed()
    }

    @Test
    fun volverATuCedulaRegresaALaCedula() {
        llegarATuSelfie()

        composeRule.onNodeWithText("Volver a tu cédula").performClick()

        composeRule.esperarTexto("Tu cédula, por las dos caras")
    }

    @Test
    fun unaSelfieQueSirveSeVeBienYDejaContinuar() {
        bff.responderSiempreEn(RUTA_EVIDENCIAS, 201, subida("ACEPTADA", tipo = "selfie"), metodo = "POST")
        llegarATuSelfie()

        tomarLaSelfie()

        // AC-4: primero "revisando" y despues el resultado.
        composeRule.esperarTexto("Se ve bien")
        composeRule.onNodeWithText("Continuar").assertIsEnabled()
    }

    @Test
    fun unaSelfieQueNoSirvePideOtraConElMotivo() {
        bff.responderSiempreEn(
            RUTA_EVIDENCIAS,
            200,
            subida("REINTENTAR", motivo = "NO_COINCIDE_CON_CEDULA", intentos = 2, tipo = "selfie"),
            metodo = "POST",
        )
        llegarATuSelfie()

        tomarLaSelfie()

        composeRule.esperarTexto("Mejor otra foto: no se parece a la foto de tu cédula. Te quedan 2 intentos.")
        composeRule.onNodeWithText("Tomarla otra vez").assertIsDisplayed()
        composeRule.onNodeWithText("Continuar").assertIsNotEnabled()
    }

    @Test
    fun alRetomarUnaSelfieEnRevisionManualDejaSeguir() {
        llegarATuSelfie(selfieGuardada = "REVISION_MANUAL")

        // AC-5: en revision manual el cliente puede seguir.
        composeRule.esperarTexto("La revisará una persona. Puedes seguir.")
        composeRule.onNodeWithText("Continuar").assertIsEnabled()
    }

    @Test
    fun siFallaLaRedLoDiceYDejaTomarlaOtraVez() {
        bff.responderSiempreEn(RUTA_EVIDENCIAS, 503, metodo = "POST")
        llegarATuSelfie()

        tomarLaSelfie()

        composeRule.esperarTexto("No pudimos enviar la foto. Inténtalo de nuevo.")
        composeRule.onNodeWithText("Tomarla otra vez").assertIsDisplayed()
    }

    @Test
    fun trasUnRechazoVolverALaCedulaYRegresarDejaTomarLaSelfie() {
        bff.responderSiempreEn(
            RUTA_EVIDENCIAS,
            200,
            subida("REINTENTAR", motivo = "SIN_ROSTRO", intentos = 2, tipo = "selfie"),
            metodo = "POST",
        )
        llegarATuSelfie()
        tomarLaSelfie()
        composeRule.esperarTexto("Tomarla otra vez")

        // El caso de la revision del PR: vuelve a la cedula con el gesto atras y regresa.
        Espresso.pressBack()
        composeRule.esperarTexto("Tu cédula, por las dos caras")
        composeRule.onNodeWithText("Continuar").performClick()

        composeRule.esperarTexto("Tomar la selfie")
    }

    // Las dos caras ya estan aceptadas: "Continuar" en la cedula lleva a la selfie.
    private fun llegarATuSelfie(selfieGuardada: String = "PENDIENTE") {
        composeRule.llegarATuCedula(
            bff,
            almacen,
            estadoGuardado(frente = "ACEPTADA", reverso = "ACEPTADA", selfie = selfieGuardada),
        )
        composeRule.esperarTexto("Se ve bien")
        composeRule.onNodeWithText("Continuar").performClick()
        composeRule.esperarTexto("Confirma que eres tú")
    }

    private fun tomarLaSelfie() {
        composeRule.onNodeWithText("Tomar la selfie").performClick()
        composeRule.usarFotoDePrueba()
    }
}
