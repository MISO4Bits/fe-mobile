package com.solventa4bits.movil.siniestro.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solventa4bits.movil.siniestro.dominio.ArchivoDelReporte
import com.solventa4bits.movil.siniestro.dominio.ClaseDeArchivo
import com.solventa4bits.movil.siniestro.dominio.DestinoDePago
import com.solventa4bits.movil.siniestro.dominio.DetalleDeSiniestro
import com.solventa4bits.movil.siniestro.dominio.FuenteDeUbicacion
import com.solventa4bits.movil.siniestro.dominio.ProblemaDeFecha
import com.solventa4bits.movil.siniestro.dominio.ReporteDeSiniestro
import com.solventa4bits.movil.siniestro.dominio.ResultadoEntrega
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

/** Lo que salio mal al enviar, para decirselo al cliente en "Revisa y envia". */
enum class ProblemaAlEnviar { FALLO, FUERA_DE_VIGENCIA, EN_CURSO }

/** Lo que el cliente va escribiendo de su cuenta, aunque todavia este incompleto. */
data class DatosDePago(val banco: String = "", val tipoCuenta: TipoDeCuenta? = null, val numeroCuenta: String = "") {
    val destino: DestinoDePago?
        get() = DestinoDePago.desde(banco, tipoCuenta, numeroCuenta)
}

/** El siniestro en Falta informacion al que se le envia lo que Solventa pidio (BITS-118, AC-4). */
data class LoQueFalta(val id: String, val numero: String, val pedido: String)

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
    /** Si no es null, no es un reporte nuevo: es lo que falta de ese siniestro. */
    val entrega: LoQueFalta? = null,
    /** Lo que falta llego: el siniestro, ya En estudio. */
    val recibida: DetalleDeSiniestro? = null,
    /** El siniestro ya no esta en Falta informacion. */
    val yaNoSePide: Boolean = false,
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

    /** En Vida, el dictamen. En Tu casa, lo que falta va en fotos o en documentos. */
    val listoParaEnviar: Boolean
        get() = when {
            !tipo.deTuCasa -> puedeContinuar
            entrega != null -> archivos.isNotEmpty() && !preparando
            else -> fotosListas && relatoListo && pago.destino != null
        }

    val archivos: List<ArchivoDelReporte>
        get() = if (tipo.deTuCasa) fotos + documentos else dictamen + historia

    /** El reporte nuevo que se envia. En Vida no van lo que paso ni la cuenta: se le paga al banco. */
    val reporte: ReporteDeSiniestro?
        get() = poliza?.let {
            ReporteDeSiniestro(
                polizaId = it.id,
                tipo = tipo,
                fechaOcurrencia = fecha,
                narracion = narracion.trim().takeIf { tipo.deTuCasa },
                archivos = archivos,
                destinoPago = pago.destino.takeIf { tipo.deTuCasa },
            )
        }

    companion object {
        private const val MAXIMO_DE_ARCHIVOS = 10

        /** Como en el Figma M05. El contrato admite mas, pero el cliente no necesita mas. */
        const val LARGO_DE_LA_NARRACION = 500
    }
}

/**
 * El reporte que el cliente va armando (BITS-111): incapacidad total en Vida, incendio o terremoto
 * en Tu casa. Tambien lo que falta de un siniestro (BITS-118, AC-4), con las mismas fotos y
 * documentos. La llave se crea al empezar y se repite en cada reintento, para que el servidor
 * guarde uno solo (AC-7).
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

    /** Empieza a armar lo que Solventa pidio en ese siniestro. */
    fun empezarEntrega(detalle: DetalleDeSiniestro) {
        val siniestro = detalle.resumen
        val tipo = siniestro.tipo ?: return
        llave = llaveNueva()
        estado = EstadoReporte(
            tipo = tipo,
            entrega = LoQueFalta(siniestro.id, siniestro.numero, detalle.evidenciaPedida?.detalle.orEmpty()),
        )
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
        if (!estado.listoParaEnviar || estado.enviando) return
        val entrega = estado.entrega
        val reporte = estado.reporte
        val archivos = estado.archivos
        estado = estado.copy(enviando = true, problemaAlEnviar = null)
        viewModelScope.launch {
            estado = when {
                entrega != null -> estado.con(servicio.entregar(entrega.id, archivos, llave))
                reporte != null -> estado.con(servicio.reportar(reporte, llave))
                else -> estado.copy(enviando = false)
            }
        }
    }

    private companion object {
        val zonaDeColombia: ZoneId = ZoneId.of("America/Bogota")
    }
}

private fun EstadoReporte.con(resultado: ResultadoReporte): EstadoReporte = when (resultado) {
    is ResultadoReporte.Radicado -> copy(enviando = false, radicado = resultado)
    ResultadoReporte.FueraDeVigencia -> copy(enviando = false, problemaAlEnviar = ProblemaAlEnviar.FUERA_DE_VIGENCIA)
    ResultadoReporte.SiniestroEnCurso -> copy(enviando = false, problemaAlEnviar = ProblemaAlEnviar.EN_CURSO)
    ResultadoReporte.SesionVencida -> copy(enviando = false, sesionVencida = true)
    ResultadoReporte.TipoNoCubierto, ResultadoReporte.Fallo ->
        copy(enviando = false, problemaAlEnviar = ProblemaAlEnviar.FALLO)
}

private fun EstadoReporte.con(resultado: ResultadoEntrega): EstadoReporte = when (resultado) {
    is ResultadoEntrega.Recibida -> copy(enviando = false, recibida = resultado.detalle)
    ResultadoEntrega.YaNoSePide -> copy(enviando = false, yaNoSePide = true)
    ResultadoEntrega.SesionVencida -> copy(enviando = false, sesionVencida = true)
    ResultadoEntrega.Fallo -> copy(enviando = false, problemaAlEnviar = ProblemaAlEnviar.FALLO)
}
