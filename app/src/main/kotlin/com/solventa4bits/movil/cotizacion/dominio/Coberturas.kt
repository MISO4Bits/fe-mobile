package com.solventa4bits.movil.cotizacion.dominio

/** Las coberturas que puede tener el plan. HOGAR es "Tu casa". */
enum class CodigoCobertura { VIDA, HOGAR }

/** El precio mensual de una cobertura, tal como lo calcula el backend (BITS-96). Valores en COP. */
data class PrecioDeCobertura(
    val codigo: CodigoCobertura,
    val sumaAsegurada: Double,
    /** Prima mensual sin IVA. */
    val primaNeta: Double,
    val iva: Double,
    /** Prima mensual con IVA. */
    val primaTotal: Double,
)

/** Lo que el cliente paga cada mes con las coberturas que eligio, ya sumado. */
data class PlanElegido(
    val vida: Double,
    /** Prima de "Tu casa" sin IVA. Null si no la eligio. */
    val tuCasa: Double?,
    val subtotal: Double,
    val iva: Double,
    val total: Double,
)

/** La cobertura de "Tu casa", si el backend la ofrece para esta cotizacion. */
val Cotizacion.tuCasa: PrecioDeCobertura?
    get() = coberturas.find { it.codigo == CodigoCobertura.HOGAR }

/**
 * Suma las coberturas que el cliente marco (BITS-96, AC-2): la app no calcula
 * precios, solo suma los que manda el backend. La vida va siempre. Si el
 * backend no trae el desglose, la vida es la prima mensual de la cotizacion.
 */
fun Cotizacion.plan(conTuCasa: Boolean): PlanElegido {
    val vida = coberturas.find { it.codigo == CodigoCobertura.VIDA }
        ?: PrecioDeCobertura(CodigoCobertura.VIDA, sumaAsegurada, primaMensual, 0.0, primaMensual)
    val casa = tuCasa?.takeIf { conTuCasa }
    return PlanElegido(
        vida = vida.primaNeta,
        tuCasa = casa?.primaNeta,
        subtotal = vida.primaNeta + (casa?.primaNeta ?: 0.0),
        iva = vida.iva + (casa?.iva ?: 0.0),
        total = vida.primaTotal + (casa?.primaTotal ?: 0.0),
    )
}
