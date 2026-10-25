package com.solventa4bits.movil.identidad.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.solventa4bits.movil.R
import com.solventa4bits.movil.identidad.dominio.CaraDeCedula
import com.solventa4bits.movil.identidad.dominio.MotivoDeRechazo
import com.solventa4bits.movil.ui.BarraSolventa
import com.solventa4bits.movil.ui.tema.TemaSolventa

// Las fotos de la cedula son horizontales, como la tarjeta.
internal const val PROPORCION_DE_CEDULA = 1.6f

// El borde punteado del recuadro sin foto: trazo y espacio, en pixeles.
private val PUNTEADO = floatArrayOf(12f, 8f)

/**
 * Figma 03 a 03D: las dos caras de la cedula, cada una con su estado
 * (BITS-275, AC-2 y AC-4). Un solo boton, "Continuar", y sin "Atras" (AC-6).
 */
@Composable
fun PantallaCedula(
    estado: EstadoCedula,
    alTomarFoto: (CaraDeCedula) -> Unit,
    alContinuar: () -> Unit,
    alCerrarSesion: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        BarraSolventa(stringResource(R.string.cedula_titulo), alCerrarSesion)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column {
                Text(stringResource(R.string.cedula_dos_caras), style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = stringResource(R.string.cedula_subtitulo),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.large)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CaraDeCedula.entries.forEach { cara ->
                    Cara(
                        cara = cara,
                        estado = estado.estadoDe(cara),
                        miniatura = estado.miniaturas[cara],
                        alTomarFoto = { alTomarFoto(cara) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.medium)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(stringResource(R.string.cedula_que_hacemos_titulo), style = MaterialTheme.typography.titleSmall)
                Text(stringResource(R.string.cedula_que_hacemos_detalle), style = MaterialTheme.typography.bodySmall)
            }
        }
        // Lo que sigue es la selfie (BITS-235): por ahora "Continuar" no hace nada.
        Button(
            onClick = alContinuar,
            enabled = estado.puedeContinuar,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        ) {
            Text(stringResource(R.string.cedula_continuar))
        }
    }
}

@Composable
private fun Cara(
    cara: CaraDeCedula,
    estado: EstadoDeCara,
    miniatura: ByteArray?,
    alTomarFoto: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Recuadro(estado, miniatura, alTomarFoto)
        Text(
            text = stringResource(if (cara == CaraDeCedula.FRENTE) R.string.cedula_frente else R.string.cedula_reverso),
            style = MaterialTheme.typography.titleSmall,
        )
        Text(text = mensajeDe(estado), style = MaterialTheme.typography.bodySmall, color = colorDe(estado))
        // Revisando, sin foto o aceptada no se repite desde el texto: se toca el recuadro.
        if (estado.sePuedeRepetir) {
            TextButton(onClick = alTomarFoto, contentPadding = PaddingValues(0.dp)) {
                Text(stringResource(R.string.cedula_tomar_otra_vez))
            }
        }
    }
}

/** Sin foto: borde punteado. Con foto: la miniatura. Guardada sin foto en el telefono: gris. */
@Composable
private fun Recuadro(estado: EstadoDeCara, miniatura: ByteArray?, alTomarFoto: () -> Unit) {
    val forma = MaterialTheme.shapes.medium
    val base = Modifier
        .fillMaxWidth()
        .aspectRatio(PROPORCION_DE_CEDULA)
        .clip(forma)
        .clickable(enabled = estado != EstadoDeCara.Revisando, onClick = alTomarFoto)
    val imagen = remember(miniatura) {
        miniatura?.let { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() }
    }
    when {
        imagen != null -> Image(
            bitmap = imagen,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = base,
        )
        estado == EstadoDeCara.SinFoto -> {
            val color = MaterialTheme.colorScheme.primary
            Box(
                modifier = base.drawBehind {
                    drawRoundRect(
                        color = color,
                        cornerRadius = CornerRadius(8.dp.toPx()),
                        style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(PUNTEADO)),
                    )
                },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.cedula_toca_para_tomar),
                    style = MaterialTheme.typography.bodySmall,
                    color = color,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(8.dp),
                )
            }
        }
        else -> Box(base.background(MaterialTheme.colorScheme.surfaceVariant))
    }
}

@Preview(showBackground = true)
@Composable
private fun PantallaCedulaPreview() {
    TemaSolventa {
        PantallaCedula(
            estado = EstadoCedula(
                cargando = false,
                caras = mapOf(
                    CaraDeCedula.FRENTE to EstadoDeCara.Aceptada,
                    CaraDeCedula.REVERSO to EstadoDeCara.Reintentar(MotivoDeRechazo.BORROSA, 2),
                ),
            ),
            alTomarFoto = {},
            alContinuar = {},
            alCerrarSesion = {},
        )
    }
}
