package com.solventa4bits.movil.identidad.dominio

/** Las dos caras de la cedula que el cliente sube (BITS-275). */
enum class CaraDeCedula { FRENTE, REVERSO }

/** Por que una foto no sirvio. Lo decide svc-core; la app solo lo muestra. */
enum class MotivoDeRechazo { BORROSA, OSCURA, REFLEJO, RECORTADA, NO_ES_CEDULA }

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

/** Lo que ya quedo guardado de cada cara, para retomar el paso donde quedo. */
enum class EstadoGuardado { ACEPTADA, REVISION_MANUAL, PENDIENTE }

sealed interface ResultadoEstado {
    data class Exito(val caras: Map<CaraDeCedula, EstadoGuardado>) : ResultadoEstado

    data object SesionVencida : ResultadoEstado

    data object Fallo : ResultadoEstado
}

interface ServicioDeEvidencias {
    /** Sube una foto ya preparada (JPEG) de una cara y devuelve el veredicto. */
    suspend fun subir(cara: CaraDeCedula, imagen: ByteArray): ResultadoSubida

    /** Lo que ya esta guardado de cada cara. */
    suspend fun consultar(): ResultadoEstado
}
