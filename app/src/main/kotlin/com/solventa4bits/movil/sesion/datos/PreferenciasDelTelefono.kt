package com.solventa4bits.movil.sesion.datos

import android.content.Context
import androidx.core.content.edit
import com.solventa4bits.movil.sesion.dominio.AlmacenDePreferencias
import com.solventa4bits.movil.sesion.dominio.PreferenciaDeHuella

/**
 * Preferencias en el archivo privado de la app. Van en claro: no son un
 * secreto, solo recuerdan lo que el cliente eligio. Se borran al desinstalar.
 *
 * Depende del dispositivo, asi que no entra en Kover: se verifica con
 * PreferenciasDelTelefonoTest, instrumentada.
 */
class PreferenciasDelTelefono(contexto: Context) : AlmacenDePreferencias {

    private val preferencias = contexto.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)

    override fun huella(): PreferenciaDeHuella {
        val guardada = preferencias.getString(CLAVE_HUELLA, null)
        // Un valor que la app ya no conoce se trata como si no hubiera decision.
        return PreferenciaDeHuella.entries.firstOrNull { it.name == guardada } ?: PreferenciaDeHuella.SIN_DECIDIR
    }

    override fun guardarHuella(preferencia: PreferenciaDeHuella) {
        preferencias.edit { putString(CLAVE_HUELLA, preferencia.name) }
    }

    override fun dispositivoId(): String? = preferencias.getString(CLAVE_DISPOSITIVO, null)

    override fun guardarDispositivoId(id: String?) {
        preferencias.edit { if (id == null) remove(CLAVE_DISPOSITIVO) else putString(CLAVE_DISPOSITIVO, id) }
    }

    override fun permisoDeAvisosPedido(): Boolean = preferencias.getBoolean(CLAVE_PERMISO_PEDIDO, false)

    override fun marcarPermisoDeAvisosPedido() {
        preferencias.edit { putBoolean(CLAVE_PERMISO_PEDIDO, true) }
    }

    private companion object {
        const val ARCHIVO = "preferencias"
        const val CLAVE_HUELLA = "huella"
        const val CLAVE_DISPOSITIVO = "dispositivo_id"
        const val CLAVE_PERMISO_PEDIDO = "permiso_avisos_pedido"
    }
}
