package com.solventa4bits.movil.solicitud.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.solventa4bits.movil.solicitud.dominio.CodigoDePendiente
import com.solventa4bits.movil.solicitud.dominio.Pendiente

// La linea de tiempo del resultado de la solicitud (Figma 08, 08A y 08B) y sus iconos.

/** Un paso de la linea de tiempo: su icono, su nombre, su estado y, si hay, lo que falta. */
@Composable
internal fun Paso(
    icono: @Composable () -> Unit,
    titulo: String,
    detalle: String,
    colorDelDetalle: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    contenido: @Composable () -> Unit = {},
) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) { icono() }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(titulo, style = MaterialTheme.typography.titleSmall)
            Text(detalle, style = MaterialTheme.typography.bodySmall, color = colorDelDetalle)
            contenido()
        }
    }
}

/** Lo que falta, con el sobre si es del correo y la persona si lo revisa alguien. */
@Composable
internal fun PendienteDeLaSolicitud(pendiente: Pendiente) {
    val icono: ImageVector =
        if (pendiente.codigo == CodigoDePendiente.CORREO_SIN_CONFIRMAR) Icons.Outlined.Email else Icons.Outlined.Person
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icono, contentDescription = null, modifier = Modifier.size(18.dp))
        Text(pendiente.mensaje, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
internal fun IconoListo(tamano: Dp = 24.dp) {
    Icon(
        Icons.Outlined.CheckCircle,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(tamano),
    )
}

@Composable
internal fun IconoRechazo(tamano: Dp) {
    val rojo = MaterialTheme.colorScheme.error
    Box(
        modifier = Modifier.size(tamano).border(2.dp, rojo, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Filled.Close, contentDescription = null, tint = rojo, modifier = Modifier.size(tamano / 2))
    }
}

@Composable
internal fun IconoVacio() {
    Box(Modifier.size(20.dp).border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape))
}
