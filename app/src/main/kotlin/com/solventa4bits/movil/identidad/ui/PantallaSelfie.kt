package com.solventa4bits.movil.identidad.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.solventa4bits.movil.R
import com.solventa4bits.movil.ui.BarraSolventa
import com.solventa4bits.movil.ui.tema.TemaSolventa

// Colores del Figma 08: el recuadro oscuro con el ovalo.
private val FondoDelOvalo = Color(0xFF0E2A2C)
private val TrazoClaro = Color(0xFFFFFFFF)

// El borde punteado del ovalo: trazo y espacio, en pixeles.
private val PUNTEADO_DEL_OVALO = floatArrayOf(14f, 10f)

// Las selfies son verticales.
private const val PROPORCION_DE_SELFIE = 0.78f

// El ovalo blanco del recuadro gris: alto relativo al recuadro y su proporcion.
private const val ALTO_DEL_OVALO = 0.8f
private const val PROPORCION_DEL_OVALO = 0.7f
private const val REDONDO = 50

/**
 * Figma 08: confirmar la identidad con una selfie (BITS-235, AC-4 y AC-5).
 * Sin selfie se explica como tomarla; con selfie se ve su estado.
 */
@Composable
fun PantallaSelfie(
    estado: EstadoSelfie,
    miniatura: ByteArray?,
    alTomarSelfie: () -> Unit,
    alVolverACedula: () -> Unit,
    alContinuar: () -> Unit,
    alCerrarSesion: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sinSelfie = estado.selfie == EstadoDeCara.SinFoto && miniatura == null
    Column(modifier = modifier.fillMaxSize()) {
        BarraSolventa(stringResource(R.string.selfie_titulo), alCerrarSesion)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column {
                Text(stringResource(R.string.selfie_confirma), style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = stringResource(if (sinSelfie) R.string.selfie_para_que else R.string.selfie_al_instante),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (sinSelfie) {
                Ovalo()
                Consejos()
            } else {
                TarjetaDeSelfie(estado.selfie, miniatura, alTomarSelfie)
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.medium)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(stringResource(R.string.selfie_que_hacemos_titulo), style = MaterialTheme.typography.titleSmall)
                Text(stringResource(R.string.selfie_que_hacemos_detalle), style = MaterialTheme.typography.bodySmall)
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (sinSelfie) {
                Button(onClick = alTomarSelfie, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.selfie_tomar))
                }
                TextButton(onClick = alVolverACedula) {
                    Text(stringResource(R.string.selfie_volver_a_cedula))
                }
            } else {
                // Lo que sigue es enviar la solicitud (BITS-278): por ahora "Continuar" no hace nada.
                Button(onClick = alContinuar, enabled = estado.puedeContinuar, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.cedula_continuar))
                }
            }
        }
    }
}

/** El ejemplo del Figma: un ovalo punteado con el icono de una cara. No es la camara. */
@Composable
private fun Ovalo() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(FondoDelOvalo, RoundedCornerShape(16.dp))
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .width(140.dp)
                .height(180.dp)
                .drawBehind {
                    drawOval(
                        color = TrazoClaro,
                        style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(PUNTEADO_DEL_OVALO)),
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.Face, contentDescription = null, tint = TrazoClaro, modifier = Modifier.size(36.dp))
        }
        Text(
            text = stringResource(R.string.selfie_pon_tu_cara),
            style = MaterialTheme.typography.bodyMedium,
            color = TrazoClaro,
        )
    }
}

@Composable
private fun Consejos() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.selfie_para_que_salga_bien),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        listOf(R.string.selfie_consejo_frente, R.string.selfie_consejo_luz, R.string.selfie_consejo_sin_accesorios)
            .forEach { consejo ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(stringResource(consejo), style = MaterialTheme.typography.bodyMedium)
                }
            }
    }
}

/** La selfie tomada (o un recuadro gris si se retomo del servidor) y su estado. */
@Composable
private fun TarjetaDeSelfie(estado: EstadoDeCara, miniatura: ByteArray?, alTomarSelfie: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.large)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        val imagen = remember(miniatura) {
            miniatura?.let { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() }
        }
        val recuadro = Modifier.width(100.dp).aspectRatio(PROPORCION_DE_SELFIE).clip(MaterialTheme.shapes.medium)
        if (imagen != null) {
            Image(bitmap = imagen, contentDescription = null, contentScale = ContentScale.Crop, modifier = recuadro)
        } else {
            Box(recuadro.background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .fillMaxHeight(ALTO_DEL_OVALO)
                        .aspectRatio(PROPORCION_DEL_OVALO)
                        .border(2.dp, TrazoClaro, RoundedCornerShape(REDONDO)),
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.selfie_tu_selfie), style = MaterialTheme.typography.titleSmall)
            Text(text = mensajeDe(estado), style = MaterialTheme.typography.bodySmall, color = colorDe(estado))
            if (estado.sePuedeRepetir) {
                TextButton(onClick = alTomarSelfie, contentPadding = PaddingValues(0.dp)) {
                    Text(stringResource(R.string.selfie_tomarla_otra_vez))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PantallaSelfiePreview() {
    TemaSolventa {
        PantallaSelfie(
            estado = EstadoSelfie(cargando = false),
            miniatura = null,
            alTomarSelfie = {},
            alVolverACedula = {},
            alContinuar = {},
            alCerrarSesion = {},
        )
    }
}
