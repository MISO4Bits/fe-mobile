package com.solventa4bits.movil.avisos.datos

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.solventa4bits.movil.BuildConfig
import com.solventa4bits.movil.avisos.dominio.huella
import com.solventa4bits.movil.avisos.dominio.novedades
import com.solventa4bits.movil.sesion.datos.AdaptadorSesion
import com.solventa4bits.movil.sesion.datos.AlmacenDeSesionSeguro
import com.solventa4bits.movil.sesion.datos.BovedaKeystore
import com.solventa4bits.movil.sesion.datos.ConSesion
import com.solventa4bits.movil.sesion.datos.crearSesionApi
import com.solventa4bits.movil.solicitud.datos.AdaptadorSolicitudes
import com.solventa4bits.movil.solicitud.datos.crearSolicitudesApi
import com.solventa4bits.movil.solicitud.dominio.ResultadoLista
import com.solventa4bits.movil.solicitud.dominio.Solicitud
import com.solventa4bits.movil.traza.Traza
import kotlinx.serialization.json.Json

/**
 * Spike BITS-352: lo que corre en segundo plano. Arma las mismas piezas que
 * MainActivity, lista las solicitudes con la sesion guardada y avisa las que
 * cambiaron desde la ultima vez.
 */
class RevisorDeSolicitudes(
    contexto: Context,
    parametros: WorkerParameters,
) : CoroutineWorker(contexto, parametros) {

    override suspend fun doWork(): Result {
        val traza = Traza { origen, mensaje, causa -> Log.e(origen, mensaje, causa) }
        val almacen = AlmacenDeSesionSeguro(BovedaKeystore(applicationContext, traza), traza)
        val sesion = AdaptadorSesion(crearSesionApi(BuildConfig.URL_BASE_BFF), traza)
        val solicitudes = AdaptadorSolicitudes(
            crearSolicitudesApi(BuildConfig.URL_BASE_BFF),
            ConSesion(almacen, sesion),
            traza,
        )
        val foto = FotoDeSolicitudes(applicationContext)
        val refrescoAntes = almacen.leer()?.tokenDeRefresco
        Log.i(Avisos.ETIQUETA, "revision: empieza, intento=$runAttemptCount")

        when (val lista = solicitudes.listar()) {
            is ResultadoLista.Exito -> {
                val cambios = novedades(foto.leer(), lista.solicitudes)
                Log.i(
                    Avisos.ETIQUETA,
                    "revision: ${lista.solicitudes.size} solicitudes, ${cambios.size} novedades, " +
                        "ahora=${lista.solicitudes.map { "${it.id.take(8)}=${huella(it)}" }}",
                )
                cambios.forEach { Avisos.notificar(applicationContext, it) }
                foto.guardar(lista.solicitudes)
            }
            ResultadoLista.SesionVencida ->
                Log.i(Avisos.ETIQUETA, "revision: sin sesion guardada o vencida, no se puede preguntar al BFF")
            ResultadoLista.Fallo ->
                Log.i(Avisos.ETIQUETA, "revision: el BFF no respondio")
        }

        // El BFF entrega un refresco nuevo de 24 horas cada vez que se renueva.
        val refrescoDespues = almacen.leer()?.tokenDeRefresco
        if (refrescoAntes != null && refrescoDespues != null && refrescoAntes != refrescoDespues) {
            Log.i(Avisos.ETIQUETA, "revision: renovo la sesion en segundo plano, sin que el cliente abriera la app")
        }
        return Result.success()
    }
}

/** Lo ultimo que se vio de cada solicitud: id, estado y codigos de pendientes. */
private class FotoDeSolicitudes(contexto: Context) {
    private val preferencias = contexto.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)

    fun leer(): Map<String, String>? =
        preferencias.getString(CLAVE, null)?.let { Json.decodeFromString<Map<String, String>>(it) }

    fun guardar(solicitudes: List<Solicitud>) {
        val texto = Json.encodeToString(solicitudes.associate { it.id to huella(it) })
        preferencias.edit { putString(CLAVE, texto) }
    }

    private companion object {
        const val ARCHIVO = "avisos_locales"
        const val CLAVE = "ultima_foto"
    }
}
