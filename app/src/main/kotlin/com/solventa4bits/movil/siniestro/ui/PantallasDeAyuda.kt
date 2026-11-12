package com.solventa4bits.movil.siniestro.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.solventa4bits.movil.R
import com.solventa4bits.movil.siniestro.dominio.Localidad
import com.solventa4bits.movil.siniestro.dominio.Prestador
import com.solventa4bits.movil.siniestro.dominio.PrestadorCercano
import com.solventa4bits.movil.siniestro.dominio.PuntoDeAyuda
import com.solventa4bits.movil.siniestro.dominio.TipoDeAyuda
import com.solventa4bits.movil.siniestro.dominio.Ubicacion
import com.solventa4bits.movil.ui.BarraSolventa

/**
 * M13: que ayuda necesita el cliente (BITS-239, AC-2). Si alguna foto tiene ubicacion, la ayuda va
 * alli, y se puede escribir otra direccion (AC-3).
 */
@Composable
fun PantallaTipoDeAyuda(
    estado: EstadoAyuda,
    alElegir: (TipoDeAyuda) -> Unit,
    alOtraDireccion: () -> Unit,
    alContinuar: () -> Unit,
    alReintentar: () -> Unit,
    alVolver: () -> Unit,
    alCerrarSesion: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PasoDelReporte(
        titulo = stringResource(R.string.ayuda_tipo_titulo),
        subtitulo = stringResource(R.string.ayuda_tipo_subtitulo),
        boton = stringResource(R.string.reporte_continuar),
        botonActivo = estado.tipo != null,
        alBoton = alContinuar,
        alVolver = alVolver,
        alCerrarSesion = alCerrarSesion,
        modifier = modifier,
        barra = stringResource(R.string.ayuda_barra),
    ) {
        when {
            estado.cargando -> CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            estado.fallo -> {
                TextoTenue(stringResource(R.string.ayuda_fallo))
                TextButton(onClick = alReintentar) { Text(stringResource(R.string.tu_siniestro_reintentar)) }
            }
            else -> {
                estado.catalogo.tiposEnOrden.chunked(TIPOS_POR_FILA).forEach { fila ->
                    Row(
                        modifier = Modifier.height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        fila.forEach { tipo ->
                            CuadroDeTipo(tipo, estado.tipo?.id == tipo.id, Modifier.weight(1f)) { alElegir(tipo) }
                        }
                        repeat(TIPOS_POR_FILA - fila.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
                estado.punto?.let { VaA(it, alOtraDireccion) }
            }
        }
    }
}

@Composable
private fun CuadroDeTipo(tipo: TipoDeAyuda, elegido: Boolean, modifier: Modifier, alElegir: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = if (elegido) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLowest
        },
        border = if (elegido) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = modifier.fillMaxHeight().selectable(selected = elegido, onClick = alElegir),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                painterResource(iconoDeAyuda(tipo.id)),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(tipo.nombre, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        }
    }
}

/** A donde va la ayuda, con la opcion de escribir otra direccion. */
@Composable
private fun VaA(punto: PuntoDeAyuda, alOtraDireccion: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column {
                TextoTenue(stringResource(R.string.ayuda_va_a))
                Text(
                    text = textoDelPunto(punto),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                TextButton(onClick = alOtraDireccion, modifier = Modifier.padding(top = 4.dp)) {
                    Text(stringResource(R.string.ayuda_otra_direccion))
                }
            }
        }
    }
}

/**
 * M13A: si el cliente esta en la casa, la ayuda va a donde esta el telefono; si no, o si el
 * telefono no da su ubicacion, escribe la direccion y elige la localidad de Bogota (AC-4).
 */
