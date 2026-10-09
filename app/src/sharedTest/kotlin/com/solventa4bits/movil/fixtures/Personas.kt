package com.solventa4bits.movil.fixtures

import java.time.LocalDate

/**
 * Credito hipotecario tal como lo devuelve el simulador de Open Finance
 * (deploy/apps/wiremock, BITS-274). Si cambia un mapping alla, cambia aqui.
 */
data class CreditoHipotecario(
    val entidad: String,
    val valorOriginal: Long,
    val saldo: Long,
    val cuotaMensual: Long,
    val plazoMeses: Int,
    val mesesRestantes: Int,
    val diasDeMora: Int,
    val valorInmueble: Long,
)

data class PersonaDePrueba(
    val documento: String,
    val nombres: String,
    val apellidos: String,
    val fechaDeNacimiento: LocalDate,
    /** Null cuando la persona no tiene credito hipotecario en Open Finance. */
    val credito: CreditoHipotecario?,
)

/**
 * El juego unico de personas de prueba (BITS-280). Los documentos son las
 * llaves de los mappings de WireMock: cada uno devuelve un desenlace conocido.
 */
object Personas {

    val DANIEL = PersonaDePrueba(
        documento = "1000000002",
        nombres = "Daniel",
        apellidos = "Rojas",
        fechaDeNacimiento = LocalDate.of(1988, 4, 12),
        credito = CreditoHipotecario(
            entidad = "BANCOLOMBIA S.A.",
            valorOriginal = 180_000_000,
            saldo = 98_000_000,
            cuotaMensual = 1_650_000,
            plazoMeses = 180,
            mesesRestantes = 96,
            diasDeMora = 0,
            valorInmueble = 310_000_000,
        ),
    )

    val DAVID = PersonaDePrueba(
        documento = "1000000001",
        nombres = "David",
        apellidos = "Castro",
        fechaDeNacimiento = LocalDate.of(1981, 9, 3),
        credito = CreditoHipotecario(
            entidad = "BANCO DE BOGOTÁ S.A.",
            valorOriginal = 150_000_000,
            saldo = 142_000_000,
            cuotaMensual = 1_480_000,
            plazoMeses = 240,
            mesesRestantes = 228,
            diasDeMora = 45,
            valorInmueble = 165_000_000,
        ),
    )

    val SOFIA = PersonaDePrueba(
        documento = "1000000003",
        nombres = "Sofía",
        apellidos = "Pedraza",
        fechaDeNacimiento = LocalDate.of(1990, 11, 23),
        credito = CreditoHipotecario(
            entidad = "DAVIVIENDA S.A.",
            valorOriginal = 220_000_000,
            saldo = 178_500_000,
            cuotaMensual = 3_200_000,
            plazoMeses = 240,
            mesesRestantes = 165,
            diasDeMora = 0,
            valorInmueble = 260_000_000,
        ),
    )

    val NICOLAS = PersonaDePrueba(
        documento = "1000000004",
        nombres = "Nicolás",
        apellidos = "Herrera",
        fechaDeNacimiento = LocalDate.of(1995, 2, 17),
        credito = null,
    )

    val TODAS = listOf(DANIEL, DAVID, SOFIA, NICOLAS)
}

/**
 * Documentos que disparan fallas en el simulador. Cualquier otro documento
 * recibe la respuesta por defecto: un hipotecario en BBVA COLOMBIA S.A.
 */
object DocumentosDeFalla {
    /** Open Finance y Open Data responden 503. */
    const val FUENTES_CAIDAS = "8888888888"

    /** Solo Open Finance responde 503; Open Data responde normal. */
    const val SOLO_OPEN_FINANCE_CAIDO = "7777777777"

    /** Open Finance y Open Data tardan 5 segundos, mas que cualquier timeout de la app. */
    const val FUENTES_LENTAS = "9999999999"
}

/** Credenciales del usuario de demostracion de bff-mobile en modo fake (ver su README). */
object UsuarioDemo {
    const val CORREO = "ana.rios@example.com"
    const val CONTRASENA = "unaClaveSegura1"
}
