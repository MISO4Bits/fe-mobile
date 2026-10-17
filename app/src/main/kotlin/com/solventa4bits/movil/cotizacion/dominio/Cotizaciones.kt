package com.solventa4bits.movil.cotizacion.dominio

/** Los datos del credito con los que se cotiza. Valores en pesos, sin decimales. */
data class DatosDelCredito(
    val entidadAcreedora: String,
    val valorCredito: Long,
    val saldoInsoluto: Long,
    val plazoRestanteMeses: Int,
)

enum class EfectoEnElPrecio { BAJA, SUBE }

/** Una senal del perfil de riesgo que explica el precio ("Por que este precio"). */
data class FactorDelPrecio(
    val descripcion: String,
    val efecto: EfectoEnElPrecio,
    /** Cuanto pesa frente a los demas factores. Null si el perfil no lo dice. */
    val peso: Double?,
)

/**
 * La cotizacion tal como la calculo el backend. La app no calcula nada: solo
 * muestra lo que recibe (BITS-95). Valores en COP.
 */
data class Cotizacion(
    /** False pasados 30 dias de creada: el cliente debe cotizar de nuevo. */
    val vigente: Boolean,
    val datosCredito: DatosDelCredito,
    /** Los datos los escribio el cliente: la cotizacion es sobre datos no verificados. */
    val datosDeclarados: Boolean,
    val primaMensual: Double,
    /** La tarifa estandar, sin el ajuste del perfil. */
    val primaBaseMensual: Double,
    val sumaAsegurada: Double,
    val coberturaMeses: Int,
    /** El precio aplica el ajuste del perfil de riesgo del cliente. */
    val personalizada: Boolean,
    val factores: List<FactorDelPrecio>,
)

/** Todo lo que puede pasar al cotizar o al consultar la cotizacion activa. */
sealed interface ResultadoCotizacion {
    data class Exito(val cotizacion: Cotizacion) : ResultadoCotizacion

    /** El backend rechazo los datos (400), por ejemplo si el credito termina despues de los 75 anos. */
    data class DatosRechazados(val mensaje: String?) : ResultadoCotizacion

    /** El cliente no tiene cotizacion activa (404). */
    data object SinCotizacion : ResultadoCotizacion

    data object SesionVencida : ResultadoCotizacion

    /** No se pudo completar: no hay red o el servidor fallo. */
    data object Fallo : ResultadoCotizacion
}

interface ServicioDeCotizaciones {
    /** Cotiza y deja la cotizacion como la activa del cliente: reemplaza la anterior. */
    suspend fun cotizar(datos: DatosDelCredito): ResultadoCotizacion

    suspend fun consultarActual(): ResultadoCotizacion
}
