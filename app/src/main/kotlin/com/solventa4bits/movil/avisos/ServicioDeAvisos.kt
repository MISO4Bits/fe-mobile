package com.solventa4bits.movil.avisos

import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.solventa4bits.movil.AplicacionSolventa
import com.solventa4bits.movil.MainActivity
import com.solventa4bits.movil.R
import com.solventa4bits.movil.avisos.dominio.DestinoDeAviso

/**
 * Recibe de Firebase los avisos con la app abierta y los cambios de token. Con la app cerrada o
 * en segundo plano, el sistema muestra el aviso solo; con ella abierta Firebase no lo hace, y
 * aqui se muestra con el mismo canal y el mismo destino. Depende de Android: no entra en Kover.
 */
class ServicioDeAvisos : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        (application as AplicacionSolventa).alCambiarElToken(token)
    }

    override fun onMessageReceived(mensaje: RemoteMessage) {
        val aviso = mensaje.notification ?: return
        val manager = NotificationManagerCompat.from(this)
        if (!manager.areNotificationsEnabled()) return
        val abrir = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(DestinoDeAviso.CLAVE_DESTINO, mensaje.data[DestinoDeAviso.CLAVE_DESTINO])
            .putExtra(DestinoDeAviso.CLAVE_ID, mensaje.data[DestinoDeAviso.CLAVE_ID])
        val notificacion = NotificationCompat.Builder(this, getString(R.string.canal_avisos_id))
            .setSmallIcon(R.drawable.ic_escudo)
            .setContentTitle(aviso.title)
            .setContentText(aviso.body)
            .setContentIntent(PendingIntent.getActivity(this, 0, abrir, BANDERAS_DE_LA_INTENCION))
            .setAutoCancel(true)
            .build()
        // La etiqueta del aviso reemplaza al que Firebase ya haya mostrado en lugar de duplicarlo.
        @Suppress("MissingPermission") // areNotificationsEnabled() ya se comprobo arriba.
        manager.notify(aviso.tag ?: ETIQUETA, ID_NOTIFICACION, notificacion)
    }

    private companion object {
        const val ETIQUETA = "avisos"
        const val BANDERAS_DE_LA_INTENCION = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        const val ID_NOTIFICACION = 1
    }
}
