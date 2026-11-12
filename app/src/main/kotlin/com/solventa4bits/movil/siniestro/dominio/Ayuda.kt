package com.solventa4bits.movil.siniestro.dominio

/** Un tipo de ayuda en sitio: plomeria, electricidad, cerrajeria... (BITS-239, AC-2). */
data class TipoDeAyuda(val id: String, val nombre: String, val orden: Int)

/** Una localidad, con su punto central para medir distancias cuando el cliente escribe la direccion. */
data class Localidad(
    val id: String,
    val nombre: String,
    val ciudad: String,
    val latitud: Double,
    val longitud: Double,
) {
    val centro: Ubicacion
        get() = Ubicacion(latitud, longitud)
}

/** Un prestador aliado. [tipos] son ids de [TipoDeAyuda]; [localidad], el id de su [Localidad]. */
data class Prestador(
    val id: String,
    val nombre: String,
    val tipos: List<String>,
    val localidad: String,
    val direccion: String,
    val latitud: Double,
    val longitud: Double,
    val tiempoLlegadaMin: Int,
)

data class PrestadorCercano(val prestador: Prestador, val km: Double)

/** Todo el catalogo de siniestros: la ayuda en sitio (BITS-239) y las oficinas del cheque (BITS-116). */
data class CatalogoDeSiniestros(
    val tiposAyuda: List<TipoDeAyuda> = emptyList(),
    val localidades: List<Localidad> = emptyList(),
    val prestadores: List<Prestador> = emptyList(),
    val oficinas: List<Oficina> = emptyList(),
) {
    /** AC-2: en el orden del catalogo. */
    val tiposEnOrden: List<TipoDeAyuda>
        get() = tiposAyuda.sortedBy { it.orden }

    /** AC-3 y AC-4: solo se atiende en Bogota. */
    val localidadesDeBogota: List<Localidad>
        get() = localidades.filter { it.ciudad == CIUDAD_ATENDIDA }

    /** AC-5: los prestadores de [tipo], del mas cercano al mas lejano de [punto]. */
    fun prestadoresCerca(tipo: String, punto: Ubicacion): List<PrestadorCercano> = prestadores
        .filter { tipo in it.tipos }
        .map { PrestadorCercano(it, punto.kmHasta(Ubicacion(it.latitud, it.longitud))) }
        .sortedBy { it.km }

    private companion object {
        const val CIUDAD_ATENDIDA = "Bogotá"
    }
}

/** A donde va la ayuda: donde se tomaron las fotos, donde esta el telefono o una direccion escrita. */
enum class OrigenDeAyuda { FOTOS, TELEFONO, DIRECCION }

/**
 * El punto al que va la ayuda (AC-3 y AC-4). [ubicacion] es desde donde se miden las distancias:
 * con una direccion escrita es el centro de su localidad, porque la app no ubica direcciones.
 */
sealed interface PuntoDeAyuda {
    val origen: OrigenDeAyuda
    val ubicacion: Ubicacion

    data class DondeTomoLasFotos(override val ubicacion: Ubicacion) : PuntoDeAyuda {
        override val origen = OrigenDeAyuda.FOTOS
    }

    data class DondeEstaElTelefono(override val ubicacion: Ubicacion) : PuntoDeAyuda {
        override val origen = OrigenDeAyuda.TELEFONO
    }

    data class EnUnaDireccion(val direccion: String, val localidad: Localidad) : PuntoDeAyuda {
        override val origen = OrigenDeAyuda.DIRECCION
        override val ubicacion: Ubicacion
            get() = localidad.centro
    }
}

/** Lo que la app manda al pedir la ayuda (AC-6). */
data class SolicitudDeAyuda(val tipo: TipoDeAyuda, val prestador: Prestador, val punto: PuntoDeAyuda)

/** Una ayuda ya pedida, como la guarda el siniestro (AC-6 y AC-7). [tipo] es el id del tipo de ayuda. */
data class Asistencia(
    val id: String,
    val numero: String,
    val tipo: String,
    val prestador: String,
    val tiempoLlegadaMin: Int,
    val origen: OrigenDeAyuda?,
    val direccion: String?,
    val localidad: String?,
    val solicitadaEn: String,
)

sealed interface ResultadoAyuda {
    data class Pedida(val asistencia: Asistencia) : ResultadoAyuda

    /** El siniestro no es de incendio o terremoto, o ya esta Pagado o Rechazado. */
    data object NoSePuede : ResultadoAyuda

    data object SesionVencida : ResultadoAyuda

    /** No llego al servidor o el servidor fallo. El cliente reintenta con la misma llave. */
    data object Fallo : ResultadoAyuda
}

/** AC-1: solo incendio o terremoto, En estudio o en Falta informacion. */
val DetalleDeSiniestro.puedePedirAyuda: Boolean
    get() = resumen.tipo == TipoDeSiniestro.INCENDIO_TERREMOTO && resumen.estado?.enCurso == true
