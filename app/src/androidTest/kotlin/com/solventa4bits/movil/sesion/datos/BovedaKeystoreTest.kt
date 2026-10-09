package com.solventa4bits.movil.sesion.datos

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

/**
 * La boveda contra el Keystore real del dispositivo. Corre en un emulador o
 * telefono: el Keystore no existe en las pruebas unitarias.
 */
@RunWith(AndroidJUnit4::class)
class BovedaKeystoreTest {

    private val contexto: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val boveda = BovedaKeystore(contexto) { _, _, _ -> }

    @After
    fun limpiar() {
        boveda.borrar(CLAVE)
    }

    @Test
    fun devuelveLoQueSeGuardo() {
        boveda.guardar(CLAVE, SECRETO)

        assertEquals(SECRETO, boveda.leer(CLAVE))
    }

    @Test
    fun loQueQuedaEnDiscoNoEstaEnClaro() {
        boveda.guardar(CLAVE, SECRETO)

        val enDisco = contexto.getSharedPreferences("boveda", Context.MODE_PRIVATE).getString(CLAVE, null)
        assertNotNull(enDisco)
        assertFalse(enDisco!!.contains(SECRETO))
    }

    @Test
    fun alBorrarNoQuedaNada() {
        boveda.guardar(CLAVE, SECRETO)

        boveda.borrar(CLAVE)

        assertNull(boveda.leer(CLAVE))
    }

    @Test
    fun siLoGuardadoFueAlteradoNoDevuelveNada() {
        boveda.guardar(CLAVE, SECRETO)
        contexto.getSharedPreferences("boveda", Context.MODE_PRIVATE)
            .edit().putString(CLAVE, "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA").commit()

        assertNull(boveda.leer(CLAVE))
    }

    private companion object {
        const val CLAVE = "prueba"
        const val SECRETO = "token-secreto-de-prueba"
    }
}
