package com.solventa4bits.movil.seguro.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.solventa4bits.movil.R
import com.solventa4bits.movil.identidad.ui.Camara
import com.solventa4bits.movil.identidad.ui.QueFoto
import com.solventa4bits.movil.red.GuardiaDeRed
import com.solventa4bits.movil.siniestro.dominio.DetalleDeSiniestro
import com.solventa4bits.movil.siniestro.dominio.PuntoDeAyuda
import com.solventa4bits.movil.siniestro.dominio.TipoDeSiniestro
import com.solventa4bits.movil.siniestro.dominio.puedePedirAyuda
import com.solventa4bits.movil.siniestro.ui.AccionesDeLaOficina
import com.solventa4bits.movil.siniestro.ui.AccionesDeLasFotos
import com.solventa4bits.movil.siniestro.ui.AccionesDeRevision
import com.solventa4bits.movil.siniestro.ui.AccionesDelDictamen
import com.solventa4bits.movil.siniestro.ui.AccionesDelRelato
import com.solventa4bits.movil.siniestro.ui.AyudaViewModel
import com.solventa4bits.movil.siniestro.ui.Escaneo
import com.solventa4bits.movil.siniestro.ui.MisSiniestrosViewModel
import com.solventa4bits.movil.siniestro.ui.OficinasViewModel
import com.solventa4bits.movil.siniestro.ui.PantallaAyudaPedida
import com.solventa4bits.movil.siniestro.ui.PantallaDondeVaLaAyuda
import com.solventa4bits.movil.siniestro.ui.PantallaOficinaDelCheque
import com.solventa4bits.movil.siniestro.ui.PantallaFotosDelDano
import com.solventa4bits.movil.siniestro.ui.PantallaPrestadores
import com.solventa4bits.movil.siniestro.ui.PantallaQuePaso
import com.solventa4bits.movil.siniestro.ui.PantallaReporteEnviado
import com.solventa4bits.movil.siniestro.ui.PantallaRevisaLoQueFalta
import com.solventa4bits.movil.siniestro.ui.PantallaRevisaYEnvia
import com.solventa4bits.movil.siniestro.ui.PantallaTipoDeAyuda
import com.solventa4bits.movil.siniestro.ui.PantallaTuDictamen
import com.solventa4bits.movil.siniestro.ui.PantallaTuPago
import com.solventa4bits.movil.siniestro.ui.PantallaTuSiniestro
import com.solventa4bits.movil.siniestro.ui.ReporteViewModel
import com.solventa4bits.movil.siniestro.ui.SiniestrosDelSeguro
import com.solventa4bits.movil.solicitud.dominio.Solicitud

/**
 * Donde va el cliente dentro de "Mi seguro" (BITS-111). Vida: dictamen y revisar. Tu casa: fotos,
 * que paso, la cuenta para el pago y revisar. Los dos terminan en el radicado y en "Tu siniestro".
 * Desde "Tu siniestro", lo que falta (BITS-118): el dictamen o las fotos, revisar y recibido. Y en
 * Tu casa, la ayuda en sitio (BITS-239): el tipo, a donde va, quien la presta y la ayuda pedida.
 */
private enum class PasoEnMiSeguro {
    POLIZA, DICTAMEN, FOTOS, QUE_PASO, PAGO, OFICINA, REVISA, ENVIADO, SINIESTRO,
    FALTA_DICTAMEN, FALTA_FOTOS, FALTA_REVISA, RECIBIDO,
    AYUDA_TIPO, AYUDA_DONDE, AYUDA_PRESTADOR, AYUDA_PEDIDA,
}

private val PASOS_DE_LA_AYUDA = setOf(
    PasoEnMiSeguro.AYUDA_TIPO,
    PasoEnMiSeguro.AYUDA_DONDE,
    PasoEnMiSeguro.AYUDA_PRESTADOR,
    PasoEnMiSeguro.AYUDA_PEDIDA,
)

/** Las piezas que "Mi seguro" necesita para el camino del siniestro. */
class PiezasDelSiniestro(
    val misSiniestros: MisSiniestrosViewModel,
    val reporte: ReporteViewModel,
    val camara: Camara,
    val red: GuardiaDeRed,
    val oficinas: OficinasViewModel,
    val ayuda: AyudaViewModel,
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
    AlVencerLaSesion(paso, piezas, alCerrarSesion)
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
    AlTerminarLaEntrega(paso, piezas) { paso = it }
    CaminoDelSiniestro(paso, { paso = it }, poliza, piezas, alCerrarSesion, modifier)
}