@Composable
fun PantallaDondeVaLaAyuda(
    estado: EstadoAyuda,
    alUsarElTelefono: () -> Unit,
    alCambiarDireccion: (DireccionEscrita) -> Unit,
    alUsarLaDireccion: () -> Unit,
    alVolver: () -> Unit,
    alCerrarSesion: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var enCasa by rememberSaveable { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(estado.sinUbicacion) {
        if (estado.sinUbicacion) enCasa = false
    }
    // Con o sin permiso se busca: sin el, la ubicacion no llega y se pide la direccion.
    val pedirPermiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        alUsarElTelefono()
    }
    val escribe = enCasa == false
    PasoDelReporte(
        titulo = stringResource(R.string.ayuda_donde_titulo),
        subtitulo = stringResource(R.string.ayuda_donde_subtitulo),
        boton = stringResource(if (estado.buscandoUbicacion) R.string.ayuda_buscando else R.string.reporte_continuar),
        botonActivo = !estado.buscandoUbicacion && if (escribe) estado.escrita.completa else enCasa == true,
        alBoton = {
            if (escribe) {
                alUsarLaDireccion()
            } else {
                pedirPermiso.launch(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                )
            }
        },
        alVolver = alVolver,
        alCerrarSesion = alCerrarSesion,
        modifier = modifier,
        barra = stringResource(R.string.ayuda_barra),
    ) {
        Opcion(
            titulo = stringResource(R.string.ayuda_donde_si),
            detalle = stringResource(R.string.ayuda_donde_si_detalle),
            elegida = enCasa == true,
        ) { enCasa = true }
        Opcion(titulo = stringResource(R.string.ayuda_donde_no), elegida = escribe) { enCasa = false }
        if (escribe) {
            if (estado.sinUbicacion) Text(stringResource(R.string.ayuda_sin_ubicacion))
            LaDireccion(estado.escrita, estado.catalogo.localidadesDeBogota, alCambiarDireccion)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LaDireccion(
    escrita: DireccionEscrita,
    localidades: List<Localidad>,
    alCambiar: (DireccionEscrita) -> Unit,
) {
    OutlinedTextField(
        value = escrita.direccion,
        onValueChange = { alCambiar(escrita.copy(direccion = it)) },
        label = { Text(stringResource(R.string.ayuda_direccion)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    var abierto by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = abierto, onExpandedChange = { abierto = it }) {
        OutlinedTextField(
            value = escrita.localidad?.nombre.orEmpty(),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.ayuda_localidad)) },
            supportingText = { Text(stringResource(R.string.ayuda_solo_bogota)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = abierto) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            localidades.forEach { localidad ->
                DropdownMenuItem(
                    text = { Text(localidad.nombre) },
                    onClick = {
                        alCambiar(escrita.copy(localidad = localidad))
                        abierto = false
                    },
                )
            }
        }
    }
}

/**
 * M14: los prestadores del tipo elegido, en el mapa y en la lista, del mas cercano al mas lejano,
 * con su tiempo de llegada (AC-5). Si no hay ninguno, se elige otra ayuda.
 */
@Composable
fun PantallaPrestadores(
    estado: EstadoAyuda,
    alPedir: (Prestador) -> Unit,
    alOtraAyuda: () -> Unit,
    alVolver: () -> Unit,
    alCerrarSesion: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val opciones = estado.prestadores
    var marcado by rememberSaveable(estado.tipo?.id, estado.punto?.ubicacion?.toString()) {
        mutableStateOf(opciones.firstOrNull()?.prestador?.id)
    }
    val prestador = opciones.firstOrNull { it.prestador.id == marcado }?.prestador
    val vacio = opciones.isEmpty()
    PasoDelReporte(
        titulo = stringResource(R.string.ayuda_prestador_titulo),
        subtitulo = if (vacio) "" else stringResource(R.string.ayuda_prestador_subtitulo),
        boton = stringResource(
            when {
                vacio -> R.string.ayuda_otra_ayuda
                estado.pidiendo -> R.string.ayuda_pidiendo
                else -> R.string.ayuda_pedir
            },
        ),
        botonActivo = vacio || prestador != null && !estado.pidiendo && !estado.noSePuede,
        alBoton = { if (vacio) alOtraAyuda() else prestador?.let(alPedir) },
        alVolver = alVolver,
        alCerrarSesion = alCerrarSesion,
        modifier = modifier,
        barra = stringResource(R.string.ayuda_barra),
    ) {
        val punto = estado.punto
        if (vacio || punto == null) {
            Text(stringResource(R.string.ayuda_sin_prestadores, estado.tipo?.nombre.orEmpty()))
        } else {
            MapaDeLugares(
                lugares = opciones.map { LugarEnElMapa(it.prestador.id, it.prestador.latitud, it.prestador.longitud) },
                elegido = marcado,
                centro = prestador?.let { Ubicacion(it.latitud, it.longitud) } ?: punto.ubicacion,
                cliente = punto.ubicacion,
            )
            Datos {
                opciones.forEachIndexed { i, opcion ->
                    if (i > 0) HorizontalDivider()
                    FilaDePrestador(opcion, opcion.prestador.id == marcado) { marcado = opcion.prestador.id }
                }
            }
            LaFacturaYElError(estado)
        }
    }
}

@Composable
private fun LaFacturaYElError(estado: EstadoAyuda) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Outlined.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(stringResource(R.string.ayuda_factura), style = MaterialTheme.typography.bodyMedium)
        }
    }
    val error = when {
        estado.noSePuede -> R.string.ayuda_no_se_puede
        estado.falloAlPedir -> R.string.ayuda_fallo_al_pedir
        else -> null
    }
    error?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
}

