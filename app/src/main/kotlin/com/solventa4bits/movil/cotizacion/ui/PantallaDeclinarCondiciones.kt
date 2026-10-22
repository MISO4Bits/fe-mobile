package com.solventa4bits.movil.cotizacion.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.solventa4bits.movil.R

/**
 * Figma 05K: confirma que el cliente quiere declinar, porque la cotizacion se
 * elimina por completo (BITS-337, AC-1). Si el borrado falla, lo dice aqui
 * mismo y deja reintentar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PanelDeclinarCondiciones(
    declinando: Boolean,
    fallo: Boolean,
    alDeclinar: () -> Unit,
    alVolver: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = alVolver,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.declinar_etiqueta),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.declinar_titulo),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = stringResource(R.string.declinar_detalle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (fallo) {
                Text(
                    text = stringResource(R.string.declinar_fallo),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Button(onClick = alDeclinar, enabled = !declinando, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.declinar_confirmar))
                }
                TextButton(onClick = alVolver) {
                    Text(stringResource(R.string.declinar_volver))
                }
            }
        }
    }
}
