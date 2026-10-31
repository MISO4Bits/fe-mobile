package com.solventa4bits.movil.avisos.dominio

import com.solventa4bits.movil.solicitud.dominio.EstadoDeSolicitud
import com.solventa4bits.movil.solicitud.dominio.Solicitud

/**
 * Spike BITS-352. Lo que se recuerda de cada solicitud para saber si cambio:
 * su estado y los pendientes que le quedan. Asi tambien se nota cuando un
 * agente aprueba un pendiente aunque la solicitud siga pendiente.
 */
fun huella(solicitud: Solicitud): String =
    solicitud.estado.name + ":" +
        solicitud.pendientes.map { it.codigo?.name ?: it.mensaje }.sorted().joinToString(",")

/**
 * Compara lo que el BFF devuelve ahora con lo ultimo que vio la app. Es
 * novedad la solicitud conocida que cambio, o la nueva que ya no esta
 * pendiente. Sin foto anterior no hay con que comparar: la primera consulta
 * no avisa.
 */
fun novedades(antes: Map<String, String>?, ahora: List<Solicitud>): List<Solicitud> {
    if (antes == null) return emptyList()
    return ahora.filter { solicitud ->
        val anterior = antes[solicitud.id]
        if (anterior == null) solicitud.estado != EstadoDeSolicitud.PENDIENTE else anterior != huella(solicitud)
    }
}
