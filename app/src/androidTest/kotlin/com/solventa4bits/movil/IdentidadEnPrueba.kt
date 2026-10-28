package com.solventa4bits.movil

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.solventa4bits.movil.fixtures.BffFalso
import com.solventa4bits.movil.fixtures.Personas
import com.solventa4bits.movil.fixtures.RespuestasBff
import com.solventa4bits.movil.identidad.ui.Camara
import com.solventa4bits.movil.sesion.dominio.AlmacenDeSesion
import java.io.ByteArrayOutputStream

/*
 * Lo que comparten las pruebas de la cedula (BITS-275) y de la selfie
 * (BITS-235): el camino hasta "Tu cedula" y la camara de prueba.
 */

/** La ruta de las fotos de identidad en el BFF: el GET y el POST comparten ruta. */
const val RUTA_EVIDENCIAS = "/v1/cuenta/evidencias-identidad"

const val USAR_FOTO = "Usar foto de prueba"

/**
 * Inicia sesion, cotiza con los datos del banco, acepta las condiciones y
 * llega a "Tu cedula". [guardado] es lo que responde el GET de las fotos.
 */
fun ComposeContentTestRule.llegarATuCedula(bff: BffFalso, almacen: AlmacenDeSesion, guardado: String = "[]") {
    bff.responderSiempreEn(RUTA_EVIDENCIAS, 200, guardado, metodo = "GET")
    bff.responder(200, RespuestasBff.sesionValida())
    bff.responder(200, RespuestasBff.creditosDe(Personas.DANIEL))
    bff.responder(201, RespuestasBff.cotizacion())
    abrirApp(bff, almacen)
    iniciarSesion()

    onNodeWithText("Cotizar mi seguro").performClick()
    elegirBanco("Bancolombia")
    esperarTexto("$180.000.000")
    onNodeWithText("Ver mi cotización").performClick()
    continuarDesdeCoberturas()
    esperarTexto("Aceptar y continuar")
    onNodeWithText("Aceptar y continuar").performClick()
    esperarTexto("Tu cédula, por las dos caras")
}

/** La ruta de las condiciones del seguro, tal como la pide la app. */
const val RUTA_CONDICIONES = "/v1/documentos-legales?tipo=poliza&mercado=CO"

const val RUTA_POLIZAS = "/v1/cuenta/polizas"

/** Las condiciones de la poliza que responde el BFF. */
const val CONDICIONES_DEL_SEGURO = """[
    {"tipo": "poliza", "version": "V1", "titulo": "Condiciones de tu seguro",
     "baseLegal": "Ley 1328 de 2009",
     "contenido": "<h3>1 · Qué estás aceptando</h3><p>Que Solventa le envíe a tu banco tu solicitud.</p>",
     "notaPie": "Aceptar estas condiciones no contrata todavía."}
]"""

/** Lo que responde el POST de la solicitud. */
fun polizaEnviada(estado: String = "PENDIENTE") = """{
    "polizaId": "7c0f2a52-3b6f-4a0e-9e7e-2f6c9d1a8b11", "numero": null, "estado": "$estado",
    "producto": "VIDA_HIPOTECARIO", "creadaEn": "2026-10-27T14:41:00Z", "pendientes": []
}"""

/**
 * Con la cedula y la selfie ya aceptadas, llega a "Revisa y envia tu
 * solicitud". Las condiciones responden lo que diga [condiciones].
 */
fun ComposeContentTestRule.llegarATuSolicitud(
    bff: BffFalso,
    almacen: AlmacenDeSesion,
    condiciones: Pair<Int, String> = 200 to CONDICIONES_DEL_SEGURO,
) {
    bff.responderSiempreEn(RUTA_CONDICIONES, condiciones.first, condiciones.second, metodo = "GET")
    llegarATuCedula(bff, almacen, estadoGuardado(frente = "ACEPTADA", reverso = "ACEPTADA", selfie = "ACEPTADA"))
    esperarTexto("Se ve bien")
    onNodeWithText("Continuar").performClick()
    esperarTexto("Confirma que eres tú")
    onNodeWithText("Continuar").performClick()
    esperarTexto("Revisa y envía tu solicitud")
}

/** En la camara de prueba, entrega la foto. */
fun ComposeContentTestRule.usarFotoDePrueba() {
    esperarTexto(USAR_FOTO)
    onNodeWithText(USAR_FOTO).performClick()
}

/** Lo que responde el GET de las fotos guardadas. */
fun estadoGuardado(frente: String, reverso: String, selfie: String = "PENDIENTE") = """[
    {"tipo": "cedula_frente", "estado": "$frente", "intentosRestantes": 3},
    {"tipo": "cedula_reverso", "estado": "$reverso", "intentosRestantes": 3},
    {"tipo": "selfie", "estado": "$selfie", "intentosRestantes": 3}
]"""

/** Lo que responde el POST de una foto. */
fun subida(estado: String, motivo: String? = null, intentos: Int = 3, tipo: String = "cedula_frente") =
    """{"evidenciaId": null, "tipo": "$tipo", "estado": "$estado",
       "motivo": ${motivo?.let { "\"$it\"" } ?: "null"}, "intentosRestantes": $intentos}"""

/** En lugar de la camara: un boton que entrega una foto JPEG generada en el momento. */
val camaraDePrueba: Camara = { _, alTenerFoto, _ ->
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
