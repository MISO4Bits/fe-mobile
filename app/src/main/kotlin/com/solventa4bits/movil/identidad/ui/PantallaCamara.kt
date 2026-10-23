package com.solventa4bits.movil.identidad.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.solventa4bits.movil.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

// Colores del Figma 02: fondo oscuro y texto claro sobre la camara.
private val FondoDeCamara = Color(0xFF0E2A2C)
private val TarjetaDeConsejos = Color(0xFF173B3E)
private val TextoClaro = Color(0xFFFFFFFF)
private val TextoTenue = Color(0xFFB7C9CB)

/**
 * Figma 02: la camara con el marco de la cedula (BITS-275, AC-6). Desde aqui
 * tambien se elige una foto de la galeria. [alTenerFoto] recibe la foto tal
 * como sale: el ViewModel la prepara antes de enviarla.
 */
@Composable
fun PantallaCamara(alTenerFoto: (ByteArray) -> Unit, modifier: Modifier = Modifier) {
    val contexto = LocalContext.current
    val alcance = rememberCoroutineScope()
    var conPermiso by remember { mutableStateOf(tienePermiso(contexto)) }
    var fallo by remember { mutableStateOf(false) }
    val pedirPermiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        conPermiso = it
    }
    // El selector de fotos de Android no pide permisos.
    val galeria = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        alcance.launch {
            val foto = withContext(Dispatchers.IO) { leer(contexto, uri) }
            if (foto != null) alTenerFoto(foto) else fallo = true
        }
    }
    DisposableEffect(Unit) {
        if (!conPermiso) pedirPermiso.launch(Manifest.permission.CAMERA)
        onDispose {}
    }
    val captura = remember { ImageCapture.Builder().build() }

    Column(
        modifier = modifier.fillMaxSize().background(FondoDeCamara).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.camara_titulo),
            style = MaterialTheme.typography.titleLarge,
            color = TextoClaro,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.camara_subtitulo),
            style = MaterialTheme.typography.bodyMedium,
            color = TextoTenue,
            textAlign = TextAlign.Center,
        )
        Box(
            modifier = Modifier.fillMaxWidth().aspectRatio(PROPORCION_DE_CEDULA),
            contentAlignment = Alignment.Center,
        ) {
            if (conPermiso) {
                VistaDeCamara(captura)
                Marco()
            } else {
                SinPermiso { pedirPermiso.launch(Manifest.permission.CAMERA) }
            }
        }
        Consejos()
        if (fallo) {
            Text(
                text = stringResource(R.string.camara_fallo),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.errorContainer,
            )
        }
        Spacer(Modifier.weight(1f))
        if (conPermiso) {
            Disparador {
                fallo = false
                tomarFoto(contexto, captura, alTenerFoto) { fallo = true }
            }
        }
        TextButton(
            onClick = {
                fallo = false
                galeria.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
        ) {
            Text(stringResource(R.string.camara_galeria), color = MaterialTheme.colorScheme.inversePrimary)
        }
    }
}

/** La imagen de la camara trasera en vivo, ligada a la pantalla: se apaga al salir. */
@Composable
private fun VistaDeCamara(captura: ImageCapture) {
    val contexto = LocalContext.current
    val ciclo = LocalLifecycleOwner.current
    val vista = remember { PreviewView(contexto) }
    DisposableEffect(ciclo) {
        val proveedor = ProcessCameraProvider.getInstance(contexto)
        proveedor.addListener(
            {
                val vivo = Preview.Builder().build().also { it.surfaceProvider = vista.surfaceProvider }
                proveedor.get().run {
                    unbindAll()
                    bindToLifecycle(ciclo, CameraSelector.DEFAULT_BACK_CAMERA, vivo, captura)
                }
            },
            ContextCompat.getMainExecutor(contexto),
        )
        onDispose { proveedor.get().unbindAll() }
    }
    AndroidView(factory = { vista }, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)))
}

/** El marco blanco con sus cuatro esquinas, sobre la camara. */
@Composable
private fun BoxScope.Marco() {
    Box(Modifier.matchParentSize().border(2.dp, TextoClaro, RoundedCornerShape(4.dp)))
    listOf(Alignment.TopStart, Alignment.TopEnd, Alignment.BottomStart, Alignment.BottomEnd).forEach { esquina ->
        Box(
            Modifier
                .align(esquina)
                .size(20.dp)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp)),
        )
    }
}

@Composable
private fun SinPermiso(alDarPermiso: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.camara_sin_permiso),
            style = MaterialTheme.typography.bodyMedium,
            color = TextoClaro,
            textAlign = TextAlign.Center,
        )
        Button(onClick = alDarPermiso) { Text(stringResource(R.string.camara_dar_permiso)) }
    }
}

/** Los consejos del Figma, como texto fijo: no hay deteccion automatica. */
@Composable
private fun Consejos() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(TarjetaDeConsejos, RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        listOf(R.string.camara_consejo_luz, R.string.camara_consejo_reflejos, R.string.camara_consejo_esquinas)
            .forEach { consejo ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = TextoTenue)
                    Text(stringResource(consejo), style = MaterialTheme.typography.bodyMedium, color = TextoClaro)
                }
            }
    }
}

@Composable
private fun Disparador(alTocar: () -> Unit) {
    val descripcion = stringResource(R.string.camara_tomar)
    Box(
        modifier = Modifier
            .size(72.dp)
            .border(4.dp, TextoClaro, CircleShape)
            .padding(8.dp)
            .background(MaterialTheme.colorScheme.primary, CircleShape)
            .clickable(onClick = alTocar)
            .semantics { contentDescription = descripcion },
    )
}

private fun tienePermiso(contexto: Context) =
    ContextCompat.checkSelfPermission(contexto, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

// CameraX guarda la foto con su marca de orientacion; el preparador la endereza.
private fun tomarFoto(
    contexto: Context,
    captura: ImageCapture,
    alTenerFoto: (ByteArray) -> Unit,
    alFallar: () -> Unit,
) {
    val archivo = File.createTempFile("cedula", ".jpg", contexto.cacheDir)
    captura.takePicture(
        ImageCapture.OutputFileOptions.Builder(archivo).build(),
        ContextCompat.getMainExecutor(contexto),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(resultado: ImageCapture.OutputFileResults) {
                val foto = archivo.readBytes()
                archivo.delete()
                alTenerFoto(foto)
            }

            override fun onError(error: ImageCaptureException) {
                archivo.delete()
                alFallar()
            }
        },
    )
}

private fun leer(contexto: Context, uri: Uri): ByteArray? =
    try {
        contexto.contentResolver.openInputStream(uri)?.use { it.readBytes() }
    } catch (_: IOException) {
        null
    }
