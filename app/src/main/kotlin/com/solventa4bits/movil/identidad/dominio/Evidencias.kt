package com.solventa4bits.movil.identidad.dominio

/** Las dos caras de la cedula que el cliente sube (BITS-275). */
enum class CaraDeCedula { FRENTE, REVERSO }

/** Por que una foto no sirvio. Lo decide svc-core; la app solo lo muestra. */
enum class MotivoDeRechazo {
    BORROSA,
    OSCURA,
    REFLEJO,
    RECORTADA,
    NO_ES_CEDULA,

    // Los de la selfie (BITS-235).
    SIN_ROSTRO,
    VARIOS_ROSTROS,
    POSIBLE_SUPLANTACION,
    NO_COINCIDE_CON_CEDULA,
}

/** Lo que el servidor decidio sobre una foto. */
sealed interface Veredicto {
    data object Aceptada : Veredicto

    /**
     * La foto no sirve: el cliente toma otra. [motivo] es null si el servidor
     * mando uno que la app aun no conoce.
     */
    data class Reintentar(val motivo: MotivoDeRechazo?, val intentosRestantes: Int) : Veredicto

    /** Una persona la revisa y el cliente puede seguir. */
    data object RevisionManual : Veredicto
}

sealed interface ResultadoSubida {
    data class Exito(val veredicto: Veredicto) : ResultadoSubida

    data object SesionVencida : ResultadoSubida

    /** No llego al servidor o el servidor fallo. No se reintenta solo: lo decide el cliente. */
    data object Fallo : ResultadoSubida
}

/** Lo que ya quedo guardado de cada foto, para retomar el paso donde quedo. */
enum class EstadoGuardado { ACEPTADA, REVISION_MANUAL, PENDIENTE }

sealed interface ResultadoEstado {
    data class Exito(
        val caras: Map<CaraDeCedula, EstadoGuardado>,
        val selfie: EstadoGuardado = EstadoGuardado.PENDIENTE,
    ) : ResultadoEstado

    data object SesionVencida : ResultadoEstado

    data object Fallo : ResultadoEstado
}

interface ServicioDeEvidencias {
    /** Sube una foto ya preparada (JPEG) de una cara y devuelve el veredicto. */
    suspend fun subir(cara: CaraDeCedula, imagen: ByteArray): ResultadoSubida

    /**
     * Sube la selfie ya preparada (BITS-235). Si falta una cara vigente de la
     * cedula, el servidor la rechaza (422) y queda como fallo.
     */
    suspend fun subirSelfie(imagen: ByteArray): ResultadoSubida

    /** Lo que ya esta guardado de cada cara y de la selfie. */
    suspend fun consultar(): ResultadoEstado
}