/**
 * Cierra la sesion si alguna consulta la encontro vencida. El reporte, las oficinas y la ayuda solo
 * cuentan mientras se estan usando: lo de una sesion anterior ya se atendio.
 */
@Composable
private fun AlVencerLaSesion(paso: PasoEnMiSeguro, piezas: PiezasDelSiniestro, alCerrarSesion: () -> Unit) {
    val delReporte = paso != PasoEnMiSeguro.POLIZA && piezas.reporte.estado.sesionVencida
    val deLasOficinas = paso == PasoEnMiSeguro.OFICINA && piezas.oficinas.estado.sesionVencida
    val deLaAyuda = paso in PASOS_DE_LA_AYUDA && piezas.ayuda.estado.sesionVencida
    val vencida = piezas.misSiniestros.estado.sesionVencida || delReporte || deLasOficinas || deLaAyuda
    LaunchedEffect(vencida) {
        if (vencida) alCerrarSesion()
    }
}

/** Llego lo que faltaba, o ya no se pedia: en los dos casos se ve el siniestro como esta ahora. */
@Composable
private fun AlTerminarLaEntrega(paso: PasoEnMiSeguro, piezas: PiezasDelSiniestro, irA: (PasoEnMiSeguro) -> Unit) {
    val contexto = LocalContext.current
    val entrega = piezas.reporte.estado
    LaunchedEffect(entrega.recibida, entrega.yaNoSePide) {
        val recibida = entrega.recibida
        if (paso != PasoEnMiSeguro.FALTA_REVISA) return@LaunchedEffect
        if (recibida != null) {
            piezas.misSiniestros.mostrar(recibida)
            irA(PasoEnMiSeguro.RECIBIDO)
        } else if (entrega.yaNoSePide) {
            Toast.makeText(contexto, R.string.falta_ya_no_se_pide, Toast.LENGTH_LONG).show()
            piezas.misSiniestros.reintentarDetalle()
            irA(PasoEnMiSeguro.SINIESTRO)
        }
    }
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
    val alAnterior = { irA(anterior(paso, reporte.estado.tipo, piezas.ayuda.estado.punto)) }
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
                alQuitar = reporte::quitarUno,
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
            alElegirOficina = {
                piezas.oficinas.cargar()
                irA(PasoEnMiSeguro.OFICINA)
            },
            alContinuar = { irA(PasoEnMiSeguro.REVISA) },
            alVolver = alAnterior,
            alCerrarSesion = alCerrarSesion,
            modifier = modifier,
        )
        PasoEnMiSeguro.FALTA_DICTAMEN, PasoEnMiSeguro.FALTA_FOTOS, PasoEnMiSeguro.FALTA_REVISA,
        PasoEnMiSeguro.RECIBIDO,
        -> LoQueFalta(paso, irA, piezas, { escaneando = it }, alAnterior, alCerrarSesion, modifier)
        in PASOS_DE_LA_AYUDA -> PedirAyuda(paso, irA, piezas, alAnterior, alCerrarSesion, modifier)
        else -> FinalDelCamino(paso, irA, poliza, piezas, alAnterior, alCerrarSesion, modifier)
    }
}

