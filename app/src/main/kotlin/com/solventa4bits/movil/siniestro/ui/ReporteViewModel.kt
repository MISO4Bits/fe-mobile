package com.solventa4bits.movil.siniestro.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solventa4bits.movil.siniestro.dominio.ArchivoDelReporte
import com.solventa4bits.movil.siniestro.dominio.ClaseDeArchivo
import com.solventa4bits.movil.siniestro.dominio.DestinoDePago
import com.solventa4bits.movil.siniestro.dominio.FuenteDeUbicacion
import com.solventa4bits.movil.siniestro.dominio.ProblemaDeFecha
import com.solventa4bits.movil.siniestro.dominio.ReporteDeSiniestro
import com.solventa4bits.movil.siniestro.dominio.ResultadoReporte
import com.solventa4bits.movil.siniestro.dominio.ServicioDeSiniestros
import com.solventa4bits.movil.siniestro.dominio.TipoDeCuenta
import com.solventa4bits.movil.siniestro.dominio.TipoDeSiniestro
import com.solventa4bits.movil.siniestro.dominio.problemaConLaFecha
import com.solventa4bits.movil.solicitud.dominio.Solicitud
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

/**
 * Lo que se toma con la camara. En Vida, el dictamen (obligatorio) y la historia clinica; en Tu
 * casa, las fotos del dano (al menos una) y los documentos. Cada foto es una pagina.
 */
enum class Escaneo(val clase: ClaseDeArchivo) {
    DICTAMEN(ClaseDeArchivo.DOCUMENTO),
    HISTORIA_CLINICA(ClaseDeArchivo.DOCUMENTO),
    FOTO_DEL_DANO(ClaseDeArchivo.FOTO),
    DOCUMENTO(ClaseDeArchivo.DOCUMENTO),
}

/** Lo que el cliente hace en "Tu dictamen". */
class AccionesDelDictamen(
    val alCambiarFecha: (LocalDate) -> Unit,
    val alEscanear: (Escaneo) -> Unit,
    val alEscanearOtraVez: () -> Unit,
    val alQuitar: (Escaneo) -> Unit,
    val alContinuar: () -> Unit,
    val alVolver: () -> Unit,
)

/** Lo que el cliente puede hacer en "Revisa y envia". */
class AccionesDeRevision(
    val alQuitar: (Escaneo) -> Unit,
    val alQuitarUno: (Escaneo, Int) -> Unit,
    val alEnviar: () -> Unit,
    val alVolver: () -> Unit,
)

/** Lo que salio mal al enviar, para decirselo al cliente en "Revisa y envia". */
enum class ProblemaAlEnviar { FALLO, FUERA_DE_VIGENCIA, EN_CURSO }

/** Lo que el cliente va escribiendo de su cuenta, aunque todavia este incompleto. */
data class DatosDePago(val banco: String = "", val tipoCuenta: TipoDeCuenta? = null, val numeroCuenta: String = "") {
    val destino: DestinoDePago?
        get() = DestinoDePago.desde(banco, tipoCuenta, numeroCuenta)
}

data class EstadoReporte(
    val poliza: Solicitud? = null,
    val tipo: TipoDeSiniestro = TipoDeSiniestro.INCAPACIDAD_TOTAL,
    val hoy: LocalDate = LocalDate.MIN,
    /** Por defecto, hoy (AC-1). */
    val fecha: LocalDate = LocalDate.MIN,
    val escaneados: Map<Escaneo, List<ArchivoDelReporte>> = emptyMap(),
    /** Lo que paso, con las palabras del cliente. Solo en Tu casa. */
    val narracion: String = "",
    val pago: DatosDePago = DatosDePago(),
    /** Si las fotos llevan ubicacion. Null mientras el cliente no ha decidido (M04A). */
    val usarUbicacion: Boolean? = null,
    val preparando: Boolean = false,
    val falloAlPreparar: Boolean = false,
    val enviando: Boolean = false,
    val problemaAlEnviar: ProblemaAlEnviar? = null,
    val radicado: ResultadoReporte.Radicado? = null,
    val sesionVencida: Boolean = false,
) {
    fun de(escaneo: Escaneo): List<ArchivoDelReporte> = escaneados[escaneo].orEmpty()

    val dictamen: List<ArchivoDelReporte>
        get() = de(Escaneo.DICTAMEN)

    val historia: List<ArchivoDelReporte>
        get() = de(Escaneo.HISTORIA_CLINICA)

    val fotos: List<ArchivoDelReporte>
        get() = de(Escaneo.FOTO_DEL_DANO)

    val documentos: List<ArchivoDelReporte>
        get() = de(Escaneo.DOCUMENTO)

    /** AC-2: el error que se muestra en el campo de la fecha. */
    val problemaConLaFecha: ProblemaDeFecha?
        get() = poliza?.problemaConLaFecha(fecha, hoy)

    /** El servidor recibe hasta 10 archivos por reporte. */
    val puedeEscanear: Boolean
        get() = !preparando && escaneados.values.sumOf { it.size } < MAXIMO_DE_ARCHIVOS

    /** AC-3: sin el dictamen no se sigue. */
    val puedeContinuar: Boolean
        get() = problemaConLaFecha == null && dictamen.isNotEmpty() && !preparando

    /** AC-4: al menos una foto del dano. */
    val fotosListas: Boolean
        get() = fotos.isNotEmpty() && !preparando

    val relatoListo: Boolean
        get() = problemaConLaFecha == null && narracion.isNotBlank() && !preparando

    val listoParaEnviar: Boolean
        get() = when (tipo) {
            TipoDeSiniestro.INCAPACIDAD_TOTAL -> puedeContinuar
            else -> fotosListas && relatoListo && pago.destino != null
        }

    companion object {
        private const val MAXIMO_DE_ARCHIVOS = 10

        /** Como en el Figma M05. El contrato admite mas, pero el cliente no necesita mas. */
        const val LARGO_DE_LA_NARRACION = 500
    }
}

