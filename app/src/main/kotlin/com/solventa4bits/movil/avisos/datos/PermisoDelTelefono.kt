package com.solventa4bits.movil.avisos.datos

import android.content.Context
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import com.solventa4bits.movil.avisos.dominio.PermisoDeNotificaciones

/**
 * Pregunta al sistema si las notificaciones de la app estan habilitadas. Depende del dispositivo:
 * no entra en Kover.
 */
class PermisoDelTelefono(private val contexto: Context) : PermisoDeNotificaciones {

    // En Android 13 o superior esto refleja el permiso POST_NOTIFICATIONS; antes, el ajuste del usuario.
    override fun concedido(): Boolean = NotificationManagerCompat.from(contexto).areNotificationsEnabled()

    override fun sePuedePedir(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
}
