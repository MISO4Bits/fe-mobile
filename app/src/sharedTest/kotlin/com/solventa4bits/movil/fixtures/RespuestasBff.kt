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

    /**
     * Respuesta de POST /v1/cotizaciones y GET /v1/cotizaciones/actual: el
     * ejemplo del contrato de bff-mobile. Sin perfil, tarifa estandar. Con perfil
     * neutro, personalizada pero sin factores y a tarifa estandar (cuenta t3 de dev).
     */
    fun cotizacion(conPerfil: Boolean = true, perfilNeutro: Boolean = false): String {
        val prima = if (conPerfil && !perfilNeutro) PRIMA_CON_PERFIL else PRIMA_BASE
        val perfil = if (perfilNeutro) {
            """{ "nivelRiesgo": "MEDIO", "factores": [] }"""
        } else if (conPerfil) {
            """
                {
                  "nivelRiesgo": "BAJO",
                  "factores": [
                    { "descripcion": "Historial de pagos excelente", "efecto": "POSITIVO", "pesoRelativo": 0.1 },
                    { "descripcion": "Endeudamiento bajo", "efecto": "POSITIVO", "pesoRelativo": 0.2 }
                  ]
                }
            """.trimIndent()
        } else {
            "null"
        }
        return """
            {
              "estado": "VIGENTE",
              "producto": "VIDA_HIPOTECARIO",
              "datosCredito": {
                "valorCredito": 250000000,
                "plazoRestanteMeses": 180,
                "entidadAcreedora": "Bancolombia",
                "saldoInsoluto": 238500000
              },
              "origenDatos": "DECLARADO",
              "oferta": {
                "primaMensual": $prima,
                "primaBaseMensual": $PRIMA_BASE,
                "sumaAsegurada": 238500000,
                "coberturaMeses": 180,
                "moneda": "COP",
                "personalizado": $conPerfil
              },
              "perfilRiesgo": $perfil,
              "vigenciaCotizacion": { "desde": "2026-10-06T14:35:00Z", "hasta": "2026-11-05T14:35:00Z" },
              "creadaEn": "2026-10-06T14:35:00Z"
            }
        """.trimIndent()
    }

    // Prima del ejemplo de BITS-95: tarifa estandar y con el ajuste de un perfil de riesgo bajo.
    private const val PRIMA_BASE = 46_746
    private const val PRIMA_CON_PERFIL = 39_734
}
