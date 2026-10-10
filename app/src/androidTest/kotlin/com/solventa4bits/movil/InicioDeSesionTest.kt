package com.solventa4bits.movil

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.solventa4bits.movil.fixtures.BffFalso
import com.solventa4bits.movil.fixtures.RespuestasBff
import com.solventa4bits.movil.fixtures.SesionLimpia
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Camino feliz de extremo a extremo: el cliente escribe sus credenciales,
 * inicia sesion y llega al inicio de la app.
 */
@RunWith(AndroidJUnit4::class)
class InicioDeSesionTest {

    private val almacen = almacenDelDispositivo()

    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val bff = BffFalso()

    @get:Rule
    val sesionLimpia = SesionLimpia(almacen)

    @Test
    fun conCredencialesCorrectasLlegaAlInicio() {
        bff.responder(200, RespuestasBff.sesionValida())
        composeRule.abrirApp(bff, almacen)

        composeRule.iniciarSesion()

        composeRule.onNodeWithText("Cotizar mi seguro").assertIsDisplayed()
        assertNotNull(almacen.leer())
    }
}
