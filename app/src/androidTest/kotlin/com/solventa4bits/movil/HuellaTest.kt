package com.solventa4bits.movil

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.solventa4bits.movil.fixtures.BffFalso
import com.solventa4bits.movil.fixtures.RespuestasBff
import com.solventa4bits.movil.fixtures.SesionLimpia
import com.solventa4bits.movil.fixtures.UsuarioDemo
import com.solventa4bits.movil.sesion.dominio.AutenticadorFalso
import com.solventa4bits.movil.sesion.dominio.PreferenciaDeHuella
import com.solventa4bits.movil.sesion.dominio.PreferenciasEnMemoria
import com.solventa4bits.movil.sesion.dominio.ResultadoHuella
import com.solventa4bits.movil.sesion.dominio.Sesion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Entrar con la huella (BITS-235, AC-1, AC-2 y AC-3) de extremo a extremo,
 * con el sensor reemplazado por un doble: el dialogo del sistema no se puede
 * automatizar. Las preferencias van en memoria, nuevas en cada prueba.
 */
@RunWith(AndroidJUnit4::class)
class HuellaTest {

    private val almacen = almacenDelDispositivo()
    private val preferencias = PreferenciasEnMemoria()

    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val bff = BffFalso()

    @get:Rule
    val sesionLimpia = SesionLimpia(almacen)

    @Test
    fun trasEntrarConContrasenaOfreceLaHuellaYAlActivarlaLlegaAlInicio() {
        bff.responder(200, RespuestasBff.sesionValida())
        composeRule.abrirApp(bff, almacen, AutenticadorFalso(), preferencias)

        composeRule.escribirCredencialesYEntrar()

        composeRule.esperarTexto("Entra más rápido con tu huella")
        composeRule.onNodeWithText("Activar huella").performClick()

        composeRule.esperarTexto("Nueva cotización")
        assertEquals(PreferenciaDeHuella.ACTIVADA, preferencias.huella())
    }

    @Test
    fun conAhoraNoEntraIgualYNoVuelveAPreguntar() {
        bff.responder(200, RespuestasBff.sesionValida())
        composeRule.abrirApp(bff, almacen, AutenticadorFalso(), preferencias)

        composeRule.escribirCredencialesYEntrar()
        composeRule.esperarTexto("Entra más rápido con tu huella")
        composeRule.onNodeWithText("Ahora no").performClick()

        composeRule.esperarTexto("Nueva cotización")
        assertEquals(PreferenciaDeHuella.RECHAZADA, preferencias.huella())
    }

    @Test
    fun sinSensorEntraComoSiempreSinHablarDeHuella() {
        bff.responder(200, RespuestasBff.sesionValida())
        composeRule.abrirApp(bff, almacen, AutenticadorFalso(disponible = false), preferencias)

        composeRule.iniciarSesion()

        assertEquals(PreferenciaDeHuella.SIN_DECIDIR, preferencias.huella())
    }

    @Test
    fun conLaHuellaActivadaAbreConElSensorYEntra() {
        almacen.guardar(Sesion("jwt-guardado", "refresco-guardado", 3600))
        preferencias.guardarHuella(PreferenciaDeHuella.ACTIVADA)
        // El refresco de la sesion guardada, una vez pasada la huella.
        bff.responder(200, RespuestasBff.sesionValida())

        composeRule.abrirApp(bff, almacen, AutenticadorFalso(respuesta = ResultadoHuella.EXITO), preferencias)

        composeRule.esperarTexto("Nueva cotización")
        assertNotNull(almacen.leer())
    }

    @Test
    fun siElSensorNoLaReconoceVuelveALasCredencialesYConservaLaSesion() {
        almacen.guardar(Sesion("jwt-guardado", "refresco-guardado", 3600))
        preferencias.guardarHuella(PreferenciaDeHuella.ACTIVADA)

        composeRule.abrirApp(bff, almacen, AutenticadorFalso(respuesta = ResultadoHuella.FALLO), preferencias)

        composeRule.esperarTexto("Iniciar sesión")
        composeRule.onNodeWithText("Correo electrónico").assertIsDisplayed()
        assertNotNull(almacen.leer())
        assertEquals(PreferenciaDeHuella.ACTIVADA, preferencias.huella())
    }

    /** Como [iniciarSesion], pero sin esperar el inicio: lo que sigue es la oferta de la huella. */
    private fun ComposeContentTestRule.escribirCredencialesYEntrar() {
        onNodeWithText("Correo electrónico").performTextInput(UsuarioDemo.CORREO)
        onNodeWithText("Contraseña").performTextInput(UsuarioDemo.CONTRASENA)
        onNodeWithText("Iniciar sesión").performClick()
    }
}
