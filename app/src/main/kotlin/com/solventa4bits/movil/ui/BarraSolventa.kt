package com.solventa4bits.movil.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import com.solventa4bits.movil.R

/**
 * Barra superior de las pantallas con sesion: el menu, el titulo de la
 * pantalla y el monograma. El menu tiene una sola opcion, cerrar sesion.
 */
@Composable
fun BarraSolventa(titulo: String, alCerrarSesion: () -> Unit) {
    var menuAbierto by remember { mutableStateOf(false) }
    // La sesion solo se cierra despues de que el cliente confirma.
    var confirmando by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box {
            IconButton(onClick = { menuAbierto = true }) {
                Icon(Icons.Filled.Menu, contentDescription = stringResource(R.string.barra_menu))
            }
            DropdownMenu(expanded = menuAbierto, onDismissRequest = { menuAbierto = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.barra_cerrar_sesion)) },
                    onClick = {
                        menuAbierto = false
                        confirmando = true
                    },
                )
            }
        }
        Text(
            text = titulo,
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
            title = { Text(stringResource(R.string.barra_confirmar_cierre)) },
            confirmButton = {
                TextButton(onClick = alCerrarSesion) {
                    Text(stringResource(R.string.barra_cerrar_sesion))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmando = false }) {
                    Text(stringResource(R.string.barra_cancelar))
                }
            },
        )
    }
}
