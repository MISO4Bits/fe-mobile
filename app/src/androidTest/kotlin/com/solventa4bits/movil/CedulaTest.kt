package com.solventa4bits.movil

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.material3.Button
import androidx.compose.material3.Text
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
import com.solventa4bits.movil.fixtures.Personas
import com.solventa4bits.movil.fixtures.RespuestasBff
import com.solventa4bits.movil.fixtures.SesionLimpia
import com.solventa4bits.movil.identidad.ui.Camara
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream

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
        llegarATuCedula()

        // AC-1: un espacio para cada cara; AC-4: sin las dos, no se continua.
        composeRule.onAllNodesWithText("Sin foto todavía").assertCountEquals(2)
        composeRule.onNodeWithText("Continuar").assertIsNotEnabled()
    }

    @Test
    fun alRetomarMuestraLoGuardadoYDejaContinuar() {
        llegarATuCedula(guardado = estadoGuardado(frente = "ACEPTADA", reverso = "REVISION_MANUAL"))

        composeRule.esperarTexto("Se ve bien")
        composeRule.onNodeWithText("La revisará una persona. Puedes seguir.").assertIsDisplayed()
        // AC-4: aceptada y en revision manual bastan para seguir.
        composeRule.onNodeWithText("Continuar").assertIsEnabled()
    }

    @Test
    fun unaFotoQueSirveSeVeBien() {
        bff.responderSiempreEn(RUTA, 201, subida("ACEPTADA"), metodo = "POST")
        llegarATuCedula()

        tomarFotoDelFrente()

        // AC-2: primero "revisando" y despues el resultado.
        composeRule.esperarTexto("Se ve bien")
    }

    @Test
    fun unaFotoQueNoSirvePideOtraConElMotivo() {
        bff.responderSiempreEn(RUTA, 200, subida("REINTENTAR", motivo = "BORROSA", intentos = 2), metodo = "POST")
        llegarATuCedula()

        tomarFotoDelFrente()

        composeRule.esperarTexto("Mejor otra foto: está borrosa. Te quedan 2 intentos.")
        composeRule.onNodeWithText("Tomar otra vez").assertIsDisplayed()
    }

    @Test
    fun siFallaLaRedLoDiceYDejaIntentarDeNuevo() {
        bff.responderSiempreEn(RUTA, 503, metodo = "POST")
        llegarATuCedula()

        tomarFotoDelFrente()

        composeRule.esperarTexto("No pudimos enviar la foto. Inténtalo de nuevo.")
        composeRule.onNodeWithText("Tomar otra vez").assertIsDisplayed()
    }

    private fun llegarATuCedula(guardado: String = "[]") {
        bff.responderSiempreEn(RUTA, 200, guardado, metodo = "GET")
        bff.responder(200, RespuestasBff.sesionValida())
        bff.responder(200, RespuestasBff.creditosDe(Personas.DANIEL))
        bff.responder(201, RespuestasBff.cotizacion())
        composeRule.abrirApp(bff, almacen)
        composeRule.iniciarSesion()

        composeRule.onNodeWithText("Cotizar mi seguro").performClick()
        composeRule.elegirBanco("Bancolombia")
        composeRule.esperarTexto("$180.000.000")
        composeRule.onNodeWithText("Ver mi cotización").performClick()
        composeRule.continuarDesdeCoberturas()
        composeRule.esperarTexto("Aceptar y continuar")
        composeRule.onNodeWithText("Aceptar y continuar").performClick()
        composeRule.esperarTexto("Tu cédula, por las dos caras")
    }

    // El primer recuadro es el del frente.
    private fun tomarFotoDelFrente() {
        composeRule.onAllNodesWithText("Toca para tomar la foto")[0].performClick()
        composeRule.esperarTexto(USAR_FOTO)
        composeRule.onNodeWithText(USAR_FOTO).performClick()
    }

    private fun estadoGuardado(frente: String, reverso: String) = """[
        {"tipo": "cedula_frente", "estado": "$frente", "intentosRestantes": 3},
        {"tipo": "cedula_reverso", "estado": "$reverso", "intentosRestantes": 0},
        {"tipo": "selfie", "estado": "PENDIENTE", "intentosRestantes": 3}
    ]"""

    private fun subida(estado: String, motivo: String? = null, intentos: Int = 3) =
        """{"evidenciaId": null, "tipo": "cedula_frente", "estado": "$estado",
           "motivo": ${motivo?.let { "\"$it\"" } ?: "null"}, "intentosRestantes": $intentos}"""

    private companion object {
        const val RUTA = "/v1/cuenta/evidencias-identidad"
    }
}

private const val USAR_FOTO = "Usar foto de prueba"

/** En lugar de la camara: un boton que entrega una foto JPEG generada en el momento. */
val camaraDePrueba: Camara = { alTenerFoto, _ ->
    Button(onClick = { alTenerFoto(fotoGenerada()) }) { Text(USAR_FOTO) }
}

private fun fotoGenerada(): ByteArray {
    val bitmap = Bitmap.createBitmap(ANCHO_DE_PRUEBA, ALTO_DE_PRUEBA, Bitmap.Config.ARGB_8888)
    bitmap.eraseColor(Color.GRAY)
    return ByteArrayOutputStream().use { salida ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, CALIDAD_DE_PRUEBA, salida)
        bitmap.recycle()
        salida.toByteArray()
    }
}

private const val ANCHO_DE_PRUEBA = 800
private const val ALTO_DE_PRUEBA = 500
private const val CALIDAD_DE_PRUEBA = 90
