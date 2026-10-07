package com.solventa4bits.movil.sesion.datos

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.core.content.edit
import com.solventa4bits.movil.traza.Traza
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Boveda sobre el Android Keystore.
 *
 * La llave AES se crea dentro del Keystore y nunca sale de el: la app solo le
 * pide que cifre o descifre. Lo cifrado se guarda en preferencias privadas de
 * la app. Aunque alguien copie ese archivo, sin la llave no puede leerlo.
 *
 * Depende del dispositivo, asi que no entra en las pruebas unitarias ni en
 * Kover: se verifica con BovedaKeystoreTest, instrumentada.
 */
class BovedaKeystore(
    contexto: Context,
    private val traza: Traza,
) : Boveda {

    private val preferencias = contexto.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)

    override fun guardar(clave: String, valor: String) {
        val cifrador = Cipher.getInstance(TRANSFORMACION).apply { init(Cipher.ENCRYPT_MODE, llave()) }
        val cifrado = cifrador.doFinal(valor.toByteArray())
        // El vector de inicializacion no es secreto, pero hace falta para
        // descifrar: se guarda pegado delante del texto cifrado.
        val empaquetado = Base64.encodeToString(cifrador.iv + cifrado, Base64.NO_WRAP)
        preferencias.edit { putString(clave, empaquetado) }
    }

    override fun leer(clave: String): String? {
        val empaquetado = preferencias.getString(clave, null) ?: return null
        return try {
            val bytes = Base64.decode(empaquetado, Base64.NO_WRAP)
            val cifrador = Cipher.getInstance(TRANSFORMACION).apply {
                init(Cipher.DECRYPT_MODE, llave(), GCMParameterSpec(BITS_DE_ETIQUETA, bytes, 0, BYTES_DE_IV))
            }
            String(cifrador.doFinal(bytes, BYTES_DE_IV, bytes.size - BYTES_DE_IV))
        } catch (e: GeneralSecurityException) {
            traza.error(ORIGEN, "No se pudo descifrar un valor guardado", e)
            null
        } catch (e: IllegalArgumentException) {
            traza.error(ORIGEN, "Un valor guardado no tiene el formato esperado", e)
            null
        }
    }

    override fun borrar(clave: String) {
        preferencias.edit { remove(clave) }
    }

    private fun llave(): SecretKey {
        val keystore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        val existente = keystore.getKey(ALIAS, null) as? SecretKey
        return existente ?: crearLlave()
    }

    private fun crearLlave(): SecretKey {
        val especificacion = KeyGenParameterSpec.Builder(
            ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(BITS_DE_LLAVE)
            .build()
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
            .apply { init(especificacion) }
            .generateKey()
    }

    private companion object {
        const val ORIGEN = "BovedaKeystore"
        const val ARCHIVO = "boveda"
        const val KEYSTORE = "AndroidKeyStore"
        const val ALIAS = "solventa_boveda"
        const val TRANSFORMACION = "AES/GCM/NoPadding"
        const val BITS_DE_LLAVE = 256
        const val BITS_DE_ETIQUETA = 128
        const val BYTES_DE_IV = 12
    }
}
