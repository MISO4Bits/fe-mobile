package com.solventa4bits.movil

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.media.ExifInterface
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.solventa4bits.movil.identidad.datos.PreparadorDeFotos
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.Random

/** Lo que BITS-275 pide de la foto antes de enviarla. Usa imagenes reales de Android. */
@RunWith(AndroidJUnit4::class)
class PreparadorDeFotosTest {

    @Test
    fun unaFotoGrandeYAcostadaSaleDerechaA1600pxYSinMetadatos() {
        // Como la guarda una camara: 4000 x 3000, con la marca de girar 90 y GPS.
        val original = fotoDePrueba(ancho = 4000, alto = 3000, orientacion = ExifInterface.ORIENTATION_ROTATE_90)

        val lista = checkNotNull(PreparadorDeFotos.preparar(original))

        val medidas = medidasDe(lista)
        assertEquals(1200, medidas.outWidth)
        assertEquals(1600, medidas.outHeight)
        assertTrue("pesa ${lista.size} bytes", lista.size < PreparadorDeFotos.PESO_MAXIMO)
        val exif = ExifInterface(lista.inputStream())
        assertNull(exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE))
        assertNull(exif.getAttribute(ExifInterface.TAG_MAKE))
    }

    @Test
    fun unaFotoPequenaNoSeAgranda() {
        val original = fotoDePrueba(ancho = 800, alto = 600, orientacion = ExifInterface.ORIENTATION_NORMAL)

        val medidas = medidasDe(checkNotNull(PreparadorDeFotos.preparar(original)))

        assertEquals(800, medidas.outWidth)
        assertEquals(600, medidas.outHeight)
    }

    @Test
    fun loQueNoEsUnaImagenNoSePrepara() {
        assertNull(PreparadorDeFotos.preparar("no es una foto".toByteArray()))
    }

    private fun medidasDe(jpeg: ByteArray) = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
        BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size, this)
    }

    // Cuadros de colores: el JPEG pesa como una foto real y no como un color plano.
    private fun fotoDePrueba(ancho: Int, alto: Int, orientacion: Int): ByteArray {
        val azar = Random(SEMILLA)
        val bitmap = Bitmap.createBitmap(ancho, alto, Bitmap.Config.ARGB_8888)
        val lienzo = Canvas(bitmap)
        val pintura = Paint()
        for (x in 0 until ancho step CUADRO) {
            for (y in 0 until alto step CUADRO) {
                pintura.color = Color.rgb(azar.nextInt(256), azar.nextInt(256), azar.nextInt(256))
                lienzo.drawRect(x.toFloat(), y.toFloat(), (x + CUADRO).toFloat(), (y + CUADRO).toFloat(), pintura)
            }
        }
        val archivo = File.createTempFile("foto", ".jpg", contexto().cacheDir)
        try {
            archivo.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, CALIDAD_DE_CAMARA, it) }
            ExifInterface(archivo.absolutePath).apply {
                setAttribute(ExifInterface.TAG_ORIENTATION, orientacion.toString())
                setAttribute(ExifInterface.TAG_GPS_LATITUDE, "4/1,36/1,0/1")
                setAttribute(ExifInterface.TAG_MAKE, "Celular de prueba")
                saveAttributes()
            }
            return archivo.readBytes()
        } finally {
            bitmap.recycle()
            archivo.delete()
        }
    }

    private fun contexto() = InstrumentationRegistry.getInstrumentation().targetContext

    private companion object {
        const val SEMILLA = 42L
        const val CALIDAD_DE_CAMARA = 95
        const val CUADRO = 16
    }
}
