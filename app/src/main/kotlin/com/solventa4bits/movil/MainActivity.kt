package com.solventa4bits.movil

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.solventa4bits.movil.cotizacion.datos.AdaptadorCreditos
import com.solventa4bits.movil.cotizacion.datos.crearCreditosApi
import com.solventa4bits.movil.cotizacion.dominio.ServicioDeCreditos
import com.solventa4bits.movil.cotizacion.ui.NuevaCotizacionViewModel
import com.solventa4bits.movil.cotizacion.ui.PantallaNuevaCotizacion
import com.solventa4bits.movil.inicio.ui.PantallaInicio
import com.solventa4bits.movil.sesion.datos.AdaptadorSesion
import com.solventa4bits.movil.sesion.datos.AlmacenDeSesionSeguro
import com.solventa4bits.movil.sesion.datos.BovedaKeystore
import com.solventa4bits.movil.sesion.datos.crearSesionApi
import com.solventa4bits.movil.sesion.dominio.AlmacenDeSesion
import com.solventa4bits.movil.sesion.dominio.ServicioDeSesion
import com.solventa4bits.movil.sesion.ui.LoginViewModel
import com.solventa4bits.movil.sesion.ui.PantallaLogin
import com.solventa4bits.movil.traza.Traza
import com.solventa4bits.movil.ui.tema.TemaSolventa

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Aqui se arman las piezas de la app y se conectan entre si.
        val traza = Traza { origen, mensaje, causa -> Log.e(origen, mensaje, causa) }
        val almacen = AlmacenDeSesionSeguro(BovedaKeystore(applicationContext, traza), traza)
        val sesion = AdaptadorSesion(crearSesionApi(BuildConfig.URL_BASE_BFF), traza)
        val creditos = AdaptadorCreditos(crearCreditosApi(BuildConfig.URL_BASE_BFF), almacen, sesion, traza)

        setContent {
            TemaSolventa { AppSolventa(sesion, almacen, creditos) }
        }
    }
}

/**
 * Decide que pantalla se ve: sin sesion, el inicio de sesion; con sesion, el
 * inicio o la nueva cotizacion.
 */
@Composable
fun AppSolventa(sesion: ServicioDeSesion, almacen: AlmacenDeSesion, creditos: ServicioDeCreditos) {
    val login = viewModel { LoginViewModel(sesion, almacen) }
    var cotizando by rememberSaveable { mutableStateOf(false) }
    val cerrarSesion = {
        cotizando = false
        login.cerrarSesion()
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { relleno ->
        val conRelleno = Modifier.padding(relleno)
        when {
            !login.estado.sesionIniciada -> PantallaLogin(
                estado = login.estado,
                alCambiarCorreo = login::cambiarCorreo,
                alCambiarContrasena = login::cambiarContrasena,
                alIniciarSesion = login::iniciarSesion,
                modifier = conRelleno,
            )

            cotizando -> {
                val cotizacion = viewModel { NuevaCotizacionViewModel(creditos) }
                // Cada vez que se entra a la pantalla se consulta de nuevo.
                LaunchedEffect(Unit) { cotizacion.empezar() }
                // La sesion ya no se pudo refrescar: toca iniciar sesion de nuevo.
                LaunchedEffect(cotizacion.estado.sesionVencida) {
                    if (cotizacion.estado.sesionVencida) cerrarSesion()
                }
                BackHandler { cotizando = false }
                PantallaNuevaCotizacion(
                    estado = cotizacion.estado,
                    acciones = cotizacion,
                    alVolver = { cotizando = false },
                    alCerrarSesion = cerrarSesion,
                    modifier = conRelleno,
                )
            }

            else -> PantallaInicio(
                alCotizar = { cotizando = true },
                alCerrarSesion = cerrarSesion,
                modifier = conRelleno,
            )
        }
    }
}
