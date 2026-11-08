package com.solventa4bits.movil.seguro.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.solventa4bits.movil.identidad.ui.Camara
import com.solventa4bits.movil.identidad.ui.QueFoto
import com.solventa4bits.movil.red.GuardiaDeRed
import com.solventa4bits.movil.siniestro.ui.AccionesDelDictamen
import com.solventa4bits.movil.siniestro.ui.Escaneo
import com.solventa4bits.movil.siniestro.ui.MisSiniestrosViewModel
import com.solventa4bits.movil.siniestro.ui.PantallaReporteEnviado
import com.solventa4bits.movil.siniestro.ui.PantallaRevisaYEnvia
import com.solventa4bits.movil.siniestro.ui.PantallaTuDictamen
import com.solventa4bits.movil.siniestro.ui.PantallaTuSiniestro
import com.solventa4bits.movil.siniestro.ui.ReporteViewModel
import com.solventa4bits.movil.siniestro.ui.SiniestrosDelSeguro
import com.solventa4bits.movil.solicitud.dominio.Solicitud

/** Donde va el cliente dentro de "Mi seguro" (BITS-111). */
private enum class PasoEnMiSeguro {
    POLIZA,
    DICTAMEN,
    ESCANEANDO_DICTAMEN,
    ESCANEANDO_HISTORIA,
    REVISA,
    ENVIADO,
    SINIESTRO,
}

/** Las piezas que "Mi seguro" necesita para el camino del siniestro. */
class PiezasDelSiniestro(
    val misSiniestros: MisSiniestrosViewModel,
    val reporte: ReporteViewModel,
    val camara: Camara,
    val red: GuardiaDeRed,
)

/**
 * "Mi seguro": el detalle de la poliza con sus siniestros (M01) y, desde ahi, el camino para
 * reportar una incapacidad total (M03 a M09V). Sin conexion solo se ve la poliza guardada.
 */
@Composable
fun MiSeguro(
    seguro: SeguroViewModel,
    piezas: PiezasDelSiniestro,
    abrirEnlace: ((String) -> Unit)?,
    alInicio: () -> Unit,
    alCerrarSesion: () -> Unit,
    modifier: Modifier,
) {
    val misSiniestros = piezas.misSiniestros
    val reporte = piezas.reporte
    var paso by rememberSaveable { mutableStateOf(PasoEnMiSeguro.POLIZA) }
    val poliza = seguro.estado.seguro
    val enLaPoliza = paso == PasoEnMiSeguro.POLIZA
    val conConexion = seguro.estado.guardadoEn == null
    // Cada vez que se vuelve a la poliza la lista se trae de nuevo: un siniestro pudo cambiar (AC-2A).
    // Va antes de mirar la sesion: listar olvida una sesion vencida de antes.
    LaunchedEffect(poliza?.id, enLaPoliza) {
        if (enLaPoliza && poliza != null && conConexion) misSiniestros.listar()
    }
    // El reporte solo cuenta mientras se esta reportando: el de una sesion anterior ya se atendio.
    LaunchedEffect(misSiniestros.estado.sesionVencida, reporte.estado.sesionVencida) {
        if (misSiniestros.estado.sesionVencida || (!enLaPoliza && reporte.estado.sesionVencida)) alCerrarSesion()
    }
    LaunchedEffect(reporte.estado.radicado) {
        val radicado = reporte.estado.radicado
        if (radicado != null && paso == PasoEnMiSeguro.REVISA) {
            misSiniestros.abrir(radicado.id)
            paso = PasoEnMiSeguro.ENVIADO
        }
    }
    if (poliza == null || enLaPoliza) {
        TuSeguro(seguro, abrirEnlace, alInicio, alCerrarSesion, modifier) {
            if (poliza != null && conConexion) {
                SiniestrosDelSeguro(
                    poliza = poliza,
                    estado = misSiniestros.estado,
                    alReportarIncapacidad = {
                        piezas.red.conRed {
                            reporte.empezar(poliza)
                            paso = PasoEnMiSeguro.DICTAMEN
                        }
                    },
                    alVerSiniestro = {
                        piezas.red.conRed {
                            misSiniestros.abrir(it.id)
                            paso = PasoEnMiSeguro.SINIESTRO
                        }
                    },
                )
            }
        }
        return
    }
    CaminoDelSiniestro(paso, { paso = it }, poliza, piezas, alCerrarSesion, modifier)
}

