package com.solventa4bits.movil.inicio.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.solventa4bits.movil.R
import com.solventa4bits.movil.ui.tema.TemaSolventa

/** Inicio del cliente que todavia no tiene seguros ni cotizaciones (Figma 02A). */
@Composable
fun PantallaInicio(
    alCotizar: () -> Unit,
    alCerrarSesion: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        BarraDeInicio(alCerrarSesion)
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column {
                Text(
                    text = stringResource(R.string.inicio_sin_seguros),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = stringResource(R.string.inicio_lema),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            TarjetaCotizar(alCotizar)
            TarjetaNuevaCotizacion(alCotizar)
        }
    }
}

@Composable
private fun BarraDeInicio(alCerrarSesion: () -> Unit) {
    var menuAbierto by remember { mutableStateOf(false) }
    // La sesion solo se cierra despues de que el cliente confirma.
    var confirmando by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box {
            IconButton(onClick = { menuAbierto = true }) {
                Icon(Icons.Filled.Menu, contentDescription = stringResource(R.string.inicio_menu))
            }
            DropdownMenu(expanded = menuAbierto, onDismissRequest = { menuAbierto = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.inicio_cerrar_sesion)) },
                    onClick = {
                        menuAbierto = false
                        confirmando = true
                    },
                )
            }
        }
        Text(
            text = stringResource(R.string.inicio_titulo),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(1f).padding(start = 8.dp),
        )
        Image(
            painter = painterResource(R.drawable.monograma_solventa),
            contentDescription = null,
            modifier = Modifier.padding(end = 12.dp).height(28.dp),
        )
    }

    if (confirmando) {
        AlertDialog(
            onDismissRequest = { confirmando = false },
            title = { Text(stringResource(R.string.inicio_confirmar_cierre)) },
            confirmButton = {
                TextButton(onClick = alCerrarSesion) {
                    Text(stringResource(R.string.inicio_cerrar_sesion))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmando = false }) {
                    Text(stringResource(R.string.inicio_cancelar))
                }
            },
        )
    }
}

@Composable
private fun TarjetaCotizar(alCotizar: () -> Unit) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.inicio_sin_seguros),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.inicio_cotiza_detalle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Button(onClick = alCotizar) {
                Text(stringResource(R.string.inicio_cotizar))
            }
        }
    }
}

@Composable
private fun TarjetaNuevaCotizacion(alCotizar: () -> Unit) {
    OutlinedCard(onClick = alCotizar, modifier = Modifier.width(168.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(
                Icons.Filled.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.inicio_nueva_cotizacion),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = stringResource(R.string.inicio_nueva_cotizacion_detalle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.inicio_empezar),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PantallaInicioPreview() {
    TemaSolventa { PantallaInicio(alCotizar = {}, alCerrarSesion = {}) }
}
