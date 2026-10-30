package com.solventa4bits.movil

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.solventa4bits.movil.fixtures.CuentaEnDev
import com.solventa4bits.movil.fixtures.SesionLimpia
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * El camino feliz del movil contra el BFF de dev (BITS-146): iniciar sesion,
 * cotizar con el credito del banco, sumar "Tu casa" y enviar la solicitud con
 * la cedula y la selfie. Solo corre si se le pasa la URL de dev:
 *
 *   adb shell am instrument -w -e bffDev https://dev.solventa4bits.com/ ...
 *
 * Sin el argumento se salta, asi la suite de siempre sigue contra el BFF falso.
 */
@RunWith(AndroidJUnit4::class)
class RecorridoEnDevTest {

    private val urlDev: String? = InstrumentationRegistry.getArguments().getString("bffDev")
    private val almacen = almacenDelDispositivo()
    private val cuenta = CuentaEnDev(urlDev.orEmpty())

    private val composeRule = createComposeRule()

    // Primero se decide si corre; solo entonces se siembra la cuenta.
    @get:Rule
    val reglas: RuleChain = RuleChain
        .outerRule { base, _ -> assumeTrue("Sin -e bffDev no corre contra dev", urlDev != null); base }
        .around(cuenta)
        .around(SesionLimpia(almacen))
        .around(composeRule)

    @Test
    fun desdeElInicioDeSesionHastaEnviarLaSolicitud() {
        composeRule.abrirApp(cuenta.urlBffMovil, almacen)
        composeRule.iniciarSesion(cuenta.correo, cuenta.contrasena, ESPERA_DEV_MS)

        // BITS-219 y BITS-95: cotiza con el credito que trae el banco.
        composeRule.onNodeWithText("Cotizar mi seguro").performClick()
        composeRule.elegirBanco("BBVA Colombia")
        // El saldo de la persona por defecto: el banco ya respondio.
        composeRule.esperarTexto("$160.000.000", ESPERA_DEV_MS)
        composeRule.onNodeWithText("Ver mi cotización").performClick()

        // BITS-96: "Tu casa" se suma a la vida.
        composeRule.esperarTexto("Elige tus coberturas", ESPERA_DEV_MS)
        composeRule.onNodeWithText("Tu casa").performClick()
        composeRule.onNodeWithText("Continuar").performClick()
        composeRule.esperarTexto("Aceptar y continuar")
        composeRule.onNodeWithText("Aceptar y continuar").performClick()

        // BITS-275 y BITS-235: las fotos en revision manual dejan seguir.
        composeRule.esperarTexto("La revisará una persona. Puedes seguir.", ESPERA_DEV_MS)
        composeRule.onNodeWithText("Continuar").performClick()
        composeRule.esperarTexto("Confirma que eres tú")
        composeRule.esperarTexto("La revisará una persona. Puedes seguir.", ESPERA_DEV_MS)
        composeRule.onNodeWithText("Continuar").performClick()

        // BITS-278: acepta las condiciones y envia; BITS-345 muestra que quedo en revision.
        composeRule.esperarTexto("Revisa y envía tu solicitud", ESPERA_DEV_MS)
        // Los requisitos llegan junto con las condiciones: antes, "Leer las condiciones" no abre nada.
        composeRule.esperarTexto("Tu selfie", ESPERA_DEV_MS)
        // El titulo lo pone svc-productos; se espera el boton, que es el mismo en dev y en el BFF falso.
        composeRule.onNodeWithText("Leer las condiciones").performClick()
        composeRule.esperarTexto("Acepto estas condiciones", ESPERA_DEV_MS)
        composeRule.onNodeWithText("Acepto estas condiciones").performClick()
        composeRule.esperarQueDesaparezca("Acepto estas condiciones")
        composeRule.onNodeWithText("Enviar solicitud").performClick()
        composeRule.esperarTexto("Revisamos tu solicitud", ESPERA_DEV_MS)
    }

    private companion object {
        const val ESPERA_DEV_MS = 20_000L
    }
}