/** Lo que Solventa pidio: se escanea o se fotografia, se revisa y se envia (BITS-118, AC-4). */
@Composable
private fun LoQueFalta(
    paso: PasoEnMiSeguro,
    irA: (PasoEnMiSeguro) -> Unit,
    piezas: PiezasDelSiniestro,
    escanear: (Escaneo) -> Unit,
    alAnterior: () -> Unit,
    alCerrarSesion: () -> Unit,
    modifier: Modifier,
) {
    val reporte = piezas.reporte
    val alRevisar = { irA(PasoEnMiSeguro.FALTA_REVISA) }
    when (paso) {
        PasoEnMiSeguro.FALTA_DICTAMEN -> PantallaTuDictamen(
            estado = reporte.estado,
            acciones = AccionesDelDictamen(
                alCambiarFecha = reporte::cambiarFecha,
                alEscanear = escanear,
                alEscanearOtraVez = {
                    reporte.quitar(Escaneo.DICTAMEN)
                    escanear(Escaneo.DICTAMEN)
                },
                alQuitar = reporte::quitar,
                alContinuar = alRevisar,
                alVolver = alAnterior,
            ),
            alCerrarSesion = alCerrarSesion,
            modifier = modifier,
        )
        PasoEnMiSeguro.FALTA_FOTOS -> PantallaFotosDelDano(
            estado = reporte.estado,
            acciones = AccionesDeLasFotos(
                alTomarFoto = { escanear(Escaneo.FOTO_DEL_DANO) },
                alDecidirUbicacion = reporte::decidirUbicacion,
                alQuitar = reporte::quitarUno,
                alContinuar = alRevisar,
                alVolver = alAnterior,
                alEscanearDocumento = { escanear(Escaneo.DOCUMENTO) },
            ),
            alCerrarSesion = alCerrarSesion,
            modifier = modifier,
        )
        PasoEnMiSeguro.FALTA_REVISA -> PantallaRevisaLoQueFalta(
            estado = reporte.estado,
            acciones = AccionesDeRevision(
                alQuitar = reporte::quitar,
                alQuitarUno = reporte::quitarUno,
                alEnviar = { piezas.red.conRed(reporte::enviar) },
                alVolver = alAnterior,
            ),
            alCerrarSesion = alCerrarSesion,
            modifier = modifier,
        )
        PasoEnMiSeguro.RECIBIDO -> PantallaReporteEnviado(
            numero = reporte.estado.entrega?.numero.orEmpty(),
            fechaLimite = piezas.misSiniestros.estado.detalle?.fechaLimite,
            alVerSiniestro = { irA(PasoEnMiSeguro.SINIESTRO) },
            alCerrarSesion = alCerrarSesion,
            modifier = modifier,
            loQueFaltaba = true,
        )
        else -> Unit
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
    val alPedirAyuda = { detalle: DetalleDeSiniestro ->
        piezas.red.conRed {
            piezas.ayuda.empezar(detalle)
            irA(PasoEnMiSeguro.AYUDA_TIPO)
        }
    }
    when (paso) {
        PasoEnMiSeguro.OFICINA -> PantallaOficinaDelCheque(
            estado = piezas.oficinas.estado,
            banco = reporte.estado.pago.bancoDelCheque.orEmpty(),
            elegida = reporte.estado.pago.oficina,
            acciones = AccionesDeLaOficina(
                alElegir = { oficina ->
                    reporte.cambiarPago(reporte.estado.pago.copy(oficina = oficina))
                    irA(PasoEnMiSeguro.PAGO)
                },
                alElegirCiudad = piezas.oficinas::elegirCiudad,
                alRecargar = piezas.oficinas::cargar,
                alVolver = alAnterior,
            ),
            alCerrarSesion = alCerrarSesion,
            modifier = modifier,
        )
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
            alPedirAyuda = misSiniestros.estado.detalle?.takeIf { it.puedePedirAyuda }
                ?.let { detalle -> { alPedirAyuda(detalle) } },
        )
        PasoEnMiSeguro.SINIESTRO -> PantallaTuSiniestro(
            estado = misSiniestros.estado,
            nombreDeLaPoliza = nombreDelSeguro(poliza),
            bancoDelCredito = poliza.credito?.banco,
            alEnviarLoQueFalta = { detalle ->
                piezas.red.conRed {
                    reporte.empezarEntrega(detalle)
                    val deTuCasa = detalle.resumen.tipo?.deTuCasa == true
                    irA(if (deTuCasa) PasoEnMiSeguro.FALTA_FOTOS else PasoEnMiSeguro.FALTA_DICTAMEN)
                }
            },
            alPedirAyuda = alPedirAyuda,
            alReintentar = misSiniestros::reintentarDetalle,
            alVolver = alAnterior,
            alCerrarSesion = alCerrarSesion,
            modifier = modifier,
        )
        else -> Unit
    }
}

/**
 * La ayuda en sitio para la casa (BITS-239). Despues de escribir otra direccion se sigue al tipo si
 * aun no se eligio. Al ver el siniestro, ya trae la ayuda pedida (AC-7).
 */