/**
 * El reporte que el cliente va armando (BITS-111): incapacidad total en Vida, incendio o terremoto
 * en Tu casa. La llave del reporte se crea al empezar y se repite en cada reintento, para que el
 * servidor cree uno solo (AC-7).
 */
class ReporteViewModel(
    private val servicio: ServicioDeSiniestros,
    private val preparar: suspend (ByteArray) -> ByteArray?,
    private val ubicacion: FuenteDeUbicacion = FuenteDeUbicacion { null },
    private val ahora: () -> Instant = Instant::now,
    private val llaveNueva: () -> String = { UUID.randomUUID().toString() },
) : ViewModel() {

    var estado by mutableStateOf(EstadoReporte())
        private set

    private var llave = ""

    fun empezar(poliza: Solicitud, tipo: TipoDeSiniestro = TipoDeSiniestro.INCAPACIDAD_TOTAL) {
        val hoy = ahora().atZone(zonaDeColombia).toLocalDate()
        llave = llaveNueva()
        estado = EstadoReporte(poliza = poliza, tipo = tipo, hoy = hoy, fecha = hoy)
    }

    fun cambiarFecha(fecha: LocalDate) {
        estado = estado.copy(fecha = fecha)
    }

    fun cambiarNarracion(narracion: String) {
        estado = estado.copy(narracion = narracion.take(EstadoReporte.LARGO_DE_LA_NARRACION))
    }

    fun cambiarPago(pago: DatosDePago) {
        estado = estado.copy(pago = pago)
    }

    /** M04A: el cliente dio el permiso de ubicacion, o siguio sin el. */
    fun decidirUbicacion(usar: Boolean) {
        estado = estado.copy(usarUbicacion = usar)
    }

    /**
     * Una foto mas de la camara. Si es del dano y el cliente acepto, lleva donde y cuando se tomo;
     * si no, queda sin ubicacion y el reporte se envia igual (AC-4 y AC-5).
     */
    fun agregarPagina(escaneo: Escaneo, foto: ByteArray) {
        val capturadoEn = ahora().toString()
        val conUbicacion = escaneo == Escaneo.FOTO_DEL_DANO && estado.usarUbicacion == true
        estado = estado.copy(preparando = true, falloAlPreparar = false)
        viewModelScope.launch {
            val donde = if (conUbicacion) ubicacion.actual() else null
            val jpeg = preparar(foto)
            estado = if (jpeg == null) {
                estado.copy(preparando = false, falloAlPreparar = true)
            } else {
                val pagina = ArchivoDelReporte(escaneo.clase, jpeg, capturadoEn, donde?.latitud, donde?.longitud)
                val paginas = estado.de(escaneo) + pagina
                estado.copy(preparando = false, escaneados = estado.escaneados + (escaneo to paginas))
            }
        }
    }

    /** Quita todas las paginas de ese documento (AC-6). */
    fun quitar(escaneo: Escaneo) {
        estado = estado.copy(escaneados = estado.escaneados - escaneo)
    }

    /** Quita una sola foto o un solo documento (AC-6). */
    fun quitarUno(escaneo: Escaneo, indice: Int) {
        val quedan = estado.de(escaneo).filterIndexed { i, _ -> i != indice }
        estado = estado.copy(escaneados = estado.escaneados + (escaneo to quedan))
    }

    fun enviar() {
        val poliza = estado.poliza ?: return
        if (!estado.listoParaEnviar || estado.enviando) return
        val deTuCasa = estado.tipo != TipoDeSiniestro.INCAPACIDAD_TOTAL
        val reporte = ReporteDeSiniestro(
            polizaId = poliza.id,
            tipo = estado.tipo,
            fechaOcurrencia = estado.fecha,
            narracion = estado.narracion.trim().takeIf { deTuCasa },
            archivos = if (deTuCasa) estado.fotos + estado.documentos else estado.dictamen + estado.historia,
            destinoPago = estado.pago.destino.takeIf { deTuCasa },
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
