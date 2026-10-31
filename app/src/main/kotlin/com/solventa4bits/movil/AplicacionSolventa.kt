package com.solventa4bits.movil

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import com.solventa4bits.movil.avisos.datos.AdaptadorDispositivos
import com.solventa4bits.movil.avisos.datos.FuenteDeTokenFirebase
import com.solventa4bits.movil.avisos.datos.PermisoDelTelefono
import com.solventa4bits.movil.avisos.datos.crearDispositivosApi
import com.solventa4bits.movil.avisos.dominio.AvisosDelCliente
import com.solventa4bits.movil.avisos.dominio.RegistroDeDispositivo
import com.solventa4bits.movil.sesion.datos.AdaptadorSesion
import com.solventa4bits.movil.sesion.datos.AlmacenDeSesionSeguro
import com.solventa4bits.movil.sesion.datos.BovedaKeystore
import com.solventa4bits.movil.sesion.datos.ConSesion
import com.solventa4bits.movil.sesion.datos.PreferenciasDelTelefono
import com.solventa4bits.movil.sesion.datos.crearSesionApi
import com.solventa4bits.movil.traza.Traza
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Crea el canal de los avisos al arrancar y guarda las piezas que el servicio de mensajeria
 * necesita: Android lo levanta sin pasar por [MainActivity]. Depende de Android: no entra en Kover.
 */
class AplicacionSolventa : Application() {

    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val almacen by lazy {
        val traza = Traza { origen, mensaje, causa -> Log.e(origen, mensaje, causa) }
        AlmacenDeSesionSeguro(BovedaKeystore(applicationContext, traza), traza)
    }

    /** Los avisos de esta instalacion. [MainActivity] los usa al iniciar y cerrar la sesion. */
    val avisos: AvisosDelCliente by lazy {
        val traza = Traza { origen, mensaje, causa -> Log.e(origen, mensaje, causa) }
        val preferencias = PreferenciasDelTelefono(applicationContext)
        val sesion = AdaptadorSesion(crearSesionApi(BuildConfig.URL_BASE_BFF), traza)
        val dispositivos = AdaptadorDispositivos(
            crearDispositivosApi(BuildConfig.URL_BASE_BFF),
            ConSesion(almacen, sesion),
            traza,
        )
        AvisosDelCliente(
            registro = RegistroDeDispositivo(
                dispositivos,
                FuenteDeTokenFirebase(traza),
                preferencias,
                BuildConfig.VERSION_NAME,
            ),
            preferencias = preferencias,
            permiso = PermisoDelTelefono(applicationContext),
            haySesion = { almacen.leer() != null },
        )
    }

    override fun onCreate() {
        super.onCreate()
        crearCanalDeAvisos()
    }

    /** Firebase rota el token de vez en cuando: si hay sesion, el servidor recibe el nuevo. */
    fun alCambiarElToken(token: String) {
        alcance.launch { avisos.alCambiarElToken(token) }
    }

    // Con minSdk 26 el canal es obligatorio. Crearlo de nuevo no cambia el que ya existe.
    private fun crearCanalDeAvisos() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val canal = NotificationChannel(
            getString(R.string.canal_avisos_id),
            getString(R.string.canal_avisos_nombre),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply { description = getString(R.string.canal_avisos_descripcion) }
        getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
    }
}
