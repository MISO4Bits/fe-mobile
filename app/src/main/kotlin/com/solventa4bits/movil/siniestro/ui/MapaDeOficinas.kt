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
private const val RADIO_DEL_LUGAR = 6.0
private const val RADIO_DEL_ELEGIDO = 10.0
private const val RADIO_DEL_CLIENTE = 7.0
private const val BORDE = 2.0
private val AZUL_DEL_CLIENTE = Color(0xFF1A3A6B)

/** Un lugar en el mapa: una oficina o un prestador. */
internal data class LugarEnElMapa(val id: String, val latitud: Double, val longitud: Double)

/**
 * M06A: el mapa de Mapbox con las oficinas y, si se sabe, donde esta el cliente. Se centra en la
 * oficina elegida. Sin token de Mapbox (local.properties) no se dibuja: la lista basta para elegir.
 */
@Composable
internal fun MapaDeOficinas(oficinas: List<Oficina>, elegida: Oficina?, cliente: Ubicacion?) {
    val centro = elegida ?: oficinas.firstOrNull() ?: return
    MapaDeLugares(
        lugares = oficinas.map { LugarEnElMapa(it.id, it.latitud, it.longitud) },
        elegido = elegida?.id,
        centro = Ubicacion(centro.latitud, centro.longitud),
        cliente = cliente,
    )
}

/** Los [lugares], con el [elegido] mas grande, y el [cliente] en otro color. Se mueve cuando cambia el [centro]. */
@Composable
internal fun MapaDeLugares(lugares: List<LugarEnElMapa>, elegido: String?, centro: Ubicacion, cliente: Ubicacion?) {
    if (stringResource(R.string.mapbox_access_token).isBlank()) return
    val vista = rememberMapViewportState {
        setCameraOptions(CameraOptions.Builder().center(punto(centro)).zoom(ACERCAMIENTO).build())
    }
    LaunchedEffect(centro) {
        vista.flyTo(CameraOptions.Builder().center(punto(centro)).build(), null, null)
    }
    val color = MaterialTheme.colorScheme.primary
    MapboxMap(
        modifier = Modifier.fillMaxWidth().height(180.dp).clip(MaterialTheme.shapes.medium),
        mapViewportState = vista,
    ) {
        lugares.forEach { lugar ->
            CircleAnnotation(point = Point.fromLngLat(lugar.longitud, lugar.latitud)) {
                circleRadius = if (lugar.id == elegido) RADIO_DEL_ELEGIDO else RADIO_DEL_LUGAR
                circleColor = color
                circleStrokeWidth = BORDE
                circleStrokeColor = Color.White
            }
        }
        cliente?.let {
            CircleAnnotation(point = punto(it)) {
                circleRadius = RADIO_DEL_CLIENTE
                circleColor = AZUL_DEL_CLIENTE
                circleStrokeWidth = BORDE
                circleStrokeColor = Color.White
            }
        }
    }
}

private fun punto(ubicacion: Ubicacion): Point = Point.fromLngLat(ubicacion.longitud, ubicacion.latitud)
