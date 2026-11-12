package com.solventa4bits.movil.siniestro.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.solventa4bits.movil.R
import com.solventa4bits.movil.siniestro.dominio.Oficina
import com.solventa4bits.movil.siniestro.dominio.OficinaCercana
import com.solventa4bits.movil.ui.BarraSolventa
import java.util.Locale

/**
 * M06A: la oficina donde se reclama el cheque (BITS-116, AC-4). Con la ubicacion, propone la mas
 * cercana y ordena las demas por distancia, en el telefono. Sin ella, el cliente elige su ciudad.
 */
@Composable
fun PantallaOficinaDelCheque(
    estado: EstadoOficinas,
    banco: String,
    elegida: Oficina?,
    acciones: AccionesDeLaOficina,
    alCerrarSesion: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val opciones = estado.paraElegir(banco)
    // La que el cliente marca en la lista; por defecto, la que ya tenia o la mas cercana.
    var marcada by rememberSaveable(opciones.firstOrNull()?.oficina?.id) {
        mutableStateOf(elegida?.id?.takeIf { id -> opciones.any { it.oficina.id == id } }
            ?: opciones.firstOrNull()?.oficina?.id)
    }
    val oficina = opciones.firstOrNull { it.oficina.id == marcada }?.oficina
    Column(modifier = modifier.fillMaxSize()) {
        BarraSolventa(stringResource(R.string.oficina_barra), alCerrarSesion, alVolver = acciones.alVolver)
        when {
            estado.cargando -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            estado.fallo -> Column(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                TextoTenue(stringResource(R.string.oficina_fallo))
                TextButton(onClick = acciones.alRecargar) { Text(stringResource(R.string.tu_siniestro_reintentar)) }
            }
            else -> Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column {
                    Text(stringResource(R.string.oficina_titulo), style = MaterialTheme.typography.headlineSmall)
                    val subtitulo = if (estado.ubicacion != null) {
                        R.string.oficina_subtitulo_cerca
                    } else {
                        R.string.oficina_subtitulo_ciudad
                    }
                    TextoTenue(stringResource(subtitulo))
                }
                if (estado.ubicacion == null) SinUbicacion(estado, banco, acciones)
                MapaDeOficinas(opciones.map { it.oficina }, oficina, estado.ubicacion)
                if (opciones.isNotEmpty()) {
                    Datos {
                        opciones.forEachIndexed { i, opcion ->
                            if (i > 0) HorizontalDivider()
                            FilaDeOficina(opcion, opcion.oficina.id == marcada) { marcada = opcion.oficina.id }
                        }
                    }
                }
                if (estado.ubicacion != null) TextoTenue(stringResource(R.string.oficina_privacidad))
            }
        }
        Button(
            onClick = { oficina?.let(acciones.alElegir) },
            enabled = oficina != null,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        ) {
            Text(stringResource(R.string.oficina_elegir))
        }
    }
}

/** Sin ubicacion: la ciudad, y la opcion de dar el permiso para ordenar por distancia. */
@Composable
private fun SinUbicacion(estado: EstadoOficinas, banco: String, acciones: AccionesDeLaOficina) {
    val pedirPermiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (it.values.any { concedido -> concedido }) acciones.alRecargar()
    }
    Ciudad(estado.ciudad, estado.ciudades(banco), acciones.alElegirCiudad)
    TextButton(
        onClick = {
            pedirPermiso.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
            )
        },
    ) {
        Icon(Icons.Outlined.LocationOn, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
        Text(stringResource(R.string.oficina_usar_ubicacion))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Ciudad(ciudad: String?, ciudades: List<String>, alElegir: (String) -> Unit) {
    var abierto by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = abierto, onExpandedChange = { abierto = it }) {
        OutlinedTextField(
            value = ciudad.orEmpty(),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.oficina_ciudad)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = abierto) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            ciudades.forEach { nombre ->
                DropdownMenuItem(
                    text = { Text(nombre) },
                    onClick = {
                        alElegir(nombre)
                        abierto = false
                    },
                )
            }
        }
    }
}

@Composable
private fun FilaDeOficina(opcion: OficinaCercana, marcada: Boolean, alMarcar: () -> Unit) {
    val oficina = opcion.oficina
    Row(
        modifier = Modifier.fillMaxWidth().selectable(selected = marcada, onClick = alMarcar),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RadioButton(selected = marcada, onClick = null)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = nombreDeLaOficina(oficina),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            TextoTenue(oficina.direccion)
        }
        opcion.km?.let {
            Text(stringResource(R.string.oficina_km, enKm(it)), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** "Bancolombia · Oficina Cedritos". */
@Composable
internal fun nombreDeLaOficina(oficina: Oficina): String =
    stringResource(R.string.oficina_nombre, oficina.banco, oficina.nombre)

/** "0,8": con coma decimal, como se escribe en Colombia. */
private fun enKm(km: Double): String = String.format(Locale.forLanguageTag("es-CO"), "%.1f", km)
