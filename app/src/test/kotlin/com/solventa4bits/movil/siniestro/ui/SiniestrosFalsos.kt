package com.solventa4bits.movil.siniestro.ui

import com.solventa4bits.movil.siniestro.dominio.ArchivoDelReporte
import com.solventa4bits.movil.siniestro.dominio.ReporteDeSiniestro
import com.solventa4bits.movil.siniestro.dominio.ResultadoAyuda
import com.solventa4bits.movil.siniestro.dominio.ResultadoCatalogo
import com.solventa4bits.movil.siniestro.dominio.SolicitudDeAyuda
import com.solventa4bits.movil.siniestro.dominio.ResultadoDetalle
import com.solventa4bits.movil.siniestro.dominio.ResultadoEntrega
import com.solventa4bits.movil.siniestro.dominio.ResultadoReporte
import com.solventa4bits.movil.siniestro.dominio.ResultadoSiniestros
import com.solventa4bits.movil.siniestro.dominio.ServicioDeSiniestros

/** Doble de prueba: responde lo que la prueba le diga y anota lo que le pidieron. */
class SiniestrosFalsos(
    var alReportar: ResultadoReporte = ResultadoReporte.Fallo,
    var alListar: ResultadoSiniestros = ResultadoSiniestros.Exito(emptyList()),
    var alConsultar: ResultadoDetalle = ResultadoDetalle.Fallo,
) : ServicioDeSiniestros {
    val reportes = mutableListOf<Pair<ReporteDeSiniestro, String>>()
    val consultados = mutableListOf<String>()

    override suspend fun reportar(reporte: ReporteDeSiniestro, llave: String): ResultadoReporte {
        reportes += reporte to llave
        return alReportar
    }

    override suspend fun listar() = alListar

    override suspend fun consultar(id: String): ResultadoDetalle {
        consultados += id
        return alConsultar
    }

    var alPedirCatalogo: ResultadoCatalogo = ResultadoCatalogo.Fallo
    var catalogosPedidos = 0

    override suspend fun catalogo(): ResultadoCatalogo {
        catalogosPedidos++
        return alPedirCatalogo
    }

    var alPedirAyuda: ResultadoAyuda = ResultadoAyuda.Fallo
    val ayudas = mutableListOf<Triple<String, SolicitudDeAyuda, String>>()

    override suspend fun pedirAyuda(siniestroId: String, solicitud: SolicitudDeAyuda, llave: String): ResultadoAyuda {
        ayudas += Triple(siniestroId, solicitud, llave)
        return alPedirAyuda
    }

    var alEntregar: ResultadoEntrega = ResultadoEntrega.Fallo
    val entregas = mutableListOf<Triple<String, List<ArchivoDelReporte>, String>>()

    override suspend fun entregar(id: String, archivos: List<ArchivoDelReporte>, llave: String): ResultadoEntrega {
        entregas += Triple(id, archivos, llave)
        return alEntregar
    }
}
