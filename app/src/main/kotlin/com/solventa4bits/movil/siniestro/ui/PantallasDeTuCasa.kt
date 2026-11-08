package com.solventa4bits.movil.siniestro.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.solventa4bits.movil.R
import com.solventa4bits.movil.siniestro.dominio.ArchivoDelReporte
import com.solventa4bits.movil.siniestro.dominio.TipoDeCuenta
import com.solventa4bits.movil.solicitud.ui.fechaDeSolicitud
import java.time.LocalDate

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

/** Lo que el cliente hace en "Fotos del dano". */
class AccionesDeLasFotos(
    val alTomarFoto: () -> Unit,
    val alDecidirUbicacion: (Boolean) -> Unit,
    val alQuitar: (Int) -> Unit,
    val alContinuar: () -> Unit,
    val alVolver: () -> Unit,
)

/** Lo que el cliente hace en "Cuentanos que paso". */
class AccionesDelRelato(
    val alCambiarFecha: (LocalDate) -> Unit,
    val alCambiarNarracion: (String) -> Unit,
    val alEscanearDocumento: () -> Unit,
    val alQuitarDocumento: (Int) -> Unit,
    val alContinuar: () -> Unit,
    val alVolver: () -> Unit,
)

private val permisosDeUbicacion =
    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

/**
 * M04: las fotos del dano, solo con la camara y cada una con donde y cuando se tomo (BITS-111,
 * AC-4). M04A: antes de la primera foto, si no hay permiso de ubicacion, se explica para que es;
 * sin el, la foto queda sin ubicacion y el reporte se envia igual (AC-5).
 */
@Composable
fun PantallaFotosDelDano(
    estado: EstadoReporte,
    acciones: AccionesDeLasFotos,
    alCerrarSesion: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contexto = LocalContext.current
    var preguntando by remember { mutableStateOf(false) }
    val pedirPermiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        preguntando = false
        acciones.alDecidirUbicacion(it.values.any { concedido -> concedido })
        acciones.alTomarFoto()
    }
    val tomar = {
        when {
            conPermisoDeUbicacion(contexto) -> {
                acciones.alDecidirUbicacion(true)
                acciones.alTomarFoto()
            }
            estado.usarUbicacion != null -> acciones.alTomarFoto()
            else -> preguntando = true
        }
    }
    PasoDelReporte(
        titulo = stringResource(R.string.fotos_titulo),
        subtitulo = stringResource(R.string.fotos_subtitulo),
        boton = stringResource(R.string.reporte_continuar),
        botonActivo = estado.fotosListas,
        alBoton = acciones.alContinuar,
        alVolver = acciones.alVolver,
        alCerrarSesion = alCerrarSesion,
        modifier = modifier,
    ) {
        if (estado.fotos.isNotEmpty()) {
            TituloDeSeccion(pluralStringResource(R.plurals.fotos_cuantas, estado.fotos.size, estado.fotos.size))
        }
        estado.fotos.forEachIndexed { i, foto -> TarjetaDeFoto(i + 1, foto) { acciones.alQuitar(i) } }
        Preparando(estado)
        BotonDeCamara(
            icono = ImageVector.vectorResource(R.drawable.ic_camara),
            texto = stringResource(if (estado.fotos.isEmpty()) R.string.fotos_tomar else R.string.fotos_tomar_otra),
            activo = estado.puedeEscanear,
            alTocar = tomar,
        )
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Outlined.Info, contentDescription = null)
                Text(stringResource(R.string.fotos_solo_camara), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
    if (preguntando) {
        HojaDeUbicacion(
            alPermitir = { pedirPermiso.launch(permisosDeUbicacion) },
            alSeguirSin = {
                preguntando = false
                acciones.alDecidirUbicacion(false)
                acciones.alTomarFoto()
            },
            alCerrar = { preguntando = false },
        )
    }
}

/** M04A: para que se usa la ubicacion, antes de que Android pida el permiso. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HojaDeUbicacion(alPermitir: () -> Unit, alSeguirSin: () -> Unit, alCerrar: () -> Unit) {
    ModalBottomSheet(onDismissRequest = alCerrar) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(stringResource(R.string.ubicacion_titulo), style = MaterialTheme.typography.titleLarge)
            TextoTenue(stringResource(R.string.ubicacion_detalle))
            Button(onClick = alPermitir, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.ubicacion_permitir))
            }
            TextButton(onClick = alSeguirSin, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.ubicacion_seguir_sin))
            }
        }
    }
}

@Composable
private fun TarjetaDeFoto(numero: Int, foto: ArchivoDelReporte, alQuitar: () -> Unit) {
    val nombre = stringResource(R.string.foto_numero, numero)
    val cuando = fechaDeSolicitud(foto.capturadoEn)?.lowercase().orEmpty()
    val conUbicacion = foto.latitud != null
    Tarjeta {
        val icono = ImageVector.vectorResource(R.drawable.ic_foto)
        Icon(icono, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column(modifier = Modifier.weight(1f)) {
            Text(nombre, style = MaterialTheme.typography.bodyMedium)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(
                    if (conUbicacion) Icons.Outlined.LocationOn else Icons.Outlined.Close,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp),
                )
                val donde = if (conUbicacion) R.string.foto_con_ubicacion else R.string.foto_sin_ubicacion
                TextoTenue(stringResource(donde, cuando))
            }
        }
        IconButton(onClick = alQuitar) {
            Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.reporte_quitar, nombre))
        }
    }
}

@Composable
private fun BotonDeCamara(icono: ImageVector, texto: String, activo: Boolean, alTocar: () -> Unit) {
    OutlinedButton(onClick = alTocar, enabled = activo, modifier = Modifier.fillMaxWidth()) {
        Icon(icono, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
        Text(texto)
    }
}

/**
 * M05: cuando ocurrio (AC-2), que paso con las palabras del cliente (obligatorio en Tu casa) y los
 * documentos que tenga, escaneados con la camara (AC-4).
 */