@Composable
private fun PedirAyuda(
    paso: PasoEnMiSeguro,
    irA: (PasoEnMiSeguro) -> Unit,
    piezas: PiezasDelSiniestro,
    alAnterior: () -> Unit,
    alCerrarSesion: () -> Unit,
    modifier: Modifier,
) {
    val ayuda = piezas.ayuda
    val alUbicar = {
        irA(if (ayuda.estado.tipo == null) PasoEnMiSeguro.AYUDA_TIPO else PasoEnMiSeguro.AYUDA_PRESTADOR)
    }
    when (paso) {
        PasoEnMiSeguro.AYUDA_TIPO -> PantallaTipoDeAyuda(
            estado = ayuda.estado,
            alElegir = ayuda::elegirTipo,
            alOtraDireccion = { irA(PasoEnMiSeguro.AYUDA_DONDE) },
            alContinuar = {
                irA(if (ayuda.estado.punto == null) PasoEnMiSeguro.AYUDA_DONDE else PasoEnMiSeguro.AYUDA_PRESTADOR)
            },
            alReintentar = ayuda::cargar,
            alVolver = alAnterior,
            alCerrarSesion = alCerrarSesion,
            modifier = modifier,
        )
        PasoEnMiSeguro.AYUDA_DONDE -> PantallaDondeVaLaAyuda(
            estado = ayuda.estado,
            alUsarElTelefono = { ayuda.usarUbicacionDelTelefono(alUbicar) },
            alCambiarDireccion = ayuda::cambiarDireccion,
            alUsarLaDireccion = {
                ayuda.usarDireccion()
                alUbicar()
            },
            alVolver = alAnterior,
            alCerrarSesion = alCerrarSesion,
            modifier = modifier,
        )
        PasoEnMiSeguro.AYUDA_PRESTADOR -> PantallaPrestadores(
            estado = ayuda.estado,
            alPedir = { prestador ->
                piezas.red.conRed { ayuda.pedir(prestador) { irA(PasoEnMiSeguro.AYUDA_PEDIDA) } }
            },
            alOtraAyuda = { irA(PasoEnMiSeguro.AYUDA_TIPO) },
            alVolver = alAnterior,
            alCerrarSesion = alCerrarSesion,
            modifier = modifier,
        )
        PasoEnMiSeguro.AYUDA_PEDIDA -> {
            val alVerSiniestro = {
                ayuda.estado.siniestroConLaAyuda?.let(piezas.misSiniestros::mostrar)
                irA(PasoEnMiSeguro.SINIESTRO)
            }
            BackHandler(onBack = alVerSiniestro)
            PantallaAyudaPedida(ayuda.estado, alVerSiniestro, alCerrarSesion, modifier)
        }
        else -> Unit
    }
}

/** A donde vuelve el gesto atras o la flecha de cada paso. */
private fun anterior(paso: PasoEnMiSeguro, tipo: TipoDeSiniestro, punto: PuntoDeAyuda?) = when (paso) {
    PasoEnMiSeguro.QUE_PASO -> PasoEnMiSeguro.FOTOS
    PasoEnMiSeguro.PAGO -> PasoEnMiSeguro.QUE_PASO
    PasoEnMiSeguro.OFICINA -> PasoEnMiSeguro.PAGO
    PasoEnMiSeguro.REVISA -> if (tipo.deTuCasa) PasoEnMiSeguro.PAGO else PasoEnMiSeguro.DICTAMEN
    PasoEnMiSeguro.FALTA_REVISA -> if (tipo.deTuCasa) PasoEnMiSeguro.FALTA_FOTOS else PasoEnMiSeguro.FALTA_DICTAMEN
    PasoEnMiSeguro.FALTA_DICTAMEN, PasoEnMiSeguro.FALTA_FOTOS, PasoEnMiSeguro.RECIBIDO,
    PasoEnMiSeguro.AYUDA_TIPO, PasoEnMiSeguro.AYUDA_PEDIDA,
    -> PasoEnMiSeguro.SINIESTRO
    PasoEnMiSeguro.AYUDA_DONDE -> PasoEnMiSeguro.AYUDA_TIPO
    // Si la ayuda va a donde se tomaron las fotos, no se paso por "Estas en tu casa".
    PasoEnMiSeguro.AYUDA_PRESTADOR ->
        if (punto is PuntoDeAyuda.DondeTomoLasFotos) PasoEnMiSeguro.AYUDA_TIPO else PasoEnMiSeguro.AYUDA_DONDE
    else -> PasoEnMiSeguro.POLIZA
}