@Composable
private fun FilaDePrestador(opcion: PrestadorCercano, marcado: Boolean, alMarcar: () -> Unit) {
    val prestador = opcion.prestador
    Row(
        modifier = Modifier.fillMaxWidth().selectable(selected = marcado, onClick = alMarcar),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RadioButton(selected = marcado, onClick = null)
        Column(modifier = Modifier.weight(1f)) {
            Text(prestador.nombre, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            TextoTenue(prestador.direccion)
            Text(
                text = stringResource(R.string.ayuda_km_llega, enKm(opcion.km), prestador.tiempoLlegadaMin),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

/** M15: la ayuda quedo pedida, con su numero, quien va, cuando llega y a donde (AC-6). */
@Composable
fun PantallaAyudaPedida(
    estado: EstadoAyuda,
    alVerSiniestro: () -> Unit,
    alCerrarSesion: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pedida = estado.pedida ?: return
    Column(modifier = modifier.fillMaxSize()) {
        BarraSolventa(stringResource(R.string.ayuda_barra), alCerrarSesion)
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp),
            )
            Text(stringResource(R.string.ayuda_pedida_titulo), style = MaterialTheme.typography.headlineSmall)
            Text(
                text = stringResource(R.string.ayuda_pedida_numero, pedida.numero),
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center,
            )
            Datos {
                FilaDeDato(stringResource(R.string.ayuda_prestador), pedida.prestador)
                FilaDeDato(stringResource(R.string.ayuda_tipo), estado.tipo?.nombre ?: nombreDeAyuda(pedida.tipo))
                FilaDeDato(
                    stringResource(R.string.ayuda_llega_en),
                    stringResource(R.string.ayuda_minutos, pedida.tiempoLlegadaMin),
                )
                estado.punto?.let { FilaDeDato(stringResource(R.string.ayuda_va_a), textoDelPunto(it)) }
                estado.siniestro?.let { FilaDeDato(stringResource(R.string.ayuda_siniestro), it.resumen.numero) }
            }
        }
        Button(onClick = alVerSiniestro, modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(stringResource(R.string.enviado_ver))
        }
    }
}

@Composable
private fun textoDelPunto(punto: PuntoDeAyuda): String = when (punto) {
    is PuntoDeAyuda.DondeTomoLasFotos -> stringResource(R.string.ayuda_va_a_fotos)
    is PuntoDeAyuda.DondeEstaElTelefono -> stringResource(R.string.ayuda_va_a_telefono)
    is PuntoDeAyuda.EnUnaDireccion ->
        stringResource(R.string.ayuda_va_a_direccion, punto.direccion, punto.localidad.nombre)
}

private const val TIPOS_POR_FILA = 2
