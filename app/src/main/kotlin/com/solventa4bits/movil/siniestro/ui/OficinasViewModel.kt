package com.solventa4bits.movil.siniestro.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solventa4bits.movil.siniestro.dominio.CatalogoDeSiniestros
import com.solventa4bits.movil.siniestro.dominio.FuenteDeUbicacion
import com.solventa4bits.movil.siniestro.dominio.Oficina
import com.solventa4bits.movil.siniestro.dominio.OficinaCercana
import com.solventa4bits.movil.siniestro.dominio.ResultadoCatalogo
import com.solventa4bits.movil.siniestro.dominio.ServicioDeSiniestros
import com.solventa4bits.movil.siniestro.dominio.Ubicacion
import com.solventa4bits.movil.siniestro.dominio.ciudadesDe
import com.solventa4bits.movil.siniestro.dominio.paraElegir
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

/** Lo que el cliente hace en "Oficina del cheque" (M06A). */
class AccionesDeLaOficina(
    val alElegir: (Oficina) -> Unit,
    val alElegirCiudad: (String) -> Unit,
    /** Dio el permiso de ubicacion: se vuelve a cargar para ordenar por distancia. */
    val alRecargar: () -> Unit,
    val alVolver: () -> Unit,
)

data class EstadoOficinas(
    val oficinas: List<Oficina> = emptyList(),
    /** Donde esta el cliente. Null sin permiso o si no se supo a tiempo: entonces elige la ciudad. */
    val ubicacion: Ubicacion? = null,
    /** Sin ubicacion, la ciudad que eligio el cliente. */
    val ciudad: String? = null,
    val cargando: Boolean = false,
    val fallo: Boolean = false,
    val sesionVencida: Boolean = false,
) {
    fun paraElegir(banco: String): List<OficinaCercana> = oficinas.paraElegir(banco, ubicacion, ciudad)

    fun ciudades(banco: String): List<String> = oficinas.ciudadesDe(banco)
}

/** M06A: las oficinas donde se reclama el cheque y donde esta el cliente (BITS-116, AC-4). */
class OficinasViewModel(
    private val servicio: ServicioDeSiniestros,
    private val ubicacion: FuenteDeUbicacion,
) : ViewModel() {

    var estado by mutableStateOf(EstadoOficinas())
        private set

    /**
     * Cada vez que se abre M06A. Las oficinas se traen una sola vez; la ubicacion, cada vez, porque
     * el cliente se mueve o pudo dar el permiso despues.
     */
    fun cargar() {
        estado = estado.copy(cargando = true, fallo = false, sesionVencida = false)
        viewModelScope.launch {
            val donde = async { ubicacion.actual() }
            val catalogo = estado.oficinas.takeIf { it.isNotEmpty() }
                ?.let { ResultadoCatalogo.Exito(CatalogoDeSiniestros(oficinas = it)) }
                ?: servicio.catalogo()
            estado = when (catalogo) {
                is ResultadoCatalogo.Exito ->
                    estado.copy(oficinas = catalogo.catalogo.oficinas, ubicacion = donde.await(), cargando = false)
                ResultadoCatalogo.SesionVencida -> estado.copy(cargando = false, sesionVencida = true)
                ResultadoCatalogo.Fallo -> estado.copy(cargando = false, fallo = true)
            }
        }
    }

    fun elegirCiudad(ciudad: String) {
        estado = estado.copy(ciudad = ciudad)
    }
}
