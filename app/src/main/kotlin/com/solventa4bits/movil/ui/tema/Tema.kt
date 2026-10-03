package com.solventa4bits.movil.ui.tema

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/**
 * Tema de Solventa.
 *
 * Los colores, la escala tipografica y los radios salen de las variables del
 * archivo de Figma, exportadas por tools/tokens_to_kotlin.py. Son los mismos
 * tokens que usa el cliente web, que es lo que sostiene la paridad de estilo
 * entre los dos canales.
 *
 * El producto es claro: el prototipo se diseno en claro y es lo que se valido
 * con usuarios. El esquema oscuro queda declarado porque el archivo de Figma
 * lo trae, y solo se usa si quien llama lo pide de forma explicita.
 */
@Composable
fun TemaSolventa(
    oscuro: Boolean = false,
    contenido: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (oscuro) EsquemaOscuro else EsquemaClaro,
        typography = Tipografia,
        shapes = Formas,
        content = contenido,
    )
}

/** Sigue la preferencia del sistema. No se usa todavia: el producto es claro. */
@Composable
fun TemaSolventaSegunSistema(contenido: @Composable () -> Unit) {
    TemaSolventa(oscuro = isSystemInDarkTheme(), contenido = contenido)
}
