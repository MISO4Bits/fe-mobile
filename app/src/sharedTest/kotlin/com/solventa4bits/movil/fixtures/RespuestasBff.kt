package com.solventa4bits.movil.fixtures

/** Cuerpos de respuesta del BFF movil, con la forma de su contrato OpenAPI. */
object RespuestasBff {

    fun sesionValida(
        tokenDeAcceso: String = "jwt-de-acceso",
        tokenDeRefresco: String = "token-de-refresco",
        segundosDeVigencia: Long = 3600,
    ): String = """
        {
          "accessToken": "$tokenDeAcceso",
          "tokenType": "Bearer",
          "expiresIn": $segundosDeVigencia,
          "refreshToken": "$tokenDeRefresco"
        }
    """.trimIndent()

    /**
     * Respuesta de GET /v1/creditos-hipotecarios para una persona de prueba:
     * su hipoteca si la tiene, y siempre la lista de bancos.
     */
    fun creditosDe(persona: PersonaDePrueba): String {
        val credito = persona.credito
        val creditos = if (credito == null) {
            ""
        } else {
            """
                {
                  "entidadId": "bancolombia",
                  "entidadNombre": "${credito.entidad}",
                  "valorCredito": ${credito.valorOriginal},
                  "saldoInsoluto": ${credito.saldo},
                  "plazoRestanteMeses": ${credito.mesesRestantes},
                  "cuotaMensual": ${credito.cuotaMensual}
                }
            """.trimIndent()
        }
        return """
            {
              "estado": "${if (credito == null) "SIN_HIPOTECAS" else "DISPONIBLE"}",
              "creditos": [$creditos],
              "entidades": [
                { "id": "bancolombia", "nombre": "Bancolombia" },
                { "id": "davivienda", "nombre": "Davivienda" },
                { "id": "banco-de-bogota", "nombre": "Banco de Bogotá" }
              ],
              "origen": "OPEN_FINANCE"${if (credito == null) "" else ", \"fechaConsulta\": \"2026-10-09T12:00:00Z\""}
            }
        """.trimIndent()
    }
}