@Composable
fun PantallaQuePaso(
    estado: EstadoReporte,
    acciones: AccionesDelRelato,
    alCerrarSesion: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PasoDelReporte(
        titulo = stringResource(R.string.relato_titulo),
        subtitulo = stringResource(R.string.relato_subtitulo),
        boton = stringResource(R.string.reporte_continuar),
        botonActivo = estado.relatoListo,
        alBoton = acciones.alContinuar,
        alVolver = acciones.alVolver,
        alCerrarSesion = alCerrarSesion,
        modifier = modifier,
    ) {
        CuandoOcurrio(estado, acciones.alCambiarFecha)
        OutlinedTextField(
            value = estado.narracion,
            onValueChange = acciones.alCambiarNarracion,
            label = { Text(stringResource(R.string.relato_que_paso)) },
            minLines = LINEAS_DEL_RELATO,
            supportingText = {
                Text(
                    text = "${estado.narracion.length} / ${EstadoReporte.LARGO_DE_LA_NARRACION}",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End,
                )
            },
            modifier = Modifier.fillMaxWidth(),
        )
        TituloDeSeccion(stringResource(R.string.relato_documentos))
        TextoTenue(stringResource(R.string.relato_documentos_ejemplo))
        estado.documentos.forEachIndexed { i, documento ->
            ArchivoEscaneado(stringResource(R.string.documento_numero, i + 1), listOf(documento)) {
                acciones.alQuitarDocumento(i)
            }
        }
        Preparando(estado)
        BotonDeCamara(
            icono = Icons.AutoMirrored.Outlined.List,
            texto = stringResource(R.string.relato_escanear_documento),
            activo = estado.puedeEscanear,
            alTocar = acciones.alEscanearDocumento,
        )
    }
}

/**
 * La cuenta a la que se transfiere el pago de Tu casa. El Figma no la tiene: es lo minimo que el
 * contrato pide, solo por transferencia.
 */
@Composable
fun PantallaTuPago(
    estado: EstadoReporte,
    alCambiarPago: (DatosDePago) -> Unit,
    alContinuar: () -> Unit,
    alVolver: () -> Unit,
    alCerrarSesion: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pago = estado.pago
    PasoDelReporte(
        titulo = stringResource(R.string.pago_titulo),
        subtitulo = stringResource(R.string.pago_subtitulo),
        boton = stringResource(R.string.reporte_continuar),
        botonActivo = pago.destino != null,
        alBoton = alContinuar,
        alVolver = alVolver,
        alCerrarSesion = alCerrarSesion,
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = pago.banco,
            onValueChange = { alCambiarPago(pago.copy(banco = it.take(LARGO_DEL_BANCO))) },
            label = { Text(stringResource(R.string.pago_banco)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        TituloDeSeccion(stringResource(R.string.pago_tipo))
        OpcionDeCuenta(stringResource(R.string.pago_ahorros), pago.tipoCuenta == TipoDeCuenta.AHORROS) {
            alCambiarPago(pago.copy(tipoCuenta = TipoDeCuenta.AHORROS))
        }
        OpcionDeCuenta(stringResource(R.string.pago_corriente), pago.tipoCuenta == TipoDeCuenta.CORRIENTE) {
            alCambiarPago(pago.copy(tipoCuenta = TipoDeCuenta.CORRIENTE))
        }
        OutlinedTextField(
            value = pago.numeroCuenta,
            onValueChange = { numero ->
                alCambiarPago(pago.copy(numeroCuenta = numero.filter(Char::isDigit).take(DIGITOS_DE_LA_CUENTA)))
            },
            label = { Text(stringResource(R.string.pago_numero)) },
            supportingText = { Text(stringResource(R.string.pago_numero_ayuda)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun OpcionDeCuenta(texto: String, elegida: Boolean, alElegir: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().selectable(selected = elegida, onClick = alElegir),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = elegida, onClick = null)
        Text(texto, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 8.dp))
    }
}

private fun conPermisoDeUbicacion(contexto: Context) = permisosDeUbicacion.any {
    ContextCompat.checkSelfPermission(contexto, it) == PackageManager.PERMISSION_GRANTED
}

private const val LINEAS_DEL_RELATO = 4
private const val LARGO_DEL_BANCO = 100
private const val DIGITOS_DE_LA_CUENTA = 20
