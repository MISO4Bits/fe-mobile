package com.solventa4bits.movil.cotizacion.ui

import com.solventa4bits.movil.cotizacion.dominio.Cotizacion

/**
 * Que version de "Lo que pagas" ve el cliente, segun de donde salen los datos
 * del credito y si el precio ya aplica su perfil de riesgo (Figma 05, 05E,
 * 05F y 05G).
 */
enum class VarianteDeCotizacion {
    /** Datos del banco y precio con perfil: no hay nada pendiente. */
    PREAPROBADA,

    /** Datos declarados y precio con perfil: falta confirmar el credito con el banco (05F). */
    DECLARADA_CON_PERFIL,

    /** Datos declarados y tarifa estandar: falta el credito y el perfil (05E). */
    DECLARADA_SIN_PERFIL,

    /** Datos del banco y tarifa estandar: falta que termine el perfil (05G). */
    VERIFICADA_SIN_PERFIL,

    /**
     * El cliente no autorizo la consulta financiera: el perfil nunca llega y
     * el precio se queda en la tarifa estandar (05F sin autorizacion).
     */
    SIN_AUTORIZACION,
}

/**
 * [sinAutorizacion] no viene en la cotizacion: la app lo sabe porque la
 * consulta de creditos respondio SIN_CONSENTIMIENTO.
 */
fun Cotizacion.variante(sinAutorizacion: Boolean = false): VarianteDeCotizacion = when {
    // Sin autorizacion no hay perfil; si aun asi llega personalizada, manda la cotizacion.
    sinAutorizacion && !personalizada -> VarianteDeCotizacion.SIN_AUTORIZACION
    datosDeclarados && personalizada -> VarianteDeCotizacion.DECLARADA_CON_PERFIL
    datosDeclarados -> VarianteDeCotizacion.DECLARADA_SIN_PERFIL
    personalizada -> VarianteDeCotizacion.PREAPROBADA
    else -> VarianteDeCotizacion.VERIFICADA_SIN_PERFIL
}
