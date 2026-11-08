package com.solventa4bits.movil.siniestro.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solventa4bits.movil.siniestro.dominio.ArchivoDelReporte
import com.solventa4bits.movil.siniestro.dominio.ClaseDeArchivo
import com.solventa4bits.movil.siniestro.dominio.ProblemaDeFecha
import com.solventa4bits.movil.siniestro.dominio.ReporteDeSiniestro
import com.solventa4bits.movil.siniestro.dominio.ResultadoReporte
import com.solventa4bits.movil.siniestro.dominio.ServicioDeSiniestros
import com.solventa4bits.movil.siniestro.dominio.TipoDeSiniestro
import com.solventa4bits.movil.siniestro.dominio.problemaConLaFecha
import com.solventa4bits.movil.solicitud.dominio.Solicitud
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

/** Lo que se escanea en el camino de Vida: el dictamen es obligatorio y la historia clinica, opcional. */
enum class Escaneo { DICTAMEN, HISTORIA_CLINICA }

/** Lo que el cliente hace en "Tu dictamen". */
class AccionesDelDictamen(
    val alCambiarFecha: (LocalDate) -> Unit,
    val alEscanear: (Escaneo) -> Unit,
    val alEscanearOtraVez: () -> Unit,
    val alQuitar: (Escaneo) -> Unit,
    val alContinuar: () -> Unit,
    val alVolver: () -> Unit,
)

/** Lo que salio mal al enviar, para decirselo al cliente en "Revisa y envia". */
enum class ProblemaAlEnviar { FALLO, FUERA_DE_VIGENCIA, EN_CURSO }

data class EstadoReporte(
    val poliza: Solicitud? = null,
    val hoy: LocalDate = LocalDate.MIN,
    /** Por defecto, hoy (AC-1). */
    val fecha: LocalDate = LocalDate.MIN,
    val dictamen: List<ArchivoDelReporte> = emptyList(),
    val historia: List<ArchivoDelReporte> = emptyList(),
    val preparando: Boolean = false,
    val falloAlPreparar: Boolean = false,
    val enviando: Boolean = false,
    val problemaAlEnviar: ProblemaAlEnviar? = null,
    val radicado: ResultadoReporte.Radicado? = null,
    val sesionVencida: Boolean = false,
) {
    /** AC-2: el error que se muestra en el campo de la fecha. */
    val problemaConLaFecha: ProblemaDeFecha?
        get() = poliza?.problemaConLaFecha(fecha, hoy)

    /** El servidor recibe hasta 10 archivos por reporte. */
    val puedeEscanear: Boolean
        get() = !preparando && dictamen.size + historia.size < MAXIMO_DE_ARCHIVOS

    /** AC-3: sin el dictamen no se sigue. */
    val puedeContinuar: Boolean
        get() = problemaConLaFecha == null && dictamen.isNotEmpty() && !preparando

    private companion object {
        const val MAXIMO_DE_ARCHIVOS = 10
    }
}

/**
 * El reporte de incapacidad total que el cliente va armando (BITS-111, camino de Vida). La llave
 * del reporte se crea al empezar y se repite en cada reintento, para que el servidor cree uno solo.
 */
class ReporteViewModel(
    private val servicio: ServicioDeSiniestros,
    private val preparar: suspend (ByteArray) -> ByteArray?,
    private val ahora: () -> Instant = Instant::now,
    private val llaveNueva: () -> String = { UUID.randomUUID().toString() },
) : ViewModel() {

    var estado by mutableStateOf(EstadoReporte())
        private set

    private var llave = ""

    fun empezar(poliza: Solicitud) {
        val hoy = ahora().atZone(zonaDeColombia).toLocalDate()
        llave = llaveNueva()
        estado = EstadoReporte(poliza = poliza, hoy = hoy, fecha = hoy)
    }

    fun cambiarFecha(fecha: LocalDate) {
        estado = estado.copy(fecha = fecha)
    }

    /** Una foto mas de la camara es una pagina mas de lo que se esta escaneando. */
    fun agregarPagina(escaneo: Escaneo, foto: ByteArray) {
        val capturadoEn = ahora().toString()
        estado = estado.copy(preparando = true, falloAlPreparar = false)
        viewModelScope.launch {
            val jpeg = preparar(foto)
            estado = if (jpeg == null) {
                estado.copy(preparando = false, falloAlPreparar = true)
            } else {
                val pagina = ArchivoDelReporte(ClaseDeArchivo.DOCUMENTO, jpeg, capturadoEn)
                when (escaneo) {
                    Escaneo.DICTAMEN -> estado.copy(preparando = false, dictamen = estado.dictamen + pagina)
                    Escaneo.HISTORIA_CLINICA -> estado.copy(preparando = false, historia = estado.historia + pagina)
                }
            }
        }
    }

    /** Quita todas las paginas de ese documento (AC-6). */
    fun quitar(escaneo: Escaneo) {
        estado = when (escaneo) {
            Escaneo.DICTAMEN -> estado.copy(dictamen = emptyList())
            Escaneo.HISTORIA_CLINICA -> estado.copy(historia = emptyList())
        }
    }

    fun enviar() {
        val poliza = estado.poliza ?: return
        if (!estado.puedeContinuar || estado.enviando) return
        val reporte = ReporteDeSiniestro(
            polizaId = poliza.id,
            tipo = TipoDeSiniestro.INCAPACIDAD_TOTAL,
            fechaOcurrencia = estado.fecha,
            narracion = null,
            archivos = estado.dictamen + estado.historia,
        )
        estado = estado.copy(enviando = true, problemaAlEnviar = null)
        viewModelScope.launch {
            estado = when (val resultado = servicio.reportar(reporte, llave)) {
                is ResultadoReporte.Radicado -> estado.copy(enviando = false, radicado = resultado)
                ResultadoReporte.FueraDeVigencia -> conProblema(ProblemaAlEnviar.FUERA_DE_VIGENCIA)
                ResultadoReporte.SiniestroEnCurso -> conProblema(ProblemaAlEnviar.EN_CURSO)
                ResultadoReporte.SesionVencida -> estado.copy(enviando = false, sesionVencida = true)
                ResultadoReporte.TipoNoCubierto, ResultadoReporte.Fallo -> conProblema(ProblemaAlEnviar.FALLO)
            }
        }
    }

    private fun conProblema(problema: ProblemaAlEnviar) = estado.copy(enviando = false, problemaAlEnviar = problema)

    private companion object {
        val zonaDeColombia: ZoneId = ZoneId.of("America/Bogota")
    }
}
