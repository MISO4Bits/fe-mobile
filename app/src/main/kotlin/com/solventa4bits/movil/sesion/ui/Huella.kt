package com.solventa4bits.movil.sesion.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.solventa4bits.movil.R

// Piezas que comparten las dos pantallas de la huella (Figma 01 "Entrar con
// tu huella" y 02 "Activar huella"): el icono grande y la nota de seguridad.

/** El icono de la huella dentro de un circulo. Con [alTocar], invita a tocarlo. */
@Composable
internal fun IconoDeHuella(alTocar: (() -> Unit)? = null) {
    val circulo = Modifier
        .size(120.dp)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.surfaceContainer)
    Box(
        modifier = if (alTocar == null) circulo else circulo.clickable(onClick = alTocar),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_huella),
            contentDescription = if (alTocar == null) null else stringResource(R.string.huella_toca_el_sensor),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(60.dp),
        )
    }
}

/** "Tu huella no sale del telefono": lo que dice BITS-82 contado al cliente. */
@Composable
internal fun NotaSobreLaHuella() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.medium)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(stringResource(R.string.huella_nota_titulo), style = MaterialTheme.typography.titleSmall)
        Text(
            text = stringResource(R.string.huella_nota_detalle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
