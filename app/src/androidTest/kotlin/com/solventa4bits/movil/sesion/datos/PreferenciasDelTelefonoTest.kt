package com.solventa4bits.movil.sesion.datos

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.solventa4bits.movil.sesion.dominio.PreferenciaDeHuella
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Las preferencias contra el archivo real del dispositivo. Corre en un
 * emulador o telefono: SharedPreferences no existe en las pruebas unitarias.
 */
@RunWith(AndroidJUnit4::class)
class PreferenciasDelTelefonoTest {

    private val contexto: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val preferencias = PreferenciasDelTelefono(contexto)

    // Antes y despues: ninguna prueba hereda lo que otra dejo en el archivo.
    @Before
    @After
    fun limpiar() {
        contexto.getSharedPreferences("preferencias", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun sinNadaGuardadoNoHayDecision() {
        assertEquals(PreferenciaDeHuella.SIN_DECIDIR, preferencias.huella())
    }

    @Test
    fun devuelveLoQueSeGuardo() {
        preferencias.guardarHuella(PreferenciaDeHuella.ACTIVADA)

        assertEquals(PreferenciaDeHuella.ACTIVADA, preferencias.huella())
    }

    @Test
    fun unValorQueLaAppNoConoceCuentaComoSinDecidir() {
        contexto.getSharedPreferences("preferencias", Context.MODE_PRIVATE)
            .edit().putString("huella", "LO_QUE_SEA").commit()

        assertEquals(PreferenciaDeHuella.SIN_DECIDIR, preferencias.huella())
    }
}
