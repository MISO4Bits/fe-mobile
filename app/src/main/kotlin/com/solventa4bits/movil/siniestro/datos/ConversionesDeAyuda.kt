package com.solventa4bits.movil.siniestro.datos

import com.solventa4bits.movil.siniestro.dominio.Asistencia
import com.solventa4bits.movil.siniestro.dominio.CatalogoDeSiniestros
import com.solventa4bits.movil.siniestro.dominio.Localidad
import com.solventa4bits.movil.siniestro.dominio.Oficina
import com.solventa4bits.movil.siniestro.dominio.OrigenDeAyuda
import com.solventa4bits.movil.siniestro.dominio.Prestador
import com.solventa4bits.movil.siniestro.dominio.PuntoDeAyuda
import com.solventa4bits.movil.siniestro.dominio.SolicitudDeAyuda
import com.solventa4bits.movil.siniestro.dominio.TipoDeAyuda

/* El catalogo y la ayuda en sitio, del contrato del BFF al dominio y de vuelta. */

internal fun CatalogoSiniestrosDto.aDominio() = CatalogoDeSiniestros(
    tiposAyuda = tiposAyuda.map { TipoDeAyuda(it.id, it.nombre, it.orden) },
    localidades = localidades.map { Localidad(it.id, it.nombre, it.ciudad, it.latitud, it.longitud) },
    prestadores = prestadores.map {
        Prestador(it.id, it.nombre, it.tipos, it.localidad, it.direccion, it.latitud, it.longitud, it.tiempoLlegadaMin)
    },
    oficinas = oficinas.map { Oficina(it.id, it.banco, it.nombre, it.direccion, it.ciudad, it.latitud, it.longitud) },
)

internal fun SolicitudDeAyuda.aDto(): PedirAyudaDto {
    val delPrestador =
        PrestadorAsistenciaDto(prestador.id, prestador.nombre, prestador.direccion, prestador.tiempoLlegadaMin)
    val base = PedirAyudaDto(tipo = tipo.id, prestador = delPrestador, origen = punto.origen.name)
    return when (val donde = punto) {
        is PuntoDeAyuda.EnUnaDireccion -> base.copy(direccion = donde.direccion, localidad = donde.localidad.id)
        else -> base.copy(latitud = donde.ubicacion.latitud, longitud = donde.ubicacion.longitud)
    }
}

/** Un origen que la app no conoce queda en null. */
internal fun AsistenciaDto.aDominio() = Asistencia(
    id = id,
    numero = numero,
    tipo = tipo,
    prestador = prestador.nombre,
    tiempoLlegadaMin = tiempoLlegadaMin,
    origen = OrigenDeAyuda.entries.find { it.name == origen },
    direccion = direccion,
    localidad = localidad,
    solicitadaEn = solicitadaEn,
)
