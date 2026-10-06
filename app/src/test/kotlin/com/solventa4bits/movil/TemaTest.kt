package com.solventa4bits.movil

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.solventa4bits.movil.ui.tema.EsquemaClaro
import com.solventa4bits.movil.ui.tema.EsquemaOscuro
import com.solventa4bits.movil.ui.tema.Formas
import com.solventa4bits.movil.ui.tema.Tipografia
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Verifica que el tema que se compila es el del archivo de Figma. Si alguien
 * escribe un color a mano o se regenera contra otro archivo, esto falla.
 *
 * Los valores esperados son los del Design System, los mismos que usa el
 * cliente web: esa coincidencia es la paridad entre los dos canales.
 */
class TemaTest {

    @Test
    @DisplayName("el esquema claro trae los colores del Design System")
    fun esquemaClaro() {
        assertEquals(Color(0xFF006971), EsquemaClaro.primary)
        assertEquals(Color(0xFF9DF0FA), EsquemaClaro.primaryContainer)
        assertEquals(Color(0xFFBA1A1A), EsquemaClaro.error)
        assertEquals(Color(0xFFF5FAFB), EsquemaClaro.surface)
    }

    @Test
    @DisplayName("el esquema oscuro trae los colores del Design System")
    fun esquemaOscuro() {
        assertEquals(Color(0xFF81D3DD), EsquemaOscuro.primary)
        assertEquals(Color(0xFF0E1415), EsquemaOscuro.surface)
    }

    @Test
    @DisplayName("la escala tipografica conserva los tamanos de Figma")
    fun escalaTipografica() {
        assertEquals(57f, Tipografia.displayLarge.fontSize.value)
        assertEquals(16f, Tipografia.bodyLarge.fontSize.value)
        assertEquals(11f, Tipografia.labelSmall.fontSize.value)
    }

    @Test
    @DisplayName("los radios salen del Design System")
    fun radios() {
        assertEquals(RoundedCornerShape(4.dp), Formas.extraSmall)
        assertEquals(RoundedCornerShape(12.dp), Formas.medium)
        assertEquals(RoundedCornerShape(28.dp), Formas.extraLarge)
    }
}
