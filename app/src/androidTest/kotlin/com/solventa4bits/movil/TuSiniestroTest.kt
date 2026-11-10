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

/** En que va un siniestro de Tu casa, segun su estado (BITS-118; M09A, M09B, M09C y M09E). */
@RunWith(AndroidJUnit4::class)
class TuSiniestroTest {

    private val almacen = almacenDelDispositivo()

    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val bff = BffFalso()

    @get:Rule
    val sesionLimpia = SesionLimpia(almacen)

    @Test
    fun enFaltaInformacionMuestraLoQueSePidio() {
        abrirElSiniestro(
            "FALTA_INFORMACION",
            """"evidenciaPedida": {"detalle": "El informe de los bomberos", "pedidaEn": "2026-10-28T15:00:00Z"}""",
        )

        // AC-2: lo que falta y que el plazo espera a que llegue.
        composeRule.esperarTexto("PENDIENTES")
        composeRule.onNodeWithText("El informe de los bomberos").assertIsDisplayed()
        composeRule.onNodeWithText("Pedido el 28 oct 2026").assertIsDisplayed()
        composeRule.onNodeWithText("El plazo para responderte empieza a contar cuando recibamos lo que falta.")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Archivos").assertDoesNotExist()
    }

    @Test
    fun pagadoMuestraElPagoAlCliente() {
        abrirElSiniestro(
            "PAGADO",
            """"pago": {"pagadoEn": "2026-11-20T15:00:00Z", "lineas": [{"beneficiario": "CLIENTE",
                "valor": 18500000, "destino": "Cuenta de ahorros terminada en 4821"}]}""",
        )

        // AC-5: a quien, cuanto, a donde y cuando.
        composeRule.esperarTexto("TU PAGO")
        composeRule.onNodeWithText("A ti").assertIsDisplayed()
        composeRule.onNodeWithText("$18.500.000").assertIsDisplayed()
        composeRule.onNodeWithText("Cuenta de ahorros terminada en 4821").assertIsDisplayed()
        composeRule.onNodeWithText("20 nov 2026").assertIsDisplayed()
        composeRule.onNodeWithText("LO QUE REPORTASTE").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun rechazadoMuestraElMotivo() {
        abrirElSiniestro(
            "RECHAZADO",
            """"motivoRechazo": "las fotos muestran daños por humedad en el techo."""",
        )

        composeRule.esperarTexto("NUESTRA RESPUESTA")
        composeRule.onNodeWithText("No podemos pagar este siniestro.").assertIsDisplayed()
        composeRule.onNodeWithText("Motivo: las fotos muestran daños por humedad en el techo.").assertIsDisplayed()
        composeRule.onNodeWithText("Si tienes dudas, escríbenos a ayuda@solventa4bits.com.").assertIsDisplayed()
    }

    @Test
    fun elPagoAutomaticoMuestraElSismoYElBancoDelCredito() {
        abrirElSiniestro(
            "PAGADO",
            """"pago": {"pagadoEn": "2026-11-02T15:00:00Z", "lineas": [{"beneficiario": "BANCO",
                "valor": 1850000, "destino": "Tu próxima cuota del crédito"}]},
              "evento": {"fecha": "2026-11-02", "municipio": "Bogotá", "magnitud": 6.1}""",
            automatico = true,
        )

        // AC-6: nadie lo reporto; se ve el pago al banco y el sismo que lo disparo.
        composeRule.esperarTexto("Terremoto, pagado el 2 nov 2026")
        composeRule.onNodeWithText("Bancolombia").assertIsDisplayed()
        composeRule.onNodeWithText("$1.850.000").assertIsDisplayed()
        composeRule.onNodeWithText("Tu próxima cuota del crédito").assertIsDisplayed()
        composeRule.onNodeWithText("EL SISMO").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Bogotá").assertIsDisplayed()
        composeRule.onNodeWithText("6,1").assertIsDisplayed()
        composeRule.onNodeWithText("LO QUE REPORTASTE").assertDoesNotExist()
    }

    /** Abre "Mi seguro" con un siniestro en [estado] y entra a su detalle, que trae [loDelEstado]. */
    private fun abrirElSiniestro(estado: String, loDelEstado: String, automatico: Boolean = false) {
        val tipo = if (automatico) "SISMO_PARAMETRICO" else "INCENDIO_TERREMOTO"
        val comun = """"id": "$ID_DE_SINIESTRO", "numero": "SIN-2026-00042", "polizaId": "$ID_DE_POLIZA",
            "tipo": "$tipo", "automatico": $automatico, "estado": "$estado",
            "reportadoEn": "2026-11-02T14:12:00Z", "actualizadoEn": "2026-11-20T14:12:00Z""""
        bff.responder(200, RespuestasBff.sesionValida())
        composeRule.abrirApp(bff, almacen)
        bff.responderSiempreEn(
            RUTA_POLIZAS,
            200,
            "[${polizaEnviada("EMITIDA", conTuCasa = true)}]",
            metodo = "GET",
        )
        bff.responderSiempreEn(RUTA_SINIESTROS, 200, "[{$comun}]", metodo = "GET")
        bff.responderSiempreEn(
            "$RUTA_SINIESTROS/$ID_DE_SINIESTRO",
            200,
            """{$comun, "fechaOcurrencia": "2026-11-02", $loDelEstado,
                "archivos": [{"id": "a1", "clase": "FOTO", "entregadoEn": "2026-11-02T14:12:00Z"}]}""",
            metodo = "GET",
        )
        composeRule.iniciarSesion()
        composeRule.esperarTexto("Tus seguros")
        composeRule.onNodeWithText("SOL-VH-2026-K7M2Q9").performScrollTo().performClick()
        composeRule.esperarTexto("VIDA PARA TU CRÉDITO")
        composeRule.onNodeWithText("Radicado SIN-2026-00042").performScrollTo().performClick()
        composeRule.esperarTexto("Tu siniestro")
    }

    private companion object {
        const val RUTA_SINIESTROS = "/v1/cuenta/siniestros"
        const val ID_DE_SINIESTRO = "9b4e7c21-3a5d-4f8e-b612-7d0c2e9f4a83"
    }
}