/** Los pasos del reporte de incapacidad total, de "Tu dictamen" a "Tu siniestro". */
@Composable
private fun CaminoDelSiniestro(
    paso: PasoEnMiSeguro,
    irA: (PasoEnMiSeguro) -> Unit,
    poliza: Solicitud,
    piezas: PiezasDelSiniestro,
    alCerrarSesion: () -> Unit,
    modifier: Modifier,
) {
    val reporte = piezas.reporte
    val misSiniestros = piezas.misSiniestros
    val alaPoliza = { irA(PasoEnMiSeguro.POLIZA) }
    val alDictamen = { irA(PasoEnMiSeguro.DICTAMEN) }
    // Desde la camara y desde "Revisa y envia" se vuelve al dictamen; desde lo demas, a la poliza.
    BackHandler(onBack = if (paso == PasoEnMiSeguro.DICTAMEN || enUltimosPasos(paso)) alaPoliza else alDictamen)
    when (paso) {
        PasoEnMiSeguro.POLIZA -> Unit
        PasoEnMiSeguro.DICTAMEN -> PantallaTuDictamen(
            estado = reporte.estado,
            acciones = AccionesDelDictamen(
                alCambiarFecha = reporte::cambiarFecha,
                alEscanear = { irA(escaneandoPara(it)) },
                alEscanearOtraVez = {
                    reporte.quitar(Escaneo.DICTAMEN)
                    irA(PasoEnMiSeguro.ESCANEANDO_DICTAMEN)
                },
                alQuitar = reporte::quitar,
                alContinuar = { irA(PasoEnMiSeguro.REVISA) },
                alVolver = alaPoliza,
            ),
            alCerrarSesion = alCerrarSesion,
            modifier = modifier,
        )
        PasoEnMiSeguro.ESCANEANDO_DICTAMEN, PasoEnMiSeguro.ESCANEANDO_HISTORIA -> {
            val escaneo = if (paso == PasoEnMiSeguro.ESCANEANDO_DICTAMEN) Escaneo.DICTAMEN else Escaneo.HISTORIA_CLINICA
            piezas.camara(
                QueFoto.DOCUMENTO,
                { foto ->
                    irA(PasoEnMiSeguro.DICTAMEN)
                    reporte.agregarPagina(escaneo, foto)
                },
                modifier,
            )
        }
        PasoEnMiSeguro.REVISA -> PantallaRevisaYEnvia(
            estado = reporte.estado,
            nombreDeLaPoliza = nombreDelSeguro(poliza),
            alQuitar = reporte::quitar,
            alEnviar = { piezas.red.conRed(reporte::enviar) },
            alVolver = alDictamen,
            alCerrarSesion = alCerrarSesion,
            modifier = modifier,
        )
        PasoEnMiSeguro.ENVIADO -> PantallaReporteEnviado(
            numero = reporte.estado.radicado?.numero.orEmpty(),
            fechaLimite = misSiniestros.estado.detalle?.fechaLimite,
            alVerSiniestro = { irA(PasoEnMiSeguro.SINIESTRO) },
            alCerrarSesion = alCerrarSesion,
            modifier = modifier,
        )
        PasoEnMiSeguro.SINIESTRO -> PantallaTuSiniestro(
            estado = misSiniestros.estado,
            nombreDeLaPoliza = nombreDelSeguro(poliza),
            alReintentar = misSiniestros::reintentarDetalle,
            alVolver = alaPoliza,
            alCerrarSesion = alCerrarSesion,
            modifier = modifier,
        )
    }
}

private fun enUltimosPasos(paso: PasoEnMiSeguro) = paso == PasoEnMiSeguro.ENVIADO || paso == PasoEnMiSeguro.SINIESTRO

private fun escaneandoPara(escaneo: Escaneo) = when (escaneo) {
    Escaneo.DICTAMEN -> PasoEnMiSeguro.ESCANEANDO_DICTAMEN
    Escaneo.HISTORIA_CLINICA -> PasoEnMiSeguro.ESCANEANDO_HISTORIA
}
