package com.solventa4bits.movil

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.solventa4bits.movil.fixtures.BffFalso
import com.solventa4bits.movil.fixtures.RespuestasBff
import com.solventa4bits.movil.fixtures.SesionLimpia
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * El resultado de la solicitud de extremo a extremo (BITS-97, CP-M): al
 * enviarla y al volver a verla desde el Inicio. Las fechas las cubre FechasTest.
 */
@RunWith(AndroidJUnit4::class)
class ResultadoTest {

    private val almacen = almacenDelDispositivo()

    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val bff = BffFalso()

    @get:Rule
    val sesionLimpia = SesionLimpia(almacen)

    @Test
    fun alEnviarQuedaPendienteYMuestraLoQueFalta() {
        enviarYRecibir(
            polizaEnviada(
                "PENDIENTE",
                pendientes = """[
                    {"codigo": "CORREO_SIN_CONFIRMAR", "tipo": "USUARIO", "mensaje": "Revisa el enlace en tu correo"},
                    {"codigo": "CREDITO_NO_VERIFICADO", "tipo": "AGENTE",
                     "mensaje": "Los datos de tu crédito deben ser verificados por un agente"}
                ]""",
            ),
        )

        // AC-2: queda pendiente y se ve que falta, con el mensaje del servidor.
        composeRule.esperarTexto("Revisamos tu solicitud")
        composeRule.onNodeWithText("En curso").assertIsDisplayed()
        composeRule.onNodeWithText("Revisa el enlace en tu correo").assertIsDisplayed()
        composeRule.onNodeWithText("Los datos de tu crédito deben ser verificados por un agente").assertIsDisplayed()

        composeRule.onNodeWithText("Inicio").performClick()
        composeRule.esperarTexto("Cotizar mi seguro")
    }

    @Test
    fun alEnviarSinPendientesQuedaEmitida() {
        enviarYRecibir(polizaEnviada("EMITIDA"))

        // AC-1: no falta nada y la poliza se emite sola.
        composeRule.esperarTexto("Listo, tu seguro quedó activo")
        composeRule.onNodeWithText("Solicitud aprobada").assertIsDisplayed()
        composeRule.onNodeWithText("SOL-VH-2026-K7M2Q9").assertIsDisplayed()
        // BITS-102: el valor del seguro dentro de la cuota, desde el dia en que se emitio.
        mostrado("$39.734 al mes desde el 28 de octubre")
        mostrado("Lo cobra Bancolombia con la cuota de tu crédito")
        mostrado("Vida para tu crédito: $39.734")
        mostrado("Total: $39.734 al mes. Es el valor que aceptaste.")
        composeRule.onNodeWithText("Tu casa, con IVA", substring = true).assertDoesNotExist()

        composeRule.onNodeWithText("Ir al inicio").performClick()
        composeRule.esperarTexto("Nueva cotización")
    }

    @Test
    fun conTuCasaMuestraLoQueValeCadaCobertura() {
        enviarYRecibir(polizaEnviada("EMITIDA", conTuCasa = true))

        composeRule.esperarTexto("Listo, tu seguro quedó activo")
        mostrado("Vida para tu crédito: $30.000")
        mostrado("Tu casa, con IVA: $9.734")
        mostrado("Total: $39.734 al mes. Es el valor que aceptaste.")
    }

    private fun mostrado(texto: String) {
        composeRule.onNodeWithText(texto).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun desdeElInicioSeVuelveAVerYLlegaEmitidaSiSeCumplioLoUltimo() {
        llegarAlInicioCon(polizaEnviada("PENDIENTE"))
        bff.responderSiempreEn("$RUTA_POLIZAS/$ID_DE_POLIZA", 200, polizaEnviada("EMITIDA"), metodo = "GET")

        // AC-2: tambien se ve al volver a la cuenta.
        composeRule.esperarTexto("Tu solicitud está en revisión")
        composeRule.onNodeWithText("En revisión").assertIsDisplayed()
        composeRule.onNodeWithText("Ver mi solicitud").performClick()

        // AC-3: al consultarla, el servidor la revisa de nuevo y ya llega emitida.
        composeRule.esperarTexto("Solicitud aprobada")
    }

    @Test
    fun unaSolicitudRechazadaMuestraElMotivo() {
        val rechazada = polizaEnviada(
            "RECHAZADA",
            rechazo = """{"motivos": [{"codigo": "CREDITO_NO_VERIFICADO",
                "mensaje": "los datos de tu crédito no coinciden con los de tu banco."}],
                "rechazadaEn": "2026-10-28T10:00:00Z"}""",
        )
        llegarAlInicioCon(rechazada)
        bff.responderSiempreEn("$RUTA_POLIZAS/$ID_DE_POLIZA", 200, rechazada, metodo = "GET")

        composeRule.esperarTexto("No aprobada")
        composeRule.onNodeWithText("Ver mi solicitud").performClick()

        // AC-4: no fue aprobada, y el motivo.
        composeRule.esperarTexto("No aprobamos tu solicitud")
        composeRule.onNodeWithText("Motivo: los datos de tu crédito no coinciden con los de tu banco.")
            .assertIsDisplayed()
    }

    @Test
    fun siNoSePuedeConsultarLoDiceYDejaReintentar() {
        llegarAlInicioCon(polizaEnviada("PENDIENTE"))
        bff.responderSiempreEn("$RUTA_POLIZAS/$ID_DE_POLIZA", 503, metodo = "GET")

        composeRule.esperarTexto("Ver mi solicitud")
        composeRule.onNodeWithText("Ver mi solicitud").performClick()

        composeRule.esperarTexto("No pudimos cargar tu solicitud. Inténtalo de nuevo.")
        composeRule.onNodeWithText("Reintentar").assertIsDisplayed()
    }

    private fun enviarYRecibir(respuesta: String) {
        bff.responderSiempreEn(RUTA_POLIZAS, 201, respuesta, metodo = "POST")
        composeRule.llegarATuSolicitud(bff, almacen)
        composeRule.aceptarLasCondiciones()
        composeRule.onNodeWithText("Enviar solicitud").performClick()
    }

    private fun llegarAlInicioCon(solicitud: String) {
        bff.responder(200, RespuestasBff.sesionValida())
        composeRule.abrirApp(bff, almacen)
        bff.responderSiempreEn(RUTA_POLIZAS, 200, "[$solicitud]", metodo = "GET")
        composeRule.iniciarSesion()
    }
}
