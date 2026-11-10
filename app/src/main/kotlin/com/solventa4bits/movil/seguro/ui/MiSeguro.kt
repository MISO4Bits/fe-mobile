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
import com.solventa4bits.movil.siniestro.dominio.TipoDeSiniestro
import com.solventa4bits.movil.siniestro.ui.AccionesDeLasFotos
import com.solventa4bits.movil.siniestro.ui.AccionesDeRevision
import com.solventa4bits.movil.siniestro.ui.AccionesDelDictamen
import com.solventa4bits.movil.siniestro.ui.AccionesDelRelato
import com.solventa4bits.movil.siniestro.ui.Escaneo
import com.solventa4bits.movil.siniestro.ui.MisSiniestrosViewModel
import com.solventa4bits.movil.siniestro.ui.PantallaFotosDelDano
import com.solventa4bits.movil.siniestro.ui.PantallaQuePaso
import com.solventa4bits.movil.siniestro.ui.PantallaReporteEnviado
import com.solventa4bits.movil.siniestro.ui.PantallaRevisaYEnvia
import com.solventa4bits.movil.siniestro.ui.PantallaTuDictamen
import com.solventa4bits.movil.siniestro.ui.PantallaTuPago
import com.solventa4bits.movil.siniestro.ui.PantallaTuSiniestro
import com.solventa4bits.movil.siniestro.ui.ReporteViewModel
import com.solventa4bits.movil.siniestro.ui.SiniestrosDelSeguro
import com.solventa4bits.movil.solicitud.dominio.Solicitud

/**
 * Donde va el cliente dentro de "Mi seguro" (BITS-111). Vida: dictamen y revisar. Tu casa: fotos,
 * que paso, la cuenta para el pago y revisar. Los dos terminan en el radicado y en "Tu siniestro".
 */
private enum class PasoEnMiSeguro { POLIZA, DICTAMEN, FOTOS, QUE_PASO, PAGO, REVISA, ENVIADO, SINIESTRO }

/** Las piezas que "Mi seguro" necesita para el camino del siniestro. */
class PiezasDelSiniestro(
    val misSiniestros: MisSiniestrosViewModel,
    val reporte: ReporteViewModel,
    val camara: Camara,
    val red: GuardiaDeRed,
)

