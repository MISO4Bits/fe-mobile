package com.solventa4bits.movil

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Prueba instrumentada de arranque. Verifica que la actividad levanta con el
 * tema aplicado. Las pruebas de los recorridos llegan con sus pantallas.
 */
@RunWith(AndroidJUnit4::class)
class BienvenidaTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun muestraLaMarca() {
        composeRule.onNodeWithText("Solventa").assertIsDisplayed()
    }
}
