package com.solventa4bits.movil

import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import com.solventa4bits.movil.cotizacion.datos.AdaptadorCotizaciones
import com.solventa4bits.movil.cotizacion.datos.AdaptadorCreditos
import com.solventa4bits.movil.cotizacion.datos.crearCotizacionesApi
import com.solventa4bits.movil.cotizacion.datos.crearCreditosApi
import com.solventa4bits.movil.fixtures.BffFalso
import com.solventa4bits.movil.fixtures.UsuarioDemo
import com.solventa4bits.movil.identidad.datos.AdaptadorEvidencias
import com.solventa4bits.movil.identidad.datos.crearEvidenciasApi
import com.solventa4bits.movil.solicitud.datos.AdaptadorSolicitudes
import com.solventa4bits.movil.solicitud.datos.crearSolicitudesApi
import com.solventa4bits.movil.sesion.datos.AdaptadorSesion
import com.solventa4bits.movil.sesion.datos.AlmacenDeSesionSeguro
import com.solventa4bits.movil.sesion.datos.BovedaKeystore
import com.solventa4bits.movil.sesion.datos.ConSesion
import com.solventa4bits.movil.sesion.datos.crearSesionApi
import com.solventa4bits.movil.sesion.dominio.AlmacenDePreferencias
import com.solventa4bits.movil.sesion.dominio.AlmacenDeSesion
import com.solventa4bits.movil.sesion.dominio.AutenticadorDeHuella
import com.solventa4bits.movil.sesion.dominio.AutenticadorFalso
import com.solventa4bits.movil.sesion.dominio.PreferenciasEnMemoria
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

fun ComposeContentTestRule.abrirApp(
    bff: BffFalso,
    almacen: AlmacenDeSesion,
    // Por defecto el telefono de prueba no tiene sensor de huella: el inicio
    // de sesion de siempre. HuellaTest le pone uno falso.
    autenticador: AutenticadorDeHuella = AutenticadorFalso(disponible = false),
    preferencias: AlmacenDePreferencias = PreferenciasEnMemoria(),
) {
    // Por defecto el cliente no tiene cotizacion en curso. Una prueba que la
    // necesite responde otra cosa en la misma ruta.
    bff.responderSiempreEn("/v1/cotizaciones/actual", 404)
    // Ni solicitudes enviadas: el Inicio las consulta cada vez que se muestra.
    bff.responderSiempreEn("/v1/cuenta/polizas", 200, "[]", metodo = "GET")
    val sesion = AdaptadorSesion(crearSesionApi(bff.url), sinTraza)
    val conSesion = ConSesion(almacen, sesion)
    val creditos = AdaptadorCreditos(crearCreditosApi(bff.url), conSesion, sinTraza)
    val cotizaciones = AdaptadorCotizaciones(crearCotizacionesApi(bff.url), conSesion, sinTraza)
    val evidencias = AdaptadorEvidencias(crearEvidenciasApi(bff.url), conSesion, sinTraza)
    val solicitudes = AdaptadorSolicitudes(crearSolicitudesApi(bff.url), conSesion, sinTraza)
    setContent {
        TemaSolventa {
            AppSolventa(
                sesion,
                almacen,
                creditos,
                cotizaciones,
                evidencias,
                solicitudes,
                autenticador,
                preferencias,
                camaraDePrueba,
            )
        }
    }
}

/** Escribe las credenciales del usuario de demostracion y entra hasta el inicio. */
fun ComposeContentTestRule.iniciarSesion() {
    onNodeWithText("Correo electrónico").performTextInput(UsuarioDemo.CORREO)
    onNodeWithText("Contraseña").performTextInput(UsuarioDemo.CONTRASENA)
    onNodeWithText("Iniciar sesión").performClick()
    // "Nueva cotizacion" esta en el Home con y sin cotizacion en curso.
    esperarTexto("Nueva cotización")
}

/** Abre la lista de bancos de la nueva cotizacion y elige uno. */
fun ComposeContentTestRule.elegirBanco(nombre: String) {
    esperarTexto("Banco o entidad")
    onNodeWithText("Banco o entidad").performClick()
    esperarTexto(nombre)
    onNodeWithText(nombre).performClick()
}

/** En "Tus coberturas", sigue a "Lo que pagas" sin marcar "Tu casa". */
fun ComposeContentTestRule.continuarDesdeCoberturas() {
    esperarTexto("Elige tus coberturas")
    onNodeWithText("Continuar").performClick()
}

fun ComposeContentTestRule.esperarTexto(texto: String) {
    waitUntil(ESPERA_MS) { onAllNodesWithText(texto).fetchSemanticsNodes().isNotEmpty() }
}

/** Espera a que un texto ya no este, por ejemplo al cerrarse un modal. */
fun ComposeContentTestRule.esperarQueDesaparezca(texto: String) {
    waitUntil(ESPERA_MS) { onAllNodesWithText(texto).fetchSemanticsNodes().isEmpty() }
}