/**
 * "Mi seguro": el detalle de la poliza con sus siniestros (M01) y, desde ahi, el camino de cada
 * cobertura para reportar uno. Sin conexion solo se ve la poliza guardada.
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
                    alReportar = { tipo ->
                        piezas.red.conRed {
                            reporte.empezar(poliza, tipo)
                            paso = if (tipo.deTuCasa) PasoEnMiSeguro.FOTOS else PasoEnMiSeguro.DICTAMEN
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

/** Los pasos del reporte, de la primera pantalla de cada cobertura a "Tu siniestro". */
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
    // Lo que se esta tomando con la camara. Al tener la foto se vuelve a la pantalla de donde salio.
    var escaneando by rememberSaveable { mutableStateOf<Escaneo?>(null) }
    val actual = escaneando
    if (actual != null) {
        // El gesto atras cierra la camara sin agregar nada.
        BackHandler { escaneando = null }
        piezas.camara(
            if (actual == Escaneo.FOTO_DEL_DANO) QueFoto.FOTO_DEL_DANO else QueFoto.DOCUMENTO,
            { foto ->
                escaneando = null
                reporte.agregarPagina(actual, foto)
            },
            modifier,
        )
        return
    }
    val alAnterior = { irA(anterior(paso, reporte.estado.tipo)) }
    BackHandler(onBack = alAnterior)
    when (paso) {
        PasoEnMiSeguro.POLIZA -> Unit
        PasoEnMiSeguro.DICTAMEN -> PantallaTuDictamen(
            estado = reporte.estado,
            acciones = AccionesDelDictamen(
                alCambiarFecha = reporte::cambiarFecha,
                alEscanear = { escaneando = it },
                alEscanearOtraVez = {
                    reporte.quitar(Escaneo.DICTAMEN)
                    escaneando = Escaneo.DICTAMEN
                },
                alQuitar = reporte::quitar,
                alContinuar = { irA(PasoEnMiSeguro.REVISA) },
                alVolver = alAnterior,
            ),
            alCerrarSesion = alCerrarSesion,
            modifier = modifier,
        )
        PasoEnMiSeguro.FOTOS -> PantallaFotosDelDano(
            estado = reporte.estado,
            acciones = AccionesDeLasFotos(
                alTomarFoto = { escaneando = Escaneo.FOTO_DEL_DANO },
                alDecidirUbicacion = reporte::decidirUbicacion,
                alQuitar = { reporte.quitarUno(Escaneo.FOTO_DEL_DANO, it) },
                alContinuar = { irA(PasoEnMiSeguro.QUE_PASO) },
                alVolver = alAnterior,
            ),
            alCerrarSesion = alCerrarSesion,
            modifier = modifier,
        )
        PasoEnMiSeguro.QUE_PASO -> PantallaQuePaso(
            estado = reporte.estado,
            acciones = AccionesDelRelato(
                alCambiarFecha = reporte::cambiarFecha,
                alCambiarNarracion = reporte::cambiarNarracion,
                alEscanearDocumento = { escaneando = Escaneo.DOCUMENTO },
                alQuitarDocumento = { reporte.quitarUno(Escaneo.DOCUMENTO, it) },
                alContinuar = { irA(PasoEnMiSeguro.PAGO) },
                alVolver = alAnterior,
            ),
            alCerrarSesion = alCerrarSesion,
            modifier = modifier,
        )
        PasoEnMiSeguro.PAGO -> PantallaTuPago(
            estado = reporte.estado,
            alCambiarPago = reporte::cambiarPago,
            alContinuar = { irA(PasoEnMiSeguro.REVISA) },
            alVolver = alAnterior,
            alCerrarSesion = alCerrarSesion,
            modifier = modifier,
        )
        else -> FinalDelCamino(paso, irA, poliza, piezas, alAnterior, alCerrarSesion, modifier)
    }
}

/** Revisar y enviar, el radicado y "Tu siniestro": iguales en las dos coberturas. */
@Composable
private fun FinalDelCamino(
    paso: PasoEnMiSeguro,
    irA: (PasoEnMiSeguro) -> Unit,
    poliza: Solicitud,
    piezas: PiezasDelSiniestro,
    alAnterior: () -> Unit,
    alCerrarSesion: () -> Unit,
    modifier: Modifier,
) {
    val reporte = piezas.reporte
    val misSiniestros = piezas.misSiniestros
    when (paso) {
        PasoEnMiSeguro.REVISA -> PantallaRevisaYEnvia(
            estado = reporte.estado,
            nombreDeLaPoliza = nombreDelSeguro(poliza),
            acciones = AccionesDeRevision(
                alQuitar = reporte::quitar,
                alQuitarUno = reporte::quitarUno,
                alEnviar = { piezas.red.conRed(reporte::enviar) },
                alVolver = alAnterior,
            ),
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
            bancoDelCredito = poliza.credito?.banco,
            alReintentar = misSiniestros::reintentarDetalle,
            alVolver = alAnterior,
            alCerrarSesion = alCerrarSesion,
            modifier = modifier,
        )
        else -> Unit
    }
}

/** A donde vuelve el gesto atras o la flecha de cada paso. */
private fun anterior(paso: PasoEnMiSeguro, tipo: TipoDeSiniestro) = when (paso) {
    PasoEnMiSeguro.QUE_PASO -> PasoEnMiSeguro.FOTOS
    PasoEnMiSeguro.PAGO -> PasoEnMiSeguro.QUE_PASO
    PasoEnMiSeguro.REVISA -> if (tipo.deTuCasa) PasoEnMiSeguro.PAGO else PasoEnMiSeguro.DICTAMEN
    else -> PasoEnMiSeguro.POLIZA
}
