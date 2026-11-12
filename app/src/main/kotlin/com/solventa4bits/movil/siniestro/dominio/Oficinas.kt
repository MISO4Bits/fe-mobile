package com.solventa4bits.movil.siniestro.dominio

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** Una oficina de BBVA o Bancolombia donde se reclama el cheque (BITS-116, AC-4). */
data class Oficina(
    val id: String,
    val banco: String,
    val nombre: String,
    val direccion: String,
    val ciudad: String,
    val latitud: Double,
    val longitud: Double,
)

/** Los bancos donde se puede reclamar el cheque, como vienen en el catalogo. */
val BANCOS_DEL_CHEQUE = listOf("BBVA", "Bancolombia")

/** Una oficina y que tan lejos queda del cliente. [km] es null si no se sabe donde esta el cliente. */
data class OficinaCercana(val oficina: Oficina, val km: Double?)

sealed interface ResultadoCatalogo {
    data class Exito(val oficinas: List<Oficina>) : ResultadoCatalogo

    data object SesionVencida : ResultadoCatalogo

    data object Fallo : ResultadoCatalogo
}

private const val RADIO_DE_LA_TIERRA_KM = 6371.0

/** La distancia en linea recta, en km. Se calcula en el telefono: la ubicacion no sale de el. */
fun Oficina.kmHasta(ubicacion: Ubicacion): Double {
    val lat1 = Math.toRadians(ubicacion.latitud)
    val lat2 = Math.toRadians(latitud)
    val dLat = lat2 - lat1
    val dLon = Math.toRadians(longitud - ubicacion.longitud)
    val a = sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2)
    return 2 * RADIO_DE_LA_TIERRA_KM * asin(sqrt(a))
}

/**
 * Las oficinas de [banco] que se le ofrecen al cliente (AC-4). Con su ubicacion, todas, de la mas
 * cercana a la mas lejana. Sin ella, las de [ciudad], por nombre.
 */
fun List<Oficina>.paraElegir(banco: String, ubicacion: Ubicacion?, ciudad: String?): List<OficinaCercana> {
    val delBanco = filter { it.banco == banco }
    return if (ubicacion != null) {
        delBanco.map { OficinaCercana(it, it.kmHasta(ubicacion)) }.sortedBy { it.km }
    } else {
        delBanco.filter { it.ciudad == ciudad }.sortedBy { it.nombre }.map { OficinaCercana(it, null) }
    }
}

/** Las ciudades donde [banco] tiene oficinas, en orden alfabetico. */
fun List<Oficina>.ciudadesDe(banco: String): List<String> =
    filter { it.banco == banco }.map { it.ciudad }.distinct().sorted()
