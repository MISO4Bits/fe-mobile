package com.solventa4bits.movil

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.solventa4bits.movil.avisos.dominio.DestinoDeAviso
import com.solventa4bits.movil.fixtures.BffFalso
import com.solventa4bits.movil.fixtures.RespuestasBff
import com.solventa4bits.movil.fixtures.SesionLimpia
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Con la app abierta en otra pantalla, tocar el aviso abre el seguro del siniestro (BITS-356). */
@RunWith(AndroidJUnit4::class)
class AvisoConLaAppAbiertaTest {

    private val almacen = almacenDelDispositivo()
    private val aviso = mutableStateOf<DestinoDeAviso?>(null)

    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val bff = BffFalso()

    @get:Rule
    val sesionLimpia = SesionLimpia(almacen)

    @Test
    fun elAvisoAbreElSeguroConElEstadoNuevo() {
        bff.responder(200, RespuestasBff.sesionValida())
        composeRule.abrirApp(bff, almacen, aviso = aviso)
        val poliza = polizaEnviada("EMITIDA", conTuCasa = true)
        bff.responderSiempreEn(RUTA_POLIZAS, 200, "[$poliza]", metodo = "GET")
        bff.responderSiempreEn("$RUTA_POLIZAS/$ID_DE_POLIZA", 200, poliza, metodo = "GET")
        bff.responderSiempreEn(RUTA_SINIESTROS, 200, "[{${siniestro("FALTA_INFORMACION")}}]", metodo = "GET")
        bff.responderSiempreEn(
            "$RUTA_SINIESTROS/$ID_DE_SINIESTRO",
            200,
            """{${siniestro("FALTA_INFORMACION")}, "fechaOcurrencia": "2026-11-02", $PEDIDO, "archivos": []}""",
            metodo = "GET",
        )
        composeRule.iniciarSesion()
        composeRule.esperarTexto("Tus seguros")
        composeRule.onNodeWithText("SOL-VH-2026-K7M2Q9").performScrollTo().performClick()
        composeRule.esperarTexto("VIDA PARA TU CRÉDITO")
        composeRule.onNodeWithText("Radicado SIN-2026-00042").performScrollTo().performClick()
        composeRule.esperarTexto("PENDIENTES")

        // AC-2: el siniestro se rechaza y el cliente toca el aviso, sin salir de "Tu siniestro".
        bff.responderSiempreEn(RUTA_SINIESTROS, 200, "[{${siniestro("RECHAZADO")}}]", metodo = "GET")
        composeRule.runOnIdle { aviso.value = DestinoDeAviso(ID_DE_POLIZA) }

        composeRule.esperarTexto("Rechazado")
        composeRule.onNodeWithText("VIDA PARA TU CRÉDITO").assertIsDisplayed()
        composeRule.onNodeWithText("PENDIENTES").assertDoesNotExist()
    }

    private fun siniestro(estado: String) = """"id": "$ID_DE_SINIESTRO", "numero": "SIN-2026-00042",
        "polizaId": "$ID_DE_POLIZA", "tipo": "INCENDIO_TERREMOTO", "automatico": false, "estado": "$estado",
        "reportadoEn": "2026-11-02T14:12:00Z", "actualizadoEn": "2026-11-13T14:12:00Z""""

    private companion object {
        const val RUTA_SINIESTROS = "/v1/cuenta/siniestros"
        const val ID_DE_SINIESTRO = "5d2a9e14-7b3c-4f61-8a20-c9e1f0b4d7a2"
        const val PEDIDO =
            """"evidenciaPedida": {"detalle": "El informe de los bomberos", "pedidaEn": "2026-11-10T15:00:00Z"}"""
    }
}
