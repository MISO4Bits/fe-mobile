package com.solventa4bits.movil

import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import com.solventa4bits.movil.cotizacion.datos.AdaptadorCreditos
import com.solventa4bits.movil.cotizacion.datos.crearCreditosApi
import com.solventa4bits.movil.fixtures.BffFalso
import com.solventa4bits.movil.fixtures.UsuarioDemo
import com.solventa4bits.movil.sesion.datos.AdaptadorSesion
import com.solventa4bits.movil.sesion.datos.AlmacenDeSesionSeguro
import com.solventa4bits.movil.sesion.datos.BovedaKeystore
import com.solventa4bits.movil.sesion.datos.crearSesionApi
import com.solventa4bits.movil.sesion.dominio.AlmacenDeSesion
import com.solventa4bits.movil.traza.Traza
import com.solventa4bits.movil.ui.tema.TemaSolventa

/*
 * Lo que comparten las pruebas de extremo a extremo: la app se arma con todo
 * real (pantallas, ViewModels, adaptadores, peticiones HTTP y boveda) menos el
 * BFF, que es uno falso levantado por la propia prueba.
 */

private const val ESPERA_MS = 5_000L
private val sinTraza = Traza { _, _, _ -> }

/** El almacen real de la app, sobre el Keystore del dispositivo. */
fun almacenDelDispositivo(): AlmacenDeSesion {
    val contexto = InstrumentationRegistry.getInstrumentation().targetContext
    return AlmacenDeSesionSeguro(BovedaKeystore(contexto, sinTraza), sinTraza)
}

fun ComposeContentTestRule.abrirApp(bff: BffFalso, almacen: AlmacenDeSesion) {
    val sesion = AdaptadorSesion(crearSesionApi(bff.url), sinTraza)
    val creditos = AdaptadorCreditos(crearCreditosApi(bff.url), almacen, sesion, sinTraza)
    setContent {
        TemaSolventa { AppSolventa(sesion, almacen, creditos) }
    }
}

/** Escribe las credenciales del usuario de demostracion y entra hasta el inicio. */
fun ComposeContentTestRule.iniciarSesion() {
    onNodeWithText("Correo electrónico").performTextInput(UsuarioDemo.CORREO)
    onNodeWithText("Contraseña").performTextInput(UsuarioDemo.CONTRASENA)
    onNodeWithText("Iniciar sesión").performClick()
    esperarTexto("Cotizar mi seguro")
}

fun ComposeContentTestRule.esperarTexto(texto: String) {
    waitUntil(ESPERA_MS) { onAllNodesWithText(texto).fetchSemanticsNodes().isNotEmpty() }
}
