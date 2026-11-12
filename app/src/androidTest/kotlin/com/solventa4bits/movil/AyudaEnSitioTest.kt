package com.solventa4bits.movil

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.solventa4bits.movil.fixtures.BffFalso
import com.solventa4bits.movil.fixtures.RespuestasBff
import com.solventa4bits.movil.fixtures.SesionLimpia
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Pedir ayuda en sitio para la casa danada (BITS-239; M13, M13A, M14 y M15). */
@RunWith(AndroidJUnit4::class)
class AyudaEnSitioTest {

    private val almacen = almacenDelDispositivo()

    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val bff = BffFalso()

    @get:Rule
    val sesionLimpia = SesionLimpia(almacen)

    @Test
    fun pideLaAyudaDondeSeTomaronLasFotos() {
        abrirElSiniestro(FOTO_CON_UBICACION)

        // AC-1 y AC-7: la seccion con la entrada.
        composeRule.onNodeWithText("Pedir ayuda en sitio").performScrollTo().performClick()

        // AC-2 y AC-3: el tipo; la ayuda va a donde se tomaron las fotos.
        composeRule.esperarTexto("¿Qué ayuda necesitas?")
        composeRule.onNodeWithText("Donde tomaste las fotos").assertIsDisplayed()
        composeRule.onNodeWithText("Continuar").assertIsNotEnabled()
        composeRule.onNodeWithText("Plomería").performClick()
        composeRule.onNodeWithText("Continuar").performClick()

        // AC-5: el mas cercano primero, con su tiempo de llegada.
        composeRule.esperarTexto("Elige quién te ayuda")
        composeRule.onNodeWithText("Plomería Rápida Chapinero").assertIsDisplayed()
        composeRule.onNodeWithText("0,1 km · llega en 35 min").assertIsDisplayed()
        bff.responderSiempreEn("$RUTA_SINIESTROS/$ID_DE_SINIESTRO/asistencias", 201, ASISTENCIA, metodo = "POST")
        composeRule.onNodeWithText("Pedir la ayuda").performClick()

        // AC-6: el numero, quien va, cuando llega y a donde.
        composeRule.esperarTexto("Pedimos la ayuda")
        composeRule.onNodeWithText("Tu número de ayuda es ASI-2026-K7M2Q9").assertIsDisplayed()
        composeRule.onNodeWithText("35 min").assertIsDisplayed()
        composeRule.onNodeWithText("Donde tomaste las fotos").assertIsDisplayed()
        composeRule.onNodeWithText("SIN-2026-00042").assertIsDisplayed()
        assertTrue(bff.recibidas.contains("POST $RUTA_SINIESTROS/$ID_DE_SINIESTRO/asistencias"))

        // AC-7: "Tu siniestro" ya la muestra.
        composeRule.onNodeWithText("Ver mi siniestro").performClick()
        composeRule.esperarTexto("AYUDA EN SITIO")
        composeRule.onNodeWithText("ASI-2026-K7M2Q9 · pedida el 12 nov 2026").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun sinFotosConUbicacionSeEscribeLaDireccion() {
        abrirElSiniestro(FOTO_SIN_UBICACION)
        composeRule.onNodeWithText("Pedir ayuda en sitio").performScrollTo().performClick()
        composeRule.esperarTexto("¿Qué ayuda necesitas?")
        composeRule.onNodeWithText("Plomería").performClick()
        composeRule.onNodeWithText("Continuar").performClick()

        // AC-4: no esta en la casa; la direccion y la localidad de Bogota.
        composeRule.esperarTexto("¿Estás en tu casa?")
        composeRule.onNodeWithText("No, escribiré la dirección").performClick()
        composeRule.onNodeWithText("Continuar").assertIsNotEnabled()
        composeRule.onNodeWithText("Dirección").performTextInput("Calle 63 # 11-40")
        composeRule.onNodeWithText("Localidad").performClick()
        composeRule.onNodeWithText("Chía").assertDoesNotExist()
        composeRule.onNodeWithText("Chapinero").performClick()
        composeRule.onNodeWithText("Continuar").performClick()

        composeRule.esperarTexto("Elige quién te ayuda")
        composeRule.onNodeWithText("Plomería Rápida Chapinero").assertIsDisplayed()
    }

    /** Abre "Mi seguro" con un siniestro de Tu casa En estudio y entra a su detalle, con su [foto]. */
    private fun abrirElSiniestro(foto: String) {
        val comun = """"id": "$ID_DE_SINIESTRO", "numero": "SIN-2026-00042", "polizaId": "$ID_DE_POLIZA",
            "tipo": "INCENDIO_TERREMOTO", "automatico": false, "estado": "EN_ESTUDIO",
            "reportadoEn": "2026-11-02T14:12:00Z", "actualizadoEn": "2026-11-02T14:12:00Z""""
        bff.responder(200, RespuestasBff.sesionValida())
        composeRule.abrirApp(bff, almacen)
        bff.responderSiempreEn(RUTA_POLIZAS, 200, "[${polizaEnviada("EMITIDA", conTuCasa = true)}]", metodo = "GET")
        bff.responderSiempreEn(RUTA_SINIESTROS, 200, "[{$comun}]", metodo = "GET")
        bff.responderSiempreEn(
            "$RUTA_SINIESTROS/$ID_DE_SINIESTRO",
            200,
            """{$comun, "fechaOcurrencia": "2026-11-02", "fechaLimite": "2026-12-02T14:12:00Z",
                "archivos": [$foto], "asistencias": []}""",
            metodo = "GET",
        )
        bff.responderSiempreEn("/v1/catalogos/siniestros", 200, CATALOGO)
        composeRule.iniciarSesion()
        composeRule.esperarTexto("Tus seguros")
        composeRule.onNodeWithText("SOL-VH-2026-K7M2Q9").performScrollTo().performClick()
        composeRule.esperarTexto("VIDA PARA TU CRÉDITO")
        composeRule.onNodeWithText("Radicado SIN-2026-00042").performScrollTo().performClick()
        composeRule.esperarTexto("AYUDA EN SITIO")
    }

    private companion object {
        const val RUTA_SINIESTROS = "/v1/cuenta/siniestros"
        const val ID_DE_SINIESTRO = "3f6b8a1c-2d4e-4f70-9a81-b2c3d4e5f607"

        const val FOTO_CON_UBICACION = """{"id": "a1", "clase": "FOTO", "latitud": 4.6497, "longitud": -74.0631,
            "entregadoEn": "2026-11-02T14:12:00Z"}"""
        const val FOTO_SIN_UBICACION = """{"id": "a1", "clase": "FOTO", "entregadoEn": "2026-11-02T14:12:00Z"}"""

        /** El catalogo de Daniel, recortado: un plomero cerca, otro lejos y una localidad fuera de Bogota. */
        const val CATALOGO = """{"mercado": "CO", "version": 1,
            "tiposAyuda": [{"id": "plomeria", "nombre": "Plomería", "orden": 1},
              {"id": "electricidad", "nombre": "Electricidad", "orden": 2}],
            "localidades": [
              {"id": "chapinero", "nombre": "Chapinero", "ciudad": "Bogotá", "latitud": 4.649, "longitud": -74.063},
              {"id": "chia", "nombre": "Chía", "ciudad": "Chía", "latitud": 4.861, "longitud": -74.032}],
            "prestadores": [
              {"id": "pr-020", "nombre": "Plomeros de Kennedy", "tipos": ["plomeria"], "localidad": "kennedy",
               "direccion": "Calle 38 Sur # 78-10", "latitud": 4.628, "longitud": -74.155, "tiempoLlegadaMin": 50},
              {"id": "pr-001", "nombre": "Plomería Rápida Chapinero", "tipos": ["plomeria"], "localidad": "chapinero",
               "direccion": "Calle 60 # 9-12", "latitud": 4.65, "longitud": -74.062, "tiempoLlegadaMin": 35}],
            "oficinas": []}"""

        const val ASISTENCIA = """{"id": "as1", "numero": "ASI-2026-K7M2Q9", "tipo": "plomeria",
            "prestador": {"id": "pr-001", "nombre": "Plomería Rápida Chapinero", "direccion": "Calle 60 # 9-12",
              "tiempoLlegadaMin": 35},
            "tiempoLlegadaMin": 35, "origen": "FOTOS", "latitud": 4.6497, "longitud": -74.0631,
            "direccion": null, "localidad": null, "solicitadaEn": "2026-11-12T14:30:00Z"}"""
    }
}
