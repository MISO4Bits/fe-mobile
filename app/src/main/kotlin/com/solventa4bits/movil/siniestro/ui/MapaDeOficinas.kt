package com.solventa4bits.movil.siniestro.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.generated.CircleAnnotation
import com.solventa4bits.movil.R
import com.solventa4bits.movil.siniestro.dominio.Oficina
import com.solventa4bits.movil.siniestro.dominio.Ubicacion

private const val ACERCAMIENTO = 12.5
private const val RADIO_DE_OFICINA = 6.0
private const val RADIO_DE_LA_ELEGIDA = 10.0
private const val RADIO_DEL_CLIENTE = 7.0
private const val BORDE = 2.0
private val AZUL_DEL_CLIENTE = Color(0xFF1A3A6B)

/**
 * M06A: el mapa de Mapbox con las oficinas y, si se sabe, donde esta el cliente. Se centra en la
 * oficina elegida. Sin token de Mapbox (local.properties) no se dibuja: la lista basta para elegir.
 */
@Composable
internal fun MapaDeOficinas(oficinas: List<Oficina>, elegida: Oficina?, cliente: Ubicacion?) {
    if (stringResource(R.string.mapbox_access_token).isBlank()) return
    val centro = elegida ?: oficinas.firstOrNull() ?: return
    val vista = rememberMapViewportState {
        setCameraOptions(CameraOptions.Builder().center(punto(centro)).zoom(ACERCAMIENTO).build())
    }
    LaunchedEffect(centro.id) {
        vista.flyTo(CameraOptions.Builder().center(punto(centro)).build(), null, null)
    }
    val color = MaterialTheme.colorScheme.primary
    MapboxMap(
        modifier = Modifier.fillMaxWidth().height(180.dp).clip(MaterialTheme.shapes.medium),
        mapViewportState = vista,
    ) {
        oficinas.forEach { oficina ->
            CircleAnnotation(point = punto(oficina)) {
                circleRadius = if (oficina.id == elegida?.id) RADIO_DE_LA_ELEGIDA else RADIO_DE_OFICINA
                circleColor = color
                circleStrokeWidth = BORDE
                circleStrokeColor = Color.White
            }
        }
        cliente?.let {
            CircleAnnotation(point = Point.fromLngLat(it.longitud, it.latitud)) {
                circleRadius = RADIO_DEL_CLIENTE
                circleColor = AZUL_DEL_CLIENTE
                circleStrokeWidth = BORDE
                circleStrokeColor = Color.White
            }
        }
    }
}

private fun punto(oficina: Oficina): Point = Point.fromLngLat(oficina.longitud, oficina.latitud)
