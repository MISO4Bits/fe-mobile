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
}
