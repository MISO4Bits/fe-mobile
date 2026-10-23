package com.solventa4bits.movil.identidad.datos

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import java.io.ByteArrayOutputStream
import java.io.IOException

/**
 * Deja la foto como la pide BITS-275 antes de enviarla: a unos 1600 px por el
 * lado largo, en menos de 1,5 MB, derecha y sin metadatos. Los metadatos (GPS,
 * modelo del celular) se pierden solos al volver a guardarla como JPEG.
 */
object PreparadorDeFotos {

    const val LADO_MAXIMO = 1600
    const val PESO_MAXIMO = 1_500_000

    private const val CALIDAD_INICIAL = 90
    private const val CALIDAD_MINIMA = 50
    private const val PASO_DE_CALIDAD = 10

    private const val GIRO_90 = 90f
    private const val GIRO_180 = 180f
    private const val GIRO_270 = 270f

    /** El JPEG listo para enviar, o null si [original] no es una imagen. */
    fun preparar(original: ByteArray): ByteArray? {
        val leida = leerReducida(original) ?: return null
        val derecha = Bitmap.createBitmap(
            leida,
            0,
            0,
            leida.width,
            leida.height,
            ajuste(leida, orientacionDe(original)),
            true,
        )
        if (derecha !== leida) leida.recycle()
        return comprimir(derecha).also { derecha.recycle() }
    }

    // Lee la foto ya reducida a la potencia de 2 mas cercana sin bajar de
    // LADO_MAXIMO: una foto de 12 MP completa ocuparia demasiada memoria.
    private fun leerReducida(original: ByteArray): Bitmap? {
        val medidas = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(original, 0, original.size, medidas)
        if (medidas.outWidth <= 0 || medidas.outHeight <= 0) return null
        var muestreo = 1
        while (maxOf(medidas.outWidth, medidas.outHeight) / (muestreo * 2) >= LADO_MAXIMO) muestreo *= 2
        val opciones = BitmapFactory.Options().apply { inSampleSize = muestreo }
        return BitmapFactory.decodeByteArray(original, 0, original.size, opciones)
    }

    private fun orientacionDe(original: ByteArray): Int =
        try {
            ExifInterface(original.inputStream())
                .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } catch (_: IOException) {
            ExifInterface.ORIENTATION_NORMAL
        }

    // Escala hasta LADO_MAXIMO (nunca agranda) y gira segun la marca de la camara.
    private fun ajuste(foto: Bitmap, orientacion: Int): Matrix {
        val escala = minOf(1f, LADO_MAXIMO.toFloat() / maxOf(foto.width, foto.height))
        return Matrix().apply {
            postScale(escala, escala)
            when (orientacion) {
                ExifInterface.ORIENTATION_ROTATE_90 -> postRotate(GIRO_90)
                ExifInterface.ORIENTATION_ROTATE_180 -> postRotate(GIRO_180)
                ExifInterface.ORIENTATION_ROTATE_270 -> postRotate(GIRO_270)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> postScale(1f, -1f)
            }
        }
    }

    // Baja la calidad de a poco hasta que quepa en PESO_MAXIMO.
    private fun comprimir(foto: Bitmap): ByteArray {
        var calidad = CALIDAD_INICIAL
        var jpeg: ByteArray
        do {
            jpeg = ByteArrayOutputStream().use { salida ->
                foto.compress(Bitmap.CompressFormat.JPEG, calidad, salida)
                salida.toByteArray()
            }
            calidad -= PASO_DE_CALIDAD
        } while (jpeg.size >= PESO_MAXIMO && calidad >= CALIDAD_MINIMA)
        return jpeg
    }
}
