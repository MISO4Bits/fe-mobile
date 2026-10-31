package com.solventa4bits.movil.avisos.datos

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.solventa4bits.movil.MainActivity
import com.solventa4bits.movil.R
import com.solventa4bits.movil.solicitud.dominio.EstadoDeSolicitud
import com.solventa4bits.movil.solicitud.dominio.Solicitud
import java.util.concurrent.TimeUnit

/**
 * Spike BITS-352: avisos locales. La app pregunta al BFF cada cierto
 * tiempo si cambio alguna solicitud y, si cambio, muestra un aviso local.
 * Todo queda en logcat con la etiqueta [ETIQUETA] para medir que pasa.
 */
object Avisos {
    const val ETIQUETA = "AvisosLocales"
    const val EXTRA_SOLICITUD = "solicitudId"
    private const val CANAL = "avisos"
    private const val TRABAJO = "revisar-solicitudes"

    // Se pide cada minuto a proposito, para ver si WorkManager lo respeta.
    private const val MINUTOS_PEDIDOS = 1L

    fun programar(contexto: Context) {
        crearCanal(contexto)
        val pedido = PeriodicWorkRequestBuilder<RevisorDeSolicitudes>(MINUTOS_PEDIDOS, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(contexto)
            .enqueueUniquePeriodicWork(TRABAJO, ExistingPeriodicWorkPolicy.KEEP, pedido)
        Log.i(ETIQUETA, "programado: se pidio revisar cada $MINUTOS_PEDIDOS min")
    }

    fun notificar(contexto: Context, solicitud: Solicitud) {
        if (!puedeNotificar(contexto)) {
            Log.i(ETIQUETA, "aviso no mostrado: el cliente no dio permiso de notificaciones")
            return
        }
        val abrir = Intent(contexto, MainActivity::class.java)
            .putExtra(EXTRA_SOLICITUD, solicitud.id)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val alTocar = PendingIntent.getActivity(
            contexto,
            solicitud.id.hashCode(),
            abrir,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        // AC-7: ni montos ni datos de la poliza, ni siquiera desbloqueado.
        val aviso = NotificationCompat.Builder(contexto, CANAL)
            .setSmallIcon(R.drawable.ic_reloj)
            .setContentTitle("Solventa")
            .setContentText(texto(solicitud.estado))
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setContentIntent(alTocar)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(contexto).notify(solicitud.id.hashCode(), aviso)
        Log.i(ETIQUETA, "aviso mostrado: estado=${solicitud.estado}")
    }

    private fun puedeNotificar(contexto: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(contexto, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    private fun texto(estado: EstadoDeSolicitud): String = when (estado) {
        EstadoDeSolicitud.EMITIDA -> "Tu seguro ya está activo. Toca para verlo."
        EstadoDeSolicitud.RECHAZADA -> "Ya hay respuesta sobre tu solicitud. Toca para verla."
        EstadoDeSolicitud.VENCIDA -> "Tu cotización venció. Toca para cotizar de nuevo."
        EstadoDeSolicitud.PENDIENTE -> "Hay novedades sobre tu solicitud. Toca para verla."
    }

    private fun crearCanal(contexto: Context) {
        val canal = NotificationChannel(CANAL, "Avisos de tu seguro", NotificationManager.IMPORTANCE_DEFAULT)
        contexto.getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
    }
}
