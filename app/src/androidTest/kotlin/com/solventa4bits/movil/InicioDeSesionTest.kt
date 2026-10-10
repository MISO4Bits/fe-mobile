package com.solventa4bits.movil

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.solventa4bits.movil.cotizacion.datos.AdaptadorCreditos
import com.solventa4bits.movil.cotizacion.datos.crearCreditosApi
import com.solventa4bits.movil.sesion.datos.AdaptadorSesion
import com.solventa4bits.movil.sesion.datos.AlmacenDeSesionSeguro
import com.solventa4bits.movil.sesion.datos.BovedaKeystore
import com.solventa4bits.movil.sesion.datos.crearSesionApi
import com.solventa4bits.movil.traza.Traza
import com.solventa4bits.movil.ui.tema.TemaSolventa
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Camino feliz de extremo a extremo: el cliente escribe sus credenciales,
 * inicia sesion y llega al inicio de la app.
 *
 * Todo es real (pantalla, ViewModel, adaptador, peticion HTTP y boveda)
 * menos el BFF, que es uno falso levantado por la propia prueba.
 */
@RunWith(AndroidJUnit4::class)
class InicioDeSesionTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val traza = Traza { _, _, _ -> }
    private val contexto = InstrumentationRegistry.getInstrumentation().targetContext
    private val almacen = AlmacenDeSesionSeguro(BovedaKeystore(contexto, traza), traza)
    private val bff = MockWebServer()

    @Before
    fun preparar() {
        almacen.borrar()
        bff.start()
    }

    @After
    fun limpiar() {
        bff.shutdown()
        almacen.borrar()
    }

    @Test
    fun conCredencialesCorrectasLlegaAlInicio() {
        bff.enqueue(MockResponse().setResponseCode(200).setBody(SESION))
        val servicio = AdaptadorSesion(crearSesionApi(bff.url("/").toString()), traza)
        val creditos = AdaptadorCreditos(crearCreditosApi(bff.url("/").toString()), almacen, servicio, traza)
        composeRule.setContent {
            TemaSolventa { AppSolventa(servicio, almacen, creditos) }
        }

        composeRule.onNodeWithText("Correo electrónico").performTextInput("ana.rios@example.com")
        composeRule.onNodeWithText("Contraseña").performTextInput("unaClaveSegura1")
        composeRule.onNodeWithText("Iniciar sesión").performClick()

        composeRule.waitUntil(ESPERA_MS) {
            composeRule.onAllNodesWithText("Cotizar mi seguro").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Cotizar mi seguro").assertIsDisplayed()
        assertNotNull(almacen.leer())
    }

    private companion object {
        const val ESPERA_MS = 5_000L
        const val SESION = """
            {
              "accessToken": "jwt-de-acceso",
              "tokenType": "Bearer",
              "expiresIn": 3600,
              "refreshToken": "token-de-refresco"
            }
        """
    }
}
